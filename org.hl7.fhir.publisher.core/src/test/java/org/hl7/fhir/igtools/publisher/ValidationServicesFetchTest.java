package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.services.context.IWorkerContext;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.elementmodel.Manager;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** how the validator's fetcher resolves a relative reference to one of the IG's resources, and what it reads to */
class ValidationServicesFetchTest {

  private static IWorkerContext context;
  private static String noContext;

  @BeforeAll
  static void loadContext() throws Exception {
    try {
      NpmPackage npm = new FilesystemPackageCacheManager.Builder().build().loadPackage("hl7.fhir.r5.core", "5.0.0");
      context = new SimpleWorkerContext.SimpleWorkerContextBuilder(ModelContext.fullCoreContext()).withAllowLoadingDuplicates(true).fromPackage(npm);
    } catch (IOException | FHIRException e) {
      noContext = "hl7.fhir.r5.core#5.0.0 can't be fetched: " + e;
    }
  }

  @AfterEach
  void clearListener() {
    FetchedReads.setListener(null);
  }

  private static Element parse(String json) throws Exception {
    return Manager.parseSingle(context, new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), FhirFormat.JSON);
  }

  private static ValidationServices services(FetchedFile... files) {
    IGKnowledgeProvider ipg = mock(IGKnowledgeProvider.class);
    when(ipg.getCanonical()).thenReturn("http://example.org/fhir");
    return new ValidationServices(context, ipg, null, Arrays.asList(files), new ArrayList<>(), false, null, null, null, null);
  }

  /** an instance of a logical model, loaded as a Binary (PublisherIGLoader.loadAsElementModel): a Patient stands in for the model */
  private static FetchedFile logicalAsBinary() throws Exception {
    FetchedFile f = new FetchedFile("lm.json");
    FetchedResource r = f.addResource("lm");
    r.setElement(parse("{\"resourceType\":\"Patient\",\"id\":\"lm\"}")).setId("lm");
    r.setType("Binary");
    return f;
  }

  @Test
  void aBinaryReferenceToALogicalModelInstanceIsTheBinaryItIsPublishedAs() throws Exception {
    assumeTrue(context != null, noContext);
    Element e = services(logicalAsBinary()).fetch(null, null, "Binary/lm");
    assertNotNull(e);
    assertEquals("Binary", e.fhirType());
  }

  @Test
  void aReferenceByTheElementsOwnTypeIsTheElement() throws Exception {
    assumeTrue(context != null, noContext);
    FetchedFile f = logicalAsBinary();
    assertSame(f.getResources().get(0).getElement(), services(f).fetch(null, null, "Patient/lm"));
  }

  @Test
  void onlyTheResourcesWithTheIdAreRead() throws Exception {
    assumeTrue(context != null, noContext);
    FetchedFile f = new FetchedFile("a.json");
    f.addResource("obs").setElement(parse("{\"resourceType\":\"Observation\",\"id\":\"o\"}")).setId("o");
    f.addResource("pat").setElement(parse("{\"resourceType\":\"Patient\",\"id\":\"p\"}")).setId("p");
    ValidationServices vs = services(f);
    Set<String> reads = new TreeSet<>();
    FetchedReads.setListener(new FetchedReads.Listener() {
      @Override
      public void resourceRead(FetchedResource r, FetchedReads.ResourcePart part) {
        reads.add(r.getNameForErrors() + " " + part);
      }

      @Override
      public void fileRead(FetchedFile file, FetchedReads.FilePart part) {
      }
    });
    assertSame(f.getResources().get(1).getElement(), vs.fetch(null, null, "Patient/p"));
    FetchedReads.setListener(null);
    assertEquals(Set.of("pat CONTENT"), reads);
  }
}
