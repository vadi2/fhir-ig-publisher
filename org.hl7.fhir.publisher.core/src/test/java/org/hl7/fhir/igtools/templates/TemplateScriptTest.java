package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
   * stop the next template load from clearing the template folder. Linux doesn't block the delete, so
   * look for the open handles in /proc/self/fd instead.
   */
  @Test
  @EnabledOnOs(OS.LINUX)
  void onLoadScriptLeavesNoFilesOpen() throws Exception {
    Path templateDir = Files.createDirectory(dir.resolve("template"));
    Files.writeString(templateDir.resolve("config.json"), "{\"script\": \"scripts/ant.xml\", \"targets\": {\"onLoad\": \"onLoad\"}}");
    Files.createDirectory(templateDir.resolve("scripts"));
    Files.writeString(templateDir.resolve("scripts/ant.xml"),
        "<project name=\"test\">\n"
        + "  <target name=\"onLoad\">\n"
        + "    <copy file=\"${onLoad.ig.source}xml\" tofile=\"${onLoad.ig.dest}xml\"/>\n"
        + "  </target>\n"
        + "</project>\n");
    Template template = new Template(dir.toString(), true, null, null, false, false, false);

    ImplementationGuide ig = new ImplementationGuide();
    ig.setId("test");
    ig.setUrl("http://example.org/ImplementationGuide/test");
    ig.setName("Test");
    ImplementationGuide updated = template.onLoadEvent(ig, new HashMap<>());

    assertNotNull(updated);
    assertEquals("Test", updated.getName());
    assertEquals(List.of(), filesOpenUnder(dir));
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
        } catch (IOException e) {
          // the descriptor was closed while we were looking
        }
      }
    }
    return open;
  }
}
