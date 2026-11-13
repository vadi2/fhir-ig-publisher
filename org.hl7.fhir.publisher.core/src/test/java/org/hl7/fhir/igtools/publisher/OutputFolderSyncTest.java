package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutputFolderSyncTest {

  private static final FileTime OLD = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));

  @TempDir
  Path dir;

  private Path staged() throws IOException {
    return Files.createDirectories(dir.resolve("staged"));
  }

  private Path target() {
    return dir.resolve("output");
  }

  private static void write(Path root, String rel, String content) throws IOException {
    Path p = root.resolve(rel);
    Files.createDirectories(p.getParent());
    Files.writeString(p, content);
  }

  private static void writeOld(Path root, String rel, String content) throws IOException {
    write(root, rel, content);
    Files.setLastModifiedTime(root.resolve(rel), OLD);
  }

  private static String read(Path root, String rel) throws IOException {
    return Files.readString(root.resolve(rel));
  }

  /** what is in the folder, as relative paths, folders ending with / */
  private static Set<String> contents(Path root) throws IOException {
    try (Stream<Path> s = Files.walk(root)) {
      return s.filter(p -> !p.equals(root))
          .map(p -> root.relativize(p).toString().replace(File.separatorChar, '/') + (Files.isDirectory(p) ? "/" : ""))
          .collect(Collectors.toCollection(TreeSet::new));
    }
  }

  @Test
  void keepsTheStagedFilesWhenAsked() throws IOException {
    Path staged = staged();
    writeOld(staged, "index.html", "home");
    write(staged, "en/page.html", "new page");
    write(staged, "en/same.html", "same");
    write(target(), "en/page.html", "old page");
    write(target(), "en/same.html", "same");
    write(target(), "stale.html", "stale");
    Object inode = Files.readAttributes(staged.resolve("en/page.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey();

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target(), true);

    // the output is synced as it is with a move
    assertEquals(Set.of("index.html", "en/", "en/page.html", "en/same.html"), contents(target()));
    assertEquals("new page", read(target(), "en/page.html"));
    assertEquals(OLD, Files.getLastModifiedTime(target().resolve("index.html")));
    assertEquals(2, r.getWritten());
    assertEquals(1, r.getUnchanged());
    assertEquals(1, r.getDeleted());
    // and the staged files are all still there, as they were (the same file, with the same modified time)
    assertEquals(Set.of("index.html", "en/", "en/page.html", "en/same.html"), contents(staged));
    assertEquals("new page", read(staged, "en/page.html"));
    assertEquals(OLD, Files.getLastModifiedTime(staged.resolve("index.html")));
    if (inode != null) {
      assertEquals(inode, Files.readAttributes(staged.resolve("en/page.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey());
      assertFalse(inode.equals(Files.readAttributes(target().resolve("en/page.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey()));
    }
    // no copy is left next to the files
    try (Stream<Path> st = Files.walk(target())) {
      assertFalse(st.anyMatch(p -> p.getFileName().toString().startsWith(".sync-")));
    }
  }

  @Test
  void keepingTheStagedFilesReplacesAReadOnlyFile() throws IOException {
    Path staged = staged();
    write(staged, "page.html", "new");
    write(target(), "page.html", "old");
    assumeTrue(target().resolve("page.html").toFile().setWritable(false));

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target(), true);

    assertTrue(r.getFailures().isEmpty(), r.getFailures().toString());
    assertEquals("new", read(target(), "page.html"));
    assertEquals("new", read(staged, "page.html"));
  }

  @Test
  void createsTheTargetAndCopiesEverything() throws IOException {
    Path staged = staged();
    write(staged, "index.html", "home");
    write(staged, "en/page.html", "page");
    Files.createDirectories(staged.resolve("empty/inner"));

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target());

    assertEquals(Set.of("index.html", "en/", "en/page.html", "empty/", "empty/inner/"), contents(target()));
    assertEquals("page", read(target(), "en/page.html"));
    assertEquals(2, r.getWritten());
    assertEquals(0, r.getDeleted());
    assertEquals(0, r.getUnchanged());
    assertTrue(r.getFailures().isEmpty());
  }

  @Test
  void leavesUnchangedFilesAlone() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "same.html", "same content");
    write(staged, "sub/same.css", "body {}");
    writeOld(target, "same.html", "same content");
    writeOld(target, "sub/same.css", "body {}");
    Object key = Files.readAttributes(target.resolve("same.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey();

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    assertEquals(OLD, Files.getLastModifiedTime(target.resolve("same.html")));
    assertEquals(OLD, Files.getLastModifiedTime(target.resolve("sub/same.css")));
    if (key != null) {
      // not replaced by another file with the same content
      assertEquals(key, Files.readAttributes(target.resolve("same.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey());
    }
    assertEquals(2, r.getUnchanged());
    assertEquals(0, r.getWritten());
    assertEquals(0, r.getDeleted());
  }

  @Test
  void replacesChangedFiles() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "same-size.html", "content B");
    write(staged, "longer.html", "a longer content");
    // differs only after the first 64K block that is compared
    String big = "x".repeat(70_000);
    write(staged, "big.html", big + "B");
    writeOld(target, "same-size.html", "content A");
    writeOld(target, "longer.html", "short");
    writeOld(target, "big.html", big + "A");

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    assertEquals("content B", read(target, "same-size.html"));
    assertEquals("a longer content", read(target, "longer.html"));
    assertEquals(big + "B", read(target, "big.html"));
    assertFalse(OLD.equals(Files.getLastModifiedTime(target.resolve("same-size.html"))));
    assertEquals(3, r.getWritten());
    assertEquals(0, r.getUnchanged());
  }

  @Test
  void deletesWhatTheBuildNoLongerProduces() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "index.html", "home");
    write(staged, "kept/a.html", "a");
    writeOld(target, "index.html", "home");
    writeOld(target, "kept/a.html", "a");
    write(target, "kept/stale.html", "old");
    write(target, "stale.html", "old");
    write(target, "gone/deeper/x.html", "old");
    Files.createDirectories(target.resolve("gone/empty"));

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    assertEquals(Set.of("index.html", "kept/", "kept/a.html"), contents(target));
    assertEquals(OLD, Files.getLastModifiedTime(target.resolve("kept/a.html")));
    // 3 files and 3 folders
    assertEquals(6, r.getDeleted());
    assertEquals(2, r.getUnchanged());
    assertTrue(r.getFailures().isEmpty());
  }

  @Test
  void aFileCanBecomeAFolderAndAFolderAFile() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "was-file/inner.html", "now a folder");
    write(staged, "was-folder", "now a file");
    write(target, "was-file", "a file");
    write(target, "was-folder/inner.html", "in a folder");

    OutputFolderSync.sync(staged, target);

    assertEquals(Set.of("was-file/", "was-file/inner.html", "was-folder"), contents(target));
    assertEquals("now a file", read(target, "was-folder"));
  }

  @Test
  void comparesWholeBlocksAndEmptyFiles() throws IOException {
    Path staged = staged();
    Path target = target();
    String block = "y".repeat(64 * 1024);
    write(staged, "one-block.html", block.substring(1) + "B");
    write(staged, "two-blocks.html", block + block.substring(1) + "B");
    write(staged, "two-blocks-same.html", block + block);
    write(staged, "empty.html", "");
    writeOld(target, "one-block.html", block.substring(1) + "A");
    writeOld(target, "two-blocks.html", block + block.substring(1) + "A");
    writeOld(target, "two-blocks-same.html", block + block);
    writeOld(target, "empty.html", "");

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    assertEquals(2, r.getWritten());
    assertEquals(2, r.getUnchanged());
    assertTrue(read(target, "one-block.html").endsWith("B"));
    assertTrue(read(target, "two-blocks.html").endsWith("B"));
    assertEquals(OLD, Files.getLastModifiedTime(target.resolve("empty.html")));
  }

  @Test
  void syncsWhereALinkedOutputFolderPoints() throws IOException {
    Path staged = staged();
    Path real = Files.createDirectories(dir.resolve("web-root"));
    write(staged, "new.html", "new");
    write(staged, "same.html", "same");
    writeOld(real, "same.html", "same");
    write(real, "stale.html", "old");
    try {
      Files.createSymbolicLink(target(), real);
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "can't make a link here");
    }

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target());

    assertTrue(Files.isSymbolicLink(target()));
    assertEquals(Set.of("new.html", "same.html"), contents(real));
    assertEquals(OLD, Files.getLastModifiedTime(real.resolve("same.html")));
    assertTrue(r.getFailures().isEmpty(), r.getFailures().toString());
  }

  @Test
  void leavesAFolderItCantReadAloneAndCarriesOn() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "index.html", "new");
    write(target, "index.html", "old");
    write(target, "private/secret.html", "kept");
    File priv = target.resolve("private").toFile();
    assumeTrue(priv.setReadable(false, false) && !Files.isReadable(target.resolve("private")));
    try {
      OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

      assertEquals("new", read(target, "index.html"));
      assertEquals(1, r.getFailures().size(), r.getFailures().toString());
      assertTrue(r.getFailures().get(0).contains("private"), r.getFailures().toString());
    } finally {
      priv.setReadable(true, false);
    }
    assertEquals("kept", read(target, "private/secret.html"));
  }

  /*
   * On a file system that ignores case (Windows, macOS) the stale name has to be deleted before the new one is moved
   * in, or the old case stays. On Linux the names are different files, so this only checks the result; the order is
   * also what aFileCanBecomeAFolderAndAFolderAFile depends on
   */
  @Test
  void aNameThatOnlyChangedCaseGetsTheNewCase() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "Page.html", "page");
    write(staged, "images/logo.png", "logo");
    write(target, "page.html", "page");
    write(target, "Images/logo.png", "logo");

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    // on a file system that ignores case these are the same file: the listing must still show the new names
    try (Stream<Path> s = Files.list(target)) {
      assertEquals(Set.of("Page.html", "images"), s.map(p -> p.getFileName().toString()).collect(Collectors.toSet()));
    }
    try (Stream<Path> s = Files.list(target.resolve("images"))) {
      assertEquals(List.of("logo.png"), s.map(p -> p.getFileName().toString()).collect(Collectors.toList()));
    }
    assertTrue(r.getFailures().isEmpty());
  }

  @Test
  void replacesAndDeletesReadOnlyFiles() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "changed.html", "new");
    write(target, "changed.html", "old!");
    write(target, "stale.html", "old");
    assumeTrue(target.resolve("changed.html").toFile().setWritable(false));
    assumeTrue(target.resolve("stale.html").toFile().setWritable(false));

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    assertEquals("new", read(target, "changed.html"));
    assertFalse(Files.exists(target.resolve("stale.html")));
    assertTrue(r.getFailures().isEmpty(), r.getFailures().toString());
  }

  @Test
  void reportsWhatItCouldNotDeleteAndCarriesOn() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "locked/new.html", "new");
    write(staged, "other.html", "other");
    write(target, "locked/stale.html", "old");
    File locked = target.resolve("locked").toFile();
    // a folder that can't be written to: its files can't be deleted or added (not so for root, or on Windows)
    assumeTrue(locked.setWritable(false) && !Files.isWritable(target.resolve("locked")));
    try {
      OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

      assertEquals("other", read(target, "other.html"));
      assertEquals(2, r.getFailures().size(), r.getFailures().toString());
      assertTrue(r.getFailures().get(0).contains("stale.html"), r.getFailures().toString());
      assertTrue(r.getFailures().get(1).contains("new.html"), r.getFailures().toString());
      assertEquals(1, r.getWritten());
    } finally {
      locked.setWritable(true);
    }
  }

  @Test
  void movesNewAndChangedFilesOutOfTheStagingFolder() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "new.html", "new");
    write(staged, "same.html", "same");
    write(target, "same.html", "same");

    OutputFolderSync.sync(staged, target);

    assertFalse(Files.exists(staged.resolve("new.html")));
    assertTrue(Files.exists(staged.resolve("same.html")));
  }

  @Test
  void aStagingFolderItCantReadFailsTheSyncBeforeAnythingIsDeleted() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "index.html", "new");
    write(staged, "sub/page.html", "new");
    write(target, "index.html", "old");
    write(target, "sub/page.html", "old");
    File sub = staged.resolve("sub").toFile();
    assumeTrue(sub.setReadable(false, false) && !Files.isReadable(staged.resolve("sub")));
    try {
      assertThrows(IOException.class, () -> OutputFolderSync.sync(staged, target));
    } finally {
      sub.setReadable(true, false);
    }
    assertEquals("old", read(target, "index.html"));
    assertEquals("old", read(target, "sub/page.html"));
  }

  @Test
  void aLinkInTheOutputFolderIsDeletedButNotWhatItPointsTo() throws IOException {
    Path staged = staged();
    Path target = target();
    write(staged, "index.html", "new");
    write(dir, "elsewhere/keep.html", "keep");
    Files.createDirectories(target);
    try {
      Files.createSymbolicLink(target.resolve("linked"), dir.resolve("elsewhere"));
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "can't make a link here");
    }

    OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

    assertEquals(Set.of("index.html"), contents(target));
    assertEquals("keep", read(dir, "elsewhere/keep.html"));
    assertTrue(r.getFailures().isEmpty(), r.getFailures().toString());
  }

  @Test
  void acrossFileSystemsAChangedFileIsReplacedByARename() throws IOException {
    Path shm = Paths.get("/dev/shm");
    assumeTrue(Files.isDirectory(shm) && Files.isWritable(shm));
    Path staged = Files.createTempDirectory(shm, "sync-test");
    try {
      assumeTrue(!Files.getFileStore(staged).equals(Files.getFileStore(dir)), "the same file system");
      Path target = target();
      write(staged, "page.html", "new");
      write(staged, "new.html", "new");
      write(target, "page.html", "old");

      OutputFolderSync.Result r = OutputFolderSync.sync(staged, target);

      assertEquals(Set.of("new.html", "page.html"), contents(target));
      assertEquals("new", read(target, "page.html"));
      assertEquals(2, r.getWritten());
      assertFalse(Files.exists(staged.resolve("page.html")));
    } finally {
      try (Stream<Path> s = Files.walk(staged)) {
        for (Path p : s.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
          Files.deleteIfExists(p);
        }
      }
    }
  }

  @Test
  void aTargetThatHoldsTheStagingFolderIsLeftAlone() throws IOException {
    Path staged = staged();
    write(staged, "index.html", "new");
    write(dir, "input/page.md", "source");

    assertThrows(IOException.class, () -> OutputFolderSync.sync(staged, dir));
    assertThrows(IOException.class, () -> OutputFolderSync.sync(dir, staged.resolve("out")));

    assertEquals("source", read(dir, "input/page.md"));
    assertEquals("new", read(staged, "index.html"));
  }

  @Test
  void publishingCopiesTheGivenFilesAndDeletesNothing() throws IOException {
    Path staged = staged();
    write(staged, "en/page.html", "new page");
    write(staged, "en/other.html", "not rendered again");
    writeOld(staged, "en/same.html", "same");
    write(staged, "new/deep/page.html", "a new page");
    write(target(), "en/page.html", "old page");
    write(target(), "en/other.html", "other");
    writeOld(target(), "en/same.html", "same");
    write(target(), "package.tgz", "the last build's");
    write(target(), "qa.json", "{\"errs\": 0}");

    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(),
        List.of(staged.resolve("en/page.html"), staged.resolve("en/same.html"), Path.of("new/deep/page.html"),
            staged.resolve("gone.html"), dir.resolve("elsewhere.html"), staged.resolve("en")), null, null);

    assertTrue(r.getFailures().isEmpty(), r.getFailures().toString());
    assertEquals(2, r.getWritten());
    assertEquals(1, r.getUnchanged());
    assertEquals(0, r.getDeleted());
    assertEquals("new page", read(target(), "en/page.html"));
    assertEquals("a new page", read(target(), "new/deep/page.html"));
    // a file that wasn't named, what the build hasn't made yet, and the unchanged file are as they were
    assertEquals("other", read(target(), "en/other.html"));
    assertEquals("the last build's", read(target(), "package.tgz"));
    assertEquals("{\"errs\": 0}", read(target(), "qa.json"));
    assertEquals(OLD, Files.getLastModifiedTime(target().resolve("en/same.html")));
    // the staging folder still has everything, for the rest of the build
    assertEquals(Set.of("en/", "en/page.html", "en/other.html", "en/same.html", "new/", "new/deep/", "new/deep/page.html"), contents(staged));
    try (Stream<Path> st = Files.walk(target())) {
      assertFalse(st.anyMatch(p -> p.getFileName().toString().startsWith(".sync-")));
    }
  }

  @Test
  void publishingEverythingLeavesQaJsonForTheSync() throws IOException {
    Path staged = staged();
    write(staged, "index.html", "home");
    write(staged, "en/page.html", "page");
    write(staged, "qa.json", "{\"errs\": 1}");
    write(staged, "en/qa.json", "a page's data");
    write(target(), "qa.json", "{\"errs\": 0}");
    write(target(), "stale.html", "stale");

    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), null, null, null);

    assertEquals(3, r.getWritten());
    assertEquals(Set.of("index.html", "en/", "en/page.html", "en/qa.json", "qa.json", "stale.html"), contents(target()));
    assertEquals("{\"errs\": 0}", read(target(), "qa.json"));
    assertEquals("{\"errs\": 1}", read(staged, "qa.json"));

    // and the sync that follows brings the rest
    OutputFolderSync.sync(staged, target(), true);
    assertEquals(Set.of("index.html", "en/", "en/page.html", "en/qa.json", "qa.json"), contents(target()));
    assertEquals("{\"errs\": 1}", read(target(), "qa.json"));
  }

  @Test
  void publishingReplacesAFileInOneRenameAndCreatesTheOutputFolder() throws IOException {
    Path staged = staged();
    write(staged, "page.html", "new");
    OutputFolderSync.publish(staged, target(), null, null, null);
    assertEquals("new", read(target(), "page.html"));

    // what has the old file open goes on reading the old file: the new one is another file under the same name
    write(staged, "page.html", "newer");
    Object before = Files.readAttributes(target().resolve("page.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey();
    try (InputStream in = Files.newInputStream(target().resolve("page.html"))) {
      OutputFolderSync.publish(staged, target(), List.of(staged.resolve("page.html")), null, null);
      assertEquals("new", new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
    assertEquals("newer", read(target(), "page.html"));
    if (before != null) {
      assertFalse(before.equals(Files.readAttributes(target().resolve("page.html"), java.nio.file.attribute.BasicFileAttributes.class).fileKey()));
    }
  }

  @Test
  void publishingReportsWhatItCouldNotWriteAndCarriesOn() throws IOException {
    Path staged = staged();
    write(staged, "a/page.html", "a folder that was a file");
    write(staged, "b.html", "b");
    write(target(), "a", "a file");

    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), null, null, null);

    assertEquals(1, r.getWritten());
    assertEquals(1, r.getFailures().size());
    assertTrue(r.getFailures().get(0).startsWith("Unable to update a" + File.separator + "page.html"), r.getFailures().get(0));
    assertEquals("b", read(target(), "b.html"));
    assertEquals("a file", read(target(), "a"));
    // the sync at the end of the build sorts it out
    assertTrue(OutputFolderSync.sync(staged, target(), true).getFailures().isEmpty());
    assertEquals("a folder that was a file", read(target(), "a/page.html"));
  }

  @Test
  void publishingFindsTheGivenFilesThroughALinkedFolder() throws IOException {
    Path real = Files.createDirectories(dir.resolve("elsewhere").resolve("temp"));
    Path linked = dir.resolve("temp");
    try {
      Files.createSymbolicLink(linked, real);
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "can't make a link here");
    }
    // the build names the files it wrote by the path it was given, not where the link leads
    Path staged = Files.createDirectories(linked.resolve("staged"));
    write(staged, "page.html", "new");

    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), List.of(staged.resolve("page.html")), null, null);

    assertEquals(1, r.getWritten());
    assertEquals("new", read(target(), "page.html"));
  }

  @Test
  void publishingLeavesOutTheFilesItIsToldToByName() throws IOException {
    Path staged = staged();
    write(staged, "en/page.html", "page");
    write(staged, "en/page.ai.html", "for the AI step");
    write(staged, "page.ai.html", "for the AI step");

    OutputFolderSync.Result all = OutputFolderSync.publish(staged, target(), null, n -> n.endsWith(".ai.html"), null);
    OutputFolderSync.Result named = OutputFolderSync.publish(staged, target(),
        List.of(staged.resolve("en/page.ai.html"), staged.resolve("en/page.html")), n -> n.endsWith(".ai.html"), null);

    assertEquals(1, all.getWritten());
    assertEquals(0, named.getWritten());
    assertEquals(1, named.getUnchanged());
    assertEquals(Set.of("en/", "en/page.html"), contents(target()));
  }

  @Test
  void publishingLeavesAPageThatIsAsItWasLastOfferedAsTheBuildMadeItSince() throws IOException {
    Path staged = staged();
    Map<String, String> offered = new HashMap<>();
    write(staged, "a.html", "a, as Jekyll wrote it");
    write(staged, "b.html", "b, as Jekyll wrote it");
    assertEquals(2, OutputFolderSync.publish(staged, target(), null, null, offered).getWritten());
    // the build goes on: the html checks change the pages, and the sync puts them in the output folder
    write(target(), "a.html", "a, html checked");
    write(target(), "b.html", "b, html checked");

    // the next build: Jekyll writes every page again, and one of them is different
    write(staged, "a.html", "a, as Jekyll wrote it");
    write(staged, "b.html", "b, changed");
    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), null, null, offered);

    assertEquals(1, r.getWritten());
    assertEquals(1, r.getUnchanged());
    assertEquals("a, html checked", read(target(), "a.html"));
    assertEquals("b, changed", read(target(), "b.html"));

    // and when it is back as it was two builds ago, it is put there again
    write(target(), "b.html", "b, changed and html checked");
    write(staged, "b.html", "b, as Jekyll wrote it");
    assertEquals(1, OutputFolderSync.publish(staged, target(), List.of(staged.resolve("b.html")), null, offered).getWritten());
    assertEquals("b, as Jekyll wrote it", read(target(), "b.html"));
  }

  @Test
  void publishingLeavesAFileTheBuildWroteSinceForTheSync() throws IOException {
    Path staged = staged();
    Map<String, String> offered = new HashMap<>();
    writeOld(staged, "a.html", "a, as Jekyll wrote it");
    // the package step has written the last build's package again
    write(staged, "package.tgz", "this build's package");
    write(target(), "package.tgz", "the last build's package");
    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), null, null, offered, null, FileTime.from(Instant.parse("2021-01-01T00:00:00Z")));

    assertEquals(1, r.getWritten());
    assertEquals("a, as Jekyll wrote it", read(target(), "a.html"));
    assertEquals("the last build's package", read(target(), "package.tgz"));
    assertEquals(Set.of("a.html"), offered.keySet());
  }

  @Test
  void publishingLeavesWhatTheLastBuildLeftForTheSyncWhenToldTo() throws Exception {
    Path staged = staged();
    byte[] last = new byte[8 << 20];
    Arrays.fill(last, (byte) 'o');
    byte[] next = new byte[8 << 20];
    Arrays.fill(next, (byte) 'n');
    for (int i = 0; i < 10; i++) {
      // the last build's package, left in the staging folder, as in the target
      Files.write(staged.resolve("package.tgz"), last);
      Files.createDirectories(target());
      Files.write(target().resolve("package.tgz"), last);
      // this build starts (file times are a tick coarser than the clock)
      Thread.sleep(60);
      FileTime since = FileTime.from(Instant.now().minusMillis(30));
      write(staged, "a.html", "a, as Jekyll wrote it for build " + i);
      FileTime asOf = FileTime.from(Instant.now());
      Map<String, String> offered = new HashMap<>();
      OutputFolderSync.Result[] r = new OutputFolderSync.Result[1];
      Thread copy = new Thread(() -> {
        try {
          r[0] = OutputFolderSync.publish(staged, target(), null, null, offered, since, asOf);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
      copy.start();
      // (later than asOf, as the build's package step is)
      Thread.sleep(20);
      // the package step writes it again, a block at a time (a file being written over is empty, and may still have
      // its old time, until its first write)
      try (OutputStream out = Files.newOutputStream(staged.resolve("package.tgz"))) {
        for (int k = 0; k < 8; k++) {
          out.write(next, k << 20, 1 << 20);
        }
      }
      copy.join();
      assertTrue(Arrays.equals(last, Files.readAllBytes(target().resolve("package.tgz"))), "the package in the target was changed");
      assertEquals("a, as Jekyll wrote it for build " + i, read(target(), "a.html"));
      assertEquals(1, r[0].getWritten());
      assertEquals(Set.of("a.html"), offered.keySet());
    }
  }

  @Test
  void publishingSinceABuildStartedTakesAFileCopiedWithItsSourcesTime() throws IOException {
    Path staged = staged();
    // Jekyll copies a static file (a changed stylesheet) with the time of its source, from before the build
    writeOld(staged, "assets/site.css", "new css");
    write(target(), "assets/site.css", "old css");
    FileTime since = FileTime.from(Instant.now().minusSeconds(60));
    OutputFolderSync.publish(staged, target(), null, null, new HashMap<>(), since, null);
    assertEquals("new css", read(target(), "assets/site.css"));
  }

  @Test
  void aCheckedPagePutBackInTheStagingFolderIsntTakenForWhatJekyllWrote() throws IOException {
    Path staged = staged();
    Map<String, String> offered = new HashMap<>();
    write(staged, "a.html", "a, as Jekyll wrote it");
    OutputFolderSync.publish(staged, target(), null, null, offered);
    write(target(), "a.html", "a, html checked");

    // a page rebuild: Jekyll wrote it again for the template's check, and the checked page was put back
    write(staged, "a.html", "a, html checked");
    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), List.of(staged.resolve("a.html")), null, offered);
    assertEquals(0, r.getWritten());

    // the next build: Jekyll writes it as before, and the checked page stays
    write(staged, "a.html", "a, as Jekyll wrote it");
    r = OutputFolderSync.publish(staged, target(), null, null, offered);
    assertEquals(0, r.getWritten());
    assertEquals("a, html checked", read(target(), "a.html"));
  }

  @Test
  void aPageThatIsNoLongerInTheOutputFolderIsPublishedAgain() throws IOException {
    Path staged = staged();
    Map<String, String> offered = new HashMap<>();
    write(staged, "a.html", "a");
    OutputFolderSync.publish(staged, target(), null, null, offered);
    Files.delete(target().resolve("a.html"));

    assertEquals(1, OutputFolderSync.publish(staged, target(), null, null, offered).getWritten());
    assertEquals("a", read(target(), "a.html"));
  }

  @Test
  void aPageThatCouldNotBePublishedIsOfferedAgain() throws IOException {
    Path staged = staged();
    Map<String, String> offered = new HashMap<>();
    write(staged, "a/page.html", "page");
    write(target(), "a", "a file where the folder goes");
    assertEquals(1, OutputFolderSync.publish(staged, target(), null, null, offered).getFailures().size());
    Files.delete(target().resolve("a"));

    assertEquals(1, OutputFolderSync.publish(staged, target(), null, null, offered).getWritten());
    assertEquals("page", read(target(), "a/page.html"));
  }

  @Test
  void publishesWhereALinkedOutputFolderPoints() throws IOException {
    Path staged = staged();
    Path real = Files.createDirectories(dir.resolve("web-root"));
    write(staged, "new.html", "new");
    try {
      Files.createSymbolicLink(target(), real);
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "can't make a link here");
    }

    OutputFolderSync.Result r = OutputFolderSync.publish(staged, target(), null, null, null);

    assertTrue(Files.isSymbolicLink(target()));
    assertEquals(1, r.getWritten());
    assertEquals("new", read(real, "new.html"));
  }

  @Test
  void publishingIntoAFolderThatHoldsTheStagingFolderIsRefused() throws IOException {
    Path staged = Files.createDirectories(target().resolve("temp"));
    write(staged, "page.html", "new");
    assertThrows(IOException.class, () -> OutputFolderSync.publish(staged, target(), null, null, null));
    assertEquals(Set.of("temp/", "temp/page.html"), contents(target()));
  }
}
