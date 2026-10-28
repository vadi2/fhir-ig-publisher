package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.model.core.ImplementationGuide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class TemplateScriptTest {

  @TempDir
  Path dir;

  /**
   * On Windows a file that is still open can't be deleted, so streams left open by the onLoad script run
   * stop the next template load from clearing the template folder.
   */
  @Test
  void onLoadScriptLeavesItsFilesDeletable() throws Exception {
    ImplementationGuide updated = runOnLoad("xml");

    assertEquals("Test", updated.getName());
    List<Path> files = scriptFiles();
    assertEquals(List.of("onLoad-ig-updated.xml", "onLoad-ig-working.json", "onLoad-ig-working.xml"),
        files.stream().map(f -> f.getFileName().toString()).collect(Collectors.toList()));
    for (Path f : files) {
      assertTrue(f.toFile().delete(), "couldn't delete " + f.getFileName());
    }
  }

  /**
   * Linux doesn't block deleting an open file, so look for the open handles in /proc/self/fd instead.
   */
  @Test
  @EnabledOnOs(OS.LINUX)
  void onLoadScriptLeavesNoFilesOpen() throws Exception {
    Path probe = Files.writeString(dir.resolve("probe.txt"), "probe");
    try (FileInputStream in = new FileInputStream(probe.toFile())) {
      assertEquals(List.of("probe.txt"), filesOpenUnder(dir));
    }
    assertEquals(List.of(), filesOpenUnder(dir));

    runOnLoad("xml");

    assertEquals(List.of(), filesOpenUnder(dir));
  }

  /**
   * Runs a template whose onLoad target hands back the IG it was given, written in the given format.
   */
  private ImplementationGuide runOnLoad(String format) throws Exception {
    Path templateDir = Files.createDirectory(dir.resolve("template"));
    Files.writeString(templateDir.resolve("config.json"), "{\"script\": \"scripts/ant.xml\", \"targets\": {\"onLoad\": \"onLoad\"}}");
    Files.createDirectory(templateDir.resolve("scripts"));
    Files.writeString(templateDir.resolve("scripts/ant.xml"),
        "<project name=\"test\">\n"
        + "  <target name=\"onLoad\">\n"
        + "    <copy file=\"${onLoad.ig.source}" + format + "\" tofile=\"${onLoad.ig.dest}" + format + "\"/>\n"
        + "  </target>\n"
        + "</project>\n");
    Template template = new Template(dir.toString(), true, null, null, false, false, false);

    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("test");
    ig.setUrl("http://example.org/ImplementationGuide/test");
    ig.setName("Test");
    ImplementationGuide updated = template.onLoadEvent(ig, new HashMap<>());
    assertNotNull(updated);
    return updated;
  }

  private List<Path> scriptFiles() throws IOException {
    try (Stream<Path> files = Files.list(dir.resolve("template"))) {
      return files.filter(f -> f.getFileName().toString().startsWith("onLoad-ig-")).sorted().collect(Collectors.toList());
    }
  }

  private static List<String> filesOpenUnder(Path folder) throws IOException {
    String prefix = folder.toRealPath().toString() + File.separator;
    List<String> open = new ArrayList<>();
    try (Stream<Path> fds = Files.list(Path.of("/proc/self/fd"))) {
      for (Path fd : (Iterable<Path>) fds::iterator) {
        try {
          String target = Files.readSymbolicLink(fd).toString();
          if (target.startsWith(prefix)) {
            open.add(target.substring(prefix.length()));
          }
        } catch (NoSuchFileException e) {
          // the descriptor was closed while we were looking
        }
      }
    }
    return open;
  }
}
