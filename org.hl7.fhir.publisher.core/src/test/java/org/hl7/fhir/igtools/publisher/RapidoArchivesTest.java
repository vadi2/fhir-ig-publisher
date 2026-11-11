package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.parser.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The archives of the package step are kept from the last build only when that build recorded the same key for them
 * and the files are still what it wrote. Anything else, and they are written again.
 */
class RapidoArchivesTest {

  private static final FileTime THEN = FileTime.fromMillis(1_700_000_000_000L);

  @TempDir
  Path dir;
  private Path output;
  private Path aside;

  @BeforeEach
  void folders() throws Exception {
    output = Files.createDirectories(dir.resolve("output"));
    aside = dir.resolve("temp").resolve("_rapido").resolve("archives");
  }

  private RapidoArchives archives(JsonObject last) {
    return new RapidoArchives(output.toString(), aside.toString(), last);
  }

  private void write(String name, String content) throws Exception {
    Files.write(output.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    Files.setLastModifiedTime(output.resolve(name), THEN);
  }

  private String read(String name) throws Exception {
    return new String(Files.readAllBytes(output.resolve(name)), StandardCharsets.UTF_8);
  }

  /** what a build that wrote these archives saves, as the next build reads it from the state file */
  private JsonObject built(String key, String... names) throws Exception {
    RapidoArchives a = archives(null);
    for (String name : names) {
      write(name, "content of " + name);
    }
    a.written(key, names);
    return JsonParser.parseObject(JsonParser.compose(a.getRecords()));
  }

  /** the next build, up to where Jekyll has run and removed what it didn't write */
  private RapidoArchives afterJekyll(JsonObject last) throws Exception {
    RapidoArchives a = archives(last);
    a.beforeJekyll();
    try (var s = Files.list(output)) {
      for (Path p : (Iterable<Path>) s::iterator) {
        Files.delete(p);
      }
    }
    return a;
  }

  @Test
  void sameKeyKeepsTheFilesAsTheyWere() throws Exception {
    RapidoArchives a = afterJekyll(built("k1", "definitions.json.zip"));
    assertFalse(Files.exists(output.resolve("definitions.json.zip")), "moved aside while Jekyll runs");

    assertTrue(a.keep("k1", "definitions.json.zip"));
    assertEquals("content of definitions.json.zip", read("definitions.json.zip"));
    assertEquals(THEN, Files.getLastModifiedTime(output.resolve("definitions.json.zip")), "the same file, not a copy");
    a.finish(true);
    assertEquals("1 kept, 0 written again", a.summary());

    // and the build after that keeps them as well
    RapidoArchives next = afterJekyll(JsonParser.parseObject(JsonParser.compose(a.getRecords())));
    assertTrue(next.keep("k1", "definitions.json.zip"));
  }

  @Test
  void aGroupIsKeptTogether() throws Exception {
    RapidoArchives a = afterJekyll(built("k1", "expansions.json", "expansions.xml"));
    assertTrue(a.keep("k1", "expansions.json", "expansions.xml"));
    assertEquals("content of expansions.json", read("expansions.json"));
    assertEquals("content of expansions.xml", read("expansions.xml"));
  }

  @Test
  void anotherKeyWritesAgain() throws Exception {
    RapidoArchives a = afterJekyll(built("k1", "definitions.json.zip"));
    assertFalse(a.keep("k2", "definitions.json.zip"));
    assertFalse(Files.exists(output.resolve("definitions.json.zip")), "the old file must not come back");
    write("definitions.json.zip", "new content");
    a.written("k2", "definitions.json.zip");
    a.finish(true);
    assertEquals("new content", read("definitions.json.zip"));
    assertEquals("0 kept, 1 written again (definitions.json.zip: its content changed)", a.summary());
    assertEquals("k2", a.getRecords().getJsonObject("definitions.json.zip").asString("key"));
    assertFalse(Files.exists(aside.resolve("definitions.json.zip")), "what isn't kept is deleted");
  }

  @Test
  void noKeyWritesAgainAndRecordsNothing() throws Exception {
    RapidoArchives a = afterJekyll(built("k1", "definitions.json.zip"));
    assertFalse(a.keep(null, "definitions.json.zip"));
    write("definitions.json.zip", "new content");
    a.written(null, "definitions.json.zip");
    assertTrue(a.getRecords().getNames().isEmpty());
  }

  @Test
  void aCompleteBuildKeepsNothing() throws Exception {
    built("k1", "definitions.json.zip");
    RapidoArchives a = archives(null);
    a.beforeJekyll();
    assertTrue(Files.exists(output.resolve("definitions.json.zip")), "nothing to move aside");
    assertFalse(a.keep("k1", "definitions.json.zip"));
  }

  @Test
  void aMissingFileWritesAgain() throws Exception {
    JsonObject last = built("k1", "expansions.json", "expansions.xml");
    Files.delete(output.resolve("expansions.xml"));
    RapidoArchives a = afterJekyll(last);
    assertFalse(a.keep("k1", "expansions.json", "expansions.xml"));
    assertTrue(a.summary().contains("expansions.xml is missing"), a.summary());
    assertFalse(Files.exists(output.resolve("expansions.json")), "none of the group comes back");
  }

  @Test
  void aFileOfAnotherSizeWritesAgain() throws Exception {
    JsonObject last = built("k1", "definitions.json.zip");
    write("definitions.json.zip", "something longer than it was before");
    RapidoArchives a = afterJekyll(last);
    assertFalse(a.keep("k1", "definitions.json.zip"));
    assertTrue(a.summary().contains("isn't as the last build wrote it"), a.summary());
  }

  @Test
  void aFileWithOtherContentOfTheSameSizeWritesAgain() throws Exception {
    JsonObject last = built("k1", "definitions.json.zip");
    write("definitions.json.zip", "CONTENT OF definitions.json.zip");
    RapidoArchives a = afterJekyll(last);
    assertFalse(a.keep("k1", "definitions.json.zip"));
    assertTrue(a.summary().contains("isn't as the last build wrote it"), a.summary());
  }

  @Test
  void aFileTheLastBuildDidNotRecordWritesAgain() throws Exception {
    JsonObject last = built("k1", "validator.pack");
    write("validator-ig.pack", "a copy");
    RapidoArchives a = afterJekyll(last);
    assertFalse(a.keep("k1", "validator.pack", "validator-ig.pack"));
  }

  @Test
  void aFileThatIsNotWrittenIsNotRecorded() throws Exception {
    RapidoArchives a = archives(null);
    write("expansions.json", "x");
    a.written("k1", "expansions.json", "expansions.xml");
    assertNull(a.getRecords().getJsonObject("expansions.json"));
  }

  @Test
  void whatIsNoLongerMadeIsDeleted() throws Exception {
    RapidoArchives a = afterJekyll(built("k1", "examples.json.zip"));
    a.finish(true);
    assertFalse(Files.exists(output.resolve("examples.json.zip")));
    assertFalse(Files.exists(aside.resolve("examples.json.zip")));
  }

  @Test
  void ifThePackageStepDoesNotRunTheFilesGoBack() throws Exception {
    RapidoArchives a = afterJekyll(built("k1", "examples.json.zip"));
    a.finish(false);
    assertEquals("content of examples.json.zip", read("examples.json.zip"));
    assertTrue(a.getRecords().getNames().isEmpty(), "but the next build writes them again");
  }

  @Test
  void aNameFromTheStateFileThatIsAPathIsLeftAlone() throws Exception {
    Path other = dir.resolve("other.txt");
    Files.write(other, "x".getBytes(StandardCharsets.UTF_8));
    JsonObject last = built("k1", "a.zip");
    JsonObject files = last.getJsonObject("a.zip").getJsonObject("files");
    files.add("../other.txt", files.getJsonObject("a.zip"));
    afterJekyll(last).finish(true);
    assertTrue(Files.exists(other));
  }

  @Test
  void theKeyDependsOnTheKindOfArchive() {
    assertEquals(RapidoArchives.key("a").add("x").finish(), RapidoArchives.key("a").add("x").finish());
    assertNotEquals(RapidoArchives.key("a").add("x").finish(), RapidoArchives.key("b").add("x").finish());
  }

  /** a build that wrote a zip and recorded what stands for its entries */
  private JsonObject builtZip(String key, String name) throws Exception {
    RapidoArchives a = archives(null);
    write(name, "content of " + name);
    a.written(key, new JsonObject().add("one.json", "t1"), name);
    return JsonParser.parseObject(JsonParser.compose(a.getRecords()));
  }

  @Test
  void aZipThatIsWrittenAgainHasTheZipOfTheLastBuildToGoOnFrom() throws Exception {
    RapidoArchives a = afterJekyll(builtZip("k1", "definitions.json.zip"));
    assertFalse(a.keep("k2", "definitions.json.zip"));
    RapidoArchives.Previous p = a.previous("definitions.json.zip");
    assertEquals(aside.resolve("definitions.json.zip"), p.getFile());
    assertEquals("t1", p.getEntries().asString("one.json"));
    assertNull(a.previous("other.zip"));

    // a build that kept it carries the entries on, for the build after it
    RapidoArchives kept = afterJekyll(builtZip("k1", "definitions.json.zip"));
    assertTrue(kept.keep("k1", "definitions.json.zip"));
    assertNull(kept.previous("definitions.json.zip"), "it is in the output folder again");
    RapidoArchives next = afterJekyll(JsonParser.parseObject(JsonParser.compose(kept.getRecords())));
    assertEquals("t1", next.previous("definitions.json.zip").getEntries().asString("one.json"));
  }

  @Test
  void aZipThatIsNotAsItWasWrittenIsNotGoneOnFrom() throws Exception {
    JsonObject last = builtZip("k1", "definitions.json.zip");
    write("definitions.json.zip", "CONTENT OF definitions.json.zip");
    assertNull(afterJekyll(last).previous("definitions.json.zip"));

    assertNull(archives(null).previous("definitions.json.zip"), "a complete build");
    // no record of its entries
    assertNull(afterJekyll(built("k1", "definitions.json.zip")).previous("definitions.json.zip"));
    RapidoArchives noKey = archives(null);
    write("a.zip", "x");
    noKey.written(null, new JsonObject().add("one.json", "t1"), "a.zip");
    assertTrue(noKey.getRecords().getNames().isEmpty());
  }

  @Test
  void aRecordThatIsNotOneIsLeftAlone() throws Exception {
    JsonObject last = built("k1", "a.zip");
    last.add("b.zip", "not a record");
    last.getJsonObject("a.zip").getJsonObject("files").getJsonObject("a.zip").remove("size");
    RapidoArchives a = afterJekyll(last);
    assertFalse(a.keep("k1", "a.zip"));
    assertFalse(a.keep("k1", "b.zip"));
  }
}
