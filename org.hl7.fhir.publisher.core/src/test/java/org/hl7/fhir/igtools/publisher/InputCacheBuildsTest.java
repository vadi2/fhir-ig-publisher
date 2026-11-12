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
}
