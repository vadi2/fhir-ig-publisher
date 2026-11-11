package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.igtools.templates.Template;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.DomainResource;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.model.utilities.formats.OutputStyle;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.elementmodel.JsonParser;
import org.hl7.fhir.services.renderers.utils.RenderingContext;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.MarkDownProcessor;
import org.hl7.fhir.utilities.TimeTracker;
import org.hl7.fhir.utilities.UserDataNames;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * -rapido: a resource that Rapido found unchanged gets the narrative the last build generated for it back, and is
 * then exactly as generating the narrative leaves it: the resource, its element, and all that is written from them.
 * A resource whose narrative could be different now (it changed, something it shows changed, the record isn't the
 * last finished build's, the element isn't what it was) has its narrative generated.
 */
class NarrativeReuseTest {

  private static SimpleWorkerContext context;
  private static String noContext;
  private static FilesystemPackageCacheManager pcm;

  @BeforeAll
  static void loadContext() throws Exception {
    try {
      pcm = new FilesystemPackageCacheManager.Builder().build();
      NpmPackage npm = pcm.loadPackage("hl7.fhir.r5.core", "5.0.0");
      context = new SimpleWorkerContext.SimpleWorkerContextBuilder(ModelContext.fullCoreContext()).withAllowLoadingDuplicates(true).fromPackage(npm);
    } catch (IOException | FHIRException e) {
      noContext = "hl7.fhir.r5.core#5.0.0 can't be fetched: " + e;
    }
  }

  @TempDir
  Path dir;

  private final String uuid = HierarchicalTableGenerator.uuid;
  private final List<Resource> cached = new ArrayList<>();

  @AfterEach
  void restore() {
    HierarchicalTableGenerator.uuid = uuid;
    dropCached();
  }

  private void dropCached() {
    for (Resource r : cached) {
      context.dropResource(r);
    }
    cached.clear();
  }

  /** what every build of a test does once it is ready to generate the narratives (the builds it is compared with too) */
  private java.util.function.Consumer<PublisherFields> everyBuild = pf -> { };
  /** between two resources of one source file */
  private static final String NEXT = "\n---\n";

  private static final String CS = "CodeSystem-colours.json";
  private static final String VS = "ValueSet-colours.json";
  private static final String ALL = "ValueSet-all.json";
  private static final String PATIENT = "Patient-p.json";
  private static final String OTHER = "Patient-q.json";

  /**
   * the sources of an IG: a code system, a value set of all its codes, a value set that takes in that value set (its
   * narrative names it by its title), and two patients
   */
  private static Map<String, String> sources(String valueSetTitle) {
    Map<String, String> s = new LinkedHashMap<>();
    s.put(CS, "{\"resourceType\":\"CodeSystem\",\"id\":\"colours\",\"url\":\"http://example.org/ig/CodeSystem/colours\",\"version\":\"1.0.0\",\"name\":\"Colours\","
        + "\"title\":\"Colour codes\",\"status\":\"active\",\"caseSensitive\":true,\"content\":\"complete\","
        + "\"concept\":[{\"code\":\"red\",\"display\":\"Red\",\"definition\":\"The colour *red*\"},{\"code\":\"blue\",\"display\":\"Blue\"}]}");
    s.put(VS, "{\"resourceType\":\"ValueSet\",\"id\":\"colours\",\"url\":\"http://example.org/ig/ValueSet/colours\",\"version\":\"1.0.0\",\"name\":\"ColourSet\","
        + "\"title\":\"" + valueSetTitle + "\",\"status\":\"active\",\"compose\":{\"include\":[{\"system\":\"http://example.org/ig/CodeSystem/colours\"}]}}");
    s.put(ALL, "{\"resourceType\":\"ValueSet\",\"id\":\"all\",\"url\":\"http://example.org/ig/ValueSet/all\",\"version\":\"1.0.0\",\"name\":\"AllSet\","
        + "\"title\":\"Everything\",\"status\":\"active\",\"compose\":{\"include\":[{\"valueSet\":[\"http://example.org/ig/ValueSet/colours\"]}]}}");
    s.put(PATIENT, "{\"resourceType\":\"Patient\",\"id\":\"p\",\"name\":[{\"family\":\"Family\",\"given\":[\"Given\"]}],\"gender\":\"female\"}");
    s.put(OTHER, "{\"resourceType\":\"Patient\",\"id\":\"q\",\"active\":true,\"birthDate\":\"1970-01-01\"}");
    return s;
  }

  /** one build of the IG in a folder, up to the narratives */
  private class Build {
    final Path root;
    final Map<String, String> sources;
    final List<String> langs = new ArrayList<>();
    /** called on each resource once it is loaded */
    java.util.function.Consumer<FetchedResource> loaded = r -> { };
    /** called on each resource after Rapido has compared the files with the last build, before the narratives */
    java.util.function.Consumer<FetchedResource> compared = r -> { };
    /** called once the build is ready to generate the narratives */
    java.util.function.Consumer<PublisherFields> prepared = everyBuild;
    PublisherFields pf;
    int rendered;

    Build(Path root, Map<String, String> sources) {
      this.root = root;
      this.sources = sources;
    }

    Build langs(String... more) {
      langs.addAll(List.of(more));
      return this;
    }

    Build run() throws Exception {
      assumeTrue(context != null, noContext);
      dropCached();
      Publisher publisher = new Publisher();
      publisher.getSettings().setRapidoMode(true);
      publisher.getSettings().setWatchMode(true);
      pf = new PublisherFields();
      pf.logger = mock(ILoggingService.class);
      pf.rootDir = root.toString();
      pf.vsCache = root.toString();
      pf.tempDir = root.resolve("temp").toString();
      pf.context = context;
      pf.pcm = pcm;
      pf.version = "5.0.0";
      pf.igpkp = mock(IGKnowledgeProvider.class);
      when(pf.igpkp.getCanonical()).thenReturn("http://example.org/ig");
      when(pf.igpkp.getDefinitionsName(any(FetchedResource.class))).thenReturn("definitions.html");
      pf.template = mock(Template.class);
      pf.tt = new TimeTracker();
      pf.defaultTranslationLang = "en";
      pf.translationLangs.addAll(langs);

      FetchedFile ig = file("ig.json", "{}");
      pf.fileList.add(ig);
      for (Map.Entry<String, String> s : sources.entrySet()) {
        FetchedFile f = file(s.getKey(), s.getValue());
        for (String one : s.getValue().split(NEXT)) {
          byte[] cnt = one.getBytes(StandardCharsets.UTF_8);
          Element e = new JsonParser(context).parseSingle(new ByteArrayInputStream(cnt), null);
          FetchedResource r = f.addResource(s.getKey());
          r.setElement(e).setId(e.getIdBase());
          // what the loader keeps on an element comes with it when the element is made again
          e.setUserData("test.loaded", s.getKey());
          if (!"Patient".equals(e.fhirType())) {
            // a conformance resource is loaded as a resource as well, and is in the context
            Resource res = new org.hl7.fhir.model.core.formats.JsonParser(context.getModelContext()).parse(cnt);
            r.setResource(res);
            res.setWebPath(e.fhirType() + "-" + e.getIdBase() + ".html");
            context.cacheResource(res);
            cached.add(res);
          }
          loaded.accept(r);
        }
        pf.fileList.add(f);
      }
      pf.changeList.addAll(pf.fileList); // as the loader leaves it
      publisher.setPf(pf);
      // as build() does, for a complete build to clear the temp folder
      Field loader = Publisher.class.getDeclaredField("loader");
      loader.setAccessible(true);
      ((PublisherIGLoader) loader.get(publisher)).setPf(pf);
      publisher.checkDependencies(ig, new IniFile(root.resolve(".build-tracker.ini").toString()));

      pf.rendererFactory = spy(pf.rendererFactory);
      pf.rc = new RenderingContext(context, pf.rendererFactory, new MarkDownProcessor(MarkDownProcessor.Dialect.COMMON_MARK), ValidationOptions.defaults(),
          "http://hl7.org/fhir/R5/", "", java.util.Locale.ENGLISH, RenderingContext.ResourceRendererMode.TECHNICAL, RenderingContext.GenerationRules.IG_PUBLISHER);
      pf.rc.setDestDir(pf.tempDir);
      PublisherProcessor processor = new PublisherProcessor(publisher.getSettings());
      processor.setPf(pf);
      pf.rc.setParser(processor.getTypeLoader(pf.version));
      for (FetchedFile f : pf.fileList) {
        f.getResources().forEach(compared);
      }
      prepared.accept(pf);
      processor.generateNarratives(false);
      if (pf.needsRegen) {
        processor.generateNarratives(true); // as the build does after validation
      }
      // the resources of the IG that a renderer was made for
      java.util.Set<String> made = new java.util.HashSet<>();
      for (org.mockito.invocation.Invocation i : mockingDetails(pf.rendererFactory).getInvocations()) {
        Object a = i.getArgument(0);
        for (FetchedFile f : pf.fileList) {
          for (FetchedResource r : f.getResources()) {
            if (a == r.getResource() || (a instanceof org.hl7.fhir.services.renderers.utils.ResourceWrapper && r.getResource() == null
                && r.fhirType().equals(((org.hl7.fhir.services.renderers.utils.ResourceWrapper) a).fhirType()) && r.getId().equals(((org.hl7.fhir.services.renderers.utils.ResourceWrapper) a).getId()))) {
              made.add(r.fhirType() + "/" + r.getId());
            }
          }
        }
      }
      rendered = made.size();
      return this;
    }

    /** the rest of the build, as far as Rapido goes: what each file made is recorded, and the state is saved */
    Build finish() throws Exception {
      pf.rapido.finishValidation();
      for (FetchedFile f : pf.fileList) {
        for (String phase : new String[] {RapidoTracker.PHASE_HTML, RapidoTracker.PHASE_SHEETS}) {
          if (pf.rapido.canReplay(f, phase)) {
            pf.rapido.replay(f, phase);
          } else {
            pf.rapido.startRecording(f, phase);
            pf.rapido.stopRecording();
          }
        }
      }
      new RapidoOutputs(pf.vsCache, pf.tempDir).update(pf.fileList, pf.changeList);
      pf.rapido.save();
      endedWell();
      return this;
    }

    /** the build ended without a failure */
    void endedWell() throws Exception {
      IniFile buildTracker = new IniFile(root.resolve(".build-tracker.ini").toString());
      buildTracker.setBooleanProperty("status", "complete", true, null);
      buildTracker.save();
    }

    private FetchedFile file(String name, String content) {
      FetchedFile f = new FetchedFile(name);
      f.setPath(root.resolve("input").resolve(name).toString());
      f.setSource(content.getBytes(StandardCharsets.UTF_8));
      return f;
    }

    FetchedFile file(String file) {
      return pf.fileList.stream().filter(f -> f.getLoadPath().equals(file)).findFirst().orElseThrow();
    }

    FetchedResource resource(String file) {
      return file(file).getResources().get(0);
    }

    String narrative(String file) throws Exception {
      return new XhtmlComposer(XhtmlComposer.XML).compose(resource(file).getElement().getNamedChild("text").getNamedChild("div").getXhtml());
    }

    /** every resource as the build left it, in every form it is written in */
    Map<String, String> state() throws Exception {
      Map<String, String> res = new LinkedHashMap<>();
      for (FetchedFile f : pf.fileList) {
        for (FetchedResource r : f.getResources()) {
          String k = f.getLoadPath() + " " + r.getId() + " ";
          Element e = r.getElement();
          res.put(k + "element json", compose(new JsonParser(context), e));
          res.put(k + "element xml", compose(new org.hl7.fhir.services.elementmodel.XmlParser(context), e));
          RapidoDependencyAnalyser.Hasher h = new RapidoDependencyAnalyser.Hasher();
          RapidoNativeOutputs.contentKey(h, e);
          res.put(k + "element key", h.finish());
          Element div = e.getNamedChild("text").getNamedChild("div");
          res.put(k + "element narrative", tree(div.getXhtml()) + " source " + div.getXhtmlSource());
          for (String n : new TreeSet<>(e.getUserDataNames())) {
            res.put(k + "element data " + n, String.valueOf(e.getUserData(n)));
          }
          res.put(k + "element shape", shape(e));
          res.put(k + "generated", r.isGeneratedNarrative() + " " + r.isRegenAfterValidation());
          res.put(k + "messages", r.getErrors().size() + " " + f.getErrors().size());
          res.put(k + "outputs", f.getOutputNames().size() + " " + pf.needsRegen + " " + pf.noNarrativeResources.size()
              + " " + pf.otherFilesRun.stream().map(n -> java.nio.file.Path.of(n).getFileName().toString()).sorted().collect(java.util.stream.Collectors.toList()));
          if (r.getResource() != null) {
            DomainResource dr = (DomainResource) r.getResource();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            new org.hl7.fhir.model.core.formats.JsonParser(context.getModelContext()).setOutputStyle(OutputStyle.PRETTY).compose(b, dr);
            res.put(k + "resource json", b.toString(StandardCharsets.UTF_8));
            b = new ByteArrayOutputStream();
            new org.hl7.fhir.model.core.formats.XmlParser(context.getModelContext()).setOutputStyle(OutputStyle.PRETTY).compose(b, dr);
            res.put(k + "resource xml", b.toString(StandardCharsets.UTF_8));
            res.put(k + "resource narrative", tree(dr.getText().getDiv()));
            res.put(k + "resource narrative data", dr.getText().getStatus() + " " + new TreeSet<>(dr.getText().getUserDataNames())
                + " " + dr.getText().getUserData(UserDataNames.renderer_is_generated));
          }
        }
      }
      return res;
    }

    private String compose(org.hl7.fhir.services.elementmodel.ParserBase p, Element e) throws Exception {
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      p.compose(e, b, OutputStyle.PRETTY, null);
      return b.toString(StandardCharsets.UTF_8);
    }
  }

  /** the tree node for node: an empty text node, the order of the attributes and the like are all in it */
  private static String tree(org.hl7.fhir.utilities.xhtml.XhtmlNode x) {
    StringBuilder b = new StringBuilder();
    b.append("<").append(x.getNodeType()).append(" ").append(x.getName()).append(" [").append(x.getContent()).append("] ")
        .append(x.isNoPretty()).append(" ").append(x.getEmptyExpanded()).append(" ").append(x.getLocation());
    if (x.hasAttributes()) {
      x.getAttributes().forEach((k, v) -> b.append(" @").append(k).append("=").append(v));
    }
    if (x.hasChildren()) {
      for (org.hl7.fhir.utilities.xhtml.XhtmlNode c : x.getChildNodes()) {
        b.append(tree(c));
      }
    }
    return b.append(">").toString();
  }

  /** what of an element isn't its content: the index of each child in its list (the turtle format writes it), its web path */
  private static String shape(Element e) {
    StringBuilder b = new StringBuilder();
    b.append("(").append(e.getName()).append(" ").append(e.getIndex()).append(" ").append(e.getWebPath()).append(" ").append(e.getSpecial())
        .append(" ").append(e.getFormat()).append(" ").append(e.getPath());
    for (Element c : e.getChildList()) {
      b.append(shape(c));
    }
    return b.append(")").toString();
  }

  private Build build(Map<String, String> sources) {
    return new Build(dir.resolve("ig"), sources);
  }

  /** a build of the same sources in a folder of its own: a complete build, which generates every narrative */
  private Build fresh(Map<String, String> sources, String... langs) throws Exception {
    Path root = Files.createTempDirectory(dir, "fresh");
    Build b = new Build(root, sources).langs(langs).run();
    assertTrue(b.pf.rapido.isCompleteBuild());
    return b;
  }

  private static void assertSameState(Build expected, Build actual) throws Exception {
    Map<String, String> e = expected.state();
    Map<String, String> a = actual.state();
    assertEquals(e.keySet(), a.keySet());
    for (String k : e.keySet()) {
      assertEquals(e.get(k), a.get(k), k);
    }
  }

  @Test
  void theNarrativeOfAnUnchangedResourceIsPutBackAsGenerated() throws Exception {
    for (String[] langs : new String[][] {{}, {"de"}}) {
      Path root = Files.createTempDirectory(dir, "ig");
      Map<String, String> sources = sources("All the colours");
      Build first = new Build(root, sources).langs(langs).run();
      assertTrue(first.pf.rapido.isCompleteBuild());
      assertEquals(5, first.rendered);
      first.finish();

      Build second = new Build(root, sources).langs(langs).run();
      assertFalse(second.pf.rapido.isCompleteBuild(), second.pf.rapido.getReason());
      assertEquals(0, second.rendered, "no narrative is generated");
      assertTrue(second.narrative(ALL).contains("All the colours"));
      assertSameState(fresh(sources, langs), second);
      second.finish();

      // and again from what the second build kept, which wrote no record
      Build third = new Build(root, sources).langs(langs).run();
      assertEquals(0, third.rendered);
      assertSameState(fresh(sources, langs), third);
    }
  }

  /** a value set that takes in another names it by its title in its narrative: its own file did not change */
  @Test
  void aNarrativeThatShowsTheTitleOfAnotherResourceIsGeneratedWhenThatTitleChanges() throws Exception {
    build(sources("All the colours")).run().finish();

    Map<String, String> sources = sources("Hues and shades");
    Build second = build(sources).run();
    assertFalse(second.pf.rapido.isCompleteBuild(), second.pf.rapido.getReason());
    assertEquals(3, second.rendered, "the value set, the one that names it and the code system it uses (its page lists it); the patients' narratives are put back");
    assertTrue(second.narrative(ALL).contains("Hues and shades"), second.narrative(ALL));
    assertFalse(second.narrative(ALL).contains("All the colours"), second.narrative(ALL));
    assertSameState(fresh(sources), second);
    second.finish();

    // the new narrative is the one the next build puts back
    Build third = build(sources).run();
    assertEquals(0, third.rendered);
    assertTrue(third.narrative(ALL).contains("Hues and shades"));
    assertSameState(fresh(sources), third);
  }

  @Test
  void aChangedResourceGetsItsNarrativeGenerated() throws Exception {
    build(sources("All the colours")).run().finish();

    Map<String, String> sources = sources("All the colours");
    sources.put(PATIENT, sources.get(PATIENT).replace("female", "male"));
    Build second = build(sources).run();
    assertEquals(1, second.rendered);
    assertTrue(second.narrative(PATIENT).contains(" male,"), second.narrative(PATIENT));
    assertSameState(fresh(sources), second);
  }

  /**
   * A build wrote records and then didn't finish, so the state is still the one of the build before it: what that
   * state says is unchanged is compared with that build, and the records are not that build's
   */
  @Test
  void theRecordOfABuildThatDidntFinishIsNotUsed() throws Exception {
    Map<String, String> sources = sources("All the colours");
    build(sources).run().finish();

    Build failed = build(sources("Hues and shades")).run();
    assertEquals(3, failed.rendered);
    failed.endedWell(); // as far as the build tracker knows: the Rapido state wasn't saved

    Build third = build(sources).run();
    assertFalse(third.pf.rapido.isCompleteBuild(), third.pf.rapido.getReason());
    assertEquals(3, third.rendered, "the value sets and the code system have the records of the build that did not finish");
    assertTrue(third.narrative(ALL).contains("All the colours"), third.narrative(ALL));
    assertSameState(fresh(sources), third);
  }

  /**
   * The loader changed the code system (it does: a publisher, a date, a version) without converting it to an element
   * again. Generating the narrative converts it, so the element then has the change; the element as loaded with the
   * narrative put back wouldn't
   */
  @Test
  void aNarrativeIsGeneratedWhenTheElementWouldNotBeTheOneGeneratingGives() throws Exception {
    Map<String, String> sources = sources("All the colours");
    java.util.function.Consumer<FetchedResource> loaded = r -> {
      if (r.getResource() instanceof org.hl7.fhir.model.core.CodeSystem) {
        ((org.hl7.fhir.model.core.CodeSystem) r.getResource()).setPublisher("The loader");
      }
    };
    Build first = build(sources);
    first.loaded = loaded;
    first.run().finish();

    Build second = build(sources);
    second.loaded = loaded;
    second.run();
    assertEquals(1, second.rendered, "the code system");
    Build fresh = new Build(Files.createTempDirectory(dir, "fresh"), sources);
    fresh.loaded = loaded;
    assertSameState(fresh.run(), second);
    assertTrue(second.state().get(CS + " colours element json").contains("The loader"));
  }

  /**
   * The same for a resource that only has an element: a step between loading and the narratives gives it a language
   * (as checkLanguage does) in one build and not in the other
   */
  @Test
  void aNarrativeIsGeneratedWhenTheElementIsNotTheOneItWasGeneratedOn() throws Exception {
    Map<String, String> sources = sources("All the colours");
    build(sources).run().finish();

    java.util.function.Consumer<FetchedResource> compared = r -> {
      if (r.getId().equals("p")) {
        r.getElement().setChildValue("language", "en");
      }
    };
    Build second = build(sources);
    second.compared = compared;
    second.run();
    assertEquals(1, second.rendered, "the patient");
    Build fresh = new Build(Files.createTempDirectory(dir, "fresh"), sources);
    fresh.compared = compared;
    assertSameState(fresh.run(), second);
    assertTrue(second.state().get(PATIENT + " p element json").contains("\"language\""));
  }

  /**
   * A renderer that shows what validation found has its narrative generated again after validation: that narrative is
   * not the one of the first pass, and it is not recorded. The second pass leaves the records of the others alone
   */
  @Test
  void aNarrativeThatIsGeneratedAgainAfterValidationIsNotPutBack() throws Exception {
    everyBuild = pf -> org.mockito.Mockito.doAnswer(inv -> {
      org.hl7.fhir.services.renderers.ResourceRenderer rr = (org.hl7.fhir.services.renderers.ResourceRenderer) inv.callRealMethod();
      if (inv.getArgument(0) instanceof org.hl7.fhir.model.core.CodeSystem) {
        rr = spy(rr);
        when(rr.renderingUsesValidation()).thenReturn(true);
      }
      return rr;
    }).when(pf.rendererFactory).factory(any(Resource.class), any(RenderingContext.class));
    Map<String, String> sources = sources("All the colours");
    Build first = build(sources).run();
    assertTrue(first.pf.needsRegen);
    first.finish();

    Build second = build(sources).run();
    assertEquals(1, second.rendered, "the code system");
    assertTrue(second.pf.needsRegen);
    assertTrue(second.resource(CS).isRegenAfterValidation());
    assertSameState(fresh(sources), second);
  }

  /** a renderer that writes files as well: the build is told of them when the narrative is generated, so it is, every build */
  @Test
  void aNarrativeWhoseRendererWritesFilesIsNotPutBack() throws Exception {
    everyBuild = pf -> org.mockito.Mockito.doAnswer(inv -> {
      org.hl7.fhir.services.renderers.utils.ResourceWrapper w = inv.getArgument(0);
      if ("q".equals(w.getId())) {
        Path diagram = Path.of(pf.tempDir, "q-diagram.svg");
        Files.createDirectories(diagram.getParent());
        Files.writeString(diagram, "<svg/>");
        ((RenderingContext) inv.getArgument(1)).getFiles().add(diagram.toString());
      }
      return inv.callRealMethod();
    }).when(pf.rendererFactory).factory(any(org.hl7.fhir.services.renderers.utils.ResourceWrapper.class), any(RenderingContext.class));
    Map<String, String> sources = sources("All the colours");
    build(sources).run().finish();

    Build second = build(sources).run();
    assertEquals(1, second.rendered, "the patient whose renderer writes a file");
    assertTrue(second.file(OTHER).getOutputNames().toString().contains("q-diagram.svg"));
    assertTrue(second.pf.otherFilesRun.toString().contains("q-diagram.svg"));
    assertSameState(fresh(sources), second);
  }

  /** narratives made while their source is tracked have ids from a counter the whole build shares: they are generated */
  @Test
  void nothingIsPutBackOrKeptWhileTheNarrativeSourceIsTracked() throws Exception {
    Map<String, String> sources = sources("All the colours");
    build(sources).run().finish();
    everyBuild = pf -> pf.rc.setTrackNarrativeSource(true);
    Build second = build(sources).run();
    assertEquals(5, second.rendered);
    second.finish();

    everyBuild = pf -> { };
    assertEquals(5, build(sources).run().rendered, "the build that tracked kept nothing");
  }

  /** a source file with two resources: one whose narrative is put back, one that came with its own */
  @Test
  void theResourcesOfAFileEachGetTheirOwn() throws Exception {
    Map<String, String> sources = sources("All the colours");
    String own = "{\"resourceType\":\"Patient\",\"id\":\"r\",\"text\":{\"status\":\"additional\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><p>R</p></div>\"},\"active\":false}";
    String third = "{\"resourceType\":\"Patient\",\"id\":\"s\",\"gender\":\"other\"}";
    sources.put(OTHER, sources.get(OTHER) + NEXT + own + NEXT + third);
    Build first = build(sources).run();
    assertEquals(6, first.rendered);
    first.finish();

    Build second = build(sources).run();
    assertEquals(0, second.rendered);
    assertSameState(fresh(sources), second);
    second.finish();

    // one of them is not what it was when the narratives are generated: the others still get theirs back, and the
    // file's new record has all of them
    java.util.function.Consumer<FetchedResource> compared = r -> {
      if (r.getId().equals("s")) {
        r.getElement().setChildValue("language", "en");
      }
    };
    for (int i = 0; i < 2; i++) {
      Build next = build(sources);
      next.compared = compared;
      next.run();
      assertEquals(i == 0 ? 1 : 0, next.rendered);
      Build fresh = new Build(Files.createTempDirectory(dir, "fresh"), sources);
      fresh.compared = compared;
      assertSameState(fresh.run(), next);
      next.finish();
    }
  }

  /** a narrative that came with the resource isn't generated, and isn't put back either: it is checked, every build */
  @Test
  void aNarrativeThatCameWithTheResourceIsLeftAlone() throws Exception {
    Map<String, String> sources = sources("All the colours");
    sources.put(OTHER, sources.get(OTHER).replace("\"active\":true", "\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><p><b>Generated Narrative: Patient q</b></p></div>\"},\"active\":true"));
    Build first = build(sources).run();
    assertEquals(4, first.rendered);
    assertEquals(1, first.resource(OTHER).getErrors().size());
    first.finish();

    Build second = build(sources).run();
    assertEquals(0, second.rendered);
    assertEquals(1, second.resource(OTHER).getErrors().size(), "the narrative that claims to be generated is reported again");
    Build fresh = new Build(Files.createTempDirectory(dir, "fresh"), sources).run();
    assertSameState(fresh, second);
  }

  /** without -rapido nothing is recorded */
  @Test
  void nothingIsKeptWithoutRapido() throws Exception {
    Build b = build(sources("All the colours")).run();
    assertTrue(Files.isDirectory(dir.resolve("ig").resolve("temp").resolve("_rapido").resolve("narratives")));
    try (Stream<Path> s = Files.list(dir.resolve("ig").resolve("temp").resolve("_rapido").resolve("narratives"))) {
      assertEquals(5, s.count());
    }
    b.pf.rapido = null;
    Path other = Files.createTempDirectory(dir, "plain");
    b.pf.tempDir = other.toString();
    for (FetchedFile f : b.pf.fileList) {
      for (FetchedResource r : f.getResources()) {
        r.getElement().removeChild("text");
        if (r.getResource() != null) {
          ((DomainResource) r.getResource()).setText(null);
        }
      }
    }
    PublisherProcessor processor = new PublisherProcessor(new PublisherSettings());
    processor.setPf(b.pf);
    processor.generateNarratives(false);
    assertNotNull(b.resource(VS).getElement().getNamedChild("text"));
    assertFalse(Files.exists(other.resolve("_rapido")));
  }

  // ---- the records ----

  private static org.hl7.fhir.utilities.xhtml.XhtmlNode sample() {
    org.hl7.fhir.utilities.xhtml.XhtmlNode div = new org.hl7.fhir.utilities.xhtml.XhtmlNode(org.hl7.fhir.utilities.xhtml.NodeType.Element, "div");
    div.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
    div.setAttribute("xml:lang", "en");
    div.setAttribute("lang", "en");
    div.setAttribute("class", "a b");
    org.hl7.fhir.utilities.xhtml.XhtmlNode td = div.table("grid").tr().td();
    td.tx(""); // a renderer's empty cell: reading xhtml text back drops it, and the cell is then written another way
    org.hl7.fhir.utilities.xhtml.XhtmlNode p = div.para();
    p.tx("a");
    p.tx("b"); // two text nodes: read back from text they are one
    div.pre().notPretty().tx(" x é \n");
    div.addTag("br").setEmptyExpanded(true);
    div.addComment("note").setLocation(new org.hl7.fhir.utilities.xhtml.XhtmlNode.Location(3, 14)); // as read from markdown
    return div;
  }

  @Test
  void aRecordedTreeComesBackNodeForNode() throws Exception {
    org.hl7.fhir.utilities.xhtml.XhtmlNode div = sample();
    org.hl7.fhir.utilities.xhtml.XhtmlNode copy = RapidoNarratives.copy(div);
    assertNotNull(copy);
    assertEquals(tree(div), tree(copy));

    // and from a record file
    RapidoNarratives records = new RapidoNarratives(dir.toString());
    FetchedFile f = new FetchedFile("input/a.json");
    RapidoNarratives.FileRecord rec = new RapidoNarratives.FileRecord("id-1");
    rec.resources.add(null);
    rec.resources.add(new RapidoNarratives.ResourceRecord("Patient", "p", true, "generated", div, "<div/>", true, "key"));
    assertTrue(records.save(f, rec));
    RapidoNarratives.FileRecord read = records.read(f, "id-1");
    assertNotNull(read);
    assertEquals(2, read.resources.size());
    assertNull(read.resources.get(0));
    RapidoNarratives.ResourceRecord r = read.resources.get(1);
    assertEquals("Patient p true generated <div/> true key", r.type + " " + r.id + " " + r.onResource + " " + r.status + " " + r.source + " " + r.marked + " " + r.key);
    assertEquals(tree(div), tree(r.div));

    // a tree read back from text is not the same tree
    String text = new XhtmlComposer(XhtmlComposer.XML).compose(div);
    assertNotEquals(tree(div), tree(new org.hl7.fhir.utilities.xhtml.XhtmlParser().parseFragment(text)));
  }

  @Test
  void aRecordIsOnlyReadByItsIdAndWhole() throws Exception {
    RapidoNarratives records = new RapidoNarratives(dir.toString());
    FetchedFile f = new FetchedFile("input/a.json");
    RapidoNarratives.FileRecord rec = new RapidoNarratives.FileRecord("id-1");
    rec.resources.add(new RapidoNarratives.ResourceRecord("Patient", "p", false, "generated", sample(), null, false, "key"));
    assertTrue(records.save(f, rec));

    assertNotNull(records.read(f, "id-1"));
    assertNull(records.read(f, "id-2"));
    assertNull(records.read(f, null));
    assertNull(records.read(new FetchedFile("input/b.json"), "id-1"));

    Path file;
    try (Stream<Path> s = Files.list(dir.resolve("_rapido").resolve("narratives"))) {
      file = s.findFirst().orElseThrow();
    }
    byte[] whole = Files.readAllBytes(file);
    for (int n : new int[] {0, 3, 40, whole.length / 2, whole.length - 1}) {
      Files.write(file, java.util.Arrays.copyOf(whole, n));
      assertNull(records.read(f, "id-1"), "cut to " + n);
    }
    Files.write(file, java.util.Arrays.copyOf(whole, whole.length + 1));
    assertNull(records.read(f, "id-1"));
    Files.write(file, whole);
    assertNotNull(records.read(f, "id-1"));

    records.forget(f);
    assertNull(records.read(f, "id-1"));
  }

  /** a tree with something a record doesn't hold isn't recorded: the narrative is generated in the next build */
  @Test
  void aTreeThatCantComeBackAsItIsIsNotRecorded() throws Exception {
    org.hl7.fhir.utilities.xhtml.XhtmlNode div = sample();
    div.getChildNodes().add(new org.hl7.fhir.utilities.xhtml.XhtmlNode(org.hl7.fhir.utilities.xhtml.NodeType.Element, "p") { }); // some other kind of node
    assertNull(RapidoNarratives.copy(div));

    RapidoNarratives records = new RapidoNarratives(dir.toString());
    FetchedFile f = new FetchedFile("input/a.json");
    RapidoNarratives.FileRecord rec = new RapidoNarratives.FileRecord("id-1");
    rec.resources.add(new RapidoNarratives.ResourceRecord("Patient", "p", false, "generated", div, null, false, "key"));
    assertFalse(records.save(f, rec));
    assertNull(records.read(f, "id-1"));
  }
}
