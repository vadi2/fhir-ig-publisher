package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.services.elementmodel.Manager;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.ValidatorSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** the input cache over whole builds: one build stores what a step did, and the next replays it or runs it again */
class InputCacheBuildsTest {

  private static SimpleWorkerContext base;
  private static String noContext;

  @TempDir
  File root;

  @BeforeAll
  static void loadContext() throws Exception {
    try {
      NpmPackage npm = new FilesystemPackageCacheManager.Builder().build().loadPackage("hl7.fhir.r5.core", "5.0.0");
      base = new SimpleWorkerContext.SimpleWorkerContextBuilder(ModelContext.fullCoreContext()).withAllowLoadingDuplicates(true).fromPackage(npm);
    } catch (IOException | FHIRException e) {
      noContext = "hl7.fhir.r5.core#5.0.0 can't be fetched: " + e;
    }
  }

  /** folders outside the IG's (whatever is in the IG's folder is in the build key anyway) */
  private final List<File> outside = new ArrayList<>();

  @AfterEach
  void clear() throws IOException {
    FetchedReads.setListener(null);
    for (File f : outside) {
      org.hl7.fhir.utilities.FileUtilities.clearDirectory(f.getAbsolutePath());
      Files.deleteIfExists(f.toPath());
    }
  }

  private File outside(String name) throws IOException {
    File f = new File(root.getParentFile(), root.getName() + "-" + name);
    Files.createDirectories(f.toPath());
    outside.add(f);
    return f;
  }

  /** one build of an IG that has one Patient */
  class Build {
    final PublisherFields pf = new PublisherFields();
    final PublisherSettings settings = new PublisherSettings();
    final List<String> log = new ArrayList<>();
    final FetchedFile file;
    final FetchedResource res;
    InputCache ic;
    /** said by the step as well: a step whose result isn't the same every build */
    String extra;

    Build(String family) throws Exception {
      assumeTrue(noContext == null, noContext);
      pf.rootDir = root.getAbsolutePath();
      pf.tempDir = Utilities.path(pf.rootDir, "temp");
      pf.qaDir = Utilities.path(pf.rootDir, "temp", "qa");
      pf.vsCache = Utilities.path(pf.rootDir, "txcache");
      Files.createDirectories(new File(pf.tempDir).toPath());
      SimpleWorkerContext ctx = new SimpleWorkerContext(base);
      ctx.initTxCache(pf.vsCache);
      pf.context = ctx;
      pf.validator = new InstanceValidator(ctx, null, ctx.getXVer(), null, new ValidatorSettings());
      String json = "{\"resourceType\":\"Patient\",\"id\":\"p\",\"name\":[{\"family\":\"" + family + "\"}]}";
      file = new FetchedFile("p.json");
      file.setPath(Utilities.path(pf.rootDir, "input", "p.json"));
      file.setName("p");
      file.setSource(json.getBytes(StandardCharsets.UTF_8));
      res = file.addResource("p");
      res.setElement(Manager.parseSingle(ctx, new ByteArrayInputStream(file.getSource()), FhirFormat.JSON));
      res.setId("p");
      res.setType("Patient");
      pf.fileList.add(file);
    }

    InputCache open(IniFile ini) {
      ic = InputCache.create(pf, settings, ini, log::add);
      assertNotNull(ic, log.toString());
      return ic;
    }

    InputCache open() {
      return open(null);
    }

    /**
     * the step's own work: a message that says what the family name is, and a signpost on the name, which the
     * validator gives the element as well (the QA page shows an element's signposts)
     */
    List<ValidationMessage> validate() {
      List<ValidationMessage> errs = new ArrayList<>();
      String family = res.getElement().getNamedChild("name").getNamedChildValue("family");
      errs.add(new ValidationMessage(Source.InstanceValidator, IssueType.INFORMATIONAL, 1, 1, "Patient", "family is " + family, IssueSeverity.INFORMATION));
      ValidationMessage sp = new ValidationMessage(Source.InstanceValidator, IssueType.INFORMATIONAL, 1, 1, "Patient.name[0]", "Validated against HumanName", IssueSeverity.INFORMATION).setSignpost(true);
      errs.add(sp);
      res.getElement().getNamedChild("name").addMessage(sp);
      if (extra != null) {
        errs.add(new ValidationMessage(Source.InstanceValidator, IssueType.INFORMATIONAL, 1, 1, "Patient", extra, IssueSeverity.INFORMATION));
      }
      return errs;
    }

    /** the messages the elements of the resource have, by path */
    String elementMessages() {
      List<String> res = new ArrayList<>();
      elementMessages(this.res.getElement(), res);
      return res.toString();
    }

    private void elementMessages(org.hl7.fhir.services.elementmodel.Element e, List<String> res) {
      if (e.hasMessages()) {
        res.add(e.getPath() + ": " + text(e.getMessages()));
      }
      if (e.hasChildren()) {
        for (org.hl7.fhir.services.elementmodel.Element c : e.getChildList()) {
          elementMessages(c, res);
        }
      }
    }

    /** runs the validation step as the processor does: replayed or run; the messages it ends with */
    List<ValidationMessage> validationStep(boolean expectReplay) throws Exception {
      ic.startPhase(InputCache.PHASE_VALIDATION);
      try {
        InputCache.Step s = ic.beginValidation(file, res, false);
        assertNotNull(s, log.toString());
        assertEquals(expectReplay, s.replay(), log.toString());
        List<ValidationMessage> errs = new ArrayList<>();
        if (s.replay()) {
          ic.replayValidation(s, errs);
        } else {
          errs = validate();
          ic.endValidation(s, errs);
        }
        return errs;
      } finally {
        ic.endPhase();
      }
    }

    void finish() {
      ic.finish();
    }

    /** the build's date, as the html says it */
    String buildDate() {
      return new org.hl7.fhir.model.core.DateTimeType(pf.getExecTime()).asStringValue();
    }

    /** runs the html step as the generator does: a page that says when it was built. The page's path */
    File htmlStep(boolean expectReplay) throws Exception {
      File page = new File(pf.tempDir, "p.html");
      ic.startPhase(InputCache.PHASE_HTML);
      try {
        InputCache.Step s = ic.beginHtml(file);
        assertNotNull(s, log.toString());
        assertEquals(expectReplay, s.replay(), log.toString());
        if (s.replay()) {
          ic.replayHtml(s);
        } else {
          Files.write(page.toPath(), ("<p>Patient p, built " + buildDate() + "</p>").getBytes(StandardCharsets.UTF_8));
          file.getOutputNames().add(page.getAbsolutePath());
          ic.endHtml(s, true);
        }
        return page;
      } finally {
        ic.endPhase();
      }
    }
  }

  private static String text(List<ValidationMessage> errs) {
    List<String> res = new ArrayList<>();
    for (ValidationMessage vm : errs) {
      res.add(vm.getLevel() + " " + vm.getLocation() + " " + vm.getMessage());
    }
    return res.toString();
  }

  @Test
  void aSecondBuild_replaysWhatTheFirstStored_andAChangedResourceIsAMiss() throws Exception {
    Build b1 = new Build("Smith");
    b1.open();
    List<ValidationMessage> first = b1.validationStep(false);
    b1.finish();
    assertTrue(String.join("\n", b1.log).contains("0 hits"), b1.log.toString());
    assertTrue(String.join("\n", b1.log).contains("1 stored"), b1.log.toString());
    // what the first build writes (and the cache's own folder) isn't an input
    Files.createDirectories(new File(root, "output").toPath());

    Build b2 = new Build("Smith");
    b2.open();
    List<ValidationMessage> second = b2.validationStep(true);
    b2.finish();
    assertEquals(text(first), text(second));
    assertTrue(String.join("\n", b2.log).contains("1 hits"), b2.log.toString());
    // the element has the signpost, as it does when the validation runs (the QA page lists it by element)
    assertEquals(b1.elementMessages(), b2.elementMessages());
    assertEquals("[Patient.name[0]: [INFORMATION Patient.name[0] Validated against HumanName]]", b2.elementMessages());

    Build b3 = new Build("Jones");
    b3.open();
    assertEquals("[INFORMATION Patient family is Jones, INFORMATION Patient.name[0] Validated against HumanName]", text(b3.validationStep(false)));
    b3.finish();
    assertTrue(String.join("\n", b3.log).contains("1 misses"), b3.log.toString());
  }

  /** a package folder: package/package.json and one resource */
  private File packageFolder(String name, String content) throws IOException {
    File dir = outside(name);
    File pkg = new File(dir, "package");
    Files.createDirectories(pkg.toPath());
    Files.write(new File(pkg, "package.json").toPath(), ("{\"name\":\"" + name + "\",\"version\":\"dev\",\"fhirVersions\":[\"5.0.0\"]}").getBytes(StandardCharsets.UTF_8));
    Files.write(new File(pkg, "Basic-b.json").toPath(), content.getBytes(StandardCharsets.UTF_8));
    return dir;
  }

  @Test
  void aChangedPackageInThePackagesFolder_isAMiss() throws Exception {
    File folder = outside("packages");
    File tgz = new File(folder, "example.tgz");
    Files.write(tgz.toPath(), "one".getBytes(StandardCharsets.UTF_8));

    Build b1 = new Build("Smith");
    b1.settings.setPackagesFolder(folder.getAbsolutePath());
    b1.open();
    b1.validationStep(false);
    b1.finish();

    Build b2 = new Build("Smith");
    b2.settings.setPackagesFolder(folder.getAbsolutePath());
    b2.open();
    b2.validationStep(true);
    b2.finish();

    Files.write(tgz.toPath(), "two".getBytes(StandardCharsets.UTF_8));
    Build b3 = new Build("Smith");
    b3.settings.setPackagesFolder(folder.getAbsolutePath());
    b3.open();
    b3.validationStep(false);
    b3.finish();
  }

  @Test
  void aChangedRelatedIgPackage_isAMiss() throws Exception {
    File dir = packageFolder("example.related", "{\"resourceType\":\"Basic\",\"id\":\"b\"}");

    Build b1 = new Build("Smith");
    b1.pf.relatedIGs.add(new RelatedIG(b1.pf.context, "rel", "example.related", RelatedIG.RelatedIGLoadingMode.LOCAL, RelatedIG.RelatedIGRole.MODULE,
      NpmPackage.fromFolder(dir.getAbsolutePath())));
    b1.open();
    b1.validationStep(false);
    b1.finish();

    Build b2 = new Build("Smith");
    b2.pf.relatedIGs.add(new RelatedIG(b2.pf.context, "rel", "example.related", RelatedIG.RelatedIGLoadingMode.LOCAL, RelatedIG.RelatedIGRole.MODULE,
      NpmPackage.fromFolder(dir.getAbsolutePath())));
    b2.open();
    b2.validationStep(true);
    b2.finish();

    Files.write(new File(new File(dir, "package"), "Basic-b.json").toPath(), "{\"resourceType\":\"Basic\",\"id\":\"b\",\"text\":{}}".getBytes(StandardCharsets.UTF_8));
    Build b3 = new Build("Smith");
    b3.pf.relatedIGs.add(new RelatedIG(b3.pf.context, "rel", "example.related", RelatedIG.RelatedIGLoadingMode.LOCAL, RelatedIG.RelatedIGRole.MODULE,
      NpmPackage.fromFolder(dir.getAbsolutePath())));
    b3.open();
    b3.validationStep(false);
    b3.finish();
  }

  private File inis;
  private int iniCount;

  /** an ig.ini outside the IG's folder (a file in it would change the build key) */
  private IniFile ini(String... lines) throws IOException {
    if (inis == null) {
      inis = outside("ini");
    }
    File f = new File(inis, "ig" + (iniCount++) + ".ini");
    Files.write(f.toPath(), ("[IG]\n" + String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
    return new IniFile(f.getAbsolutePath());
  }

  private String log(Build b) {
    return String.join("\n", b.log);
  }

  // ---- verify mode ----------------------------------------------------------------------------------------------------

  @Test
  void verify_aHitThatRunsTheSame_isVerified() throws Exception {
    Build b1 = new Build("Smith");
    b1.open();
    b1.validationStep(false);
    b1.finish();

    Build b2 = new Build("Smith");
    b2.open(ini("input-cache-verify = 1"));
    b2.validationStep(false);
    b2.finish();
    assertEquals(0, b2.ic.getMismatches());
    assertTrue(log(b2).contains("1 hits verified, 0 mismatches"), log(b2));
    assertNull(b2.ic.verificationMessage());
  }

  @Test
  void verify_aHitThatRunsDifferently_isAMismatch_thatTheQaPageShows_andTheEntryIsDropped() throws Exception {
    Build b1 = new Build("Smith");
    b1.open();
    b1.validationStep(false);
    b1.finish();

    Build b2 = new Build("Smith");
    b2.extra = "something else";
    b2.open(ini("input-cache-verify = 1"));
    b2.validationStep(false);
    b2.finish();
    assertEquals(1, b2.ic.getMismatches());
    assertTrue(log(b2).contains("INPUT CACHE MISMATCH: validation of Patient/p"), log(b2));
    ValidationMessage vm = b2.ic.verificationMessage();
    assertNotNull(vm);
    assertEquals(IssueSeverity.ERROR, vm.getLevel());
    assertTrue(vm.getMessage().contains("1 of the 1 hits verified"), vm.getMessage());

    Build b3 = new Build("Smith");
    b3.open();
    b3.validationStep(false);
    b3.finish();
  }

  // ---- what's stored can't always be read back ------------------------------------------------------------------------

  /** each entry in the store, replaced by what the function makes of it */
  private void rewriteEntries(java.util.function.UnaryOperator<byte[]> f) throws IOException {
    InputCacheStore store = new InputCacheStore(new File(root, "input-cache/publisher-cache"), 1 << 30);
    List<File> entries = new ArrayList<>();
    try (java.util.stream.Stream<java.nio.file.Path> w = Files.walk(store.getFolder().toPath())) {
      w.filter(p -> p.toString().endsWith(".entry")).forEach(p -> entries.add(p.toFile()));
    }
    assertEquals(1, entries.size());
    for (File e : entries) {
      String key = e.getName().substring(0, e.getName().length() - ".entry".length());
      store.write(key, f.apply(store.read(key)));
    }
  }

  @Test
  void anEntryThatCantBeDecoded_isAMiss_thatIsCounted() throws Exception {
    Build b1 = new Build("Smith");
    b1.open();
    b1.validationStep(false);
    b1.finish();
    rewriteEntries(b -> "not an entry".getBytes(StandardCharsets.UTF_8));

    Build b2 = new Build("Smith");
    b2.open();
    b2.validationStep(false);
    b2.finish();
    assertTrue(log(b2).contains("1 unreadable entries ignored"), log(b2));
  }

  @Test
  void anEntryWhoseJournalIsCorrupt_isAMiss_andIsLogged() throws Exception {
    Build b1 = new Build("Smith");
    b1.open();
    List<ValidationMessage> first = b1.validationStep(false);
    b1.finish();
    rewriteEntries(b -> {
      List<InputCache.Variant> vs = InputCache.decodeEntry(b);
      List<InputCache.Variant> res = new ArrayList<>();
      for (InputCache.Variant v : vs) {
        byte[] j = v.journal.clone();
        // the records' content is past the time and the count
        for (int i = 12; i < j.length; i++) {
          j[i] = (byte) 0xff;
        }
        res.add(new InputCache.Variant(v.reads, j, v.extras));
      }
      java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
      try {
        InputCache.encodeEntry(res, bs);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return bs.toByteArray();
    });

    Build b2 = new Build("Smith");
    b2.open();
    assertEquals(text(first), text(b2.validationStep(false)));
    b2.finish();
    assertTrue(log(b2).contains("unable to replay validation of Patient/p"), log(b2));
    assertTrue(log(b2).contains("1 stored"), log(b2));

    Build b3 = new Build("Smith");
    b3.open();
    assertEquals(text(first), text(b3.validationStep(true)));
    b3.finish();
  }

  // ---- settings -------------------------------------------------------------------------------------------------------

  @Test
  void create_isOnUnlessTurnedOff() throws Exception {
    Build b = new Build("Smith");
    assertNotNull(InputCache.create(b.pf, b.settings, null, b.log::add));
    assertNotNull(InputCache.create(b.pf, b.settings, ini("input-cache = true"), b.log::add));
    assertNull(InputCache.create(b.pf, b.settings, ini("input-cache = false"), b.log::add));
    assertNull(InputCache.create(b.pf, b.settings, ini("input-cache = FALSE"), b.log::add));
  }

  @Test
  void create_aValueItDoesntKnow_isWarnedAbout_andLeavesItOn() throws Exception {
    Build b = new Build("Smith");
    assertNotNull(InputCache.create(b.pf, b.settings, ini("input-cache = yes please"), b.log::add));
    assertTrue(log(b).contains("ignoring input-cache value 'yes please'"), log(b));
    b.log.clear();
    InputCache ic = InputCache.create(b.pf, b.settings, ini("input-cache-verify = most", "input-cache-size-mb = big"), b.log::add);
    assertNotNull(ic);
    assertTrue(log(b).contains("ignoring input-cache-verify value 'most'"), log(b));
    assertTrue(log(b).contains("ignoring input-cache-size-mb value 'big'"), log(b));
  }

  @Test
  void create_theSystemPropertyComesFirst() throws Exception {
    Build b = new Build("Smith");
    System.setProperty("fhir.igpublisher.input-cache", "false");
    try {
      assertNull(InputCache.create(b.pf, b.settings, ini("input-cache = true"), b.log::add));
    } finally {
      System.clearProperty("fhir.igpublisher.input-cache");
    }
  }

  @Test
  void create_verifyIsAFraction() throws Exception {
    Build b = new Build("Smith");
    InputCache.create(b.pf, b.settings, ini("input-cache-verify = 0.25"), b.log::add);
    assertTrue(log(b).contains("verifying 25% of hits"), log(b));
  }

  @Test
  void create_isOffInRapidoAndWatchBuilds_andWhenThereIsNothingToCache() throws Exception {
    Build b = new Build("Smith");
    b.settings.setRapidoMode(true);
    assertNull(InputCache.create(b.pf, b.settings, null, b.log::add));
    b.settings.setRapidoMode(false);
    b.settings.setWatchMode(true);
    assertNull(InputCache.create(b.pf, b.settings, null, b.log::add));
    b.settings.setWatchMode(false);
    b.settings.setGenerationOff(true);
    assertNotNull(InputCache.create(b.pf, b.settings, null, b.log::add));
    b.settings.setValidationOff(true);
    assertNull(InputCache.create(b.pf, b.settings, null, b.log::add));
  }

  // ---- what a file says about when it was built ----------------------------------------------------------------------

  @Test
  void aReplayedPage_saysWhenThisBuildRan() throws Exception {
    Build b1 = new Build("Smith");
    b1.pf.getExecTime().setTimeInMillis(1772359200000L); // 2026-03-01T10:00:00Z
    b1.open();
    File page = b1.htmlStep(false);
    b1.finish();
    assertTrue(log(b1).contains("1 stored"), log(b1));
    String first = new String(Files.readAllBytes(page.toPath()), StandardCharsets.UTF_8);
    assertTrue(first.contains(b1.buildDate()), first);
    Files.delete(page.toPath());

    Build b2 = new Build("Smith");
    b2.pf.getExecTime().setTimeInMillis(1772359200000L + 90 * 60 * 1000L);
    b2.open();
    assertFalse(b1.buildDate().equals(b2.buildDate()));
    b2.htmlStep(true);
    b2.finish();
    assertEquals("<p>Patient p, built " + b2.buildDate() + "</p>", new String(Files.readAllBytes(page.toPath()), StandardCharsets.UTF_8));
    assertTrue(b2.file.getOutputNames().contains(page.getAbsolutePath()));
  }

  @Test
  void theRequirementsTheHtmlCheckWritesInTheIgsFolder_areNotAnInput() throws Exception {
    Build b1 = new Build("Smith");
    b1.open();
    List<ValidationMessage> first = b1.validationStep(false);
    b1.finish();
    // the html check suggests a Requirements resource made from the narrative, after the steps ran
    Files.write(new File(root, "Requirements-fromNarrative.json").toPath(), "{\"resourceType\":\"Requirements\"}".getBytes(StandardCharsets.UTF_8));

    Build b2 = new Build("Smith");
    b2.open();
    assertEquals(text(first), text(b2.validationStep(true)));
    b2.finish();
  }
}
