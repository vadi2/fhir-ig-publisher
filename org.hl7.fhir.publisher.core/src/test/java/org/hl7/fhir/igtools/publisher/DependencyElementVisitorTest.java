package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.elementmodel.ElementVisitor.ElementVisitorInstruction;
import org.junit.jupiter.api.Test;

class DependencyElementVisitorTest {

  @Test
  void aCanonicalWithOnlyExtensionsHasNoDependencies() {
    // e.g. "_valueCanonical": { "extension": [...] }
    Element canonical = mock(Element.class);
    when(canonical.isPrimitive()).thenReturn(true);
    when(canonical.fhirType()).thenReturn("canonical");
    when(canonical.primitiveValue()).thenReturn(null);
    List<FetchedFile> dependencies = new ArrayList<>();
    FetchedFile focus = new FetchedFile("Group-example.json");
    List<FetchedFile> fileList = new ArrayList<>(List.of(focus, new FetchedFile("Library-example.json")));

    ElementVisitorInstruction result = new DependencyElementVisitor(fileList, dependencies, focus).visit(null, canonical);

    assertEquals(ElementVisitorInstruction.NO_VISIT_CHILDREN, result);
    assertTrue(dependencies.isEmpty());
  }
}
