package org.hl7.fhir.igtools.publisher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Monitors files and directories for changes.
 * Automatically uses the best implementation for the current OS:
 * - macOS: Polling-based (fast, reliable)
 * - Other OS: WatchService-based (efficient)
 */
public class FileChangeMonitor implements Closeable {
  private final FileChangeMonitorImpl impl;

  public static class FileChangeEvent {
    private final Path path;
    private final ChangeType changeType;
    private final long timestamp;

    public enum ChangeType {
      CREATED, MODIFIED, DELETED
    }

    public FileChangeEvent(Path path, ChangeType changeType) {
      this.path = path;
      this.changeType = changeType;
      this.timestamp = System.currentTimeMillis();
    }

    public Path getPath() { return path; }
    public ChangeType getChangeType() { return changeType; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
      return String.format("[%s] %s: %s", changeType, path, new Date(timestamp));
    }
  }

  public static class MonitoredPath {
    private final Path path;
    private final boolean recursive;

    private MonitoredPath(Path path, boolean recursive) {
      this.path = path;
      this.recursive = recursive;
    }

    public static MonitoredPath recursive(Path path) {
      return new MonitoredPath(path, true);
    }

    public static MonitoredPath nonRecursive(Path path) {
      return new MonitoredPath(path, false);
    }

    public static MonitoredPath file(Path path) {
      return new MonitoredPath(path, false);
    }
  }

  public FileChangeMonitor(List<MonitoredPath> pathsToMonitor) throws IOException {
    this(pathsToMonitor, isMacOS());
  }

  // polling can be chosen on any OS, for testing
  FileChangeMonitor(List<MonitoredPath> pathsToMonitor, boolean polling) throws IOException {
    if (polling) {
      System.out.println("Detected macOS - using polling-based file monitor");
      impl = new PollingFileChangeMonitorImpl(pathsToMonitor, 200);
    } else {
      System.out.println("Using WatchService-based file monitor");
      impl = new WatchServiceFileChangeMonitorImpl(pathsToMonitor);
    }
  }

  public FileChangeMonitor(Path... paths) throws IOException {
    this(Arrays.stream(paths)
            .map(MonitoredPath::recursive)
            .collect(Collectors.toList()));
  }

  private static boolean isMacOS() {
    String os = System.getProperty("os.name").toLowerCase();
    return os.contains("mac") || os.contains("darwin");
  }

  public void startMonitoring() {
    impl.startMonitoring(Collections.emptyList());
  }

  /**
   * Start monitoring after a build. The changes made while not monitoring are reported, except those to
   * buildOutputs: the files and folders that the build itself wrote to (e.g. fsh-generated, when SUSHI ran)
   */
  public void startMonitoring(Collection<Path> buildOutputs) {
    impl.startMonitoring(buildOutputs);
  }

  /**
   * the modified time and size of each file in a folder, so that changedFiles can tell what a step of the build wrote
   */
  static Map<Path, String> fileStates(Path folder) throws IOException {
    Map<Path, String> states = new HashMap<>();
    if (Files.isDirectory(folder)) {
      Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          states.put(file.toAbsolutePath().normalize(), attrs.lastModifiedTime() + "/" + attrs.size());
          return FileVisitResult.CONTINUE;
        }
      });
    }
    return states;
  }

  static Set<Path> changedFiles(Map<Path, String> before, Map<Path, String> after) {
    Set<Path> changed = new HashSet<>();
    for (Map.Entry<Path, String> e : after.entrySet()) {
      if (!e.getValue().equals(before.get(e.getKey()))) {
        changed.add(e.getKey());
      }
    }
    for (Path p : before.keySet()) {
      if (!after.containsKey(p)) {
        changed.add(p);
      }
    }
    return changed;
  }

  private static List<Path> normalised(Collection<Path> paths) {
    return paths.stream().map(p -> p.toAbsolutePath().normalize()).collect(Collectors.toList());
  }

  // the build output that path is, or is in; null if none
  private static Path outputFor(Path path, List<Path> buildOutputs) {
    Path p = path.toAbsolutePath().normalize();
    for (Path output : buildOutputs) {
      if (p.startsWith(output)) {
        return output;
      }
    }
    return null;
  }

  private static Path folderOf(Path output) {
    return Files.isDirectory(output) || output.getParent() == null ? output : output.getParent();
  }

  private static void logIgnored(int count, Set<Path> where) {
    if (count > 0) {
      System.out.println("Ignored " + count + " changes made by the build, in " + where.stream().sorted().map(Path::toString).collect(Collectors.joining(", ")));
    }
  }

  public boolean hasChanges() {
    return impl.hasChanges();
  }

  public List<FileChangeEvent> getChanges() {
    return impl.getChanges();
  }

  public List<Path> getChangedFiles() {
    return impl.getChangedFiles();
  }

  public void printChanges() {
    impl.printChanges();
  }

  public void printChangesSummary() {
    impl.printChangesSummary();
  }

  public FileChangeEvent waitForChange(long timeout, TimeUnit unit) throws InterruptedException {
    return impl.waitForChange(timeout, unit);
  }

  public FileChangeEvent waitForChange() throws InterruptedException {
    return impl.waitForChange();
  }

  public void clearChanges() {
    impl.clearChanges();
  }

  public void stopMonitoring() {
    impl.stopMonitoring();
  }

  @Override
  public void close() throws IOException {
    impl.close();
  }

  // Private interface for implementations
  private interface FileChangeMonitorImpl {
    void startMonitoring(Collection<Path> buildOutputs);
    boolean hasChanges();
    List<FileChangeEvent> getChanges();
    List<Path> getChangedFiles();
    void printChanges();
    void printChangesSummary();
    FileChangeEvent waitForChange(long timeout, TimeUnit unit) throws InterruptedException;
    FileChangeEvent waitForChange() throws InterruptedException;
    void clearChanges();
    void stopMonitoring();
    void close() throws IOException;
  }

  // WatchService implementation (Linux, Windows)
  private static class WatchServiceFileChangeMonitorImpl implements FileChangeMonitorImpl {
    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeys;
    // directories that are only watched for some of their files (MonitoredPath.file): directory -> those files
    private final Map<Path, Set<Path>> monitoredFiles;
    // directories watched with their sub-directories, so that a new sub-directory is watched too
    private final Set<Path> recursiveDirs = ConcurrentHashMap.newKeySet();
    // the MonitoredPath.recursive folders (absolute)
    private final Set<Path> recursiveRoots = ConcurrentHashMap.newKeySet();
    // recursive folders that don't exist (yet, or any more), watched for in their parent (e.g. fsh-generated before SUSHI runs)
    private final Set<Path> awaitedDirs = ConcurrentHashMap.newKeySet();
    private ExecutorService executor;
    private final BlockingQueue<FileChangeEvent> changeQueue;
    private volatile boolean monitoring;

    public WatchServiceFileChangeMonitorImpl(List<MonitoredPath> pathsToMonitor) throws IOException {
      this.watchService = FileSystems.getDefault().newWatchService();
      this.watchKeys = new ConcurrentHashMap<>();
      this.monitoredFiles = new ConcurrentHashMap<>();
      this.executor = Executors.newSingleThreadExecutor();
      this.changeQueue = new LinkedBlockingQueue<>();
      this.monitoring = false;

      for (MonitoredPath monitoredPath : pathsToMonitor) {
        Path path = monitoredPath.path;

        if (monitoredPath.recursive) {
          recursiveRoots.add(path.toAbsolutePath().normalize());
        }
        if (Files.isDirectory(path)) {
          if (monitoredPath.recursive) {
            registerRecursive(path);
          } else {
            registerDirectory(path);
          }
        } else if (Files.exists(path)) {
          monitoredFiles.computeIfAbsent(path.getParent(), d -> ConcurrentHashMap.newKeySet()).add(path.toAbsolutePath());
          registerDirectory(path.getParent());
        } else if (monitoredPath.recursive) {
          awaitDirectory(path);
        }
      }
    }

    private void registerRecursive(Path directory) throws IOException {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          registerDirectory(dir);
          recursiveDirs.add(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }

    // watch the parent folder for the folder to be made
    private void awaitDirectory(Path directory) throws IOException {
      Path parent = directory.getParent();
      if (parent != null && Files.isDirectory(parent)) {
        if (!recursiveDirs.contains(parent)) {
          monitoredFiles.computeIfAbsent(parent, d -> ConcurrentHashMap.newKeySet());
        }
        awaitedDirs.add(directory.toAbsolutePath().normalize());
        registerDirectory(parent);
      }
    }

    // a folder made after monitoring started (e.g. by git, or a new input/images/x): watch it, and report what's in it already
    private void watchNewDirectory(Path directory, List<Path> buildOutputs, Set<Path> ignoredIn, int[] ignored) {
      try {
        registerRecursive(directory);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            report(file.toAbsolutePath(), FileChangeEvent.ChangeType.CREATED, buildOutputs, ignoredIn, ignored);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        System.out.println("Unable to watch " + directory + ": " + e.getMessage());
      }
    }

    private void report(Path path, FileChangeEvent.ChangeType changeType, List<Path> buildOutputs, Set<Path> ignoredIn, int[] ignored) {
      Path output = outputFor(path, buildOutputs);
      if (output != null) {
        ignored[0]++;
        ignoredIn.add(folderOf(output));
      } else {
        changeQueue.offer(new FileChangeEvent(path, changeType));
      }
    }

    private void registerDirectory(Path directory) throws IOException {
      WatchKey key = directory.register(
              watchService,
              StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_MODIFY,
              StandardWatchEventKinds.ENTRY_DELETE
      );
      watchKeys.put(key, directory);
    }

    @Override
    public void startMonitoring(Collection<Path> buildOutputs) {
      if (monitoring) return;

      // If executor was shutdown, create a new one
      if (executor.isShutdown() || executor.isTerminated()) {
        executor = Executors.newSingleThreadExecutor();
      }
      monitoring = true;
      try {
        reportPendingEvents(buildOutputs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      executor.submit(() -> {
        try {
          while (monitoring) {
            WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
            if (key != null) {
              processEvents(key, Collections.emptyList(), new HashSet<>(), new int[1]);
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }

    /**
     * The watch service queues events while nothing is monitoring. Those in the folders the build wrote to are the
     * build's own (e.g. SUSHI's output in fsh-generated): reporting them would start another build straight away.
     * The others are edits made while the build ran, and need another build
     */
    private void reportPendingEvents(Collection<Path> buildOutputs) throws InterruptedException {
      List<Path> outputs = normalised(buildOutputs);
      Set<Path> ignoredIn = new HashSet<>();
      int[] ignored = new int[1];
      WatchKey key;
      // a short wait, for the events of the build's last writes to arrive
      while ((key = watchService.poll(50, TimeUnit.MILLISECONDS)) != null) {
        processEvents(key, outputs, ignoredIn, ignored);
      }
      logIgnored(ignored[0], ignoredIn);
    }

    private void processEvents(WatchKey key, List<Path> buildOutputs, Set<Path> ignoredIn, int[] ignored) {
      Path directory = watchKeys.get(key);
      if (directory != null) {
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          Path changedPath;
          FileChangeEvent.ChangeType changeType;
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            // events were lost, so something in the directory changed
            changedPath = directory.toAbsolutePath();
            changeType = FileChangeEvent.ChangeType.MODIFIED;
          } else {
            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;
            Path filename = ev.context();
            changedPath = directory.resolve(filename).toAbsolutePath();

            if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changedPath, LinkOption.NOFOLLOW_LINKS)
                && (recursiveDirs.contains(directory) || awaitedDirs.remove(changedPath.normalize()))) {
              watchNewDirectory(changedPath, buildOutputs, ignoredIn, ignored);
            }

            Set<Path> files = monitoredFiles.get(directory);
            if (files != null && !files.contains(changedPath)) {
              continue;
            }

            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
              changeType = FileChangeEvent.ChangeType.CREATED;
            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
              changeType = FileChangeEvent.ChangeType.MODIFIED;
            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
              changeType = FileChangeEvent.ChangeType.DELETED;
            } else {
              continue;
            }
          }
          report(changedPath, changeType, buildOutputs, ignoredIn, ignored);
        }
      }

      boolean valid = key.reset();
      if (!valid) {
        watchKeys.remove(key);
        recursiveDirs.remove(directory);
        if (directory != null) {
          System.out.println("No longer watching " + directory + ": it was deleted or moved");
          if (recursiveRoots.contains(directory.toAbsolutePath().normalize())) {
            // watched again when it's made again
            try {
              awaitDirectory(directory);
            } catch (IOException e) {
              System.out.println("Unable to watch for " + directory + ": " + e.getMessage());
            }
          }
        }
      }
    }

    @Override
    public boolean hasChanges() {
      return !changeQueue.isEmpty();
    }

    @Override
    public List<FileChangeEvent> getChanges() {
      List<FileChangeEvent> changes = new ArrayList<>();
      changeQueue.drainTo(changes);
      return changes;
    }

    @Override
    public List<Path> getChangedFiles() {
      return getChanges().stream()
              .map(FileChangeEvent::getPath)
              .distinct()
              .collect(Collectors.toList());
    }

    @Override
    public void printChanges() {
      List<FileChangeEvent> changes = getChanges();
      if (changes.isEmpty()) {
        System.out.println("No changes detected.");
        return;
      }

      System.out.println("=== File Changes Detected ===");
      for (FileChangeEvent change : changes) {
        System.out.println(change);
      }
      System.out.println("Total changes: " + changes.size());
    }

    @Override
    public void printChangesSummary() {
      List<FileChangeEvent> changes = getChanges();
      if (changes.isEmpty()) {
        System.out.println("No changes detected.");
        return;
      }

      Map<FileChangeEvent.ChangeType, List<FileChangeEvent>> grouped = changes.stream()
              .collect(Collectors.groupingBy(FileChangeEvent::getChangeType));

      System.out.println("=== File Changes Summary ===");

      if (grouped.containsKey(FileChangeEvent.ChangeType.CREATED)) {
        System.out.println("\nCreated (" + grouped.get(FileChangeEvent.ChangeType.CREATED).size() + "):");
        grouped.get(FileChangeEvent.ChangeType.CREATED).forEach(e ->
                System.out.println("  + " + e.getPath()));
      }

      if (grouped.containsKey(FileChangeEvent.ChangeType.MODIFIED)) {
        System.out.println("\nModified (" + grouped.get(FileChangeEvent.ChangeType.MODIFIED).size() + "):");
        grouped.get(FileChangeEvent.ChangeType.MODIFIED).forEach(e ->
                System.out.println("  ~ " + e.getPath()));
      }

      if (grouped.containsKey(FileChangeEvent.ChangeType.DELETED)) {
        System.out.println("\nDeleted (" + grouped.get(FileChangeEvent.ChangeType.DELETED).size() + "):");
        grouped.get(FileChangeEvent.ChangeType.DELETED).forEach(e ->
                System.out.println("  - " + e.getPath()));
      }

      System.out.println("\nTotal changes: " + changes.size());
    }

    @Override
    public FileChangeEvent waitForChange(long timeout, TimeUnit unit) throws InterruptedException {
      return changeQueue.poll(timeout, unit);
    }

    @Override
    public FileChangeEvent waitForChange() throws InterruptedException {
      return changeQueue.take();
    }

    @Override
    public void clearChanges() {
      changeQueue.clear();
    }

    // the watch service is left open, so that monitoring can be started again
    @Override
    public void stopMonitoring() {
      monitoring = false;
      if (executor != null && !executor.isShutdown()) {
        executor.shutdown();
        try {
          if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        } catch (InterruptedException e) {
          executor.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }

    @Override
    public void close() throws IOException {
      stopMonitoring();
      watchService.close();
    }
  }

  // Polling implementation (macOS)
  private static class PollingFileChangeMonitorImpl implements FileChangeMonitorImpl {
    private final Map<Path, FileSnapshot> fileSnapshots;
    private final List<MonitoredPath> monitoredPaths;
    private ScheduledExecutorService scheduler;
    private final BlockingQueue<FileChangeEvent> changeQueue;
    private volatile boolean monitoring;
    private final long pollIntervalMs;

    private static class FileSnapshot {
      FileTime lastModified;
      long size;
      boolean exists;

      FileSnapshot(Path path) {
        if (Files.exists(path)) {
          try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            this.lastModified = attrs.lastModifiedTime();
            this.size = attrs.size();
            this.exists = true;
          } catch (IOException e) {
            this.lastModified = null;
            this.size = 0;
            this.exists = false;
          }
        } else {
          this.lastModified = null;
          this.size = 0;
          this.exists = false;
        }
      }

      boolean hasChanged(FileSnapshot other) {
        if (this.exists != other.exists) return true;
        if (!this.exists) return false;
        return !this.lastModified.equals(other.lastModified) || this.size != other.size;
      }
    }

    public PollingFileChangeMonitorImpl(List<MonitoredPath> pathsToMonitor, long pollIntervalMs) {
      this.monitoredPaths = pathsToMonitor;
      this.pollIntervalMs = pollIntervalMs;
      this.fileSnapshots = new ConcurrentHashMap<>();
      this.changeQueue = new LinkedBlockingQueue<>();
      this.scheduler = Executors.newScheduledThreadPool(1);
      this.monitoring = false;

      scanAndSnapshot();
    }

    private void scanAndSnapshot() {
      Map<Path, FileSnapshot> newSnapshots = new ConcurrentHashMap<>();

      for (MonitoredPath monitoredPath : monitoredPaths) {
        try {
          if (Files.isDirectory(monitoredPath.path)) {
            if (monitoredPath.recursive) {
              scanDirectoryRecursive(monitoredPath.path, newSnapshots);
            } else {
              scanDirectoryNonRecursive(monitoredPath.path, newSnapshots);
            }
          } else {
            newSnapshots.put(monitoredPath.path, new FileSnapshot(monitoredPath.path));
          }
        } catch (IOException e) {
          System.err.println("Error scanning " + monitoredPath.path + ": " + e.getMessage());
        }
      }

      fileSnapshots.clear();
      fileSnapshots.putAll(newSnapshots);
    }

    private void scanDirectoryRecursive(Path directory, Map<Path, FileSnapshot> snapshots) throws IOException {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          snapshots.put(file, new FileSnapshot(file));
          return FileVisitResult.CONTINUE;
        }
      });
    }

    private void scanDirectoryNonRecursive(Path directory, Map<Path, FileSnapshot> snapshots) throws IOException {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
        for (Path file : stream) {
          if (Files.isRegularFile(file)) {
            snapshots.put(file, new FileSnapshot(file));
          }
        }
      }
    }

    // compares with the files as they were when it last looked, so the changes made while not monitoring are reported,
    // except those to the build's outputs: those files are taken as they are now
    @Override
    public void startMonitoring(Collection<Path> buildOutputs) {
      if (monitoring) return;
      acceptBuildOutputs(normalised(buildOutputs));

      // If scheduler was shutdown, create a new one
      if (scheduler.isShutdown() || scheduler.isTerminated()) {
        scheduler = Executors.newScheduledThreadPool(1);
      }

      monitoring = true;

      scheduler.scheduleAtFixedRate(() -> {
        try {
          checkForChanges();
        } catch (Exception e) {
          System.err.println("Error checking for changes: " + e.getMessage());
        }
      }, pollIntervalMs, pollIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void acceptBuildOutputs(List<Path> buildOutputs) {
      if (buildOutputs.isEmpty()) {
        return;
      }
      Map<Path, FileSnapshot> currentSnapshots = scan();
      Set<Path> ignoredIn = new HashSet<>();
      int ignored = 0;
      for (Path path : new ArrayList<>(fileSnapshots.keySet())) {
        Path output = outputFor(path, buildOutputs);
        if (output != null && !currentSnapshots.containsKey(path)) {
          fileSnapshots.remove(path);
          ignored++;
          ignoredIn.add(folderOf(output));
        }
      }
      for (Map.Entry<Path, FileSnapshot> entry : currentSnapshots.entrySet()) {
        Path output = outputFor(entry.getKey(), buildOutputs);
        if (output != null) {
          FileSnapshot old = fileSnapshots.put(entry.getKey(), entry.getValue());
          if (old == null || old.hasChanged(entry.getValue())) {
            ignored++;
            ignoredIn.add(folderOf(output));
          }
        }
      }
      logIgnored(ignored, ignoredIn);
    }

    private Map<Path, FileSnapshot> scan() {
      Map<Path, FileSnapshot> currentSnapshots = new ConcurrentHashMap<>();
      for (MonitoredPath monitoredPath : monitoredPaths) {
        try {
          if (Files.isDirectory(monitoredPath.path)) {
            if (monitoredPath.recursive) {
              scanDirectoryRecursive(monitoredPath.path, currentSnapshots);
            } else {
              scanDirectoryNonRecursive(monitoredPath.path, currentSnapshots);
            }
          } else {
            currentSnapshots.put(monitoredPath.path, new FileSnapshot(monitoredPath.path));
          }
        } catch (IOException e) {
          // Directory might have been deleted, continue
        }
      }
      return currentSnapshots;
    }

    private void checkForChanges() {
      Map<Path, FileSnapshot> currentSnapshots = scan();

      for (Map.Entry<Path, FileSnapshot> entry : fileSnapshots.entrySet()) {
        Path path = entry.getKey();
        FileSnapshot oldSnapshot = entry.getValue();
        FileSnapshot newSnapshot = currentSnapshots.get(path);

        if (newSnapshot == null) {
          changeQueue.offer(new FileChangeEvent(path, FileChangeEvent.ChangeType.DELETED));
        } else if (oldSnapshot.hasChanged(newSnapshot)) {
          changeQueue.offer(new FileChangeEvent(path, FileChangeEvent.ChangeType.MODIFIED));
        }
      }

      for (Map.Entry<Path, FileSnapshot> entry : currentSnapshots.entrySet()) {
        Path path = entry.getKey();
        if (!fileSnapshots.containsKey(path) && entry.getValue().exists) {
          changeQueue.offer(new FileChangeEvent(path, FileChangeEvent.ChangeType.CREATED));
        }
      }

      fileSnapshots.clear();
      fileSnapshots.putAll(currentSnapshots);
    }

    @Override
    public boolean hasChanges() {
      return !changeQueue.isEmpty();
    }

    @Override
    public List<FileChangeEvent> getChanges() {
      List<FileChangeEvent> changes = new ArrayList<>();
      changeQueue.drainTo(changes);
      return changes;
    }

    @Override
    public List<Path> getChangedFiles() {
      return getChanges().stream()
              .map(FileChangeEvent::getPath)
              .distinct()
              .collect(Collectors.toList());
    }

    @Override
    public void printChanges() {
      List<FileChangeEvent> changes = getChanges();
      if (changes.isEmpty()) {
        System.out.println("No changes detected.");
        return;
      }

      System.out.println("=== File Changes Detected ===");
      for (FileChangeEvent change : changes) {
        System.out.println(change);
      }
      System.out.println("Total changes: " + changes.size());
    }

    @Override
    public void printChangesSummary() {
      List<FileChangeEvent> changes = getChanges();
      if (changes.isEmpty()) {
        System.out.println("No changes detected.");
        return;
      }

      Map<FileChangeEvent.ChangeType, List<FileChangeEvent>> grouped = changes.stream()
              .collect(Collectors.groupingBy(FileChangeEvent::getChangeType));

      System.out.println("=== File Changes Summary ===");

      if (grouped.containsKey(FileChangeEvent.ChangeType.CREATED)) {
        System.out.println("\nCreated (" + grouped.get(FileChangeEvent.ChangeType.CREATED).size() + "):");
        grouped.get(FileChangeEvent.ChangeType.CREATED).forEach(e ->
                System.out.println("  + " + e.getPath()));
      }

      if (grouped.containsKey(FileChangeEvent.ChangeType.MODIFIED)) {
        System.out.println("\nModified (" + grouped.get(FileChangeEvent.ChangeType.MODIFIED).size() + "):");
        grouped.get(FileChangeEvent.ChangeType.MODIFIED).forEach(e ->
                System.out.println("  ~ " + e.getPath()));
      }

      if (grouped.containsKey(FileChangeEvent.ChangeType.DELETED)) {
        System.out.println("\nDeleted (" + grouped.get(FileChangeEvent.ChangeType.DELETED).size() + "):");
        grouped.get(FileChangeEvent.ChangeType.DELETED).forEach(e ->
                System.out.println("  - " + e.getPath()));
      }

      System.out.println("\nTotal changes: " + changes.size());
    }

    @Override
    public FileChangeEvent waitForChange(long timeout, TimeUnit unit) throws InterruptedException {
      return changeQueue.poll(timeout, unit);
    }

    @Override
    public FileChangeEvent waitForChange() throws InterruptedException {
      return changeQueue.take();
    }

    @Override
    public void clearChanges() {
      changeQueue.clear();
    }

    @Override
    public void stopMonitoring() {
      monitoring = false;
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdown();
        try {
          if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
          }
        } catch (InterruptedException e) {
          scheduler.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }

    @Override
    public void close() {
      stopMonitoring();
    }
  }

  // Example usage
  public static void main(String[] args) throws IOException, InterruptedException {
    List<MonitoredPath> paths = Arrays.asList(
            MonitoredPath.recursive(Paths.get("src")),
            MonitoredPath.file(Paths.get("pom.xml"))
    );

    FileChangeMonitor monitor = new FileChangeMonitor(paths);
    monitor.startMonitoring();

    System.out.println("Monitoring for changes...");

    while (true) {
      if (monitor.hasChanges()) {
        monitor.printChanges();
      }
      Thread.sleep(100);
    }
  }
}
