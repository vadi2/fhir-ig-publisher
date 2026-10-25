package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaleTempFilesTest {

  @TempDir
  Path temp;

  @BeforeEach
  void supported() {
    assumeTrue(StaleTempFiles.isSupported(temp.toString()), "the file system has no status-change times");
  }

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
    st.forget();
    assertFalse(st.hasClaims());
    assertTrue(st.find(Collections.emptyList()).isEmpty());
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
}
