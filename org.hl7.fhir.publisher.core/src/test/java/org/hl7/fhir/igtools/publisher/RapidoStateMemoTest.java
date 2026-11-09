package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

import org.hl7.fhir.utilities.json.model.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The Rapido state a watch build saved, kept for the next build only while the file is still what was written */
class RapidoStateMemoTest {

  @TempDir
  Path dir;

  private File file;
  private JsonObject state;
  private final RapidoTracker.StateMemo memo = new RapidoTracker.StateMemo();

  @BeforeEach
  void save() throws Exception {
    file = dir.resolve("state.json").toFile();
    state = new JsonObject();
    state.add("version", 3);
    state.add("files", new JsonObject().add("input/a \"b\".json", new JsonObject().add("oh", "h1")));
    RapidoTracker.writeState(state, file);
    memo.remember(file, state);
  }

  @Test
  void whatIsRecalledIsWhatTheFileHolds() throws Exception {
    JsonObject kept = memo.recall(file);

    assertEquals(json(RapidoTracker.readState(file)), json(kept));
  }

  @Test
  void itIsHandedOverOnce() throws Exception {
    // the build changes the state it's given: the next one reads the file
    assertNotNull(memo.recall(file));

    assertNull(memo.recall(file));
  }

  @Test
  void notOnceTheFileIsWrittenAgain() throws Exception {
    Files.writeString(file.toPath(), "{\"version\":2}");

    assertNull(memo.recall(file));
  }

  @Test
  void notOnceItsSizeChanged() throws Exception {
    FileTime modified = Files.getLastModifiedTime(file.toPath());
    try (java.io.RandomAccessFile f = new java.io.RandomAccessFile(file, "rw")) {
      f.setLength(f.length() - 1);
    }
    Files.setLastModifiedTime(file.toPath(), modified);

    assertNull(memo.recall(file));
  }

  @Test
  void notOnceTheFileIsTouched() throws Exception {
    Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(Files.getLastModifiedTime(file.toPath()).toMillis() - 10_000));

    assertNull(memo.recall(file));
  }

  @Test
  void notOnceAnotherFileTakesItsPlace() throws Exception {
    // the same size and modified time, but another file (as a sync tool or a copy from another worktree leaves it)
    Path other = dir.resolve("other.json");
    Files.copy(file.toPath(), other, StandardCopyOption.COPY_ATTRIBUTES);
    Files.writeString(other, Files.readString(file.toPath()).replace("h1", "h2"));
    Files.setLastModifiedTime(other, Files.getLastModifiedTime(file.toPath()));
    Files.move(other, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

    assertNull(memo.recall(file));
  }

  @Test
  void notForAnotherFile() throws Exception {
    Path other = dir.resolve("other.json");
    Files.copy(file.toPath(), other, StandardCopyOption.COPY_ATTRIBUTES);

    assertNull(memo.recall(other.toFile()));
  }

  /** where a file system has no file keys (Windows), it's the name that tells another state file apart */
  @Test
  void notForAnotherNameOfTheFile() throws Exception {
    Path link = dir.resolve("link.json");
    Files.createLink(link, file.toPath());

    assertNull(memo.recall(link.toFile()));
  }

  @Test
  void notOnceTheFileIsGone() throws Exception {
    Files.delete(file.toPath());

    assertNull(memo.recall(file));
  }

  @Test
  void nothingIsKeptWhenTheFileCantBeRead() throws Exception {
    memo.remember(dir.resolve("missing.json").toFile(), state);

    assertNull(memo.recall(file));
    assertNull(memo.recall(dir.resolve("missing.json").toFile()));
  }

  private static String json(JsonObject o) {
    return org.hl7.fhir.utilities.json.parser.JsonParser.compose(o);
  }
}
