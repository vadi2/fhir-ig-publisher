package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
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

  @Test
  void findsWhatOnlyAnEarlierBuildWrote() throws Exception {
    Path kept = write("pages/Patient-a.html", "a");
    Path stale = write("pages/Patient-old.html", "old");
    Path rewritten = write("Patient-b.json", "b");
    Path state = write("_rapido/state.json", "{}");
    tick();
    StaleTempFiles st = tracker();
    st.mark();
    tick();
    Path fresh = write("pages/Patient-new.html", "new");
    Files.write(rewritten, "b2".getBytes(StandardCharsets.UTF_8));

    // the publisher keeps the outputs it claims in lower case
    List<Path> found = st.find(Arrays.asList(kept.toString().toLowerCase()));
    assertEquals(Collections.singletonList(stale.toAbsolutePath().normalize()), found);

    st.delete(found);
    assertFalse(Files.exists(stale));
    assertTrue(Files.exists(kept));
    assertTrue(Files.exists(fresh));
    assertTrue(Files.exists(rewritten));
    assertTrue(Files.exists(state));
    assertTrue(Files.exists(temp.resolve("_rapido").resolve("build-start")));
    assertTrue(Files.isDirectory(temp.resolve("pages")));
  }

  @Test
  void aFileWrittenAgainWithTheSameContentCounts() throws Exception {
    Path p = write("a.html", "same");
    tick();
    StaleTempFiles st = tracker();
    st.mark();
    tick();
    Files.write(p, "same".getBytes(StandardCharsets.UTF_8));
    assertTrue(st.find(Collections.emptyList()).isEmpty());
  }

  @Test
  void nothingIsStaleInAFolderTheBuildWroteFromScratch() throws Exception {
    StaleTempFiles st = tracker();
    st.mark();
    tick();
    write("a.html", "a");
    write("sub/b.html", "b");
    assertTrue(st.find(Collections.emptyList()).isEmpty());
  }

  @Test
  void aSecondBuildMarksAgain() throws Exception {
    StaleTempFiles st = tracker();
    st.mark();
    tick();
    Path first = write("first.html", "1");
    tick();
    st.mark();
    tick();
    Path second = write("second.html", "2");
    assertEquals(Collections.singletonList(first.toAbsolutePath().normalize()), st.find(Collections.emptyList()));
    assertTrue(st.find(Collections.singletonList(first.toString())).isEmpty());
    assertTrue(Files.exists(second));
  }
}
