package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class PublisherGeneratorSpecTargetsTest {

  private static final String[] OUTPUT = { "index.html", "StructureDefinition-a.html", "StructureDefinition-a.ai.html",
      "ai.html", "package.tgz", "en" };

  @Test
  void theAiPagesTheAiStepRemovesArentTargets() {
    assertEquals(Arrays.asList("index.html", "StructureDefinition-a.html", "ai.html"),
        PublisherGenerator.specTargets(OUTPUT, true));
  }

  @Test
  void theAiPagesAreTargetsWhenTheyStay() {
    assertEquals(Arrays.asList("index.html", "StructureDefinition-a.html", "StructureDefinition-a.ai.html", "ai.html"),
        PublisherGenerator.specTargets(OUTPUT, false));
  }
}
