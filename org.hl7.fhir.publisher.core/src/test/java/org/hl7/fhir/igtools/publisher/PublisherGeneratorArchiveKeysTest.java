package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.CanonicalResource;
import org.hl7.fhir.model.core.CodeSystem;
import org.hl7.fhir.model.core.ImplementationGuide;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.utilities.formats.OutputStyle;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.hl7.fhir.services.elementmodel.Manager;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.services.validation.ValidatorSession;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.validation.ValidatorSettings;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The keys that decide whether a -rapido build keeps an archive of the package step: a change in anything that goes
 * into the archive must change its key. Each test runs builds of the same IG folder as far as the keys go: the files
 * are loaded and analysed, the keys are made, and the state is saved for the next build.
 */
class PublisherGeneratorArchiveKeysTest {

  private static SimpleWorkerContext context;
  private static FilesystemPackageCacheManager pcm;
  private static InstanceValidator validator;
  private static String noContext;

  @BeforeAll
  static void loadContext() throws Exception {
    try {
      pcm = new FilesystemPackageCacheManager.Builder().build();
      NpmPackage npm = pcm.loadPackage("hl7.fhir.r5.core", "5.0.0");
      context = new SimpleWorkerContext.SimpleWorkerContextBuilder(ModelContext.fullCoreContext()).withAllowLoadingDuplicates(true).fromPackage(npm);
      validator = new InstanceValidator(context, null, null, new ValidatorSession(), new ValidatorSettings());
    } catch (IOException | FHIRException e) {
      noContext = "hl7.fhir.r5.core#5.0.0 can't be fetched: " + e;
    }
  }

  private static final Calendar DAY = new GregorianCalendar(2026, Calendar.MARCH, 4, 10, 0, 0);

  @TempDir
  Path dir;
  /** the IG's folder */
  private Path root;
  private Path spec;
  private final Map<String, Resource> sources = new LinkedHashMap<>();

  @BeforeEach
  void setUp() throws Exception {
    assumeTrue(context != null, noContext);
    root = dir.resolve("ig");
    Files.createDirectories(root.resolve("output"));
    Files.createDirectories(root.resolve("temp").resolve("pages"));
    spec = dir.resolve("spec.internals");
    Files.write(spec, "the spec".getBytes(StandardCharsets.UTF_8));
    sources.put("CodeSystem-cs", codeSystem("a", "b"));
    ValueSet vs = new ValueSet();
    vs.setId("vs");
    vs.setUrl("http://example.org/fhir/ValueSet/vs");
    vs.getCompose().addInclude().setSystem("http://example.org/fhir/CodeSystem/cs");
    sources.put("ValueSet-vs", vs);
    CodeSystem other = codeSystem("x");
    other.setId("other");
    other.setUrl("http://example.org/fhir/CodeSystem/other");
    sources.put("CodeSystem-other", other);
  }

  private static CodeSystem codeSystem(String... codes) {
    CodeSystem cs = new CodeSystem();
    cs.setId("cs");
    cs.setUrl("http://example.org/fhir/CodeSystem/cs");
    cs.setDateElement(new org.hl7.fhir.model.core.DateTimeType("2026-01-01"));
    for (String c : codes) {
      cs.addConcept().setCode(c);
    }
    return cs;
  }

  /** a build, as far as the archive keys go */
  private class Build {
    final PublisherFields pf = new PublisherFields();
    final PublisherGenerator generator;

    Build(Calendar time) throws Exception {
      pf.pinExecTime(time);
      pf.rootDir = root.toString();
      pf.tempDir = root.resolve("temp").resolve("pages").toString();
      pf.outputDir = root.resolve("output").toString();
      // -watch: the build writes to a staging folder, which is kept from one build to the next
      pf.watchOutputDir = root.resolve("watch-output").toString();
      pf.vsCache = root.resolve("txcache").toString();
      Files.createDirectories(root.resolve("txcache"));
      pf.context = context;
      pf.pcm = pcm;
      pf.validator = validator;
      pf.version = "5.0.0";
      pf.igpkp = mock(IGKnowledgeProvider.class);
      when(pf.igpkp.getCanonical()).thenReturn("http://example.org/fhir");
      pf.sourceIg = new ImplementationGuide();
      pf.sourceIg.setId("ig");
      FetchedFile igf = new FetchedFile("ig.json");
      igf.setPath(root.resolve("ig.json").toString());
      igf.setSource("{}".getBytes(StandardCharsets.UTF_8));
      for (Map.Entry<String, Resource> e : sources.entrySet()) {
        byte[] json = new JsonParser(context.getModelContext()).setOutputStyle(OutputStyle.NORMAL).composeBytes(e.getValue());
        FetchedFile f = new FetchedFile(root.resolve(e.getKey() + ".json").toString());
        f.setPath(root.resolve(e.getKey() + ".json").toString());
        f.setSource(json);
        FetchedResource r = f.addResource(e.getKey());
        r.setElement(Manager.parseSingle(context, new ByteArrayInputStream(json), FhirFormat.JSON)).setId(e.getValue().getId());
        // the build goes on from its own copy: the snapshot, the narrative and so on are added to it, and the date of
        // the build if it has none
        Resource res = e.getValue().copy(org.hl7.fhir.model.Base.COPY_ALL);
        if (res instanceof CanonicalResource && !((CanonicalResource) res).hasDate()) {
          ((CanonicalResource) res).setDate(time.getTime());
        }
        r.setResource(res);
        pf.fileList.add(f);
      }
      pf.rapido = new RapidoTracker(pf);
      pf.rapido.analyse(igf, null);
      generator = new PublisherGenerator(new PublisherSettings());
      generator.setPf(pf);
    }

    FetchedResource resource(String name) {
      for (FetchedFile f : pf.fileList) {
        if ((f.getResources().get(0).fhirType() + "-" + f.getResources().get(0).getId()).equals(name)) {
          return f.getResources().get(0);
        }
      }
      throw new IllegalArgumentException(name);
    }

    String definitions() {
      return generator.definitionsKey(FhirFormat.JSON, spec.toString());
    }

    String pack() {
      return generator.validationPackKey(spec.toString());
    }

    /** the build ends: every file has been generated or replayed, and the state is saved */
    void finish() throws Exception {
      for (FetchedFile f : pf.fileList) {
        for (FetchedResource r : f.getResources()) {
          if (pf.rapido.reuseValidation(f, r) == null) {
            pf.rapido.recordValidation(f, r, new ArrayList<>());
          }
        }
      }
      pf.rapido.finishValidation();
      for (FetchedFile f : pf.fileList) {
        for (String phase : new String[] {RapidoTracker.PHASE_HTML, RapidoTracker.PHASE_SHEETS}) {
          if (!pf.rapido.isCompleteBuild() && pf.rapido.canReplay(f, phase)) {
            pf.rapido.replay(f, phase);
          } else {
            pf.rapido.startRecording(f, phase);
            pf.rapido.stopRecording();
          }
        }
      }
      new RapidoOutputs(pf.vsCache, pf.tempDir).update(pf.fileList, pf.fileList);
      pf.rapido.save();
    }
  }

  /** the first build of the folder: a complete one, that makes both keys and saves its state */
  private String[] firstBuild() throws Exception {
    Build b = new Build(DAY);
    assertTrue(b.pf.rapido.isCompleteBuild());
    String[] keys = {b.definitions(), b.pack()};
    assertNotNull(keys[0]);
    assertNotNull(keys[1]);
    b.finish();
    return keys;
  }

  private Build differentialBuild() throws Exception {
    Build b = new Build(DAY);
    assertFalse(b.pf.rapido.isCompleteBuild(), b.pf.rapido.getReason());
    return b;
  }

  @Test
  void nothingChangedSameKeys() throws Exception {
    String[] first = firstBuild();
    Build b = differentialBuild();
    assertEquals(0, b.pf.rapido.countRenderDirty());
    assertEquals(first[0], b.definitions());
    assertEquals(first[1], b.pack());
    b.finish();
    // and again, from the hashes that build carried over
    Build c = differentialBuild();
    assertEquals(first[0], c.definitions());
    assertEquals(first[1], c.pack());
  }

  @Test
  void whatTheBuildPutsInAFileItFoundUnchangedIsInTheKeys() throws Exception {
    String[] first = firstBuild();
    Build b = differentialBuild();
    assertEquals(0, b.pf.rapido.countRenderDirty());
    // the file is as it was, but this build leaves its resource another way (a complete build renders every value
    // set, a differential build only some of them, and rendering one changes it)
    ((CodeSystem) b.resource("CodeSystem-other").getResource()).setTitle("added by the build");
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
  }

  @Test
  void aChangedResourceChangesTheKeys() throws Exception {
    String[] first = firstBuild();
    sources.put("CodeSystem-cs", codeSystem("a", "b", "c"));
    Build b = differentialBuild();
    assertEquals(2, b.pf.rapido.countRenderDirty(), "the code system, and the value set that includes it");
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
  }

  @Test
  void whatTheBuildAddsToAChangedResourceIsInTheKeys() throws Exception {
    firstBuild();
    sources.put("CodeSystem-cs", codeSystem("a", "b", "c"));
    Build plain = differentialBuild();
    String key = plain.definitions();
    Build b = differentialBuild();
    // the value set is rendered again because its code system changed: this time its narrative comes out different
    b.resource("ValueSet-vs").getResource().setLanguage("en");
    assertNotEquals(key, b.definitions());
  }

  /** the value set of the IG, expanded by the build: its expansion is in the resource the build leaves */
  private Build withExpansion(String identifier, String time, String... codes) throws Exception {
    Build b = differentialBuild();
    ValueSet vs = (ValueSet) b.resource("ValueSet-vs").getResource();
    vs.getExpansion().setIdentifier(identifier);
    vs.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType(time));
    for (String c : codes) {
      vs.getExpansion().addContains().setSystem("http://example.org/fhir/CodeSystem/cs").setCode(c);
    }
    return b;
  }

  @Test
  void whenAValueSetWasExpandedIsNotInTheKeys() throws Exception {
    firstBuild();
    Build b = withExpansion("urn:uuid:1", "2026-03-04T10:00:00Z", "a", "b");
    String definitions = b.definitions();
    String pack = b.pack();
    // making the key leaves the resource as it was
    ValueSet vs = (ValueSet) b.resource("ValueSet-vs").getResource();
    assertEquals("urn:uuid:1", vs.getExpansion().getIdentifier());
    assertEquals("2026-03-04T10:00:00Z", vs.getExpansion().getTimestampElement().asStringValue());

    // every build expands it again: the same codes, with a new identifier and timestamp
    Build again = withExpansion("urn:uuid:2", "2026-03-04T10:07:00Z", "a", "b");
    assertEquals(definitions, again.definitions());
    assertEquals(pack, again.pack());
    assertEquals(definitions, again.generator.definitionsKey(FhirFormat.JSON, spec.toString()), "and once more in the same build");

    Build other = withExpansion("urn:uuid:1", "2026-03-04T10:00:00Z", "a", "b", "c");
    assertNotEquals(definitions, other.definitions());
    assertNotEquals(pack, other.pack());
    Build xml = withExpansion("urn:uuid:3", "2026-03-04T10:09:00Z", "a", "b");
    assertEquals(b.generator.definitionsKey(FhirFormat.XML, spec.toString()), xml.generator.definitionsKey(FhirFormat.XML, spec.toString()));
  }

  @Test
  void anAddedResourceChangesTheKeys() throws Exception {
    String[] first = firstBuild();
    CodeSystem added = codeSystem("z");
    added.setId("added");
    added.setUrl("http://example.org/fhir/CodeSystem/added");
    sources.put("CodeSystem-added", added);
    Build b = new Build(DAY);
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
  }

  @Test
  void aRemovedResourceChangesTheKeys() throws Exception {
    String[] first = firstBuild();
    sources.remove("CodeSystem-other");
    Build b = new Build(DAY);
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
  }

  @Test
  void theFormatAndTheVersionAreInTheKey() throws Exception {
    Build b = new Build(DAY);
    String json = b.definitions();
    assertNotEquals(json, b.generator.definitionsKey(FhirFormat.XML, spec.toString()));
    b.pf.version = "4.0.1";
    assertNotEquals(json, b.definitions());
  }

  @Test
  void theSpecFileIsInTheKeys() throws Exception {
    String[] first = firstBuild();
    Files.write(spec, "another spec".getBytes(StandardCharsets.UTF_8));
    Build b = differentialBuild();
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
  }

  @Test
  void theTimeOfTheBuildIsInTheKeys() throws Exception {
    String[] first = firstBuild();
    // a build at another time: the resources that have no date get another one, and the pack says when it was built
    Calendar later = (Calendar) DAY.clone();
    later.add(Calendar.MINUTE, 5);
    Build b = new Build(later);
    assertFalse(b.pf.rapido.isCompleteBuild(), b.pf.rapido.getReason());
    assertEquals(0, b.pf.rapido.countRenderDirty());
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
  }

  @Test
  void thePackSaysWhenItWasBuiltAndTheDefinitionsDoNot() throws Exception {
    for (Resource r : sources.values()) {
      ((CanonicalResource) r).setDateElement(new org.hl7.fhir.model.core.DateTimeType("2026-01-01"));
    }
    String[] first = firstBuild();
    Calendar later = (Calendar) DAY.clone();
    later.add(Calendar.MINUTE, 5);
    Build b = new Build(later);
    assertEquals(first[0], b.definitions(), "every resource has its own date");
    assertNotEquals(first[1], b.pack(), "version.info has the time of the build");
  }

  @Test
  void theFilesZippedIntoThePackAreInItsKey() throws Exception {
    String[] first = firstBuild();
    Files.write(root.resolve("output").resolve("StructureDefinition-x.sch"), "<schema/>".getBytes(StandardCharsets.UTF_8));
    Build b = differentialBuild();
    String withSch = b.pack();
    assertNotEquals(first[1], withSch);
    assertEquals(first[0], b.definitions(), "it isn't in the definitions");
    Files.write(root.resolve("output").resolve("StructureDefinition-x.sch"), "<schema> </schema>".getBytes(StandardCharsets.UTF_8));
    assertNotEquals(withSch, b.pack());
  }

  @Test
  void twoResourcesUnderTheSameNameHaveNoKey() throws Exception {
    Build b = new Build(DAY);
    // the zip can't be written with two entries of the same name, so there is nothing to keep
    FetchedFile f = new FetchedFile("again");
    FetchedResource r = f.addResource("x");
    r.setResource(codeSystem("q"));
    r.setId("cs");
    b.pf.fileList.add(f);
    assertNull(b.definitions());
    assertNull(b.pack());
  }

  @Test
  void aBuildWithoutRapidoHasNoKeys() throws Exception {
    // without -watch the output folder is empty when the build starts: nothing to keep, so no keys are made
    Build plain = new Build(DAY);
    plain.pf.watchOutputDir = null;
    assertNull(plain.definitions());
    assertNull(plain.pack());
    assertNull(plain.generator.filesKey("csvs.zip", Arrays.asList(spec.toString())));
    Build b = new Build(DAY);
    b.pf.rapido = null;
    assertNull(b.definitions());
    assertNull(b.pack());
    assertNull(b.generator.expansionsKey());
    assertNull(b.generator.filesKey("csvs.zip", Arrays.asList(spec.toString())));
  }

  @Test
  void theKeyOfAZipOfFilesIsTheirNamesAndContent() throws Exception {
    Build b = new Build(DAY);
    Path one = root.resolve("output").resolve("one.csv");
    Path two = root.resolve("output").resolve("two.csv");
    Files.write(one, "1".getBytes(StandardCharsets.UTF_8));
    Files.write(two, "2".getBytes(StandardCharsets.UTF_8));
    List<String> both = Arrays.asList(one.toString(), two.toString());
    String key = b.generator.filesKey("csvs.zip", both);
    assertEquals(key, b.generator.filesKey("csvs.zip", Arrays.asList(two.toString(), one.toString())), "whatever the order");
    assertNotEquals(key, b.generator.filesKey("excels.zip", both));
    assertNotEquals(key, b.generator.filesKey("csvs.zip", Arrays.asList(one.toString())));
    Files.write(two, "3".getBytes(StandardCharsets.UTF_8));
    assertNotEquals(key, b.generator.filesKey("csvs.zip", both));
    Files.write(two, "2".getBytes(StandardCharsets.UTF_8));
    assertEquals(key, b.generator.filesKey("csvs.zip", both));
    Files.move(two, root.resolve("output").resolve("three.csv"));
    assertNotEquals(key, b.generator.filesKey("csvs.zip", Arrays.asList(one.toString(), root.resolve("output").resolve("three.csv").toString())));
    assertNull(b.generator.filesKey("csvs.zip", both), "a file that can't be read");
  }

  @Test
  void theExpansionsKeyHasTheExpansionsAndTheTimeOfTheBuild() throws Exception {
    List<String> journal = Arrays.asList("{\"resourceType\":\"ValueSet\",\"id\":\"a\"}");
    Build b = new Build(DAY);
    b.pf.expansions.addJournaled(journal, SavedExpansions.hash(journal));
    String key = b.generator.expansionsKey();
    assertNotNull(key);

    Build same = new Build(DAY);
    same.pf.expansions.addJournaled(journal, SavedExpansions.hash(journal));
    assertEquals(key, same.generator.expansionsKey());

    // an expansion in one more language
    Build more = new Build(DAY);
    more.pf.expansions.addJournaled(journal, SavedExpansions.hash(journal));
    more.pf.expansions.addJournaled(journal, SavedExpansions.hash(journal));
    assertNotEquals(key, more.generator.expansionsKey());

    Calendar later = (Calendar) DAY.clone();
    later.add(Calendar.MINUTE, 5);
    Build after = new Build(later);
    after.pf.expansions.addJournaled(journal, SavedExpansions.hash(journal));
    assertNotEquals(key, after.generator.expansionsKey(), "the bundle says when it was updated");

    Build fresh = new Build(DAY);
    fresh.pf.expansions.add(new ValueSet());
    assertNull(fresh.generator.expansionsKey(), "an expansion that isn't in a journal");
  }

  // ---- keeping ------------------------------------------------------------------------------------------------------

  private static final FileTime THEN = FileTime.fromMillis(1_700_000_000_000L);

  /** Jekyll runs: it removes what it didn't write from the output folder */
  private void jekyll(Build b) throws Exception {
    b.pf.rapido.getArchives().beforeJekyll();
    for (String name : new String[] {"csvs.zip", "expansions.json", "expansions.xml", "expansions.json.zip", "expansions.xml.zip",
      "definitions.json.zip", "definitions.xml.zip", "validator.pack", "validator-ig.pack", "examples.json.zip", "examples.xml.zip"}) {
      Files.deleteIfExists(root.resolve("output").resolve(name));
    }
  }

  private FileTime time(String name) throws Exception {
    return Files.getLastModifiedTime(root.resolve("output").resolve(name));
  }

  @Test
  void aZipOfUnchangedFilesIsKept() throws Exception {
    Path csv = root.resolve("output").resolve("one.csv");
    Files.write(csv, "1".getBytes(StandardCharsets.UTF_8));
    Files.write(root.resolve("output").resolve("two.csv"), "second".getBytes(StandardCharsets.UTF_8));
    Build first = new Build(DAY);
    first.generator.generateOutputZip("csvs.zip", ".csv");
    Files.setLastModifiedTime(root.resolve("output").resolve("csvs.zip"), THEN);
    first.finish();

    Build second = differentialBuild();
    jekyll(second);
    second.generator.generateOutputZip("csvs.zip", ".csv");
    assertEquals(THEN, time("csvs.zip"), "the file of the first build");
    assertEquals(Arrays.asList("csvs.zip"), second.pf.rapido.getArchives().getKept());
    second.finish();

    Files.write(csv, "2".getBytes(StandardCharsets.UTF_8));
    Build third = differentialBuild();
    jekyll(third);
    third.generator.generateOutputZip("csvs.zip", ".csv");
    assertNotEquals(THEN, time("csvs.zip"), "written again");
    try (ZipFile zip = new ZipFile(root.resolve("output").resolve("csvs.zip").toFile())) {
      assertEquals("2", new String(zip.getInputStream(zip.getEntry("one.csv")).readAllBytes(), StandardCharsets.UTF_8));
      assertEquals("second", new String(zip.getInputStream(zip.getEntry("two.csv")).readAllBytes(), StandardCharsets.UTF_8));
      assertEquals(2, zip.size());
    }
    // the entry that didn't change comes from the zip of the last build
    assertEquals("its content changed, 1 of its 2 entries as they were", third.pf.rapido.getArchives().getRewritten().get("csvs.zip"));
    third.finish();

    // a complete build writes it whatever the last build recorded
    Files.setLastModifiedTime(root.resolve("output").resolve("csvs.zip"), THEN);
    sources.put("CodeSystem-added", codeSystem("z"));
    Build complete = new Build(DAY);
    assertTrue(complete.pf.rapido.isCompleteBuild());
    jekyll(complete);
    complete.generator.generateOutputZip("csvs.zip", ".csv");
    assertNotEquals(THEN, time("csvs.zip"));
  }

  @Test
  void unchangedExpansionsAreKeptAndNotParsed() throws Exception {
    ValueSet vs = new ValueSet();
    vs.setId("a");
    vs.setUrl("http://example.org/fhir/ValueSet/a");
    vs.getExpansion().addContains().setCode("c1");
    List<String> journal = Arrays.asList(RapidoTracker.saveExpansion(new JsonParser(context.getModelContext()), vs));
    String hash = SavedExpansions.hash(journal);
    String[] names = {"expansions.json", "expansions.xml", "expansions.json.zip", "expansions.xml.zip"};

    Build first = new Build(DAY);
    first.pf.expansions.addJournaled(journal, hash);
    first.generator.generateExpansions();
    for (String name : names) {
      assertTrue(Files.size(root.resolve("output").resolve(name)) > 0, name);
      Files.setLastModifiedTime(root.resolve("output").resolve(name), THEN);
    }
    assertTrue(new String(Files.readAllBytes(root.resolve("output").resolve("expansions.json")), StandardCharsets.UTF_8).contains("\"c1\""));
    first.finish();

    // the journal says the expansions are the same: they aren't read (these couldn't be)
    Build second = differentialBuild();
    jekyll(second);
    second.pf.expansions.addJournaled(Arrays.asList("not parsed"), hash);
    second.generator.generateExpansions();
    for (String name : names) {
      assertEquals(THEN, time(name), name);
    }
    second.finish();

    // an expansion with another code: all four files are written again
    vs.getExpansion().addContains().setCode("c2");
    List<String> changed = Arrays.asList(RapidoTracker.saveExpansion(new JsonParser(context.getModelContext()), vs));
    Build third = differentialBuild();
    jekyll(third);
    third.pf.expansions.addJournaled(changed, SavedExpansions.hash(changed));
    third.generator.generateExpansions();
    for (String name : names) {
      assertNotEquals(THEN, time(name), name);
    }
    assertTrue(new String(Files.readAllBytes(root.resolve("output").resolve("expansions.xml")), StandardCharsets.UTF_8).contains("\"c2\""));
  }

  /** the build generates a file that makes this expansion, and journals it */
  private String expansionsKeyOf(Build b, String identifier, String... codes) throws Exception {
    ValueSet vs = new ValueSet();
    vs.setId("a");
    vs.setUrl("http://example.org/fhir/ValueSet/a");
    vs.getExpansion().setIdentifier(identifier);
    vs.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType("2026-03-04T10:00:" + identifier.substring(identifier.length() - 2) + "Z"));
    for (String c : codes) {
      vs.getExpansion().addContains().setSystem("http://example.org/fhir/CodeSystem/cs").setCode(c);
    }
    FetchedFile f = b.pf.fileList.get(1);
    b.pf.rapido.startRecording(f, RapidoTracker.PHASE_HTML);
    b.pf.expansions.add(vs);
    b.pf.rapido.stopRecording();
    return b.generator.expansionsKey();
  }

  @Test
  void anExpansionGeneratedAgainWithTheSameContentKeepsTheKey() throws Exception {
    String key = expansionsKeyOf(new Build(DAY), "urn:uuid:11", "a", "b");
    assertNotNull(key);
    // the same codes, generated again: only its identifier and timestamp are new
    assertEquals(key, expansionsKeyOf(new Build(DAY), "urn:uuid:22", "a", "b"));
    // the value set is the same, but its code system has another code now
    assertNotEquals(key, expansionsKeyOf(new Build(DAY), "urn:uuid:33", "a", "b", "c"));
    assertNotEquals(key, expansionsKeyOf(new Build(DAY), "urn:uuid:11", "a"));
  }

  /** what a zip holds: name to content, in the order of its entries */
  private static Map<String, String> entries(Path zip) throws Exception {
    Map<String, String> res = new LinkedHashMap<>();
    try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
      for (java.util.zip.ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        res.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    return res;
  }

  @Test
  void aBuildWithoutRapidoZipsTheFilesInTheOrderItAlwaysDid() throws Exception {
    for (int i = 0; i < 40; i++) {
      Files.write(root.resolve("output").resolve("file-" + (i * 7919) + ".csv"), ("content " + i).getBytes(StandardCharsets.UTF_8));
    }
    java.util.Set<String> names = new java.util.HashSet<>();
    for (String n : root.resolve("output").toFile().list()) {
      if (n.endsWith(".csv")) {
        names.add(n);
      }
    }
    assertEquals(40, names.size());
    Build b = new Build(DAY);
    b.pf.rapido = null;
    b.generator.generateOutputZip("csvs.zip", ".csv");
    // (the order of a set of the names, which is what was zipped before)
    assertEquals(new ArrayList<>(names), new ArrayList<>(entries(root.resolve("output").resolve("csvs.zip")).keySet()));
  }

  /** the definitions zip and validator.pack as a build without -rapido writes them for the sources as they are now */
  private Map<String, Map<String, String>> plainArchives(String version) throws Exception {
    Build b = new Build(DAY);
    b.pf.rapido = null;
    b.pf.version = version;
    Path out = Files.createDirectories(dir.resolve("plain-" + System.nanoTime()));
    b.pf.outputDir = out.toString();
    b.generator.generateDefinitions(FhirFormat.JSON, spec.toString());
    b.generator.generateValidationPack(spec.toString());
    Map<String, Map<String, String>> res = new LinkedHashMap<>();
    res.put("definitions.json.zip", entries(out.resolve("definitions.json.zip")));
    res.put("validator.pack", entries(out.resolve("validator.pack")));
    assertEquals(res.get("validator.pack"), entries(out.resolve("validator-ig.pack")));
    return res;
  }

  private void theArchivesOfADifferentialBuildHoldWhatAPlainBuildWrites(String version) throws Exception {
    Build first = new Build(DAY);
    first.pf.version = version;
    first.generator.generateDefinitions(FhirFormat.JSON, spec.toString());
    first.generator.generateValidationPack(spec.toString());
    assertEquals(plainArchives(version).get("definitions.json.zip"), entries(root.resolve("output").resolve("definitions.json.zip")));
    for (String name : new String[] {"definitions.json.zip", "validator.pack"}) {
      Files.setLastModifiedTime(root.resolve("output").resolve(name), THEN);
      // every entry of the zip has a token, so the key stands for all of it
      assertEquals(new java.util.TreeSet<>(entries(root.resolve("output").resolve(name)).keySet()),
          new java.util.TreeSet<>(first.pf.rapido.getArchives().getRecords().getJsonObject(name).getJsonObject("entries").getNames()), name);
    }
    Files.setLastModifiedTime(root.resolve("output").resolve("validator-ig.pack"), THEN);
    assertEquals(new java.util.TreeSet<>(Arrays.asList("validator.pack", "validator-ig.pack")),
        new java.util.TreeSet<>(first.pf.rapido.getArchives().getRecords().getJsonObject("validator.pack").getJsonObject("files").getNames()));
    first.finish();

    // nothing changed: both are kept
    Build second = differentialBuild();
    second.pf.version = version;
    jekyll(second);
    second.generator.generateDefinitions(FhirFormat.JSON, spec.toString());
    second.generator.generateValidationPack(spec.toString());
    assertEquals(THEN, time("definitions.json.zip"));
    assertEquals(THEN, time("validator.pack"));
    assertEquals(THEN, time("validator-ig.pack"));
    second.finish();

    // a code system changes: both are written again, with the other entries as they were
    sources.put("CodeSystem-cs", codeSystem("a", "b", "c"));
    Build third = differentialBuild();
    third.pf.version = version;
    jekyll(third);
    third.generator.generateDefinitions(FhirFormat.JSON, spec.toString());
    third.generator.generateValidationPack(spec.toString());
    assertNotEquals(THEN, time("definitions.json.zip"));
    assertEquals("its content changed, 3 of its 4 entries as they were", third.pf.rapido.getArchives().getRewritten().get("definitions.json.zip"));
    assertEquals("its content changed, 4 of its 5 entries as they were", third.pf.rapido.getArchives().getRewritten().get("validator.pack"));
    Map<String, Map<String, String>> plain = plainArchives(version);
    assertTrue(plain.get("definitions.json.zip").get("CodeSystem-cs.json").contains("\"c\""));
    assertEquals(new java.util.TreeMap<>(plain.get("definitions.json.zip")), new java.util.TreeMap<>(entries(root.resolve("output").resolve("definitions.json.zip"))));
    assertEquals(new java.util.TreeMap<>(plain.get("validator.pack")), new java.util.TreeMap<>(entries(root.resolve("output").resolve("validator.pack"))));
    assertEquals(entries(root.resolve("output").resolve("validator.pack")), entries(root.resolve("output").resolve("validator-ig.pack")));
  }

  @Test
  void theArchivesOfADifferentialBuildHoldWhatAPlainBuildWrites() throws Exception {
    theArchivesOfADifferentialBuildHoldWhatAPlainBuildWrites("5.0.0");
  }

  @Test
  void theArchivesOfADifferentialBuildHoldWhatAPlainBuildWritesInAnotherVersion() throws Exception {
    theArchivesOfADifferentialBuildHoldWhatAPlainBuildWrites("4.0.1");
  }

  @Test
  void theJournalKeepsWhatStandsForItsExpansions() throws Exception {
    Build first = new Build(DAY);
    ValueSet vs = new ValueSet();
    vs.setId("a");
    vs.getExpansion().setIdentifier("urn:uuid:1");
    vs.getExpansion().addContains().setCode("c1");
    for (FetchedFile ff : first.pf.fileList) {
      for (String phase : new String[] {RapidoTracker.PHASE_HTML, RapidoTracker.PHASE_SHEETS}) {
        first.pf.rapido.startRecording(ff, phase);
        if (ff == first.pf.fileList.get(1) && phase.equals(RapidoTracker.PHASE_HTML)) {
          first.pf.expansions.add(vs);
        }
        first.pf.rapido.stopRecording();
      }
    }
    String key = first.generator.expansionsKey();
    assertNotNull(key);
    for (FetchedFile ff : first.pf.fileList) {
      for (FetchedResource r : ff.getResources()) {
        first.pf.rapido.reuseValidation(ff, r);
        first.pf.rapido.recordValidation(ff, r, new ArrayList<>());
      }
    }
    first.pf.rapido.finishValidation();
    new RapidoOutputs(first.pf.vsCache, first.pf.tempDir).update(first.pf.fileList, first.pf.fileList);
    first.pf.rapido.save();

    // the next build replays the file: the expansion comes back as it was saved, with the hash, and isn't parsed
    Build second = differentialBuild();
    for (FetchedFile ff : second.pf.fileList) {
      for (String phase : new String[] {RapidoTracker.PHASE_HTML, RapidoTracker.PHASE_SHEETS}) {
        assertTrue(second.pf.rapido.canReplay(ff, phase));
        RapidoTracker.Journal j = second.pf.rapido.replay(ff, phase);
        if (ff == second.pf.fileList.get(1) && phase.equals(RapidoTracker.PHASE_HTML)) {
          assertEquals(1, j.expansions.size());
          assertTrue(j.expansions.get(0).contains("urn:uuid:1"));
        }
        assertNotNull(j.expansionsHash);
        second.pf.expansions.addJournaled(j.expansions, j.expansionsHash);
      }
    }
    assertEquals(key, second.generator.expansionsKey());
  }

  /** the entries of a zip that is an entry of the pack */
  private static Map<String, String> nested(Path pack, String name) throws Exception {
    try (ZipFile zip = new ZipFile(pack.toFile())) {
      Path tmp = Files.createTempFile("nested", ".zip");
      try {
        Files.write(tmp, zip.getInputStream(zip.getEntry(name)).readAllBytes());
        return entries(tmp);
      } finally {
        Files.delete(tmp);
      }
    }
  }

  @Test
  void thePackHoldsTheZipsOfTheSchematronsSchemasAndShex() throws Exception {
    Path out = root.resolve("output");
    Files.write(out.resolve("x.sch"), "<schema/>".getBytes(StandardCharsets.UTF_8));
    Files.write(out.resolve("x.schema.json"), "{}".getBytes(StandardCharsets.UTF_8));
    Files.write(out.resolve("x.shex"), "shex".getBytes(StandardCharsets.UTF_8));
    List<String> names = Arrays.asList("version.info", "spec.internals", "CodeSystem-cs.json", "ValueSet-vs.json", "CodeSystem-other.json",
        "schematron.zip", "json.schema.zip", "shex.zip");

    // a build without -rapido: the entries, in the order they always had
    Build plain = new Build(DAY);
    plain.pf.rapido = null;
    plain.generator.generateValidationPack(spec.toString());
    assertEquals(names, new ArrayList<>(entries(out.resolve("validator.pack")).keySet()));
    assertEquals(Collections.singletonMap("x.sch", "<schema/>"), nested(out.resolve("validator.pack"), "schematron.zip"));
    assertEquals(Collections.singletonMap("x.schema.json", "{}"), nested(out.resolve("validator.pack"), "json.schema.zip"));
    assertEquals(Collections.singletonMap("x.shex", "shex"), nested(out.resolve("validator.pack"), "shex.zip"));

    Build first = new Build(DAY);
    first.generator.generateValidationPack(spec.toString());
    first.finish();

    // a schematron changes: its zip is made again, the rest is as it was
    Files.write(out.resolve("x.sch"), "<schema> </schema>".getBytes(StandardCharsets.UTF_8));
    Build second = differentialBuild();
    jekyll(second);
    second.generator.generateValidationPack(spec.toString());
    assertEquals("its content changed, 7 of its 8 entries as they were", second.pf.rapido.getArchives().getRewritten().get("validator.pack"));
    assertEquals(names, new ArrayList<>(entries(out.resolve("validator.pack")).keySet()));
    assertEquals(Collections.singletonMap("x.sch", "<schema> </schema>"), nested(out.resolve("validator.pack"), "schematron.zip"));
    assertEquals(Collections.singletonMap("x.shex", "shex"), nested(out.resolve("validator.pack"), "shex.zip"));
    second.finish();

    // no shex files any more: no zip of them
    Files.delete(out.resolve("x.shex"));
    Build third = differentialBuild();
    jekyll(third);
    third.generator.generateValidationPack(spec.toString());
    assertEquals(names.subList(0, 7), new ArrayList<>(entries(out.resolve("validator.pack")).keySet()));
  }

  @Test
  void theExampleZipsAreKeptWhileTheExamplesAreAsTheyWere() throws Exception {
    Path out = root.resolve("output");
    sources.forEach((name, r) -> {
      try {
        Files.write(out.resolve(name + ".json"), ("json of " + name).getBytes(StandardCharsets.UTF_8));
        Files.write(out.resolve(name + ".xml"), ("xml of " + name).getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
    Build plain = new Build(DAY);
    plain.pf.rapido = null;
    plain.pf.fileList.forEach(f -> f.getResources().get(0).setExampleUri("http://example.org/fhir/StructureDefinition/p"));
    assertTrue(plain.generator.generateExampleZip(FhirFormat.JSON));
    Map<String, String> expected = entries(out.resolve("examples.json.zip"));
    assertEquals(3, expected.size());

    Build first = new Build(DAY);
    first.pf.fileList.forEach(f -> f.getResources().get(0).setExampleUri("http://example.org/fhir/StructureDefinition/p"));
    assertTrue(first.generator.generateExampleZip(FhirFormat.JSON));
    assertTrue(first.generator.generateExampleZip(FhirFormat.XML));
    assertEquals(expected, entries(out.resolve("examples.json.zip")));
    assertEquals("xml of CodeSystem-cs", entries(out.resolve("examples.xml.zip")).get("CodeSystem-cs.xml"));
    assertNotEquals(first.pf.rapido.getArchives().getRecords().getJsonObject("examples.json.zip").asString("key"),
        first.pf.rapido.getArchives().getRecords().getJsonObject("examples.xml.zip").asString("key"));
    Files.setLastModifiedTime(out.resolve("examples.json.zip"), THEN);
    Files.setLastModifiedTime(out.resolve("examples.xml.zip"), THEN);
    first.finish();

    // one json example changes: its zip is written again with the other entries as they were, the xml zip is kept
    Files.write(out.resolve("CodeSystem-cs.json"), "json of CodeSystem-cs, changed".getBytes(StandardCharsets.UTF_8));
    Build second = differentialBuild();
    second.pf.fileList.forEach(f -> f.getResources().get(0).setExampleUri("http://example.org/fhir/StructureDefinition/p"));
    jekyll(second);
    assertTrue(second.generator.generateExampleZip(FhirFormat.JSON));
    assertTrue(second.generator.generateExampleZip(FhirFormat.XML));
    assertEquals(THEN, time("examples.xml.zip"));
    assertEquals("its content changed, 2 of its 3 entries as they were", second.pf.rapido.getArchives().getRewritten().get("examples.json.zip"));
    expected.put("CodeSystem-cs.json", "json of CodeSystem-cs, changed");
    assertEquals(new java.util.TreeMap<>(expected), new java.util.TreeMap<>(entries(out.resolve("examples.json.zip"))));
  }

  @Test
  void aBuildThatIsNotAWatchBuildKeepsNothingOfAWatchBuild() throws Exception {
    Path out = root.resolve("output");
    Files.write(out.resolve("one.csv"), "1".getBytes(StandardCharsets.UTF_8));
    Build first = new Build(DAY);
    first.generator.generateOutputZip("csvs.zip", ".csv");
    Files.setLastModifiedTime(out.resolve("csvs.zip"), THEN);
    first.finish();

    // -rapido without -watch: the same records and the same file, but it is written as it always was
    Build second = differentialBuild();
    second.pf.watchOutputDir = null;
    second.generator.generateOutputZip("csvs.zip", ".csv");
    assertNotEquals(THEN, time("csvs.zip"));
    assertTrue(second.pf.rapido.getArchives().getKept().isEmpty());
    assertTrue(second.pf.rapido.getArchives().getRecords().getNames().isEmpty());
    assertEquals(Collections.singletonMap("one.csv", "1"), entries(out.resolve("csvs.zip")));
  }

  @Test
  void theDefinitionsInXmlAreKeptAndWrittenAgainLikeTheOnesInJson() throws Exception {
    Path zip = root.resolve("output").resolve("definitions.xml.zip");
    Build first = new Build(DAY);
    first.generator.generateDefinitions(FhirFormat.XML, spec.toString());
    assertEquals(new java.util.TreeSet<>(entries(zip).keySet()),
        new java.util.TreeSet<>(first.pf.rapido.getArchives().getRecords().getJsonObject("definitions.xml.zip").getJsonObject("entries").getNames()));
    Files.setLastModifiedTime(zip, THEN);
    first.finish();

    Build second = differentialBuild();
    jekyll(second);
    second.generator.generateDefinitions(FhirFormat.XML, spec.toString());
    assertEquals(THEN, time("definitions.xml.zip"));
    second.finish();

    sources.put("CodeSystem-cs", codeSystem("a", "b", "c"));
    Build third = differentialBuild();
    jekyll(third);
    third.generator.generateDefinitions(FhirFormat.XML, spec.toString());
    assertEquals("its content changed, 3 of its 4 entries as they were", third.pf.rapido.getArchives().getRewritten().get("definitions.xml.zip"));
    Build plain = new Build(DAY);
    plain.pf.rapido = null;
    plain.pf.outputDir = Files.createDirectories(dir.resolve("plain-xml")).toString();
    plain.generator.generateDefinitions(FhirFormat.XML, spec.toString());
    assertEquals(new java.util.TreeMap<>(entries(dir.resolve("plain-xml").resolve("definitions.xml.zip"))), new java.util.TreeMap<>(entries(zip)));
  }
}
