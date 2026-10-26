package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class SimpleFetcherTest {

  @TempDir
  Path dir;

  @Test
  @EnabledOnOs(OS.LINUX)
  void undecodableFileNameSaysToUseAUtf8Locale() throws Exception {
    // the name is "levaade.md" after a u-umlaut in ISO-8859-1 (byte 0xFC), which isn't valid UTF-8 either, so under a
    // UTF-8 locale too the JVM lists it with U+FFFD in it, as it lists a UTF-8 non-ASCII name under the POSIX locale
    Process p = new ProcessBuilder("sh", "-c", "touch \"$(printf '\\374levaade.md')\"").directory(dir.toFile()).start();
    assertEquals(0, p.waitFor());
    SimpleFetcher fetcher = new SimpleFetcher(null);

    Exception e = assertThrows(Exception.class, () -> {
      for (String f : fetcher.fetch(dir.toString()).getFiles()) {
        fetcher.fetch(f);
      }
    });

    assertTrue(e.getMessage().startsWith("Unable to read the file " + new File(dir.toFile(), "\uFFFDlevaade.md").getPath()
        + ": its name can't be decoded"), e.getMessage());
    assertTrue(e.getMessage().contains("UTF-8 locale"), e.getMessage());
  }
}
