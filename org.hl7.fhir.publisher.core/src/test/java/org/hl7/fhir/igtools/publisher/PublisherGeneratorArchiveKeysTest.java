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
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.CodeSystem;
import org.hl7.fhir.model.core.ImplementationGuide;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.utilities.formats.OutputStyle;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.hl7.fhir.services.elementmodel.Manager;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.services.utilities.NPMPackageGenerator;
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
        // the build goes on from its own copy: the snapshot, the narrative and so on are added to it
        r.setResource(e.getValue().copy(org.hl7.fhir.model.Base.COPY_ALL));
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
  void aCleanFileIsNotComposedToMakeTheKey() throws Exception {
    String[] first = firstBuild();
    Build b = differentialBuild();
    // (not something a build does to a file it found unchanged: it shows that the hash is the last build's)
    ((CodeSystem) b.resource("CodeSystem-other").getResource()).setTitle("not looked at");
    assertEquals(first[0], b.definitions());
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
    // a build without -watch on the same day: the resources that have no date get another one
    Calendar later = (Calendar) DAY.clone();
    later.add(Calendar.MINUTE, 5);
    Build b = new Build(later);
    assertFalse(b.pf.rapido.isCompleteBuild(), b.pf.rapido.getReason());
    assertNotEquals(first[0], b.definitions());
    assertNotEquals(first[1], b.pack());
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
  void aResourceThatCannotBeToldHasNoKey() throws Exception {
    Build b = new Build(DAY);
    // not one of the build's files, so nothing is known about it
    FetchedFile f = new FetchedFile("regenerated");
    FetchedResource r = f.addResource("x");
    r.setResource(codeSystem("q"));
    r.setId("q");
    b.pf.fileList.add(f);
    assertNull(b.definitions());
    assertNull(b.pack());
  }

  @Test
  void aBuildWithoutRapidoHasNoKeys() throws Exception {
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

  // ---- the combined package ------------------------------------------------------------------------------------------

  /** a package.tgz with a profile in the package folder, an example, and what the checks put in the other folder */
  private NpmPackage packageTgz(String profile, String example, String validation) throws Exception {
    Path tgz = dir.resolve("package.tgz");
    Files.deleteIfExists(tgz);
    org.hl7.fhir.utilities.json.model.JsonObject npm = new org.hl7.fhir.utilities.json.model.JsonObject();
    npm.add("name", "example.ig");
    npm.add("version", "0.1.0");
    NPMPackageGenerator gen = new NPMPackageGenerator(tgz.toString(), npm, DAY.getTime(), true);
    gen.addFile(NPMPackageGenerator.Category.RESOURCE, "StructureDefinition-p.json",
        ("{\"resourceType\":\"StructureDefinition\",\"id\":\"p\",\"title\":\"" + profile + "\"}").getBytes(StandardCharsets.UTF_8));
    gen.addFile(NPMPackageGenerator.Category.EXAMPLE, "Patient-e.json",
        ("{\"resourceType\":\"Patient\",\"id\":\"e\",\"gender\":\"" + example + "\"}").getBytes(StandardCharsets.UTF_8));
    gen.addFile(NPMPackageGenerator.Category.OTHER, "validation-summary.json", validation.getBytes(StandardCharsets.UTF_8));
    gen.finish();
    try (InputStream in = Files.newInputStream(tgz)) {
      return NpmPackage.fromPackage(in);
    }
  }

  @Test
  void theCombinedPackageKeyHasTheConformanceResourcesOnly() throws Exception {
    Build b = new Build(DAY);
    b.pf.sourceIg.setPackageId("example.ig");
    String key = b.generator.combinedPackageKey(packageTgz("a profile", "male", "{}"));
    assertNotNull(key);
    assertEquals(key, b.generator.combinedPackageKey(packageTgz("a profile", "male", "{}")));
    // an example, and what the validation and the template's check say, aren't read to make it
    assertEquals(key, b.generator.combinedPackageKey(packageTgz("a profile", "female", "{\"errors\":1}")));
    assertNotEquals(key, b.generator.combinedPackageKey(packageTgz("another profile", "male", "{}")));
    b.pf.sourceIg.setPackageId("example.other");
    assertNotEquals(key, b.generator.combinedPackageKey(packageTgz("a profile", "male", "{}")));
    b.pf.rapido = null;
    assertNull(b.generator.combinedPackageKey(packageTgz("a profile", "male", "{}")));
  }

  // ---- keeping ------------------------------------------------------------------------------------------------------

  private static final FileTime THEN = FileTime.fromMillis(1_700_000_000_000L);

  /** Jekyll runs: it removes what it didn't write from the output folder */
  private void jekyll(Build b) throws Exception {
    b.pf.rapido.getArchives().beforeJekyll();
    for (String name : new String[] {"csvs.zip", "expansions.json", "expansions.xml", "expansions.json.zip", "expansions.xml.zip"}) {
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
    }
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
}
