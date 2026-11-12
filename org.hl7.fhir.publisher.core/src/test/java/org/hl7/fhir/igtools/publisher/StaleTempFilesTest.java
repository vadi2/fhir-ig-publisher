package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaleTempFilesTest {

  @TempDir
  Path temp;

  private Path write(String name, String content) throws IOException {
    Path p = temp.resolve(name);
    Files.createDirectories(p.getParent());
    Files.write(p, content.getBytes(StandardCharsets.UTF_8));
    return p;
  }

  /** the file system's clock for status-change times is coarse: let it move on */
  private static void tick() throws InterruptedException {
    Thread.sleep(50);
  }

  private StaleTempFiles tracker() {
    return new StaleTempFiles(temp.toString(), Collections.singletonList(temp.resolve("_rapido").toString()));
  }

  /**
   * Writes a file whose name java can't write, by the shell (the name has octal escapes for its bytes). False if the
   * file system refuses the name (e.g. on macOS, a name that isn't UTF-8)
   */
  private static boolean writeByShell(Path folder, String name) throws Exception {
    return new ProcessBuilder("sh", "-c", "printf s > \"$(printf '" + name + "')\"").directory(folder.toFile())
        .redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor() == 0;
  }

  private static List<String> names(Path... paths) {
    String[] res = new String[paths.length];
    for (int i = 0; i < paths.length; i++) {
      // the publisher keeps the outputs it claims in lower case
      res[i] = paths[i].toString().toLowerCase();
    }
    return Arrays.asList(res);
  }

  @Test
  void findsWhatOnlyTheLastBuildClaimed() throws Exception {
    // the last build
    Path kept = write("pages/Patient-a.html", "a");
    Path stale = write("pages/Patient-old.html", "old");
    Path rewritten = write("Patient-b.json", "b");
    Path unclaimed = write(".jekyll-cache/x", "x");
    Path state = write("_rapido/state.json", "{}");
    // claimed by the render that wrote it; the next one that uses it doesn't write or claim it again
    Path image = write("tbl_bck0123.png", "png");
    StaleTempFiles st = tracker();
    st.save(names(kept, stale, rewritten, image));
    tick();

    // this build: claims kept without writing it, writes rewritten without claiming it
    st = tracker();
    st.mark();
    tick();
    Path fresh = write("pages/Patient-new.html", "new");
    Files.write(rewritten, "b2".getBytes(StandardCharsets.UTF_8));

    List<Path> found = st.find(names(kept, fresh));
    assertEquals(Collections.singletonList(stale.toAbsolutePath().normalize()), found);

    st.delete(found);
    assertFalse(Files.exists(stale));
    for (Path p : Arrays.asList(kept, fresh, rewritten, unclaimed, state, image)) {
      assertTrue(Files.exists(p), p.toString());
    }
    assertTrue(Files.isDirectory(temp.resolve("pages")));
  }

  @Test
  void aFileWrittenAgainWithTheSameContentIsKept() throws Exception {
    Path p = write("a.html", "same");
    StaleTempFiles st = tracker();
    st.save(names(p));
    tick();
    st.mark();
    tick();
    Files.write(p, "same".getBytes(StandardCharsets.UTF_8));
    assertTrue(st.find(Collections.emptyList()).isEmpty());
  }

  @Test
  void nothingIsStaleWithoutWhatTheLastBuildClaimed() throws Exception {
    Path p = write("a.html", "a");
    StaleTempFiles st = tracker();
    tick();
    st.mark();
    assertFalse(st.hasClaims());
    assertTrue(st.find(Collections.emptyList()).isEmpty());
    st.save(names(p));
    assertTrue(st.hasClaims());
    assertEquals(Collections.singletonList(p.toAbsolutePath().normalize()), st.find(Collections.emptyList()));
  }

  @Test
  void nothingIsStaleWithoutAMark() throws Exception {
    Path p = write("a.html", "a");
    StaleTempFiles st = tracker();
    st.save(names(p));
    assertTrue(st.find(Collections.emptyList()).isEmpty());
  }

  @Test
  void eachBuildComparesWithTheOneBefore() throws Exception {
    Path first = write("first.html", "1");
    StaleTempFiles st = tracker();
    st.save(names(first));
    tick();
    st.mark();
    tick();
    Path second = write("second.html", "2");
    assertEquals(Collections.singletonList(first.toAbsolutePath().normalize()), st.find(names(second)));
    st.save(names(first, second));
    tick();
    st.mark();
    tick();
    assertEquals(Arrays.asList(first.toAbsolutePath().normalize(), second.toAbsolutePath().normalize()), st.find(Collections.emptyList()));
    assertTrue(st.find(names(first, second)).isEmpty());
  }

  @Test
  void theTemplatesWritesAreDeletedBeforeItRunsAgain() throws Exception {
    // the last build: written before the template's onGenerate, then by it
    Path before = write("pages/_includes/artifacts.xml", "a");
    StaleTempFiles st = tracker();
    st.markTemplate();
    Path svg = write("pages/_includes/sample.svg", "svg");
    Path again = write("pages/_includes/other.svg", "svg");
    st.saveTemplateWrites();
    tick();

    // a complete build doesn't need them deleted (and has no mark)
    assertTrue(tracker().findTemplateLeftovers().isEmpty());

    // a differential build that rewrites one of them before its onGenerate
    st = tracker();
    st.mark();
    tick();
    Files.write(again, "svg2".getBytes(StandardCharsets.UTF_8));
    List<Path> left = st.findTemplateLeftovers();
    assertEquals(Collections.singletonList(svg.toAbsolutePath().normalize()), left);
    st.delete(left);
    assertTrue(Files.exists(before));

    // and what its onGenerate writes is what the next build deletes
    st.markTemplate();
    Path svg2 = write("pages/_includes/sample.svg", "svg");
    st.saveTemplateWrites();
    tick();
    st = tracker();
    st.mark();
    assertEquals(Collections.singletonList(svg2.toAbsolutePath().normalize()), st.findTemplateLeftovers());
  }

  /**
   * A claimed name that isn't a valid Path (any non-ASCII name under a POSIX locale: run the tests with LC_ALL=POSIX to
   * see it) is the file java.io wrote for it, not an InvalidPathException that stops the build
   */
  @Test
  void aClaimedNameThatIsNoPathIsTheFileJavaIoWroteForIt() throws Exception {
    File pages = temp.resolve("pages").toFile();
    pages.mkdirs();
    File cyrillic = new File(pages, "\u0441\u0442\u0440.html");
    try (OutputStream o = new FileOutputStream(cyrillic)) {
      o.write('a');
    }
    Path old = write("pages/old.html", "old");
    StaleTempFiles st = tracker();
    st.save(Arrays.asList(cyrillic.getAbsolutePath(), old.toString()));
    tick();

    st = tracker();
    st.mark();
    tick();
    assertEquals(Collections.singletonList(old.toAbsolutePath().normalize()), st.find(Collections.singletonList(cyrillic.getAbsolutePath())));
    // and when this build doesn't claim it, it's found
    List<Path> found = st.find(Collections.emptyList());
    assertEquals(2, found.size());
    st.delete(found);
    assertFalse(cyrillic.exists());

    assertNull(StaleTempFiles.path("a\0b.html"));
  }

  /**
   * The template's onGenerate script writes a file whose name isn't in the file system encoding (any non-ASCII name
   * under a POSIX locale; here, under any locale, a name that isn't UTF-8 either). The next build used to fail before
   * the script ran again ("Malformed input or input contains unmappable characters")
   */
  @Test
  void theTemplatesWritesAreFoundWhateverTheirNames() throws Exception {
    assumeTrue(tracker().isSupported());
    StaleTempFiles st = tracker();
    st.markTemplate();
    Files.createDirectories(temp.resolve("pages"));
    assumeTrue(writeByShell(temp, "pages/\\321\\201\\377.svg"), "the file system takes only UTF-8 names");
    st.saveTemplateWrites();
    tick();

    st = tracker();
    st.mark();
    List<Path> left = st.findTemplateLeftovers();
    assertEquals(1, left.size());
    st.delete(left);
    try (Stream<Path> files = Files.list(temp.resolve("pages"))) {
      assertEquals(0, files.count());
    }
  }

  /**
   * The temp folder moved between builds (e.g. the IG's folder was renamed): what the template wrote is still found,
   * whatever the names
   */
  @Test
  void theTemplatesWritesAreFoundAfterTheTempFolderMoved() throws Exception {
    Path a = Files.createDirectories(temp.resolve("a").resolve("pages")).getParent();
    StaleTempFiles st = new StaleTempFiles(a.toString(), Collections.singletonList(a.resolve("_rapido").toString()));
    assumeTrue(st.isSupported());
    st.markTemplate();
    assumeTrue(writeByShell(a, "pages/\\321\\201\\377.svg"), "the file system takes only UTF-8 names");
    st.saveTemplateWrites();
    tick();

    Path b = Files.move(a, temp.resolve("b"));
    st = new StaleTempFiles(b.toString(), Collections.singletonList(b.resolve("_rapido").toString()));
    st.mark();
    assertEquals(1, st.findTemplateLeftovers().size());
  }

  /** a line of the record of what the template wrote that names no file in the temp folder is counted, for the log */
  @Test
  void aRecordOfTheTemplatesWritesThatNamesNoFileIsCounted() throws Exception {
    Path svg = write("pages/_includes/a.svg", "svg");
    write("_rapido/template-written.txt", "pages/_includes/a.svg\n%zz\n../outside.svg\n");
    tick();

    StaleTempFiles st = tracker();
    st.mark();
    assertEquals(Collections.singletonList(svg.toAbsolutePath().normalize()), st.findTemplateLeftovers());
    assertEquals(2, st.unreadableTemplateWrites());
  }

  /**
   * Without the file system encoding (the property is missing, or names no charset), a claimed name that isn't a
   * valid Path still names the file java.io wrote for it (it used to name none: the claim was dropped, and a file this
   * build claims again was taken for stale)
   */
  @Test
  void aClaimedNameIsAFileWithoutTheEncodingProperty() throws Exception {
    Path pages = temp.resolve("pages").toAbsolutePath().normalize();
    // a lone surrogate is in no encoding: what can't be encoded is '?'
    String name = pages + File.separator + "\uD800.html";
    String jnu = System.getProperty("sun.jnu.encoding");
    try {
      for (String cs : new String[] {null, "x-no-such-charset"}) {
        if (cs == null) {
          System.clearProperty("sun.jnu.encoding");
        } else {
          System.setProperty("sun.jnu.encoding", cs);
        }
        assertEquals(pages.resolve("?.html"), StaleTempFiles.path(name), "with sun.jnu.encoding " + cs);
      }
    } finally {
      System.setProperty("sun.jnu.encoding", jnu);
    }
  }

  /** a claimed name that names no file (it has a NUL) is counted, for the log */
  @Test
  void aClaimedNameThatNamesNoFileIsCounted() throws Exception {
    Path p = write("pages/a.html", "a");
    StaleTempFiles st = tracker();
    st.save(Arrays.asList(p.toString(), p.getParent() + "/a\0b.html"));
    assertEquals(Collections.singleton(p.getParent() + "/a\0b.html"), st.unnamedClaims());
  }

  /**
   * The last build ran with another file system encoding (another locale): the names it claimed may not be the names
   * this one reads, so what's stale can't be told, and the build has to be a complete one
   */
  @Test
  void aBuildUnderAnotherFileSystemEncodingHasNoClaims() throws Exception {
    assumeTrue(tracker().isSupported());
    Path pages = Files.createDirectories(temp.resolve("pages"));
    assumeTrue(writeByShell(temp, "pages/\\321\\201.html"), "no shell");
    Path page;
    try (Stream<Path> files = Files.list(pages)) {
      page = files.findFirst().get().toAbsolutePath().normalize();
    }
    String jnu = System.getProperty("sun.jnu.encoding");
    ProcessBuilder pb = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(), "-cp",
        System.getProperty("java.class.path"), SaveClaims.class.getName(), temp.toString());
    pb.environment().put("LC_ALL", "UTF-8".equals(jnu) ? "POSIX" : "C.UTF-8");
    pb.redirectError(ProcessBuilder.Redirect.DISCARD);
    Process proc = pb.start();
    String other = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(0, proc.waitFor());
    assumeTrue(!other.equals(jnu), "the other locale has the same file system encoding");
    tick();

    // this build doesn't claim the page: either it's found, or the build is a complete one
    StaleTempFiles st = tracker();
    st.mark();
    assertTrue(!st.hasClaims() || st.find(Collections.emptyList()).contains(page));
  }

  /** the last build, under another locale: claims what's in the temp folder's pages folder */
  static class SaveClaims {
    public static void main(String[] args) throws IOException {
      Path temp = Paths.get(args[0]);
      List<String> claimed = new ArrayList<>();
      try (Stream<Path> files = Files.list(temp.resolve("pages"))) {
        files.forEach(p -> claimed.add(p.toString()));
      }
      new StaleTempFiles(temp.toString(), Collections.singletonList(temp.resolve("_rapido").toString())).save(claimed);
      System.out.print(System.getProperty("sun.jnu.encoding"));
    }
  }

  /** a file system without status-change times (no 'unix' view: Windows, or here a zip file system) isn't supported */
  @Test
  void aFileSystemWithoutStatusChangeTimesIsntSupported() throws Exception {
    try (FileSystem zip = FileSystems.newFileSystem(temp.resolve("temp.zip"), Map.of("create", "true"))) {
      Path t = zip.getPath("/temp");
      assertFalse(new StaleTempFiles(t, Collections.singletonList(t.resolve("_rapido"))).isSupported());
    }
    assertTrue(tracker().isSupported());
  }

  /** a temp folder that can't be made is the build's error, not a file system without status-change times */
  @Test
  void aTempFolderThatCantBeMadeIsAnError() throws Exception {
    Path file = write("not-a-folder", "x");

    assertThrows(IOException.class, () -> new StaleTempFiles(file.resolve("temp"), Collections.emptyList()).isSupported());
  }

  /**
   * A claimed file the build wrote again, with its modification time set back to before the build started (as a copy
   * that keeps the source's time does): it's this build's, by its status-change time
   */
  @Test
  void aFileWrittenAfterTheMarkIsKeptWhateverItsModificationTime() throws Exception {
    assumeTrue(tracker().isSupported());
    Path p = write("pages/copied.html", "old");
    StaleTempFiles st = tracker();
    st.save(names(p));
    tick();
    st = tracker();
    st.mark();
    FileTime before = Files.getLastModifiedTime(temp.resolve("_rapido/build-start"));
    tick();
    write("pages/copied.html", "new");
    Files.setLastModifiedTime(p, FileTime.fromMillis(before.toMillis() - 60000));
    assertTrue(st.find(Collections.emptyList()).isEmpty());
  }

  /** The temp folder is a link (e.g. to a faster disk): what's in it is found by the paths the publisher uses */
  @Test
  void aLinkedTempFolderIsWalked() throws Exception {
    Path real = Files.createDirectories(temp.resolve("real"));
    Path link;
    try {
      link = Files.createSymbolicLink(temp.resolve("temp"), real);
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "no symbolic links: " + e);
      return;
    }
    Path stale = link.resolve("pages").resolve("old.html");
    Files.createDirectories(stale.getParent());
    Files.write(stale, new byte[] {'o'});
    List<String> skip = Collections.singletonList(link.resolve("_rapido").toString());
    StaleTempFiles st = new StaleTempFiles(link.toString(), skip);
    st.save(names(stale));
    tick();

    st = new StaleTempFiles(link.toString(), skip);
    st.mark();
    assertEquals(Collections.singletonList(stale), st.find(Collections.emptyList()));

    assertTrue(st.markTemplate());
    Path svg = Files.write(link.resolve("sample.svg"), new byte[] {'s'});
    st.saveTemplateWrites();
    tick();
    st = new StaleTempFiles(link.toString(), skip);
    st.mark();
    assertEquals(Collections.singletonList(svg), st.findTemplateLeftovers());
  }

  /**
   * The template's onGenerate script changes a file that was there (here, one a build wrote): it's not the script's. A
   * differential build that reuses the file doesn't delete it before the script runs (the script wouldn't write it)
   */
  @Test
  void aFileTheTemplateOnlyChangedIsntItsWrite() throws Exception {
    Path page = write("pages/Patient-a.html", "a");
    StaleTempFiles st = tracker();
    st.markTemplate();
    Path script = write("pages/_includes/sample.svg", "s");
    st.saveTemplateWrites();
    tick();

    // the next build writes both before the script runs; the script changes the page in place, and writes its file again
    st = tracker();
    st.mark();
    tick();
    write("pages/Patient-a.html", "a");
    write("pages/_includes/sample.svg", "s");
    st.markTemplate();
    write("pages/Patient-a.html", "a, changed by the script");
    write("pages/_includes/sample.svg", "s again");
    st.saveTemplateWrites();
    tick();

    // a build that reuses the page: only the script's file is deleted before the script runs
    st = tracker();
    st.mark();
    assertEquals(Collections.singletonList(script.toAbsolutePath().normalize()), st.findTemplateLeftovers());
    assertTrue(Files.exists(page));
  }

  /**
   * Where the status-change times don't move (a file system whose clock is too coarse), what the template's onGenerate
   * writes can't be told from what was there: it's not recorded, so the next build deletes nothing before the script runs
   */
  @Test
  void whenTheClockDoesntMoveTheTemplatesWritesArentRecorded() throws Exception {
    StaleTempFiles st = tracker();
    st.markTemplate();
    write("pages/_includes/old.svg", "s");
    st.saveTemplateWrites();
    tick();

    st = tracker();
    st.probes = 0;
    st.mark();
    assertFalse(st.markTemplate());
    write("pages/_includes/sample.svg", "s");
    st.saveTemplateWrites();
    tick();

    st = tracker();
    st.mark();
    assertTrue(st.findTemplateLeftovers().isEmpty());
  }
}
