package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.model.core.ImplementationGuide;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** the parts of the input cache that work out which of the IG's inputs a step read */
class InputCacheReadsTest {

  private static final String DATE = "2026-10-21T10:00:00+00:00";

  @AfterEach
  void clearListener() {
    FetchedReads.setListener(null);
  }

  /** what the getters reported, one line per read */
  private static class Recorder implements FetchedReads.Listener {
    final List<String> reads = new ArrayList<>();

    @Override
    public void resourceRead(FetchedResource r, FetchedReads.ResourcePart part) {
      reads.add(r.getNameForErrors() + " " + part);
    }

    @Override
    public void fileRead(FetchedFile f, FetchedReads.FilePart part) {
      reads.add("file " + part);
    }
  }

  @Test
  void fetchedReads_gettersReportTheirPartOnlyWhileThereIsAListener() {
    FetchedFile f = new FetchedFile("a.json");
    f.setSource("{}".getBytes(StandardCharsets.UTF_8));
    FetchedResource r = f.addResource("res");
    f.getSource();
    r.getResource();
    Recorder rec = new Recorder();
    FetchedReads.setListener(rec);

    f.getSource();
    f.getErrors();
    f.getOutputNames();
    r.getResource();
    r.getElement();
    r.getErrors();
    r.getFoundProfiles();
    r.isValidated();
    r.getId();
    r.fhirType();
    FetchedReads.setListener(null);
    r.getElement();

    assertEquals(Arrays.asList("file CONTENT", "file ERRORS", "file OUTPUTS", "res CONTENT", "res CONTENT", "res ERRORS", "res FOUND", "res VALIDATED"), rec.reads);
  }

  private static class MemoRecorder implements InputCacheMemoMap.Listener {
    final List<String> calls = new ArrayList<>();

    @Override
    public void memoHit(String memo, Collection<?> sources) {
      calls.add("hit " + memo + " " + sources);
    }

    @Override
    public void memoOther(String memo, String operation) {
      calls.add("other " + memo + " " + operation);
    }
  }

  @Test
  void memoMap_reportsHitsWithWhatTheyCameFrom_andAnyOtherUse() {
    MemoRecorder rec = new MemoRecorder();
    Map<String, String> real = new HashMap<>();
    InputCacheMemoMap<String, String> m = new InputCacheMemoMap<>("m", real, (k, v) -> Arrays.asList(k, v), rec);

    assertNull(m.get("a"));
    assertFalse(m.containsKey("a"));
    m.put("a", "1");
    assertEquals("1", real.get("a"));
    assertEquals("1", m.get("a"));
    assertTrue(m.containsKey("a"));
    m.put("n", null);
    assertNull(m.get("n"));
    m.size();
    m.keySet();

    assertEquals(Arrays.asList("hit m [a, 1]", "hit m [a, 1]", "hit m [n, null]", "other m size", "other m keySet"), rec.calls);
    assertSame(real, m.getMap());
  }

  @Test
  void urlOfKey_isTheUrlBetweenThePrefixAndTheHash() {
    assertEquals("http://x/sd|1.0", InputCache.urlOfKey("cm.http://x/sd|1.0#Patient.name.false", 3));
    assertEquals("http://x/sd|1.0", InputCache.urlOfKey("http://x/sd|1.0#Patient.name:given/string", 0));
    assertEquals("http://x/sd", InputCache.urlOfKey("http://x/sd", 0));
    assertNull(InputCache.urlOfKey(null, 3));
  }

  @Test
  void generated_isTheBaseDefinitionAndTheCrossVersionExtensionsOnly() {
    StructureDefinition sd = new StructureDefinition();
    sd.setUrl("http://hl7.org/fhir/StructureDefinition/Base");
    assertTrue(InputCache.generated(sd));
    sd.setUrl("http://hl7.org/fhir/5.0/StructureDefinition/extension-NamingSystem.url");
    assertTrue(InputCache.generated(sd));
    sd.setUrl("http://hl7.org/fhir/StructureDefinition/Patient");
    assertFalse(InputCache.generated(sd));
    sd.setUrl(null);
    assertFalse(InputCache.generated(sd));
    ValueSet vs = new ValueSet();
    vs.setUrl("http://hl7.org/fhir/StructureDefinition/Base");
    assertFalse(InputCache.generated(vs));
  }

  private static PublisherFields ig() {
    PublisherFields pf = new PublisherFields();
    for (String name : new String[] {"a", "b"}) {
      FetchedFile f = new FetchedFile(name + ".json");
      f.setPath("/ig/input/" + name + ".json");
      f.setName(name);
      f.setSource(("{\"id\":\"" + name + "\"}").getBytes(StandardCharsets.UTF_8));
      FetchedResource r = f.addResource(name);
      r.setId(name);
      StructureDefinition sd = new StructureDefinition();
      sd.setId(name);
      sd.setUrl("http://example.org/StructureDefinition/" + name);
      sd.setVersion("1.0");
      r.setResource(sd);
      r.setType("StructureDefinition");
      pf.fileList.add(f);
    }
    return pf;
  }

  @Test
  void inputs_locateTheIgsFilesAndResources() throws Exception {
    PublisherFields pf = ig();
    InputCacheInputs in = InputCacheInputs.of(pf, DATE);
    FetchedFile f = pf.fileList.get(1);
    FetchedResource r = f.getResources().get(0);

    assertEquals("/ig/input/b.json", in.locate(f));
    assertEquals("/ig/input/b.json#0", in.locate(r));
    assertSame(r, in.resource("/ig/input/b.json#0"));
    assertEquals("/ig/input/b.json#0", in.locateObject(r.getResource()));
    assertEquals("/ig/input/b.json#0", in.locateUrl("http://example.org/StructureDefinition/b"));
    assertEquals("/ig/input/b.json#0", in.locateUrl("http://example.org/StructureDefinition/b|1.0"));
    assertNull(in.locateUrl("http://example.org/StructureDefinition/c"));
    assertNull(in.locateObject(new ValueSet()));
    assertNull(in.locate(new FetchedResource("other")));
  }

  @Test
  void inputs_aResourcesDigestChangesWithItsContentOnly() throws Exception {
    PublisherFields pf = ig();
    InputCacheInputs before = InputCacheInputs.of(pf, DATE);
    ((StructureDefinition) pf.fileList.get(1).getResources().get(0).getResource()).setDescription("changed");
    InputCacheInputs after = InputCacheInputs.of(pf, DATE);

    assertEquals(before.evaluate("r:/ig/input/a.json#0"), after.evaluate("r:/ig/input/a.json#0"));
    assertNotEquals(before.evaluate("r:/ig/input/b.json#0"), after.evaluate("r:/ig/input/b.json#0"));
    assertEquals(before.evaluate("f:/ig/input/b.json"), after.evaluate("f:/ig/input/b.json"));
    for (String k : InputCacheInputs.SHAPES) {
      assertEquals(before.evaluate(k), after.evaluate(k), k);
    }
    assertEquals(InputCache.ABSENT, after.evaluate("r:/ig/input/c.json#0"));
    assertNull(after.evaluate("unknown:x"));
  }

  @Test
  void inputs_aResourceTooLargeToDigestIsUndigestible_notTheSameEveryBuild() throws Exception {
    PublisherFields pf = ig();
    StructureDefinition sd = (StructureDefinition) pf.fileList.get(1).getResources().get(0).getResource();
    for (int i = 0; i < 20000; i++) {
      sd.getDifferential().addElement().setPath("X.e" + i);
    }
    InputCacheInputs in = InputCacheInputs.of(pf, DATE);

    assertTrue(in.evaluate("r:/ig/input/b.json#0").startsWith(InputCacheInputs.UNDIGESTIBLE), in.evaluate("r:/ig/input/b.json#0"));
    assertFalse(in.evaluate("r:/ig/input/a.json#0").startsWith(InputCacheInputs.UNDIGESTIBLE));
  }

  @Test
  void inputs_theSetOfFilesIsTheShape() throws Exception {
    PublisherFields pf = ig();
    InputCacheInputs before = InputCacheInputs.of(pf, DATE);
    pf.fileList.remove(0);
    InputCacheInputs after = InputCacheInputs.of(pf, DATE);

    assertNotEquals(before.evaluate(InputCacheInputs.SHAPE_FILES), after.evaluate(InputCacheInputs.SHAPE_FILES));
    assertEquals(before.evaluate("r:/ig/input/b.json#0"), after.evaluate("r:/ig/input/b.json#0"));
  }

  @Test
  void inputs_whatChangesAsStepsRunIsEvaluatedNow() throws Exception {
    PublisherFields pf = ig();
    InputCacheInputs in = InputCacheInputs.of(pf, DATE);
    FetchedResource r = pf.fileList.get(0).getResources().get(0);
    String errors = in.evaluate("re:/ig/input/a.json#0");
    String found = in.evaluate("rf:/ig/input/a.json#0");
    assertEquals("0", in.evaluate("rv:/ig/input/a.json#0"));

    r.getErrors().add(new ValidationMessage(Source.InstanceValidator, IssueType.INVALID, "x", "a message", IssueSeverity.ERROR));
    r.getFoundProfiles().add("http://example.org/StructureDefinition/b");
    r.setValidated(true);

    assertNotEquals(errors, in.evaluate("re:/ig/input/a.json#0"));
    assertNotEquals(found, in.evaluate("rf:/ig/input/a.json#0"));
    assertEquals("1", in.evaluate("rv:/ig/input/a.json#0"));
  }

  @Test
  void inputs_theBuildDateInAMessageIsTheSameInputEveryBuild() throws Exception {
    PublisherFields pf = ig();
    List<ValidationMessage> errors = pf.fileList.get(0).getResources().get(0).getErrors();
    errors.add(new ValidationMessage(Source.InstanceValidator, IssueType.INVALID, "x", "built " + DATE, IssueSeverity.WARNING));
    String today = InputCacheInputs.of(pf, DATE).evaluate("re:/ig/input/a.json#0");
    errors.set(0, new ValidationMessage(Source.InstanceValidator, IssueType.INVALID, "x", "built 2026-10-22T10:00:00+00:00", IssueSeverity.WARNING));

    assertEquals(today, InputCacheInputs.of(pf, "2026-10-22T10:00:00+00:00").evaluate("re:/ig/input/a.json#0"));
  }

  @Test
  void inputs_aDateTheLoaderStampedWithTheBuildDateIsTheSameInputEveryBuild() throws Exception {
    PublisherFields pf = ig();
    StructureDefinition sd = (StructureDefinition) pf.fileList.get(0).getResources().get(0).getResource();
    sd.setDateElement(new org.hl7.fhir.model.core.DateTimeType(DATE));
    String today = InputCacheInputs.of(pf, DATE).evaluate("r:/ig/input/a.json#0");
    sd.setDateElement(new org.hl7.fhir.model.core.DateTimeType("2026-10-22T10:00:00+00:00"));

    assertEquals(today, InputCacheInputs.of(pf, "2026-10-22T10:00:00+00:00").evaluate("r:/ig/input/a.json#0"));
    assertNotEquals(today, InputCacheInputs.of(pf, DATE).evaluate("r:/ig/input/a.json#0"));
  }

  @Test
  void inputs_theIgsDateStampedWithTheBuildDateIsTheSameShapeEveryBuild() throws Exception {
    PublisherFields pf = ig();
    pf.publishedIg = new ImplementationGuide();
    pf.publishedIg.setDateElement(new org.hl7.fhir.model.core.DateTimeType(DATE));
    String today = InputCacheInputs.of(pf, DATE).evaluate(InputCacheInputs.SHAPE_PF);
    pf.publishedIg.setDateElement(new org.hl7.fhir.model.core.DateTimeType("2026-10-22T10:00:00+00:00"));

    assertEquals(today, InputCacheInputs.of(pf, "2026-10-22T10:00:00+00:00").evaluate(InputCacheInputs.SHAPE_PF));
    assertNotEquals(today, InputCacheInputs.of(pf, DATE).evaluate(InputCacheInputs.SHAPE_PF));
  }

  @Test
  void inputs_anEmptyElementTheModelMadeWhenAskedForIsTheSameAsNone() throws Exception {
    PublisherFields pf = ig();
    ValueSet vs = new ValueSet();
    vs.setUrl("http://example.org/ValueSet/a");
    FetchedResource r = pf.fileList.get(0).getResources().get(0);
    r.setResource(vs);
    String none = InputCacheInputs.of(pf, DATE).evaluate("r:/ig/input/a.json#0");
    vs.getExpansion();
    String empty = InputCacheInputs.of(pf, DATE).evaluate("r:/ig/input/a.json#0");
    vs.getExpansion().setIdentifier("x");

    assertEquals(none, empty);
    assertNotEquals(none, InputCacheInputs.of(pf, DATE).evaluate("r:/ig/input/a.json#0"));
  }

  @Test
  void inputs_fieldsThatAreNotInputsAreLeftOutOfTheShape() throws Exception {
    PublisherFields pf = ig();
    InputCacheInputs before = InputCacheInputs.of(pf, DATE);
    pf.otherFilesRun.add("/ig/temp/x.html");
    InputCacheInputs after = InputCacheInputs.of(pf, DATE);

    assertEquals(before.evaluate(InputCacheInputs.SHAPE_PF), after.evaluate(InputCacheInputs.SHAPE_PF));
    assertTrue(InputCacheInputs.changed(before.calibration(), after.calibration()).isEmpty());
    assertFalse(before.getPublisherFieldDigests().containsKey("otherFilesRun"));
  }

  @Test
  void inputs_aFolderOfPagesIsTheSameInputWhenOnlyItsTimeChanges() throws Exception {
    PublisherFields pf = ig();
    FetchedFile dir = new FetchedFile("pages");
    dir.setPath("/ig/input/pages");
    dir.setFolder(true);
    dir.getFiles().add("/ig/input/pages/index.md");
    dir.setTime(1);
    pf.altMap.put("pre-page/" + dir.getPath(), dir);
    InputCacheInputs before = InputCacheInputs.of(pf, DATE);
    dir.setTime(2);
    InputCacheInputs later = InputCacheInputs.of(pf, DATE);
    dir.getFiles().add("/ig/input/pages/other.md");
    InputCacheInputs added = InputCacheInputs.of(pf, DATE);

    assertEquals(before.evaluate(InputCacheInputs.SHAPE_PF), later.evaluate(InputCacheInputs.SHAPE_PF));
    assertNotEquals(before.evaluate(InputCacheInputs.SHAPE_PF), added.evaluate(InputCacheInputs.SHAPE_PF));
  }
}
