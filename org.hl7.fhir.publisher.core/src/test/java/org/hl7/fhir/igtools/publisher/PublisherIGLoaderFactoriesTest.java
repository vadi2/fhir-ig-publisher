package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.hl7.fhir.model.core.ImplementationGuide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublisherIGLoaderFactoriesTest {

  @TempDir
  Path dir;

  @Test
  void whatTheFactoriesWroteBeforeTheyFailedIsNotAChange() throws Exception {
    Path root = dir.resolve("ig");
    Path input = root.resolve("input");
    write(root.resolve("ig.ini"), "[IG]");
    write(input.resolve("fsh/a.fsh"), "Profile: A");
    write(input.resolve("factories/sub/Patient-1.json"), "{\"id\": \"0\"}");
    write(root.resolve("factories/factories.json"), "{\"factories\": [{\"name\": \"B\", \"filename\": \"input/tests/Bundle-$id$.json\"}]}");

    PublisherSettings settings = new PublisherSettings();
    // the factories' sources and output files are relative to the folder of ig.ini, not the folder the publisher runs in
    settings.setConfigFile(root.resolve("ig.ini").toString());
    PublisherFields pf = new PublisherFields();
    pf.rootDir = root.toString();
    pf.testDataFactories = List.of("factories/factories.json");
    PublisherIGLoader loader = new PublisherIGLoader(settings) {
      @Override
      void processFactories(List<String> factories) throws IOException {
        write(input.resolve("factories/sub/Patient-1.json"), "{\"id\": \"1\"}");
        write(input.resolve("tests/Bundle-1.json"), "{}");
        write(root.resolve("factories/log/B.log"), "failed");
        throw new IOException("the factory failed");
      }
    };
    loader.setPf(pf);
    // as ig.ini gives it ("path-factory": "input/factories*", a folder and those below it)
    ImplementationGuide.ImplementationGuideDefinitionParameterComponent p = new ImplementationGuide.ImplementationGuideDefinitionParameterComponent();
    p.setValue("input/factories*");
    pf.resourceFactoryDirs.add(loader.getPathResourceDirectory(p));
    assertTrue(pf.resourceFactoryDirs.get(0).endsWith("*"), pf.resourceFactoryDirs.toString());

    WatchSession session = new WatchSession(root, List.of(input, root.resolve("ig.ini"), root.resolve("factories")));
    loader.setWatchSession(session);
    session.startBuild();
    assertThrows(IOException.class, loader::runFactories);
    assertTrue(session.changesSinceBuildStart().isEmpty(), session.changesSinceBuildStart().toString());

    // and the next build doesn't start for it either, while an edit made after them is a change
    session.startBuild();
    assertTrue(session.changesSincePreviousBuild().isEmpty(), session.changesSincePreviousBuild().toString());
    write(input.resolve("tests/Bundle-1.json"), "{\"by\": \"hand\"}");
    assertEquals(1, session.changesSinceBuildStart().getModified().size());
  }

  private static void write(Path file, String content) throws IOException {
    Files.createDirectories(file.getParent());
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
  }
}
