package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

/**
 * A map's content signature (what the html checks look up in it) is the same for the same content, and changes with
 * any of it, so that a watch build reuses the last build's html checks only if the maps they looked at are the same
 */
class SpecMapManagerSignatureTest {

  private static SpecMapManager map(boolean reversed) {
    SpecMapManager res = new SpecMapManager("hl7.fhir.r4.core", "4.0.1", "4.0.1", "1", "1", Calendar.getInstance(), "http://hl7.org/fhir/R4");
    res.setBase("http://hl7.org/fhir/R4");
    res.setBase2("http://hl7.org/fhir/R4/2019");
    List<String> names = new ArrayList<>(Arrays.asList("patient", "observation", "index"));
    if (reversed) {
      Collections.reverse(names);
    }
    for (String n : names) {
      res.target(n + ".html");
      res.image(n + ".png");
      res.path("http://hl7.org/fhir/StructureDefinition/" + n, n + ".html");
    }
    return res;
  }

  @Test
  void theSameContentInAnotherOrder() {
    assertEquals(map(false).contentSignature(), map(true).contentSignature());
  }

  @Test
  void anyOfWhatTheChecksLookUp() {
    String was = map(false).contentSignature();
    List<Consumer<SpecMapManager>> changes = Arrays.asList(
        m -> m.setBase("http://hl7.org/fhir/R5"),
        m -> m.setBase2("http://hl7.org/fhir/R4/2020"),
        m -> m.target("encounter.html"),
        m -> m.image("encounter.png"),
        m -> m.path("http://hl7.org/fhir/StructureDefinition/encounter", "encounter.html"));
    for (int i = 0; i < changes.size(); i++) {
      SpecMapManager m = map(false);
      changes.get(i).accept(m);
      assertNotEquals(was, m.contentSignature(), "change " + i);
    }
  }
}
