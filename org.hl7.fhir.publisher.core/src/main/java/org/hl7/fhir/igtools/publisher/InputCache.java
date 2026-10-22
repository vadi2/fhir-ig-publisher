package org.hl7.fhir.igtools.publisher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hl7.fhir.model.core.CanonicalResource;
import org.hl7.fhir.model.core.CodeSystem;
import org.hl7.fhir.model.core.DateTimeType;
import org.hl7.fhir.model.core.PackageInformation;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.utilities.ImplicitValueSets;
import org.hl7.fhir.services.conformance.profile.ProfileUtilities;
import org.hl7.fhir.services.context.IResourceReadListener;
import org.hl7.fhir.services.context.IWorkerContext;
import org.hl7.fhir.services.renderers.utils.RenderingContext;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.elementmodel.Property;
import org.hl7.fhir.services.terminology.CacheToken;
import org.hl7.fhir.standalone.context.BaseWorkerContext;
import org.hl7.fhir.standalone.terminology.client.TerminologyClientContext;
import org.hl7.fhir.standalone.terminology.client.TerminologyClientManager;
import org.hl7.fhir.standalone.terminology.utilities.TerminologyCache;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.UserDataNames;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator;
import org.hl7.fhir.validation.BaseValidator;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.utils.CanonicalResourceLookupResult;

/**
 * A cache of the publisher's own work, keyed on what the work read, for full builds of an IG that has been built
 * before (in the style of ccache's direct mode).
 *
 * <p>Two kinds of step are cached: the validation of one resource, and the html fragments of one file. A step's
 * entry holds what the step read and a journal of what it did (its messages, the files it wrote, what it added to the
 * build's registries). When every read is the same, the journal is replayed instead of running the step. A step whose
 * effects can't all be journaled (it asked the terminology server, ran SQL against the package database, changed
 * something the cache doesn't track) is not stored.
 *
 * <p>What a step reads: what every step reads (K0: the publisher, the build's settings, the packages, and the files
 * that aren't the IG's own; and the IG's set of files and resources, and the publisher's state, as they stand when
 * the step's phase starts); its own file and resources; each of the IG's other resources it uses, at its content
 * digest (the getters of {@link FetchedResource} and {@link FetchedFile} report reads, as do the worker context's
 * lookups, and the memos the cache watches); the terminology cache entries it looked up; and the warn-once sets it
 * asked. A lookup whose answer can't be traced to one of those makes the step uncacheable.
 *
 * <p>The entries are kept in input-cache/publisher-cache. Settings (system property, or the [IG] section of ig.ini):
 * <ul>
 * <li>fhir.igpublisher.input-cache / input-cache: false turns it off
 * <li>fhir.igpublisher.input-cache.verify / input-cache-verify: the fraction (0 to 1) of hits that are run anyway and
 * compared with the entry; a difference is logged as an INPUT CACHE MISMATCH and the entry is dropped
 * <li>fhir.igpublisher.input-cache.size / input-cache-size-mb: the size limit in MB (default 2048)
 * <li>fhir.igpublisher.input-cache.calibrate: logs every change to the state the cache watches, by name
 * </ul>
 */
public class InputCache implements InputCacheTxCache.Listener, InputCacheRecordingSet.Listener, InputCacheRecordingMap.Listener, FetchedReads.Listener,
  IResourceReadListener, InputCacheMemoMap.Listener {

  public static final String PHASE_CONFORMANCE = "conformance";
  public static final String PHASE_VALIDATION = "validation";
  public static final String PHASE_HTML = "html";

  private static final String FORMAT = "publisher-input-cache-2";
  private static final String ENTRY_MAGIC = "ICE1";
  private static final int MAX_VARIANTS = 4;
  private static final String SEP = "\u0001";
  static final String ABSENT = "absent";
  /** a read that was a miss, after which the step stored the answer it worked out itself: absent or this answer */
  static final String ABSENT_OR = "~";
  private static final String MARK = "\u0007IC-";
  private static final String MARK_UUID = MARK + "UUID\u0007";
  private static final String MARK_BUILD_DATE = MARK + "BUILDDATE\u0007";
  private static final String PROP = "fhir.igpublisher.input-cache";

  /** an npm entry a replayed step adds: the generator adds it (to every package) */
  public static class NpmEntry {
    public final String category;
    public final String folder;
    public final String name;
    public final byte[] content;

    NpmEntry(String category, String folder, String name, byte[] content) {
      this.category = category;
      this.folder = folder;
      this.name = name;
      this.content = content;
    }
  }

  private final PublisherFields pf;
  private final InputCacheStore store;
  private final double verifyFraction;
  private final boolean calibrate;
  private final Consumer<String> log;
  private final String buildDate;
  /**
   * the build's time as the html shows it (a resource with no date gets the build's), in the forms seen, longest
   * first: each is stored as a marker and put back on a hit. The time in any other form makes a step uncacheable
   */
  private final Map<String, String> buildTimes = new LinkedHashMap<>();
  private final String timeOfDay;
  private final String today;
  private final List<String> todayForms = new ArrayList<>();
  private final Map<String, InputCacheRecordingSet> sets = new HashMap<>();
  private final Map<String, InputCacheRecordingMap<?>> maps = new HashMap<>();
  private TerminologyCache realTx;
  /** the profile the validator last validated against: the messages it raises before it starts on a profile get it */
  private Field fValidationContext;
  private PublisherSettings settings;
  /** what every step reads: the publisher, the build's settings, the packages and the files that aren't the IG's own */
  private String k0;
  private String phase;
  /** the IG's inputs as they stood when the phase started */
  private InputCacheInputs inputs;
  /** the terminology cache's value sets and code systems, by identity: what each was asked for as */
  private final Map<Object, String> txOrigins = Collections.synchronizedMap(new IdentityHashMap<>());
  private Step current;
  private Thread thread;
  /** the cache is looking at the state itself (the probe reads the sizes of the sets it watches): not a step's read */
  private boolean probing;

  // statistics
  private int hits;
  private int verified;
  private int mismatches;
  private int misses;
  private final AtomicInteger stored = new AtomicInteger();
  /**
   * entries are compressed and written on a thread of their own, so the build doesn't wait for them. At most
   * {@link #MAX_PENDING_WRITES} wait at once (each holds a step's outputs), and {@link #finish()} waits for the rest
   */
  private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "input-cache-writer");
    t.setDaemon(true);
    return t;
  });
  private static final int MAX_PENDING_WRITES = 8;
  private final Semaphore pendingWrites = new Semaphore(MAX_PENDING_WRITES);
  /** what went wrong on the writer's thread, logged by {@link #finish()} */
  private final Queue<String> writeFailures = new ConcurrentLinkedQueue<>();
  private final AtomicLong writeNanos = new AtomicLong();
  private final Map<String, Integer> uncacheable = new TreeMap<>();
  /**
   * terminology answers a cached step worked out itself in this build (they aren't there when the step is replayed),
   * with the terminology reads the step had made by then: a later step that uses one depends on those instead.
   * Concurrent, because a store from another thread removes its answer here
   */
  private final Map<String, Map<String, String>> transientAnswers = new ConcurrentHashMap<>();
  private boolean anchorsUnwatched;
  /** why lookups that found an entry didn't hit: the first recorded read that had changed, by kind */
  private final Map<String, Integer> changed = new TreeMap<>();
  /** the time the build spent in the cache's own work, by part */
  private final Map<String, Long> overheadNanos = new TreeMap<>();
  private long savedMillis;

  private InputCache(PublisherFields pf, File root, long maxBytes, double verifyFraction, boolean calibrate, Consumer<String> log) {
    this.pf = pf;
    this.store = new InputCacheStore(root, maxBytes);
    this.verifyFraction = verifyFraction;
    this.calibrate = calibrate;
    this.log = log;
    this.buildDate = new DateTimeType(pf.getExecTime()).asStringValue();
    this.timeOfDay = new SimpleDateFormat("HH:mm:ss", Locale.US).format(pf.getExecTime().getTime());
    this.today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(pf.getExecTime().getTime());
    buildTimes.put(buildDate, MARK_BUILD_DATE);
    int n = 0;
    for (String fmt : new String[] {"yyyy-MM-dd HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd HH:mm:ss"}) {
      buildTimes.putIfAbsent(new SimpleDateFormat(fmt, Locale.US).format(pf.getExecTime().getTime()), MARK + "TIME" + (n++) + "\u0007");
    }
    for (String fmt : new String[] {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "yyyyMMdd", "MMM d, yyyy", "d MMM yyyy", "EEE, MMM d, yyyy", "EEE, dd MMM, yyyy"}) {
      todayForms.add(new SimpleDateFormat(fmt, Locale.US).format(pf.getExecTime().getTime()));
    }
  }

  // ---- set up -------------------------------------------------------------------------------------------------------

  /**
   * The input cache for this build, installed in the worker context and the validator; null when it's turned off,
   * or can't be installed (the reason is logged)
   */
  public static InputCache create(PublisherFields pf, PublisherSettings settings, IniFile ini, Consumer<String> log) {
    String on = setting(PROP, ini, "input-cache");
    if (on != null && !"true".equalsIgnoreCase(on)) {
      return null;
    }
    if (settings.isRapidoMode() || settings.isWatchMode() || settings.isGenerationOff() && settings.isValidationOff()) {
      return null;
    }
    double verify = 0;
    String v = setting(PROP + ".verify", ini, "input-cache-verify");
    if (v != null) {
      try {
        verify = Math.max(0, Math.min(1, Double.parseDouble(v)));
      } catch (NumberFormatException e) {
        log.accept("Input cache: ignoring input-cache-verify value '" + v + "' (not a number)");
      }
    }
    long mb = 2048;
    String s = setting(PROP + ".size", ini, "input-cache-size-mb");
    if (s != null) {
      try {
        mb = Long.parseLong(s.trim());
      } catch (NumberFormatException e) {
        log.accept("Input cache: ignoring input-cache-size-mb value '" + s + "' (not a number)");
      }
    }
    boolean calibrate = "true".equalsIgnoreCase(System.getProperty(PROP + ".calibrate"));
    try {
      InputCache ic = new InputCache(pf, new File(Utilities.path(pf.rootDir, "input-cache", "publisher-cache")), mb * 1024L * 1024L, verify, calibrate, log);
      ic.install(settings);
      log.accept("Input cache: on" + (verify > 0 ? ", verifying " + Math.round(verify * 100) + "% of hits" : "") + " (" + ic.store.getFolder() + ")");
      return ic;
    } catch (Exception e) {
      log.accept("Input cache: off, unable to set it up: " + e);
      return null;
    }
  }

  private static String setting(String prop, IniFile ini, String name) {
    String s = System.getProperty(prop);
    if (s == null && ini != null && ini.hasProperty("IG", name)) {
      s = ini.getStringProperty("IG", name);
    }
    return s;
  }

  private void install(PublisherSettings settings) throws Exception {
    BaseWorkerContext ctxt = pf.context;
    // look everything up before changing anything, so that a failure leaves the build as it was
    Field fUnsupported = field(BaseWorkerContext.class, "unsupportedCodeSystems");
    Field fUsed = field(BaseWorkerContext.class, "codeSystemsUsed");
    Field fSupported = field(BaseWorkerContext.class, "supportedCodeSystems");
    Field fStatus = field(BaseValidator.class, "statusWarnings");
    fValidationContext = field(BaseValidator.class, "validationContext");
    if (fValidationContext.getType() != String.class) {
      throw new IllegalStateException("BaseValidator.validationContext is a " + fValidationContext.getType().getName());
    }
    @SuppressWarnings("unchecked")
    Set<String> unsupported = (Set<String>) fUnsupported.get(ctxt);
    @SuppressWarnings("unchecked")
    Set<String> used = (Set<String>) fUsed.get(ctxt);
    @SuppressWarnings("unchecked")
    Map<String, Object> supported = (Map<String, Object>) fSupported.get(ctxt);
    @SuppressWarnings("unchecked")
    Set<String> status = (Set<String>) fStatus.get(pf.validator);
    realTx = ctxt.getTxCache();
    if (realTx == null) {
      throw new IllegalStateException("no terminology cache");
    }
    InputCacheTxCache tx = new InputCacheTxCache(new Object(), realTx, ctxt, this);
    this.settings = settings;

    InputCacheRecordingSet rsUnsupported = new InputCacheRecordingSet("unsupported", unsupported, this);
    InputCacheRecordingSet rsUsed = new InputCacheRecordingSet("used", used, this);
    InputCacheRecordingSet rsStatus = new InputCacheRecordingSet("status", status, this);
    InputCacheRecordingMap<Object> rmSupported = new InputCacheRecordingMap<>("supported", supported, this);
    fUnsupported.set(ctxt, rsUnsupported);
    fUsed.set(ctxt, rsUsed);
    fSupported.set(ctxt, rmSupported);
    fStatus.set(pf.validator, rsStatus);
    ctxt.initTxCache(tx);
    sets.put(rsUnsupported.getName(), rsUnsupported);
    sets.put(rsUsed.getName(), rsUsed);
    sets.put(rsStatus.getName(), rsStatus);
    maps.put("supported", rmSupported);
  }

  /**
   * The rendering context's anchors are shared by every copy of it, so rendering one file can depend on what was
   * rendered before (an anchor that's already there isn't written again). Called when the context is made, before it
   * is copied, so that every copy shares the recording set
   */
  public void watchAnchors(RenderingContext rc) {
    try {
      Field f = field(RenderingContext.class, "anchors");
      @SuppressWarnings("unchecked")
      Set<String> anchors = (Set<String>) f.get(rc);
      InputCacheRecordingSet rs = new InputCacheRecordingSet("anchors", anchors, this);
      f.set(rc, rs);
      sets.put(rs.getName(), rs);
    } catch (ReflectiveOperationException | RuntimeException e) {
      log.accept("Input cache: html not cached, unable to watch the rendering context: " + e);
      anchorsUnwatched = true;
    }
  }

  private static Field field(Class<?> c, String name) throws NoSuchFieldException {
    Field f = c.getDeclaredField(name);
    f.setAccessible(true);
    return f;
  }

  // ---- what every step reads: the build's settings, and the IG's inputs -------------------------------------------------

  private String computeK0(PublisherSettings settings) throws IOException {
    InputCacheDigest d = new InputCacheDigest();
    d.add(FORMAT).add(IGVersionUtil.getVersion()).add(IGVersionUtil.getBuildTime());
    try {
      File jar = new File(InputCache.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      d.add(jar.getAbsolutePath());
      if (jar.isFile()) {
        d.add(jar.length()).add(jar.lastModified());
      } else {
        hashTree(jar, d, Collections.emptySet());
      }
    } catch (Exception e) {
      // can't tell which publisher this is, so nothing it stores can be found again
      log.accept("Input cache: no hits this build, unable to identify the publisher's jar: " + e);
      d.add(Long.toString(System.nanoTime()));
    }
    d.add(pf.rootDir).add(pf.tempDir).add(pf.qaDir).add(pf.igName);
    d.add("ini").bytes(readOrNull(settings.getConfigFile()));
    d.add("sushi").bytes(readOrNull(Utilities.path(pf.rootDir, "sushi-config.yaml")));
    d.add("ig").bytes(readOrNull(pf.igName));
    d.add("template");
    hashTree(new File(Utilities.path(pf.rootDir, "template")), d, Collections.emptySet());
    d.add("other files");
    hashTree(new File(pf.rootDir), d, otherFilesSkip());
    d.add("packages");
    String pcm = pf.pcm == null ? null : pf.pcm.getFolder();
    List<String> pkgs = new ArrayList<>(pf.context.getLoadedPackages());
    Collections.sort(pkgs);
    for (String p : pkgs) {
      d.add(p);
      File folder = pcm == null ? null : new File(pcm, p);
      String ver = p.contains("#") ? p.substring(p.indexOf('#') + 1) : "";
      if (folder == null || !folder.isDirectory()) {
        d.add("not in cache");
      } else if (ver.equals("current") || ver.equals("dev") || ver.contains("cibuild") || ver.contains("current$")) {
        hashTree(folder, d, Collections.emptySet());
      } else {
        d.bytes(readOrNull(Utilities.path(folder.getAbsolutePath(), "package", "package.json")));
        d.bytes(readOrNull(Utilities.path(folder.getAbsolutePath(), "package", ".index.json")));
      }
    }
    if (pcm != null) {
      String[] list = new File(pcm).list();
      if (list != null) {
        Arrays.sort(list);
        for (String s : list) {
          if (s.startsWith("hl7.fhir.r5.core#") || s.startsWith("hl7.fhir.r5.examples#")) {
            d.add(s);
          }
        }
      }
    }
    d.add("settings");
    for (Field f : allFields(PublisherSettings.class)) {
      if (!f.getName().equals("startTime")) {
        try {
          d.add(f.getName()).add(String.valueOf(f.get(settings)));
        } catch (IllegalAccessException e) {
          d.add(f.getName()).add("?");
        }
      }
    }
    d.add(Locale.getDefault().toString()).add(TimeZone.getDefault().getID()).add(System.getProperty("file.encoding")).add(System.getProperty("java.version"));
    return d.finish();
  }

  private Set<String> otherFilesSkip() throws IOException {
    Set<String> skip = new HashSet<>();
    for (String s : new String[] {Utilities.path(pf.rootDir, "output"), Utilities.path(pf.rootDir, "temp"), pf.tempDir, pf.tempLangDir,
      Utilities.path(pf.rootDir, "template"), Utilities.path(pf.rootDir, "input-cache"), pf.vsCache, pf.qaDir, Utilities.path(pf.rootDir, ".git"),
      Utilities.path(pf.rootDir, "input", "fsh"), Utilities.path(pf.rootDir, "fsh")}) {
      if (s != null) {
        skip.add(canonical(s));
      }
    }
    if (pf.killFile != null) {
      skip.add(canonical(pf.killFile.getAbsolutePath()));
    }
    for (FetchedFile f : pf.fileList) {
      if (!f.isFolder() && f.getPath() != null) {
        skip.add(canonical(f.getPath()));
      }
    }
    // written by sushi on every run
    skip.add(canonical(Utilities.path(pf.rootDir, "fsh-generated", "fsh-index.txt")));
    skip.add(canonical(Utilities.path(pf.rootDir, "fsh-generated", "data", "fsh-index.json")));
    return skip;
  }

  private static byte[] readOrNull(String path) {
    if (path == null) {
      return null;
    }
    try {
      return Files.readAllBytes(new File(path).toPath());
    } catch (IOException e) {
      return null;
    }
  }

  private static String canonical(String s) {
    try {
      return new File(s).getCanonicalPath();
    } catch (IOException e) {
      return new File(s).getAbsolutePath();
    }
  }

  private static void hashTree(File dir, InputCacheDigest d, Set<String> skip) {
    hashTree(dir, dir, d, skip, new HashSet<>());
  }

  private static void hashTree(File root, File dir, InputCacheDigest d, Set<String> skip, Set<String> visited) {
    String c = canonical(dir.getAbsolutePath());
    if (skip.contains(c)) {
      return;
    }
    String rel = root.toPath().relativize(dir.toPath()).toString();
    if (dir.isFile()) {
      d.add(rel).bytes(readOrNull(dir.getAbsolutePath()));
      return;
    }
    if (!dir.exists()) {
      d.add(rel).add("missing");
      return;
    }
    if (!visited.add(c)) {
      d.add(rel).add("(link to " + c + ")");
      return;
    }
    String[] list = dir.list();
    if (list == null) {
      d.add(rel).add("unreadable");
      return;
    }
    Arrays.sort(list);
    d.add(rel + "/").add(list.length);
    for (String s : list) {
      hashTree(root, new File(dir, s), d, skip, visited);
    }
  }

  /**
   * Starts a phase: works out the digests of the IG's inputs as they stand (each file, and each resource's content as
   * loaded, after snapshots and narratives), which a step reads as it uses them, and starts listening for those reads
   */
  public void startPhase(String phase) {
    long t = System.nanoTime();
    boolean was = probing;
    probing = true;
    try {
      this.phase = phase;
      this.inputs = null;
      if (k0 == null) {
        // worked out once the IG is loaded, so that the IG's own files (pages too) are left out by path: what's in
        // them is read with them
        k0 = computeK0(settings);
        t = spent("settings digest", t);
      }
      inputs = InputCacheInputs.of(pf, buildDate);
      watchMemos();
      t = spent("phase inputs", t);
      if (calibrate) {
        calibrateFields();
        t = spent("calibration", t);
      }
      FetchedReads.setListener(this);
      pf.context.setResourceReadListener(this);
    } catch (Exception | StackOverflowError e) {
      log.accept("Input cache: not used for the " + phase + " phase: " + e);
      endPhase();
    } finally {
      probing = was;
      spent("phase inputs", t);
    }
  }

  /** steps outside a phase aren't cached */
  public void endPhase() {
    phase = null;
    inputs = null;
    FetchedReads.setListener(null);
    pf.context.setResourceReadListener(null);
  }

  /** logs the fields in shape:pf that are different from the last build's (the calibrate setting) */
  private void calibrateFields() {
    File f = new File(store.getFolder(), "calibrate-" + phase + ".txt");
    Map<String, String> before = new TreeMap<>();
    try {
      if (f.exists()) {
        for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
          int i = line.indexOf('=');
          if (i > 0) {
            before.put(line.substring(0, i), line.substring(i + 1));
          }
        }
      }
      Map<String, String> now = new TreeMap<>();
      for (Map.Entry<String, String> e : inputs.calibration().entrySet()) {
        now.put(e.getKey().replace('\n', ' ').replace('=', '_'), String.valueOf(e.getValue()).replace('\n', ' '));
      }
      if (!before.isEmpty()) {
        List<String> changed = InputCacheInputs.changed(before, now);
        log.accept("Input cache: in the " + phase + " phase, " + changed.size() + " of the inputs are different from the last build's: "
          + changed.subList(0, Math.min(60, changed.size())));
      }
      StringBuilder b = new StringBuilder();
      for (Map.Entry<String, String> e : now.entrySet()) {
        b.append(e.getKey()).append('=').append(e.getValue()).append('\n');
      }
      if (f.exists()) {
        Files.move(f.toPath(), new File(store.getFolder(), "calibrate-" + phase + ".prev.txt").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      Files.write(f.toPath(), bytes(b.toString()));
    } catch (IOException e) {
      log.accept("Input cache: unable to compare the fields with the last build's: " + e);
    }
  }

  /**
   * Memos of answers worked out before, which a step can use without making the reads that the answer was worked out
   * from: each is replaced by one that tells the cache what the answer came from. The validator's lookups of profiles,
   * the child maps and properties of the shared profile utilities, and the publisher's own
   */
  private void watchMemos() throws ReflectiveOperationException {
    wrap(pf.validator, field(InstanceValidator.class, "crLookups"), "validator lookups",
      (k, v) -> Arrays.asList(k, v == null ? null : ((CanonicalResourceLookupResult) v).getResource()));
    Set<ProfileUtilities> pus = Collections.newSetFromMap(new IdentityHashMap<>());
    pus.add((ProfileUtilities) field(InstanceValidator.class, "profileUtilities").get(pf.validator));
    if (pf.rc != null) {
      pus.add(pf.rc.getProfileUtilities());
    }
    if (pf.rcLangs != null) {
      for (Object o : ((Map<?, ?>) field(RenderingContext.RenderingContextLangs.class, "langs").get(pf.rcLangs)).values()) {
        pus.add(((RenderingContext) o).getProfileUtilities());
      }
    }
    // the parsers' (each element's properties use the profile utilities of the parser that read it)
    Field fpu = field(Property.class, "profileUtilities");
    for (FetchedFile f : pf.fileList) {
      for (FetchedResource r : f.getResources()) {
        for (Element e : new Element[] {r.getElement(), r.getLogicalElement()}) {
          if (e != null && e.getProperty() != null) {
            pus.add((ProfileUtilities) fpu.get(e.getProperty()));
          }
        }
      }
    }
    pus.remove(null);
    Field fChildMap = field(ProfileUtilities.class, "childMapCache");
    Field fProperties = field(ProfileUtilities.class, "propertyCache");
    for (ProfileUtilities pu : pus) {
      wrap(pu, fChildMap, "child maps", (k, v) -> Arrays.asList(urlOfKey((String) k, 3), v == null ? null : ((ProfileUtilities.SourcedChildDefinitions) v).getSource()));
      wrap(pu, fProperties, "properties", (k, v) -> {
        List<Object> res = new ArrayList<>();
        res.add(urlOfKey((String) k, 0));
        if (v != null) {
          for (Object p : (List<?>) v) {
            res.add(((Property) p).getStructure());
          }
        }
        return res;
      });
    }
    if (!(pf.sdMapCache instanceof InputCacheMemoMap)) {
      pf.sdMapCache = new InputCacheMemoMap<>("element maps", pf.sdMapCache, (k, v) -> Collections.singletonList(k), this);
    }
    if (pf.sdUsageCache != null && !(pf.sdUsageCache instanceof InputCacheMemoMap)) {
      // what each resource uses is worked out from the resource
      pf.sdUsageCache = new InputCacheMemoMap<>("usages", pf.sdUsageCache, (k, v) -> Collections.singletonList(k), this);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void wrap(Object owner, Field f, String name, InputCacheMemoMap.Sources sources) throws IllegalAccessException {
    Object m = f.get(owner);
    if (m instanceof Map && !(m instanceof InputCacheMemoMap)) {
      f.set(owner, new InputCacheMemoMap(name, (Map) m, sources, this));
    }
  }

  /** the canonical URL (|version) a memo key starts with, after the prefix and up to the '#' */
  static String urlOfKey(String key, int prefix) {
    if (key == null || key.length() < prefix) {
      return null;
    }
    int i = key.indexOf('#', prefix);
    return i < 0 ? key.substring(prefix) : key.substring(prefix, i);
  }

  // ---- steps ----------------------------------------------------------------------------------------------------------

  /** one cached step: looked up, then either replayed or run (and recorded) */
  public final class Step {
    private final String kind;
    private final String key;
    private final String description;
    private final FetchedFile file;
    private final long started = System.currentTimeMillis();
    private List<Variant> variants = new ArrayList<>();
    private Variant hit;
    private Journal hitJournal;
    private boolean replay;
    private boolean verify;
    private boolean ended;

    // what the run read and did
    private final Map<String, String> reads = new LinkedHashMap<>();
    /** the parts of the IG's files and resources already read, by identity (a bit for each part) */
    private final Map<Object, Integer> seen = new IdentityHashMap<>();
    /** the lists the step has looked up in the worker context */
    private final Set<String> queries = new HashSet<>();
    private final Set<String> own = new HashSet<>();
    private final List<String[]> setAdds = new ArrayList<>();
    private String uncacheable;
    private Map<String, String> probeBefore;
    private int pfErrorsBefore;
    private int fErrorsBefore;
    private int[] rErrorsBefore;
    private Set<String> outputNamesBefore;
    private Map<String, long[]> statBefore;
    private Set<String> otherFilesBefore;
    private int expansionsBefore;
    private final List<String> allOutputsAdded = new ArrayList<>();
    private final Set<String> registered = new LinkedHashSet<>();
    private final List<NpmEntry> npm = new ArrayList<>();
    private final List<String[]> tracked = new ArrayList<>();
    private final List<FragmentUse> fragmentUses = new ArrayList<>();
    private final List<String> found = new ArrayList<>();
    private final Set<Integer> converted = new TreeSet<>();
    private Map<String, String> tempBefore;

    private Step(String kind, String key, String description, FetchedFile file) {
      this.kind = kind;
      this.key = key;
      this.description = description;
      this.file = file;
    }

    /** true when the step's journal is to be replayed instead of running it */
    public boolean replay() {
      return replay;
    }

    /** the step did something the cache can't journal: it isn't stored */
    public void uncacheable(String reason) {
      if (uncacheable == null) {
        uncacheable = reason;
      }
    }

    private Journal hitJournal() throws IOException {
      if (hitJournal == null) {
        hitJournal = Journal.decode(hit.journal);
      }
      return hitJournal;
    }

    private void read(String k, String v) {
      if (!reads.containsKey(k)) {
        reads.put(k, v);
      }
    }

    private Map<String, String> txReadsSnapshot;
    private int txReadsAt = -1;

    /** the terminology reads so far (shared between calls while there are no new reads) */
    private Map<String, String> txReads() {
      if (txReadsAt != reads.size()) {
        Map<String, String> m = new HashMap<>();
        for (Map.Entry<String, String> e : reads.entrySet()) {
          if (e.getKey().startsWith("tx:") || e.getKey().startsWith("txr:")) {
            m.put(e.getKey(), e.getValue());
          }
        }
        txReadsSnapshot = m;
        txReadsAt = reads.size();
      }
      return txReadsSnapshot;
    }

    /**
     * what a stored message or file holds that depends on when the build runs: today's date makes the date a read;
     * the build's time of day can't be replayed, and nor can the build date or the table uuid in a message (files
     * have those replaced by markers)
     */
    private void checkText(String text, boolean message) {
      int i = text.indexOf(timeOfDay);
      if (i >= 0) {
        uncacheable("holds the build's time of day: '" + text.substring(Math.max(0, i - 40), Math.min(text.length(), i + 30)).replace("\n", " ") + "'");
      }
      if (message && (text.contains(buildDate) || (HierarchicalTableGenerator.uuid != null && text.contains(HierarchicalTableGenerator.uuid)))) {
        uncacheable("message holds the build date");
      }
      if (!reads.containsKey("date")) {
        for (String d : todayForms) {
          if (text.contains(d)) {
            read("date", today);
            break;
          }
        }
      }
    }
  }

  /** what the generator does to replay an html hit, besides what the cache does itself */
  public static class HtmlReplay {
    public final List<NpmEntry> npm = new ArrayList<>();
    public final List<FragmentUse> fragmentUses = new ArrayList<>();
  }

  /** a fragment the step made: the timings report gets it (the time is the time it took when it was stored) */
  public static class FragmentUse {
    public final String key;
    public final long time;
    public final long size;

    FragmentUse(String key, long time, long size) {
      this.key = key;
      this.time = time;
      this.size = size;
    }
  }

  static class Variant {
    final Map<String, String> reads;
    /** the journal as stored; null for the variant this build made, which has {@link #fresh} instead */
    final byte[] journal;
    /** the journal this build made, encoded as the entry is written */
    final Journal fresh;
    final byte[] extras;

    /** a variant read from the store */
    Variant(Map<String, String> reads, byte[] journal, byte[] extras) {
      this.reads = reads;
      this.journal = journal;
      this.fresh = null;
      this.extras = extras;
    }

    /** the variant this build made */
    Variant(Map<String, String> reads, Journal fresh, byte[] extras) {
      this.reads = reads;
      this.journal = null;
      this.fresh = fresh;
      this.extras = extras;
    }
  }

  /**
   * The validation of one resource (the validator's part of it: what it reports, and the warn-once sets it adds to).
   * Null when it isn't cached in this phase.
   */
  public Step beginValidation(FetchedFile f, FetchedResource r, boolean ignoreIdCardinalityError) {
    if (phase == null || PHASE_HTML.equals(phase)) {
      return null;
    }
    int index = f.getResources().indexOf(r);
    String key = InputCacheDigest.of(k0, "val", phase, f.getPath(), Integer.toString(index), r.fhirType(), r.getId(), Boolean.toString(ignoreIdCardinalityError));
    Step s = begin("val", key, "validation of " + r.fhirType() + "/" + r.getId(), f);
    if (s != null && !s.replay) {
      // validation reads the date (FHIRPath today() and now(), and status and date checks)
      s.read("date", today);
      if (pf.validator.isCheckIPSCodes()) {
        // the check runs on every coding the validator has seen in the build so far
        s.uncacheable("IPS code check");
      }
    }
    return s;
  }

  /** the html fragments (and the other files) of one file. Null when it isn't cached */
  public Step beginHtml(FetchedFile f) {
    if (!PHASE_HTML.equals(phase) || anchorsUnwatched) {
      return null;
    }
    if (f.getProcessMode() == FetchedFile.PROCESS_NONE || f.getProcessMode() == FetchedFile.PROCESS_XSLT) {
      // pages: Jekyll renders them from the files themselves
      return null;
    }
    String key = InputCacheDigest.of(k0, "html", f.getPath());
    return begin("html", key, "html for " + f.getName(), f);
  }

  private Step begin(String kind, String key, String description, FetchedFile f) {
    long t = System.nanoTime();
    try {
      if (current != null) {
        // a step inside another: the outer one can't be journaled
        current.uncacheable("nested step");
        return null;
      }
      Step s = new Step(kind, key, description, f);
      s.variants = readEntry(key);
      for (Variant v : s.variants) {
        if (firstChange(v.reads, this::evaluate) == null) {
          s.hit = v;
          break;
        }
      }
      if (s.hit == null && !s.variants.isEmpty()) {
        String k = firstChange(s.variants.get(0).reads, this::evaluate);
        changed.merge(kind + ": " + readKind(k), 1, Integer::sum);
        if (calibrate) {
          log.accept("Input cache: miss for " + description + ": " + k + " changed from " + s.variants.get(0).reads.get(k) + " to " + evaluate(k));
        }
      }
      if (s.hit != null) {
        s.verify = selected(key, verifyFraction);
        s.replay = !s.verify;
      }
      if (!s.replay) {
        t = spent("lookups", t);
        startRecording(s);
        spent("recording", t);
      } else {
        spent("lookups", t);
      }
      return s;
    } catch (Exception e) {
      spent("lookups", t);
      log.accept("Input cache: lookup failed for " + description + ": " + e);
      // the step runs uncached, and nothing it does is recorded
      current = null;
      return null;
    }
  }

  /** whether the hit is one of the fraction verified: the same keys every build, so a mismatch shows up again */
  static boolean selected(String key, double fraction) {
    if (fraction <= 0) {
      return false;
    }
    if (fraction >= 1) {
      return true;
    }
    long v = Long.parseLong(InputCacheDigest.of(key, "verify").substring(0, 8), 16);
    return v < (long) (fraction * 0x100000000L);
  }

  private void startRecording(Step s) throws IOException {
    current = s;
    thread = Thread.currentThread();
    boolean was = probing;
    probing = true;
    try {
      startReads(s);
    } finally {
      probing = was;
    }
    s.probeBefore = probe(s.file);
    s.pfErrorsBefore = pf.errors.size();
    s.fErrorsBefore = s.file.getErrors().size();
    s.rErrorsBefore = new int[s.file.getResources().size()];
    for (int i = 0; i < s.rErrorsBefore.length; i++) {
      s.rErrorsBefore[i] = s.file.getResources().get(i).getErrors().size();
    }
    if ("html".equals(s.kind)) {
      s.outputNamesBefore = new HashSet<>(s.file.getOutputNames());
      s.statBefore = new HashMap<>();
      for (String p : s.outputNamesBefore) {
        File f = new File(p);
        s.statBefore.put(p, new long[] {f.length(), f.lastModified()});
      }
      s.otherFilesBefore = new HashSet<>(pf.otherFilesRun);
      s.expansionsBefore = pf.expansions.size();
      if (verifyFraction > 0 || calibrate) {
        s.tempBefore = listTree(pf.tempDir);
      }
    }
  }

  /**
   * What every step reads before it starts: the IG's set of files and resources, the publisher's state, and all of
   * its own file and resources (as it reads them anyway, and writes their messages). The rest it reads as it goes
   */
  private void startReads(Step s) throws IOException {
    for (String k : InputCacheInputs.SHAPES) {
      s.read(k, inputs.evaluate(k));
    }
    String floc = inputs.locate(s.file);
    if (floc == null) {
      s.uncacheable("not one of the IG's files");
      return;
    }
    for (String p : new String[] {InputCacheInputs.FILE, InputCacheInputs.FILE_ERRORS, InputCacheInputs.FILE_OUTPUTS}) {
      s.read(p + floc, inputs.evaluate(p + floc));
    }
    for (FetchedResource r : s.file.getResources()) {
      String loc = inputs.locate(r);
      for (String p : new String[] {InputCacheInputs.CONTENT, InputCacheInputs.ERRORS, InputCacheInputs.FOUND, InputCacheInputs.VALIDATED}) {
        s.read(p + loc, inputs.evaluate(p + loc));
      }
      // the definition it was loaded with, when that is one of the IG's (a logical model)
      Element e = r.getElement();
      String sloc = e == null || e.getProperty() == null ? null : inputs.locateObject(e.getProperty().getStructure());
      if (sloc != null) {
        s.read(InputCacheInputs.CONTENT + sloc, inputs.evaluate(InputCacheInputs.CONTENT + sloc));
      }
    }
    if ("val".equals(s.kind)) {
      s.read("valctx", evaluate("valctx"));
    }
  }

  /** replays a validation hit: the messages go into errs, and the warn-once sets get the step's additions */
  public void replayValidation(Step s, List<ValidationMessage> errs) throws IOException, IllegalAccessException {
    long t = System.nanoTime();
    try {
      Journal j = s.hitJournal();
      replayCommon(s, j);
      errs.addAll(j.list("errs"));
      for (Journal.Rec r : j.recs) {
        if ("valctx".equals(r.tag)) {
          fValidationContext.set(pf.validator, r.b == null ? null : new String(r.b, StandardCharsets.UTF_8));
        }
      }
      hits++;
      savedMillis += j.millis;
    } finally {
      s.ended = true;
      spent("replay", t);
    }
  }

  /** the file's resources that generating its html converted for rendering, by index (before the hit is replayed) */
  public List<Integer> htmlConversions(Step s) throws IOException {
    List<Integer> res = new ArrayList<>();
    for (Journal.Rec r : s.hitJournal().recs) {
      if ("conv".equals(r.tag)) {
        res.add(Integer.parseInt(r.a));
      }
    }
    return res;
  }

  /** a hit that can't be replayed after all (nothing has been replayed yet): the step is run instead, and not stored */
  public void abandon(Step s, String why) {
    s.ended = true;
    misses++;
    uncacheable.merge(s.kind + ": replay abandoned", 1, Integer::sum);
    log.accept("Input cache: unable to replay " + s.description + ", running it instead: " + why);
  }

  /**
   * Replays an html hit: writes the files, and adds what the step added to the build's registries. Returns the npm
   * entries and fragment uses, which the generator adds
   */
  public HtmlReplay replayHtml(Step s) throws IOException {
    long t = System.nanoTime();
    try {
      Journal j = s.hitJournal();
      List<Long> times = decodeExtras(s.hit.extras);
      for (Journal.Rec r : j.recs) {
        switch (r.tag) {
          case "out":
            s.file.getOutputNames().add(r.a);
            break;
          case "all":
            if (!pf.allOutputs.add(r.a)) {
              throw new Error("Error generating build: the file " + r.a + " is being generated more than once (may differ by case)");
            }
            break;
          case "other":
            pf.otherFilesRun.add(r.a);
            break;
          case "dir":
            Files.createDirectories(new File(r.a).toPath());
            break;
          case "file":
            writeIfDifferent(r.a, unplace(r.b));
            break;
          default:
            break;
        }
      }
      replayCommon(s, j);
      HtmlReplay res = new HtmlReplay();
      int fi = 0;
      for (Journal.Rec r : j.recs) {
        if ("npm".equals(r.tag)) {
          String[] p = r.a.split(SEP, 3);
          res.npm.add(new NpmEntry(p[0].isEmpty() ? null : p[0], p[1].isEmpty() ? null : p[1], p[2], unplace(r.b)));
        } else if ("track".equals(r.tag)) {
          String[] p = r.a.split(SEP, 2);
          pf.trackedFragments.computeIfAbsent(p[0], k -> new ArrayList<>()).add(p[1]);
        } else if ("frag".equals(r.tag)) {
          long time = fi < times.size() ? times.get(fi) : 0;
          fi++;
          res.fragmentUses.add(new FragmentUse(r.a, time, Long.parseLong(new String(r.b, StandardCharsets.UTF_8))));
        }
      }
      hits++;
      savedMillis += j.millis;
      return res;
    } finally {
      s.ended = true;
      spent("replay", t);
    }
  }

  private void replayCommon(Step s, Journal j) {
    for (Journal.Rec r : j.recs) {
      if ("set".equals(r.tag)) {
        InputCacheRecordingSet set = sets.get(r.a);
        if (set != null) {
          set.addQuietly(new String(r.b, StandardCharsets.UTF_8));
        }
      }
    }
    for (Journal.Rec r : j.recs) {
      if ("found".equals(r.tag)) {
        String[] p = r.a.split(SEP, 5);
        FetchedResource source = resource(p[0], Integer.parseInt(p[1]));
        FetchedResource example = resource(p[2], Integer.parseInt(p[3]));
        source.addFoundExample(example);
        example.getFoundProfiles().add(p[4]);
      }
    }
    pf.errors.addAll(j.list("pferr"));
    s.file.getErrors().addAll(j.list("ferr"));
    for (int i = 0; i < s.file.getResources().size(); i++) {
      s.file.getResources().get(i).getErrors().addAll(j.list("rerr" + i));
    }
  }

  private FetchedResource resource(String path, int index) {
    FetchedResource r = inputs.resource(path + "#" + index);
    if (r == null) {
      throw new IllegalStateException("no resource " + path + "#" + index);
    }
    return r;
  }

  /** the file path and index of the resource, or null if it isn't one of the IG's */
  private String locate(FetchedResource r) {
    String loc = inputs.locate(r);
    if (loc == null) {
      return null;
    }
    int i = loc.lastIndexOf('#');
    return loc.substring(0, i) + SEP + loc.substring(i + 1);
  }

  private void writeIfDifferent(String path, byte[] content) throws IOException {
    File f = new File(path);
    if (f.exists() && Arrays.equals(Files.readAllBytes(f.toPath()), content)) {
      return;
    }
    if (f.getParentFile() != null) {
      Files.createDirectories(f.getParentFile().toPath());
    }
    Files.write(f.toPath(), content);
  }

  /** ends a validation run; errs is null if the step failed */
  public void endValidation(Step s, List<ValidationMessage> errs) {
    end(s, errs);
  }

  /** ends an html run; ok is false if the step failed */
  public void endHtml(Step s, boolean ok) {
    end(s, ok ? Collections.emptyList() : null);
  }

  private void end(Step s, List<ValidationMessage> errs) {
    if (s.ended) {
      return;
    }
    s.ended = true;
    long t = System.nanoTime();
    try {
      current = null;
      if (errs == null) {
        return;
      }
      long millis = System.currentTimeMillis() - s.started;
      checkProbe(s);
      Journal j = s.uncacheable == null ? journal(s, errs, millis) : null;
      t = spent("journals", t);
      if (s.verify) {
        if (j == null) {
          mismatch(s, "the step ran again and could not be cached: " + s.uncacheable);
        } else {
          String diff = s.hitJournal().difference(j);
          if (diff != null) {
            mismatch(s, diff);
          } else {
            verified++;
          }
        }
        return;
      }
      misses++;
      if (j == null) {
        String reason = s.uncacheable.contains(":") ? s.uncacheable.substring(0, s.uncacheable.indexOf(':')) : s.uncacheable;
        uncacheable.merge(s.kind + ": " + reason, 1, Integer::sum);
        if (calibrate) {
          log.accept("Input cache: not cached: " + s.description + ": " + s.uncacheable);
        }
        return;
      }
      List<Variant> list = new ArrayList<>();
      list.add(new Variant(new LinkedHashMap<>(s.reads), j, encodeExtras(s)));
      for (Variant v : s.variants) {
        if (!v.reads.equals(s.reads) && list.size() < MAX_VARIANTS) {
          list.add(v);
        }
      }
      write(s.key, s.description, list);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.accept("Input cache: unable to store " + s.description + ": " + e);
    } finally {
      spent("storing", t);
    }
  }

  /** adds the time since t to the part's overhead, and returns the time now */
  private long spent(String part, long t) {
    long now = System.nanoTime();
    overheadNanos.merge(part, now - t, Long::sum);
    return now;
  }

  private void write(String key, String description, List<Variant> list) throws InterruptedException {
    pendingWrites.acquire();
    try {
      writer.execute(() -> {
        long t = System.nanoTime();
        try {
          store.write(key, os -> encodeEntry(list, os));
          stored.incrementAndGet();
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
          // the entry is missing, and the step is a miss next time
          writeFailures.add("Input cache: unable to store " + description + ": " + e);
        } finally {
          writeNanos.addAndGet(System.nanoTime() - t);
          pendingWrites.release();
        }
      });
    } catch (RuntimeException e) {
      pendingWrites.release();
      throw e;
    }
  }

  private void mismatch(Step s, String why) {
    mismatches++;
    try {
      store.delete(s.key);
      log.accept("INPUT CACHE MISMATCH: " + s.description + " (" + s.key.substring(0, 12) + "): " + why + ". The entry is dropped");
    } catch (IOException e) {
      log.accept("INPUT CACHE MISMATCH: " + s.description + " (" + s.key.substring(0, 12) + "): " + why + ". UNABLE TO DROP THE ENTRY (" + e
        + "): delete " + store.getFolder() + " before the next build");
    }
  }

  private void checkProbe(Step s) {
    Map<String, String> after = probe(s.file);
    for (Map.Entry<String, String> e : after.entrySet()) {
      String before = s.probeBefore.get(e.getKey());
      if ("html".equals(s.kind) && "null".equals(before) && !"null".equals(e.getValue()) && e.getKey().matches("res[0-9]+\\.resource")) {
        // the resource converted for rendering: the generator does that for a hit too
        s.converted.add(Integer.parseInt(e.getKey().substring(3, e.getKey().indexOf('.'))));
        continue;
      }
      if (!e.getValue().equals(before)) {
        s.uncacheable("state: " + e.getKey() + " " + before + " -> " + e.getValue());
        if (calibrate) {
          log.accept("Input cache probe: " + s.description + ": " + e.getKey() + " " + before + " -> " + e.getValue());
        } else {
          return;
        }
      }
    }
    if (pf.errors.size() < s.pfErrorsBefore || s.file.getErrors().size() < s.fErrorsBefore) {
      s.uncacheable("errors removed");
    }
    if (s.file.getResources().size() != s.rErrorsBefore.length) {
      s.uncacheable("resources changed");
    } else {
      for (int i = 0; i < s.rErrorsBefore.length; i++) {
        if (s.file.getResources().get(i).getErrors().size() < s.rErrorsBefore[i]) {
          s.uncacheable("errors removed");
        }
      }
    }
    if ("html".equals(s.kind)) {
      if (pf.expansions.size() != s.expansionsBefore) {
        s.uncacheable("expansions");
      }
      if (!s.file.getOutputNames().containsAll(s.outputNamesBefore) || !pf.otherFilesRun.containsAll(s.otherFilesBefore)) {
        s.uncacheable("outputs removed");
      }
    }
  }

  // ---- the journal ----------------------------------------------------------------------------------------------------

  private Journal journal(Step s, List<ValidationMessage> errs, long millis) throws IOException, IllegalAccessException {
    Journal j = new Journal();
    j.millis = millis;
    IdentityHashMap<ValidationMessage, Integer> ids = new IdentityHashMap<>();
    if ("val".equals(s.kind)) {
      j.messages(s, "errs", errs, ids);
    }
    j.messages(s, "pferr", pf.errors.subList(s.pfErrorsBefore, pf.errors.size()), ids);
    j.messages(s, "ferr", s.file.getErrors().subList(s.fErrorsBefore, s.file.getErrors().size()), ids);
    for (int i = 0; i < s.rErrorsBefore.length; i++) {
      List<ValidationMessage> l = s.file.getResources().get(i).getErrors();
      j.messages(s, "rerr" + i, l.subList(s.rErrorsBefore[i], l.size()), ids);
    }
    for (String[] a : s.setAdds) {
      j.add("set", a[0], bytes(a[1]));
    }
    for (String a : s.found) {
      j.add("found", a, null);
    }
    if ("val".equals(s.kind)) {
      String v = (String) fValidationContext.get(pf.validator);
      j.add("valctx", null, v == null ? null : bytes(v));
    }
    if ("html".equals(s.kind)) {
      Set<String> files = new TreeSet<>(s.registered);
      for (String p : new TreeSet<>(s.file.getOutputNames())) {
        if (!s.outputNamesBefore.contains(p)) {
          j.add("out", p, null);
          files.add(p);
        } else {
          File f = new File(p);
          long[] st = s.statBefore.get(p);
          if (st[0] != f.length() || st[1] != f.lastModified()) {
            files.add(p);
          }
        }
      }
      for (String p : s.allOutputsAdded) {
        j.add("all", p, null);
      }
      for (String p : new TreeSet<>(pf.otherFilesRun)) {
        if (!s.otherFilesBefore.contains(p)) {
          j.add("other", p, null);
          files.add(p);
        }
      }
      for (String p : files) {
        File f = new File(p);
        if (!under(p, pf.tempDir) && !under(p, pf.qaDir)) {
          s.uncacheable("writes outside the temp folder: " + p);
          return null;
        }
        if (f.isDirectory()) {
          j.add("dir", p, null);
        } else if (f.isFile()) {
          j.add("file", p, place(s, Files.readAllBytes(f.toPath())));
        }
      }
      for (NpmEntry e : s.npm) {
        j.add("npm", (e.category == null ? "" : e.category) + SEP + (e.folder == null ? "" : e.folder) + SEP + e.name, place(s, e.content));
      }
      for (String[] t : s.tracked) {
        j.add("track", t[0] + SEP + t[1], null);
      }
      for (FragmentUse u : s.fragmentUses) {
        j.add("frag", u.key, bytes(Long.toString(u.size)));
      }
      for (Integer i : s.converted) {
        j.add("conv", i.toString(), null);
      }
      if (s.tempBefore != null) {
        Map<String, String> after = listTree(pf.tempDir);
        for (Map.Entry<String, String> e : after.entrySet()) {
          if (!e.getValue().equals(s.tempBefore.get(e.getKey())) && !files.contains(e.getKey())) {
            s.uncacheable("unregistered write: " + e.getKey());
          }
        }
      }
    }
    return s.uncacheable == null ? j : null;
  }

  private static boolean under(String path, String folder) {
    return folder != null && (path.equals(folder) || path.startsWith(folder.endsWith(File.separator) ? folder : folder + File.separator));
  }

  /** every file and folder under the folder, with its size and modification time */
  private static Map<String, String> listTree(String folder) {
    Map<String, String> res = new HashMap<>();
    listTree(new File(folder), res);
    return res;
  }

  private static void listTree(File dir, Map<String, String> res) {
    File[] list = dir.listFiles();
    if (list == null) {
      return;
    }
    for (File f : list) {
      if (f.isDirectory()) {
        res.put(f.getAbsolutePath(), "dir");
        listTree(f, res);
      } else {
        res.put(f.getAbsolutePath(), f.length() + ":" + f.lastModified());
      }
    }
  }

  /** what a step did, as records (tag, name, content) in a fixed order, so that two runs can be compared */
  static class Journal {
    static class Rec {
      final String tag;
      final String a;
      final byte[] b;

      Rec(String tag, String a, byte[] b) {
        this.tag = tag;
        this.a = a;
        this.b = b;
      }
    }

    final List<Rec> recs = new ArrayList<>();
    long millis;
    private final Map<Integer, ValidationMessage> decoded = new HashMap<>();

    void add(String tag, String a, byte[] b) {
      recs.add(new Rec(tag, a == null ? "" : a, b));
    }

    /**
     * a list of messages: each message the first time it's seen (in this step) is stored as a record of its own, so
     * that a message in more than one list is still one message when it's replayed
     */
    void messages(Step s, String name, List<ValidationMessage> list, IdentityHashMap<ValidationMessage, Integer> ids) throws IOException {
      InputCacheCodec.Out refs = new InputCacheCodec.Out();
      refs.integer(list.size());
      for (ValidationMessage vm : list) {
        Integer id = ids.get(vm);
        if (id == null) {
          id = ids.size();
          ids.put(vm, id);
          InputCacheCodec.Out out = new InputCacheCodec.Out();
          try {
            out.message(vm);
          } catch (InputCacheCodec.Unstorable e) {
            s.uncacheable("message: " + e.getMessage());
          }
          byte[] b = out.toBytes();
          String text = new String(b, StandardCharsets.UTF_8);
          s.checkText(text, true);
          add("msg", Integer.toString(id), b);
        }
        refs.integer(id);
      }
      add("list", name, refs.toBytes());
    }

    List<ValidationMessage> list(String name) {
      for (Rec r : recs) {
        if ("list".equals(r.tag) && name.equals(r.a)) {
          try {
            InputCacheCodec.In in = new InputCacheCodec.In(r.b);
            int n = in.integer();
            List<ValidationMessage> res = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
              res.add(decoded.get(in.integer()));
            }
            return res;
          } catch (IOException e) {
            throw new IllegalStateException("corrupt journal", e);
          }
        }
      }
      return Collections.emptyList();
    }

    byte[] encode() throws IOException {
      InputCacheCodec.Out out = new InputCacheCodec.Out();
      encodeTo(out);
      return out.toBytes();
    }

    void encodeTo(InputCacheCodec.Out out) throws IOException {
      out.lng(millis);
      out.integer(recs.size());
      for (Rec r : recs) {
        out.str(r.tag).str(r.a).bytes(r.b);
      }
    }

    /** the length of what {@link #encodeTo} writes */
    long encodedLength() {
      long n = 8 + 4;
      for (Rec r : recs) {
        n += InputCacheCodec.Out.strLength(r.tag) + InputCacheCodec.Out.strLength(r.a) + InputCacheCodec.Out.bytesLength(r.b);
      }
      return n;
    }

    static Journal decode(byte[] b) throws IOException {
      InputCacheCodec.In in = new InputCacheCodec.In(b);
      Journal j = new Journal();
      j.millis = in.lng();
      int n = in.integer();
      for (int i = 0; i < n; i++) {
        Rec r = new Rec(in.str(), in.str(), in.bytes());
        j.recs.add(r);
        if ("msg".equals(r.tag)) {
          j.decoded.put(Integer.parseInt(r.a), new InputCacheCodec.In(r.b).message());
        }
      }
      if (!in.atEnd()) {
        throw new IOException("trailing content in journal");
      }
      return j;
    }

    /** null if the two journals record the same, else where they first differ */
    String difference(Journal other) {
      int n = Math.min(recs.size(), other.recs.size());
      for (int i = 0; i < n; i++) {
        Rec a = recs.get(i);
        Rec b = other.recs.get(i);
        if (!a.tag.equals(b.tag) || !a.a.equals(b.a) || !Arrays.equals(a.b, b.b)) {
          return "record " + i + ": stored " + a.tag + " " + a.a.replace(SEP, "|") + " (" + len(a.b) + " bytes), now " + b.tag + " " + b.a.replace(SEP, "|") + " (" + len(b.b) + " bytes)"
            + firstDifference(a.b, b.b);
        }
      }
      if (recs.size() != other.recs.size()) {
        Rec x = recs.size() > n ? recs.get(n) : other.recs.get(n);
        return "stored " + recs.size() + " records, now " + other.recs.size() + " (first extra: " + x.tag + " " + x.a.replace(SEP, "|") + ")";
      }
      return null;
    }

    private static int len(byte[] b) {
      return b == null ? 0 : b.length;
    }

    private static String firstDifference(byte[] a, byte[] b) {
      if (a == null || b == null) {
        return "";
      }
      int i = 0;
      while (i < a.length && i < b.length && a[i] == b[i]) {
        i++;
      }
      int from = Math.max(0, i - 40);
      return ", at byte " + i + ": stored '" + snippet(a, from, i + 60) + "', now '" + snippet(b, from, i + 60) + "'";
    }

    private static String snippet(byte[] b, int from, int to) {
      return new String(b, from, Math.max(0, Math.min(b.length, to) - from), StandardCharsets.UTF_8).replace("\n", "\\n").replace("\r", "\\r");
    }
  }

  private byte[] encodeExtras(Step s) throws IOException {
    InputCacheCodec.Out out = new InputCacheCodec.Out();
    out.integer(s.fragmentUses.size());
    for (FragmentUse u : s.fragmentUses) {
      out.lng(u.time);
    }
    return out.toBytes();
  }

  private static List<Long> decodeExtras(byte[] b) throws IOException {
    InputCacheCodec.In in = new InputCacheCodec.In(b);
    int n = in.integer();
    List<Long> res = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      res.add(in.lng());
    }
    return res;
  }

  /** writes the entry as it goes, so that the build's journal is never in memory twice */
  static void encodeEntry(List<Variant> list, OutputStream os) throws IOException {
    InputCacheCodec.Out out = new InputCacheCodec.Out(os);
    out.str(ENTRY_MAGIC).integer(list.size());
    for (Variant v : list) {
      out.integer(v.reads.size());
      for (Map.Entry<String, String> e : v.reads.entrySet()) {
        out.str(e.getKey()).str(e.getValue());
      }
      if (v.fresh == null) {
        out.bytes(v.journal);
      } else {
        long n = v.fresh.encodedLength();
        if (n > Integer.MAX_VALUE - 8) {
          throw new IOException("journal too large to store (" + n + " bytes)");
        }
        out.integer((int) n);
        v.fresh.encodeTo(out);
      }
      out.bytes(v.extras);
    }
    out.flush();
  }

  private List<Variant> readEntry(String key) {
    return decodeEntry(store.read(key));
  }

  /** the variants in an entry; none if there's no entry, or it can't be decoded */
  static List<Variant> decodeEntry(byte[] b) {
    List<Variant> res = new ArrayList<>();
    if (b == null) {
      return res;
    }
    try {
      InputCacheCodec.In in = new InputCacheCodec.In(b);
      if (!ENTRY_MAGIC.equals(in.str())) {
        return res;
      }
      int n = in.integer();
      for (int i = 0; i < n; i++) {
        int c = in.integer();
        Map<String, String> reads = new LinkedHashMap<>();
        for (int k = 0; k < c; k++) {
          reads.put(in.str(), in.str());
        }
        res.add(new Variant(reads, in.bytes(), in.bytes()));
      }
      if (!in.atEnd()) {
        res.clear();
      }
    } catch (IOException | RuntimeException e) {
      res.clear();
    }
    return res;
  }

  // ---- per-build values in what's stored --------------------------------------------------------------------------------

  // the build's values and the markers are ASCII, so they're replaced in the bytes read as ISO-8859-1, which keeps
  // every byte as it was

  private byte[] place(Step s, byte[] content) {
    if (isArchive(content)) {
      // an xlsx: its entries, and the workbook's properties, carry the time they were written
      s.uncacheable("writes an archive, which holds the time it was written");
      return content;
    }
    String text = new String(content, StandardCharsets.ISO_8859_1);
    if (text.contains(MARK)) {
      s.uncacheable("content holds the cache's marker");
      return content;
    }
    String res = text;
    if (HierarchicalTableGenerator.uuid != null) {
      res = res.replace(HierarchicalTableGenerator.uuid, MARK_UUID);
    }
    for (Map.Entry<String, String> e : buildTimes.entrySet()) {
      res = res.replace(e.getKey(), e.getValue());
    }
    s.checkText(res, false);
    return res == text ? content : res.getBytes(StandardCharsets.ISO_8859_1);
  }

  /** true if the content is a zip archive */
  static boolean isArchive(byte[] b) {
    return b != null && b.length > 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
  }

  private byte[] unplace(byte[] content) {
    String text = new String(content, StandardCharsets.ISO_8859_1);
    if (!text.contains(MARK)) {
      return content;
    }
    String res = text;
    if (HierarchicalTableGenerator.uuid != null) {
      res = res.replace(MARK_UUID, HierarchicalTableGenerator.uuid);
    }
    for (Map.Entry<String, String> e : buildTimes.entrySet()) {
      res = res.replace(e.getValue(), e.getKey());
    }
    return res.getBytes(StandardCharsets.ISO_8859_1);
  }

  private static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  static byte[] replace(byte[] src, String from, String to) {
    String text = new String(src, StandardCharsets.ISO_8859_1);
    String res = text.replace(from, to);
    return res == text ? src : res.getBytes(StandardCharsets.ISO_8859_1);
  }

  // ---- reads ----------------------------------------------------------------------------------------------------------

  /**
   * the first of the reads whose value now (as evaluated) isn't what was recorded, or null if none. A read recorded as
   * ABSENT_OR + value matches the value or its absence
   */
  static String firstChange(Map<String, String> reads, Function<String, String> evaluate) {
    for (Map.Entry<String, String> e : reads.entrySet()) {
      String now = evaluate.apply(e.getKey());
      String was = e.getValue();
      if (was.startsWith(ABSENT_OR)) {
        if (!ABSENT.equals(now) && !was.substring(1).equals(now)) {
          return e.getKey();
        }
      } else if (!was.equals(now)) {
        return e.getKey();
      }
    }
    return null;
  }

  private static String readKind(String key) {
    if (key.startsWith("shape:")) {
      return key;
    }
    int i = key.indexOf(':');
    if (i < 0) {
      return key;
    }
    int j = key.indexOf(SEP, i);
    return j < 0 ? key.substring(0, i + 1) : key.substring(0, j);
  }

  private String evaluate(String key) {
    try {
      if ("date".equals(key)) {
        return today;
      } else if ("valctx".equals(key)) {
        Object v = fValidationContext.get(pf.validator);
        return v == null ? ABSENT : "=" + v;
      } else if (key.startsWith("tx:")) {
        String[] p = key.split(SEP, 3);
        CacheToken t = new CacheToken();
        t.setName(p[1].equals("-") ? null : p[1].substring(1));
        t.setKey(p[2].equals("-") ? null : p[2].substring(1));
        switch (p[0]) {
          case "tx:v":
            return digest(realTx.getValidation(t));
          case "tx:e":
            return digest(realTx.getExpansion(t));
          case "tx:s":
            return digest(realTx.getSubsumes(t));
          default:
            return "?";
        }
      } else if (key.startsWith("txr:")) {
        String[] p = key.split(SEP, 2);
        switch (p[0]) {
          case "txr:hvs":
            return digest(realTx.hasValueSet(p[1]));
          case "txr:gvs":
            return digest(realTx.getValueSet(p[1]));
          case "txr:hcs":
            return digest(realTx.hasCodeSystem(p[1]));
          case "txr:gcs":
            return digest(realTx.getCodeSystem(p[1]));
          default:
            return "?";
        }
      } else if (key.startsWith("set:")) {
        String[] p = key.substring(4).split(SEP, 2);
        InputCacheRecordingSet set = sets.get(p[0]);
        return set == null ? "?" : set.containsQuietly(p[1]) ? "1" : "0";
      } else if (key.startsWith("setall:")) {
        InputCacheRecordingSet set = sets.get(key.substring(7));
        if (set == null) {
          return "?";
        }
        List<String> l = new ArrayList<>();
        set.forEachQuietly(l::add);
        Collections.sort(l);
        return InputCacheDigest.of(l.toArray(new String[0]));
      } else if (key.startsWith("map:")) {
        String[] p = key.substring(4).split(SEP, 2);
        InputCacheRecordingMap<?> map = maps.get(p[0]);
        if (map == null) {
          return "?";
        }
        return map.containsKeyQuietly(p[1]) ? digest(map.getQuietly(p[1])) : ABSENT;
      } else if (inputs != null) {
        boolean was = probing;
        probing = true;
        try {
          String v = inputs.evaluate(key);
          if (v != null) {
            return v;
          }
        } finally {
          probing = was;
        }
      }
    } catch (RuntimeException | IllegalAccessException | IOException e) {
      return "?" + e;
    }
    return "?";
  }

  private static String digest(Object o) {
    return o == null ? ABSENT : InputCacheDigest.deep(o);
  }

  private Step recording() {
    Step s = current;
    if (s == null || probing) {
      return null;
    }
    if (Thread.currentThread() != thread) {
      s.uncacheable("another thread");
      return null;
    }
    return s.uncacheable == null ? s : null;
  }

  private static String txKey(String kind, CacheToken token) {
    return "tx:" + kind + SEP + (token.getName() == null ? "-" : "+" + token.getName()) + SEP + (token.getKey() == null ? "-" : "+" + token.getKey());
  }

  @Override
  public void txRead(String kind, CacheToken token, Object result) {
    Step s = recording();
    if (s == null) {
      return;
    }
    long t = System.nanoTime();
    try {
      String k = txKey(kind, token);
      if (!s.own.contains(k) && !s.reads.containsKey(k)) {
        Map<String, String> deps = result == null ? null : transientAnswers.get(k);
        if (deps == null) {
          s.read(k, digest(result));
        } else {
          // when an earlier step is replayed this step works the answer out again, from the same reads
          s.read(k, ABSENT_OR + digest(result));
          for (Map.Entry<String, String> e : deps.entrySet()) {
            s.read(e.getKey(), e.getValue());
          }
        }
      }
    } catch (InputCacheDigest.TooLarge e) {
      s.uncacheable("terminology answer too large to digest");
    } finally {
      spent("terminology", t);
    }
  }

  @Override
  public void txStore(String kind, CacheToken token, Object result, boolean persistent) {
    Step s = recording();
    if (s == null) {
      if (!transientAnswers.isEmpty()) {
        // stored by work that isn't replayed: it's there in every build
        transientAnswers.remove(txKey(kind, token));
      }
      return;
    }
    if (persistent && !"s".equals(kind)) {
      // an answer from the terminology server (subsumption is worked out locally, but stored as if it weren't)
      s.uncacheable("terminology server");
      return;
    }
    long t = System.nanoTime();
    try {
      String k = txKey(kind, token);
      if (ABSENT.equals(s.reads.get(k))) {
        s.reads.put(k, ABSENT_OR + digest(result));
        s.txReadsAt = -1;
      }
      s.own.add(k);
      if (!persistent) {
        transientAnswers.put(k, s.txReads());
      }
    } catch (InputCacheDigest.TooLarge e) {
      s.uncacheable("terminology answer too large to digest");
    } finally {
      spent("terminology", t);
    }
  }

  @Override
  public void txResourceRead(String kind, String canonical, Object result) {
    if (result instanceof TerminologyCache.SourcedValueSet && ((TerminologyCache.SourcedValueSet) result).getVs() != null) {
      txOrigins.put(((TerminologyCache.SourcedValueSet) result).getVs(), kind + SEP + canonical);
    } else if (result instanceof TerminologyCache.SourcedCodeSystem && ((TerminologyCache.SourcedCodeSystem) result).getCs() != null) {
      txOrigins.put(((TerminologyCache.SourcedCodeSystem) result).getCs(), kind + SEP + canonical);
    }
    Step s = recording();
    if (s == null) {
      return;
    }
    try {
      String k = "txr:" + kind + SEP + canonical;
      if (!s.reads.containsKey(k)) {
        s.read(k, digest(result));
      }
    } catch (InputCacheDigest.TooLarge e) {
      s.uncacheable("terminology resource too large to digest");
    }
  }

  // ---- reads of the IG's files and resources, and lookups in the worker context ------------------------------------------

  private static final int FILE_BITS = FetchedReads.ResourcePart.values().length;

  @Override
  public void resourceRead(FetchedResource r, FetchedReads.ResourcePart part) {
    Step s = recording();
    if (s == null || !firstRead(s, r, part.ordinal())) {
      return;
    }
    String loc = inputs.locate(r);
    if (loc == null) {
      s.uncacheable("read a resource that isn't one of the IG's: " + r.fhirType() + "/" + r.getId());
      return;
    }
    switch (part) {
      case CONTENT:
        readInput(s, InputCacheInputs.CONTENT + loc);
        break;
      case ERRORS:
        readInput(s, InputCacheInputs.ERRORS + loc);
        break;
      case FOUND:
        readInput(s, InputCacheInputs.FOUND + loc);
        break;
      default:
        readInput(s, InputCacheInputs.VALIDATED + loc);
        break;
    }
  }

  @Override
  public void fileRead(FetchedFile f, FetchedReads.FilePart part) {
    Step s = recording();
    if (s == null || !firstRead(s, f, FILE_BITS + part.ordinal())) {
      return;
    }
    String loc = inputs.locate(f);
    if (loc == null) {
      s.uncacheable("read a file that isn't one of the IG's: " + f.getName());
      return;
    }
    switch (part) {
      case CONTENT:
        readInput(s, InputCacheInputs.FILE + loc);
        break;
      case ERRORS:
        readInput(s, InputCacheInputs.FILE_ERRORS + loc);
        break;
      default:
        readInput(s, InputCacheInputs.FILE_OUTPUTS + loc);
        break;
    }
  }

  /** whether this is the step's first read of this part of the object (the getters are called often) */
  private static boolean firstRead(Step s, Object o, int bit) {
    Integer m = s.seen.get(o);
    int bits = m == null ? 0 : m;
    if ((bits & (1 << bit)) != 0) {
      return false;
    }
    s.seen.put(o, bits | (1 << bit));
    return true;
  }

  /** records the read of one of the IG's inputs, at its value now */
  private void readInput(Step s, String k) {
    if (s.reads.containsKey(k)) {
      return;
    }
    boolean was = probing;
    probing = true;
    try {
      s.read(k, inputs.evaluate(k));
    } catch (IOException | RuntimeException e) {
      s.uncacheable("unable to digest " + k + ": " + e);
    } finally {
      probing = was;
    }
  }

  @Override
  public void resourceRead(String type, String canonical, String version, Resource result) {
    Step s = recording();
    if (s != null && result != null) {
      long t = System.nanoTime();
      resultRead(s, result, type + " " + canonical + (version == null ? "" : "|" + version));
      spent("context reads", t);
    }
  }

  @Override
  public void resourcesRead(String query, List<? extends Resource> result) {
    Step s = recording();
    if (s != null && result != null && s.queries.add(query)) {
      long t = System.nanoTime();
      for (Resource r : result) {
        if (r != null) {
          resultRead(s, r, query);
          if (s.uncacheable != null) {
            break;
          }
        }
      }
      spent("context reads", t);
    }
  }

  /**
   * What a lookup's answer is: one of the IG's resources (the step reads it), a package's (the packages are in K0),
   * one the terminology cache had (the step reads that), or an implicit value set (made from its URL). Anything else
   * can't be told apart from one build to the next
   */
  private void resultRead(Step s, Resource res, String what) {
    String loc = inputs.locateObject(res);
    if (loc != null) {
      readInput(s, InputCacheInputs.CONTENT + loc);
      return;
    }
    PackageInformation p = res.getSourcePackage();
    if (p != null && p != pf.packageInfo) {
      return;
    }
    String origin = txOrigins.get(res);
    if (origin == null && res.hasUserData(UserDataNames.render_external_link)) {
      // from the terminology cache before the cache was installed: it's still there, by URL
      origin = txOrigin(res);
    }
    if (origin != null) {
      String k = "txr:" + origin;
      if (!s.reads.containsKey(k)) {
        s.read(k, evaluate(k));
      }
      return;
    }
    if (p == null && generated(res)) {
      return;
    }
    if (res instanceof CanonicalResource) {
      CanonicalResource cr = (CanonicalResource) res;
      if (cr.hasUrl() && !res.hasUserData(UserDataNames.render_external_link)) {
        if (res instanceof ValueSet && IMPLICIT.isImplicitValueSet(cr.getUrl())) {
          return;
        }
        String l = cr.hasVersion() ? inputs.locateUrl(cr.getUrl() + "|" + cr.getVersion()) : null;
        if (l == null) {
          l = inputs.locateUrl(cr.getUrl());
        }
        if (l != null) {
          readInput(s, InputCacheInputs.CONTENT + l);
          return;
        }
      }
    }
    s.uncacheable("looked up a resource that can't be traced: " + what + " found " + res.fhirType() + "/" + res.getIdBase()
      + (res instanceof CanonicalResource ? " " + ((CanonicalResource) res).getVUrl() : ""));
  }

  private static final ImplicitValueSets IMPLICIT = new ImplicitValueSets(null);
  private static final String BASE_DEFINITION = "http://hl7.org/fhir/StructureDefinition/Base";
  private static final java.util.regex.Pattern XVER_EXTENSION = java.util.regex.Pattern.compile("http://hl7\\.org/fhir/[0-9.]+/StructureDefinition/extension-.*");

  /**
   * whether the worker context made the definition itself, from the FHIR version and its own data: the base
   * definition, and the cross-version extensions it makes when they're used
   */
  static boolean generated(Resource res) {
    if (!(res instanceof StructureDefinition)) {
      return false;
    }
    String url = ((StructureDefinition) res).getUrl();
    return BASE_DEFINITION.equals(url) || (url != null && XVER_EXTENSION.matcher(url).matches());
  }

  /** the terminology cache's key for a value set or code system it holds, or null */
  private String txOrigin(Resource res) {
    if (!(res instanceof CanonicalResource) || !((CanonicalResource) res).hasUrl()) {
      return null;
    }
    CanonicalResource cr = (CanonicalResource) res;
    List<String> keys = new ArrayList<>();
    if (cr.hasVersion()) {
      keys.add(cr.getUrl() + "|" + cr.getVersion());
    }
    keys.add(cr.getUrl());
    for (String k : keys) {
      if (res instanceof ValueSet && realTx.hasValueSet(k)) {
        return "gvs" + SEP + k;
      } else if (res instanceof CodeSystem && realTx.hasCodeSystem(k)) {
        return "gcs" + SEP + k;
      }
    }
    return null;
  }

  @Override
  public void memoHit(String memo, Collection<?> sources) {
    Step s = recording();
    if (s == null) {
      return;
    }
    for (Object o : sources) {
      if (o == null) {
        continue;
      } else if (o instanceof String) {
        // a canonical URL: one of the IG's, or a package's
        String l = inputs.locateUrl((String) o);
        if (l != null) {
          readInput(s, InputCacheInputs.CONTENT + l);
        }
      } else if (o instanceof Resource) {
        resultRead(s, (Resource) o, "memo " + memo);
      } else {
        String l = inputs.locateObject(o);
        if (l == null) {
          s.uncacheable("memo " + memo + ": an answer worked out from something that can't be traced (" + o.getClass().getSimpleName() + ")");
          return;
        }
        readInput(s, InputCacheInputs.CONTENT + l);
      }
    }
  }

  @Override
  public void memoOther(String memo, String operation) {
    Step s = recording();
    if (s != null) {
      s.uncacheable("memo " + memo + ": " + operation);
    }
  }

  @Override
  public void txOther(String operation) {
    Step s = recording();
    if (s != null) {
      s.uncacheable("terminology cache: " + operation);
    }
  }

  @Override
  public void setContains(String set, String value, boolean result) {
    Step s = recording();
    if (s != null && !s.own.contains("set:" + set + SEP + value)) {
      s.read("set:" + set + SEP + value, result ? "1" : "0");
    }
  }

  @Override
  public void setAdd(String set, String value, boolean added) {
    Step s = recording();
    if (s == null) {
      return;
    }
    String k = "set:" + set + SEP + value;
    if (!s.own.contains(k)) {
      s.read(k, added ? "0" : "1");
      s.own.add(k);
    }
    if (added) {
      s.setAdds.add(new String[] {set, value});
    }
  }

  @Override
  public void setWhole(InputCacheRecordingSet set) {
    Step s = recording();
    if (s == null) {
      return;
    }
    for (String[] a : s.setAdds) {
      if (a[0].equals(set.getName())) {
        s.uncacheable("whole set read after an addition: " + set.getName());
        return;
      }
    }
    if (!s.reads.containsKey("setall:" + set.getName())) {
      s.read("setall:" + set.getName(), evaluate("setall:" + set.getName()));
    }
  }

  @Override
  public void setOther(String set, String operation) {
    Step s = recording();
    if (s != null) {
      s.uncacheable("set " + set + ": " + operation);
    }
  }

  @Override
  public void mapRead(String map, String key, boolean present, Object value) {
    Step s = recording();
    if (s == null) {
      return;
    }
    try {
      String k = "map:" + map + SEP + key;
      if (!s.reads.containsKey(k)) {
        s.read(k, present ? digest(value) : ABSENT);
      }
    } catch (InputCacheDigest.TooLarge e) {
      s.uncacheable("map value too large to digest");
    }
  }

  @Override
  public void mapOther(String map, String operation) {
    Step s = recording();
    if (s != null) {
      s.uncacheable("map " + map + ": " + operation);
    }
  }

  // ---- what the generator tells the cache ---------------------------------------------------------------------------------

  /** checkMakeFile registered an output (lower case) at the path */
  public void noteOutput(String lower, String path) {
    Step s = recording();
    if (s != null) {
      s.allOutputsAdded.add(lower);
      s.registered.add(path);
    }
  }

  /** a file the step wrote that isn't registered as an output anywhere */
  public void noteFile(String path) {
    Step s = recording();
    if (s != null) {
      s.registered.add(path);
    }
  }

  public void noteNpm(String category, String folder, String name, byte[] content) {
    Step s = recording();
    if (s != null) {
      s.npm.add(new NpmEntry(category, folder, name, content.clone()));
    }
  }

  public void noteTrackedFragment(String id, String name) {
    Step s = recording();
    if (s != null) {
      s.tracked.add(new String[] {id, name});
    }
  }

  public void noteFragmentUse(String key, long time, long size) {
    Step s = recording();
    if (s != null) {
      s.fragmentUses.add(new FragmentUse(key, time, size));
    }
  }

  /** validation found that the example conforms to a profile of the IG's (the profile's source resource) */
  public void noteFoundExample(FetchedResource source, FetchedResource example, String profileUrl) {
    Step s = recording();
    if (s != null) {
      String a = locate(source);
      String b = locate(example);
      if (a == null || b == null) {
        s.uncacheable("found example outside the IG's files");
      } else {
        s.found.add(a + SEP + b + SEP + profileUrl);
      }
    }
  }

  /** the step did something the cache can't journal */
  public void noteUncacheable(String reason) {
    Step s = recording();
    if (s != null) {
      s.uncacheable(reason);
    }
  }

  // ---- the state the cache watches: anything else a step changes makes it uncacheable ---------------------------------

  /** fields that change as steps run but that a replay doesn't need to change: memos, timers, journaled state */
  private static final Set<String> NOT_PROBED = new HashSet<>(Arrays.asList(
    "pf.errors", "pf.allOutputs", "pf.otherFilesRun", "pf.trackedFragments", "pf.fragmentUses", "pf.sdUsageCache", "pf.sdMapCache",
    "pf.last", "pf.tt", "pf.inputCache", "pf.npm", "pf.vnpms", "pf.expansions", "pf.publisher.extensionList",
    "file.outputNames", "file.errors", "file.time", "file.statistics",
    "res*.errors", "res*.foundProfiles", "res*.foundExamples",
    // what the cache records itself: reads and additions are journaled, anything else makes the step uncacheable
    "ctx.resourceReadListener", "ctx.codeSystemsUsed", "ctx.unsupportedCodeSystems", "ctx.supportedCodeSystems", "val.statusWarnings", "ctx.txCache", "pf.rc.anchors",
    // memos, worked out again when they're needed (the context's go with definitionsVersion)
    "ctx.analyses", "ctx.cachedResourceNames", "ctx.cachedResourceNameSet", "ctx.pluralRules", "val.profileUtilities.childMapCache", "val.cu.allStructuresList",
    // set up afresh for every resource the validator validates, and its timers
    "val.start", "val.executionId", "val.fetchCache", "val.resourceTracker", "val.timeTracker.*",
    // journaled: the validator's last profile
    "val.validationContext",
    // memos that tell the cache what their answers were worked out from
    "val.crLookups",
    // the codings the validator saw, only looked at for the IPS code check, which isn't cached
    "val.codingObserver.list",
    // set by the dependent IG finder's own thread as it works, whatever step is running then
    "pf.dependentIgFinder.*"
  ));

  private static final Map<Class<?>, List<Field>> FIELDS = new ConcurrentHashMap<>();

  private static List<Field> allFields(Class<?> c) {
    return FIELDS.computeIfAbsent(c, k -> {
      List<Field> res = new ArrayList<>();
      for (Class<?> t = k; t != null && t != Object.class; t = t.getSuperclass()) {
        for (Field f : t.getDeclaredFields()) {
          if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
            try {
              f.setAccessible(true);
              res.add(f);
            } catch (RuntimeException e) {
              // not readable: not watched
            }
          }
        }
      }
      return res;
    });
  }

  private Map<String, String> probe(FetchedFile file) {
    probing = true;
    try {
      return probeState(file);
    } finally {
      probing = false;
    }
  }

  private Map<String, String> probeState(FetchedFile file) {
    Map<String, String> m = new HashMap<>();
    IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
    seen.put(pf.context, true);
    seen.put(pf.validator, true);
    probe("pf", "pf", pf, m, 2, seen);
    probe("ctx", "ctx", pf.context, m, 2, seen);
    probe("val", "val", pf.validator, m, 2, seen);
    probe("file", "file", file, m, 1, seen);
    for (int i = 0; i < file.getResources().size(); i++) {
      probe("res" + i, "res*", file.getResources().get(i), m, 1, seen);
    }
    if (calibrate) {
      // the other files' resources: too many to look at in every build, but a step that changes them needs a hook
      for (FetchedFile f : pf.fileList) {
        if (f != file) {
          for (int i = 0; i < f.getResources().size(); i++) {
            probe("other[" + f.getName() + "#" + i + "]", "res*", f.getResources().get(i), m, 1, seen);
          }
        }
      }
    }
    TerminologyClientManager tcm = pf.context.getTxClientManager();
    if (tcm != null) {
      StringBuilder b = new StringBuilder();
      for (TerminologyClientContext tc : tcm.serverList()) {
        b.append(tc.getAddress()).append(':').append(tc.getUseCount()).append(':').append(tc.getCached().size()).append(':').append(tc.getUseCounts().size()).append(';');
      }
      b.append(tcm.getServerMap().size()).append(':').append(tcm.getInternalLog().size());
      m.put("network", b.toString());
    }
    return m;
  }

  /** pattern: the prefix as NOT_PROBED names it */
  private void probe(String prefix, String pattern, Object o, Map<String, String> m, int depth, IdentityHashMap<Object, Boolean> seen) {
    if (NOT_PROBED.contains(pattern + ".*")) {
      return;
    }
    for (Field f : allFields(o.getClass())) {
      String name = prefix + "." + f.getName();
      if (NOT_PROBED.contains(pattern + "." + f.getName())) {
        continue;
      }
      Object v;
      try {
        v = f.get(o);
      } catch (IllegalAccessException e) {
        continue;
      }
      m.put(name, signature(v));
      if (depth > 1 && v != null && v.getClass().getName().startsWith("org.hl7.fhir.") && !(v instanceof IWorkerContext) && !v.getClass().isEnum() && seen.put(v, true) == null) {
        probe(name, pattern + "." + f.getName(), v, m, depth - 1, seen);
      }
    }
  }

  private static String signature(Object v) {
    if (v == null) {
      return "null";
    } else if (v instanceof String) {
      String s = (String) v;
      return s.length() <= 80 ? "\"" + s + "\"" : "\"" + s.length() + ":" + s.hashCode();
    } else if (v instanceof Number || v instanceof Boolean || v instanceof Character || v instanceof Enum) {
      return v.toString();
    } else if (v instanceof Collection) {
      return "size " + ((Collection<?>) v).size();
    } else if (v instanceof Map) {
      return "size " + ((Map<?, ?>) v).size();
    } else if (v instanceof CharSequence) {
      return "length " + ((CharSequence) v).length();
    } else if (v.getClass().isArray()) {
      return "length " + java.lang.reflect.Array.getLength(v);
    } else {
      return v.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(v));
    }
  }

  // ---- the end of the build -------------------------------------------------------------------------------------------

  /** logs what the cache did, and keeps it under its size limit */
  public void finish() {
    endPhase();
    writer.shutdown();
    try {
      if (!writer.awaitTermination(10, TimeUnit.MINUTES)) {
        log.accept("Input cache: gave up waiting for the cache's entries to be written; the rest are missing");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.accept("Input cache: interrupted while the cache's entries were being written; the rest are missing");
    }
    for (String f : writeFailures) {
      log.accept(f);
    }
    StringBuilder b = new StringBuilder();
    b.append("Input cache: ").append(hits).append(" hits (about ").append(savedMillis / 1000).append("s of work), ").append(misses).append(" misses, ")
      .append(stored.get()).append(" stored");
    if (verifyFraction > 0) {
      b.append(", ").append(verified).append(" hits verified, ").append(mismatches).append(" mismatches");
    }
    if (store.getCorrupt() > 0) {
      b.append(", ").append(store.getCorrupt()).append(" unreadable entries ignored");
    }
    long total = 0;
    StringBuilder parts = new StringBuilder();
    for (Map.Entry<String, Long> e : overheadNanos.entrySet()) {
      total += e.getValue();
      parts.append(parts.length() == 0 ? "" : ", ").append(e.getKey()).append(' ').append(e.getValue() / 1000000);
    }
    b.append(", overhead ").append(total / 1000000).append("ms (").append(parts).append("; and ").append(writeNanos.get() / 1000000)
      .append("ms writing entries in the background)");
    log.accept(b.toString());
    if (!uncacheable.isEmpty()) {
      log.accept("Input cache: not cached: " + uncacheable);
    }
    if (!changed.isEmpty()) {
      log.accept("Input cache: misses because an input changed: " + changed);
    }
    try {
      int n = store.evict();
      if (n > 0) {
        log.accept("Input cache: removed " + n + " entries used longest ago, to keep it under its size limit");
      }
    } catch (IOException | RuntimeException e) {
      // walking the folder fails with an unchecked exception when another build deletes a folder at the same time
      log.accept("Input cache: unable to trim the cache: " + e);
    }
  }

  public int getMismatches() {
    return mismatches;
  }
}
