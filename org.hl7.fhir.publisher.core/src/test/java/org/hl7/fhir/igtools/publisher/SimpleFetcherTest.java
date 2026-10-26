package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class SimpleFetcherTest {

  @TempDir
  Path dir;

  @Test
  void undecodableFileNameUnderAsciiSaysToUseAUtf8Locale() {
    String message = SimpleFetcher.undecodableFileNameMessage("input/pagecontent/��levaade.md", "ANSI_X3.4-1968");

    assertTrue(message.startsWith("Unable to read the file input/pagecontent/��levaade.md: its name can't be decoded with the file name encoding 'ANSI_X3.4-1968'"), message);
    assertTrue(message.contains("LC_ALL=C.UTF-8"), message);
  }

  @Test
  void undecodableFileNameUnderUtf8SaysToRenameIt() {
    String message = SimpleFetcher.undecodableFileNameMessage("input/pagecontent/�levaade.md", "UTF-8");

    assertTrue(message.startsWith("Unable to read the file input/pagecontent/�levaade.md: its name isn't valid UTF-8"), message);
    assertFalse(message.contains("locale"), message);
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  void fetchReportsUndecodableFileName() throws Exception {
    rawName("touch", "\\374levaade.md");
    SimpleFetcher fetcher = new SimpleFetcher(null);

    Exception e = assertThrows(Exception.class, () -> {
      for (String f : fetcher.fetch(dir.toString()).getFiles()) {
        fetcher.fetch(f);
      }
    });

    assertEquals(expectedMessage("�levaade.md"), e.getMessage());
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  void scanReportsUndecodableFileName() throws Exception {
    rawName("touch", "Patient-\\374.json");

    Exception e = assertThrows(Exception.class, () -> new SimpleFetcher(null).scan(dir.toString(), null, false, new ArrayList<>()));

    assertEquals(expectedMessage("Patient-�.json"), e.getMessage());
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  void scanFoldersReportsUndecodableFolderName() throws Exception {
    rawName("mkdir", "\\374bersicht");

    Exception e = assertThrows(Exception.class, () -> new SimpleFetcher(null).scanFolders(dir.toString(), new ArrayList<>()));

    assertEquals(expectedMessage("�bersicht"), e.getMessage());
  }

  /**
   * Makes a file or folder whose name has the byte 0xFC (u-umlaut in ISO-8859-1), which isn't valid UTF-8 or ASCII, so
   * the JVM lists it with U+FFFD in it under a UTF-8 or an ASCII locale, as it lists a UTF-8 non-ASCII name under the
   * POSIX locale
   */
  private void rawName(String command, String name) throws Exception {
    Process p = new ProcessBuilder("sh", "-c", command + " \"$(printf '" + name + "')\"").directory(dir.toFile()).start();
    assertEquals(0, p.waitFor());
  }

  private String expectedMessage(String name) {
    return SimpleFetcher.undecodableFileNameMessage(new File(dir.toFile(), name).getPath(), System.getProperty("sun.jnu.encoding"));
  }
}
