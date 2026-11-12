package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.igtools.templates.Template;
import org.hl7.fhir.standalone.context.BaseWorkerContext;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.NamedItemList;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator;
import org.hl7.fhir.validation.BaseValidator;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A rapido build in watch mode reuses what the last build made for what didn't change, as without watch mode: each
 * rebuild loads the files again, and compares them with the state the last build saved
 */
class PublisherRapidoWatchTest {

  @TempDir
  Path dir;

  private final String uuid = HierarchicalTableGenerator.uuid;

  @AfterEach
  void restoreUuid() {
    HierarchicalTableGenerator.uuid = uuid;
  }

  @Test
  void aWatchBuildRendersTheChangedFileAndWhatDependsOnIt() throws Exception {
    HierarchicalTableGenerator.uuid = "the-last-build";
    PublisherFields first = build("a");
    assertTrue(first.rapido.isCompleteBuild());
    finish(first);

    HierarchicalTableGenerator.uuid = "a-new-session"; // as resetForRebuild leaves it

    // the patient changes: the observation that refers to it is rendered again, the other patient isn't. The IG and
    // the page are always processed
    PublisherFields pf = build("a2");

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    assertEquals(List.of("Observation-o.json (something it depends on changed)", "Patient-a.json (changed)",
        "ig.json (always processed)", "page.md (always processed)"), pf.rapido.listRenderDirty());
    assertEquals("the-last-build", HierarchicalTableGenerator.uuid);
  }

  @Test
  void withoutARecordOfWhatTheLastBuildWroteTheBuildIsComplete() throws Exception {
    finish(build("a"));
    // an older publisher built it, or the record was removed
    Files.delete(dir.resolve(".build-outputs.json"));

    PublisherFields pf = build("a2");

    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals("what the last build wrote in the temp folder isn't all there", pf.rapido.getReason());
  }

  @Test
  void whenWhatTheLastBuildWroteInTheTempFolderIsGoneTheBuildIsComplete() throws Exception {
    finish(build("a"));
    Path page = dir.resolve("temp").resolve("Patient-b.html");
    assertTrue(Files.exists(page));
    Files.delete(page);

    PublisherFields pf = build("a");

    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals("what the last build wrote in the temp folder isn't all there", pf.rapido.getReason());
    assertTrue(Files.isDirectory(dir.resolve("temp")), "the temp folder is cleared for the complete build");
    assertFalse(Files.exists(dir.resolve("temp").resolve("Patient-a.html")));
  }

  /**
   * Rendering that failed with an exception (a file that couldn't be written, say) may not fail the same way again:
   * what it made isn't reused
   */
  @Test
  void whatRenderingThatFailedMadeIsNotReused() throws Exception {
    finish(build("a"), "Patient-b.json");

    PublisherFields pf = build("a");

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    assertEquals(List.of("Patient-b.json (outputs from the last build are missing; render only)", "ig.json (always processed)",
        "page.md (always processed)"), pf.rapido.listRenderDirty());
  }

  /** A file whose outputs are reused keeps them: they're not removed as written by no file, and the next build reuses them too */
  @Test
  void whatAReusedFileWroteIsKept() throws Exception {
    finish(build("a"));

    PublisherFields pf = build("a2");
    RapidoOutputs.Change change = finish(pf);

    assertEquals(List.of("Patient-b.json"), replayed);
    Path page = dir.resolve("temp").resolve("Patient-b.html");
    assertFalse(change.getGone().contains(page.toString()), change.getGone().toString());
    assertTrue(Files.exists(page));

    pf = build("a2");
    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    assertEquals(List.of("ig.json (always processed)", "page.md (always processed)"), pf.rapido.listRenderDirty());
  }

  /** Something else wrote over what the file wrote: what the file made isn't reused */
  @Test
  void anOutputWrittenOverIsntReused() throws Exception {
    finish(build("a"));
    Files.writeString(dir.resolve("temp").resolve("Patient-b.html"), "something else");

    PublisherFields pf = build("a");

    assertFalse(pf.rapido.canReplay(file(pf, "Patient-b.json"), RapidoTracker.PHASE_HTML));
    assertTrue(pf.rapido.listRenderDirty().contains("Patient-b.json (an output was overwritten: Patient-b.html; render only)"),
        pf.rapido.listRenderDirty().toString());
  }

  /** A reused profile says it has a UML diagram, as it did when it was rendered (_data/structuredefinitions.json) */
  @Test
  void aReusedFileStillHasItsUmlDiagram() throws Exception {
    PublisherFields pf = build("a");
    FetchedResource r = file(pf, "Patient-b.json").getResources().get(0);
    when(r.isUmlGenerated()).thenReturn(false, true); // rendering it makes the diagram
    finish(pf);

    pf = build("a2");
    FetchedFile b = file(pf, "Patient-b.json");
    assertTrue(pf.rapido.canReplay(b, RapidoTracker.PHASE_HTML));
    assertEquals(java.util.Set.of(0), pf.rapido.replay(b, RapidoTracker.PHASE_HTML).umlGenerated);
  }

  /**
   * A template script writes diagrams into _includes (onGenerate.plantUml), after the template has listed the svgs in
   * there as pages (onGenerate.artifactList): from the second build on, the diagrams are pages too, and recorded as
   * what those pages wrote. A build after the day changed clears the temp folder, so the template lists none and the
   * diagrams are the script's again: they're not gone, or Jekyll can't include them
   */
  @Test
  void aCompleteBuildDoesntRemoveWhatTheTemplateScriptsWrote() throws Exception {
    PublisherFields pf = build("a");
    writeDiagram();
    finish(pf);

    pf = build("a2");
    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    FetchedFile svg = file("diagram.svg", "<svg/>");
    svg.setProcessMode(FetchedFile.PROCESS_NONE);
    pf.fileList.add(svg); // as templateBeforeGenerate loads it
    pf.changeList.add(svg);
    writeDiagram();
    svg.getOutputNames().add(includes("diagram.svg").toString());
    svg.getOutputNames().add(includes("en", "diagram.svg").toString());
    Files.createDirectories(includes("en"));
    Files.writeString(includes("en", "diagram.svg"), "<svg/>");
    finish(pf);

    pf = build("a2", 1);
    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals("changed: date", pf.rapido.getReason());
    writeDiagram();
    RapidoOutputs.Change change = finish(pf);

    assertEquals(Set.of(), change.getGone());
    // and what the complete build wrote is recorded: the next build is differential again
    assertFalse(build("a2", 1).rapido.isCompleteBuild());
  }

  /** what onGenerate.plantUml does */
  private void writeDiagram() throws Exception {
    Files.createDirectories(includes());
    Files.writeString(includes("diagram.svg"), "<svg/>");
  }

  private Path includes(String... names) {
    Path p = dir.resolve("temp").resolve("_includes");
    for (String n : names) {
      p = p.resolve(n);
    }
    return p;
  }

  private static FetchedFile file(PublisherFields pf, String name) {
    return pf.fileList.stream().filter(f -> f.getLoadPath().equals(name)).findFirst().orElseThrow();
  }

  /** the files whose html the last finish reused */
  private final List<String> replayed = new ArrayList<>();

  /**
   * What the rest of a build does for -rapido: each file's html and spreadsheet phases are reused if they can be, and
   * recorded if not (each patient writes a page in the temp folder), what the files wrote in the temp folder is
   * recorded, and the state is saved
   */
  private RapidoOutputs.Change finish(PublisherFields pf) throws Exception {
    return finish(pf, null);
  }

  /** as finish(pf), with the html of the file named failing as generateResourceHtmlInner reports an exception */
  private RapidoOutputs.Change finish(PublisherFields pf, String failing) throws Exception {
    if (pf.context != null) {
      validate(pf);
    }
    pf.rapido.finishValidation();
    replayed.clear();
    for (FetchedFile f : pf.fileList) {
      if (pf.rapido.canReplay(f, RapidoTracker.PHASE_HTML) && pf.rapido.canReplay(f, RapidoTracker.PHASE_SHEETS)) {
        // as replayGeneration does
        replayed.add(f.getLoadPath());
        f.getOutputNames().addAll(pf.rapido.replay(f, RapidoTracker.PHASE_HTML).outputs);
        f.getOutputNames().addAll(pf.rapido.replay(f, RapidoTracker.PHASE_SHEETS).outputs);
        continue;
      }
      pf.rapido.startRecording(f, RapidoTracker.PHASE_HTML);
      if (f.getLoadPath().startsWith("Patient-")) {
        Path page = dir.resolve("temp").resolve(f.getLoadPath().replace(".json", ".html"));
        Files.createDirectories(page.getParent());
        Files.writeString(page, "<p>" + f.getLoadPath() + "</p>");
        f.getOutputNames().add(page.toString());
      }
      if (f.getLoadPath().equals(failing)) {
        f.getErrors().add(new ValidationMessage(Source.Publisher, IssueType.EXCEPTION, "Patient", "Error Rendering Resource: disk full", IssueSeverity.ERROR));
      }
      pf.rapido.stopRecording();
      pf.rapido.startRecording(f, RapidoTracker.PHASE_SHEETS);
      pf.rapido.stopRecording();
    }
    RapidoOutputs.Change change = new RapidoOutputs(pf.vsCache, pf.tempDir).update(pf.fileList, pf.changeList);
    if (change != null) {
      pf.otherFilesRun.addAll(change.getKept());
    }
    // what the build claims in the temp folder, and what it no longer does is deleted
    PublisherGenerator generator = new PublisherGenerator(new PublisherSettings());
    generator.setPf(pf);
    generator.deleteStaleTempFiles();
    pf.rapido.save();
    IniFile buildTracker = new IniFile(dir.resolve(".build-tracker.ini").toString());
    buildTracker.setBooleanProperty("status", "complete", true, null);
    buildTracker.save();
    return change;
  }

  /** checkDependencies on new files, as a watch rebuild loads them, with the given content for Patient/a */
  private PublisherFields build(String patientContent) throws Exception {
    return build(patientContent, 0);
  }

  /** as build(patientContent), on a day that many days later */
  private PublisherFields build(String patientContent, int daysLater) throws Exception {
    FetchedFile ig = file("ig.json", "{}");
    FetchedFile patient = file("Patient-a.json", patientContent, resource("Patient", "a", primitive(patientContent)));
    FetchedFile other = file("Patient-b.json", "b", resource("Patient", "b"));
    FetchedFile observation = file("Observation-o.json", "o", resource("Observation", "o", primitive("Patient/a")));
    FetchedFile page = file("page.md", "# page");
    page.setProcessMode(FetchedFile.PROCESS_NONE);

    Publisher publisher = noStatusChangeTimes == null ? new Publisher() : new Publisher() {
      @Override
      StaleTempFiles newStaleTempFiles() {
        Path t = noStatusChangeTimes.getPath("/temp");
        return new StaleTempFiles(t, List.of(t.resolve("_rapido")));
      }
    };
    publisher.getSettings().setRapidoMode(true);
    publisher.getSettings().setWatchMode(watch);
    PublisherFields pf = new PublisherFields();
    pf.logger = mock(ILoggingService.class);
    pf.rootDir = dir.toString();
    pf.vsCache = dir.toString();
    pf.tempDir = dir.resolve("temp").toString();
    pf.igpkp = mock(IGKnowledgeProvider.class);
    when(pf.igpkp.getCanonical()).thenReturn("http://example.org/ig");
    pf.template = mock(Template.class);
    pf.getExecTime().add(Calendar.DATE, daysLater);
    pf.fileList.addAll(List.of(ig, patient, other, observation, page));
    pf.changeList.addAll(pf.fileList); // as the loader leaves it
    publisher.setPf(pf);
    // as build() does, for a complete build to clear the temp folder
    Field loader = Publisher.class.getDeclaredField("loader");
    loader.setAccessible(true);
    ((PublisherIGLoader) loader.get(publisher)).setPf(pf);

    publisher.checkDependencies(ig, new IniFile(dir.resolve(".build-tracker.ini").toString()));
    this.publisher = publisher;
    return pf;
  }

  /** whether build() is a watch build */
  private boolean watch = true;
  /** if not null, the temp folder's file system for StaleTempFiles, which has no status-change times */
  private FileSystem noStatusChangeTimes;
  /** the publisher of the last build() */
  private Publisher publisher;

  /** the code systems each file's resources use, by load path (if a file is validated, see validate()) */
  private final Map<String, List<String>> systems = new HashMap<>();
  /** the files whose resources the last finish validated */
  private final List<String> validated = new ArrayList<>();

  /** the build state that validating changes, as the validator and the context keep it (see RapidoTracker.buildState()) */
  private static void validates(PublisherFields pf) throws Exception {
    pf.validator = mock(InstanceValidator.class);
    pf.context = mock(SimpleWorkerContext.class);
    set(pf.validator, BaseValidator.class, "statusWarnings");
    set(pf.context, BaseWorkerContext.class, "unsupportedCodeSystems");
    set(pf.context, BaseWorkerContext.class, "codeSystemsUsed");
  }

  private static void set(Object o, Class<?> c, String field) throws Exception {
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    f.set(o, new HashSet<String>());
  }

  @SuppressWarnings("unchecked")
  private static Set<String> codeSystemsUsed(PublisherFields pf) throws Exception {
    return (Set<String>) RapidoTracker.readSet(pf.context, "codeSystemsUsed");
  }

  /** what validateFiles does for -rapido: each resource's validation is reused if it can be, else it's validated */
  private void validate(PublisherFields pf) throws Exception {
    validated.clear();
    for (FetchedFile f : pf.fileList) {
      for (FetchedResource r : f.getResources()) {
        if (pf.rapido.reuseValidation(f, r) == null) {
          validated.add(f.getLoadPath());
          codeSystemsUsed(pf).addAll(systems.getOrDefault(f.getLoadPath(), List.of()));
          pf.rapido.recordValidation(f, r, new ArrayList<>());
        }
      }
    }
  }

  /** the file system's clock for status-change times is coarse: let it move on */
  private static void tick() throws InterruptedException {
    Thread.sleep(50);
  }

  private Path temp(String name) {
    return dir.resolve("temp").resolve(name);
  }

  /** what the last build saved as what it claims in the temp folder */
  private List<String> claimed() throws Exception {
    return Files.readAllLines(temp("_rapido").resolve("claimed.txt"));
  }

  /**
   * An unchanged file's pages aren't written again in a differential build: they're kept, and claimed again, so the
   * next build doesn't take them as stale either
   */
  @Test
  void anUnchangedFilesPagesAreKeptAndClaimed() throws Exception {
    finish(build("a"));
    tick();

    PublisherFields pf = build("a2");
    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    finish(pf);

    assertTrue(Files.exists(temp("Patient-b.html")));
    assertTrue(claimed().contains(temp("Patient-b.html").toString().toLowerCase()), claimed().toString());
  }

  /**
   * What a build without -watch claimed is what the next watch build compares with: a page that only it claimed (here
   * one the build claims besides the files' outputs) is deleted when the watch build doesn't claim it
   */
  @Test
  void whatABuildWithoutWatchClaimedIsDeletedWhenTheNextBuildDoesnt() throws Exception {
    Path extra = temp("extra.html");
    PublisherFields pf = build("a");
    Files.writeString(extra, "extra");
    pf.otherFilesRun.add(extra.toString());
    finish(pf);
    tick();

    watch = false;
    pf = build("a");
    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    pf.otherFilesRun.add(extra.toString());
    finish(pf);
    tick();

    watch = true;
    pf = build("a");
    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    finish(pf);
    assertFalse(Files.exists(extra));
    assertTrue(Files.exists(temp("Patient-b.html")));
  }

  /** without a record of what the last build claimed, what it left in the temp folder can't be told: a complete build */
  @Test
  void withoutARecordOfWhatTheLastBuildClaimedTheBuildIsComplete() throws Exception {
    finish(build("a"));
    Files.delete(temp("_rapido").resolve("claimed.txt"));

    PublisherFields pf = build("a2");

    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals("no record of what the last build claimed in the temp folder", pf.rapido.getReason());
  }

  /** each -rapido build tracks the temp folder; a differential one marks where a complete one would clear it */
  @Test
  void eachBuildTracksTheTempFolder() throws Exception {
    for (boolean w : new boolean[] {false, true}) {
      watch = w;
      PublisherFields pf = build("a", w ? 2 : 0); // a day later: a complete build
      assertTrue(pf.rapido.isCompleteBuild());
      assertTrue(pf.staleTemp != null);
      assertFalse(Files.exists(temp("_rapido").resolve("build-start")));
      finish(pf);
      tick();

      pf = build("a", w ? 2 : 0);
      assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
      assertTrue(pf.staleTemp != null);
      assertTrue(Files.exists(temp("_rapido").resolve("build-start")));
      finish(pf);
    }
  }

  /** without status-change times (Windows), a differential build says that it doesn't clean up the temp folder */
  @Test
  void withoutStatusChangeTimesTheBuildSaysItDoesntCleanUp() throws Exception {
    try (FileSystem zip = FileSystems.newFileSystem(dir.resolve("temp.zip"), Map.of("create", "true"))) {
      noStatusChangeTimes = zip;
      finish(build("a"));

      PublisherFields pf = build("a2");

      assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
      assertNull(pf.staleTemp);
      verify(pf.logger).logMessage(contains("no status-change times"));
    }
  }

  /** the output staging folder is the output: what's in it isn't stale, whatever the last build claimed */
  @Test
  void theOutputStagingFolderIsLeftAlone() throws Exception {
    finish(build("a"));
    Path tgz = temp(PublisherIGLoader.WATCH_OUTPUT_STAGING).resolve("package.tgz");
    Files.createDirectories(tgz.getParent());
    Files.writeString(tgz, "tgz");
    StaleTempFiles st = publisher.newStaleTempFiles();
    st.save(List.of(tgz.toString()));
    tick();

    st = publisher.newStaleTempFiles();
    st.mark();
    assertEquals(List.of(), st.find(List.of()));
  }

  /**
   * A reused validation leaves the build state as validating would have; a validation whose state before isn't what it
   * was is done again. A and B both use SNOMED CT; when A no longer does, B is validated again, or SNOMED CT would be
   * missing from codeSystemsUsed
   */
  @Test
  void aReusedValidationReplaysWhatValidatingDid() throws Exception {
    String sct = "http://snomed.info/sct";
    systems.put("Patient-a.json", List.of(sct));
    systems.put("Patient-b.json", List.of(sct));
    PublisherFields pf = build("a");
    validates(pf);
    finish(pf);
    assertEquals(List.of("Patient-a.json", "Patient-b.json", "Observation-o.json"), validated);

    pf = build("a");
    validates(pf);
    finish(pf);
    assertEquals(List.of(), validated);
    assertEquals(Set.of(sct), codeSystemsUsed(pf));

    systems.put("Patient-a.json", List.of());
    pf = build("a2");
    validates(pf);
    finish(pf);
    assertEquals(List.of("Patient-a.json", "Patient-b.json", "Observation-o.json"), validated);
    assertEquals(Set.of(sct), codeSystemsUsed(pf));
  }

  /** a build state that can't be read stops validations being reused, and says so once */
  @Test
  void aBuildStateThatCantBeReadIsLoggedOnce() throws Exception {
    PublisherFields pf = build("a");
    pf.validator = mock(InstanceValidator.class); // its statusWarnings is null
    FetchedFile b = file(pf, "Patient-b.json");
    pf.rapido.reuseValidation(b, b.getResources().get(0));
    pf.rapido.reuseValidation(b, b.getResources().get(0));
    verify(pf.logger, times(1)).logMessage(contains("statusWarnings"));
  }

  private FetchedFile file(String name, String content, FetchedResource... resources) {
    FetchedFile f = new FetchedFile(name);
    f.setPath(dir.resolve("input").resolve(name).toString());
    f.setSource(content.getBytes(StandardCharsets.UTF_8));
    f.getResources().addAll(List.of(resources));
    return f;
  }

  private static FetchedResource resource(String type, String id, Element... children) {
    Element element = mock(Element.class);
    NamedItemList<Element> list = new NamedItemList<>();
    list.addAll(List.of(children));
    when(element.getChildList()).thenReturn(list);
    when(element.getName()).thenReturn(type);
    when(element.fhirType()).thenReturn(type);
    when(element.getComments()).thenReturn(new ArrayList<>());
    FetchedResource resource = mock(FetchedResource.class);
    when(resource.fhirType()).thenReturn(type);
    when(resource.getId()).thenReturn(id);
    when(resource.getElement()).thenReturn(element);
    return resource;
  }

  private static Element primitive(String value) {
    Element element = mock(Element.class);
    when(element.isPrimitive()).thenReturn(true);
    when(element.getName()).thenReturn("value");
    when(element.fhirType()).thenReturn("string");
    when(element.hasValue()).thenReturn(true);
    when(element.getValue()).thenReturn(value);
    when(element.primitiveValue()).thenReturn(value);
    when(element.getComments()).thenReturn(new ArrayList<>());
    return element;
  }
}
