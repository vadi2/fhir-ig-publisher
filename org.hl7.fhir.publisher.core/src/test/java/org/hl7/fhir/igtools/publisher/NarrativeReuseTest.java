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

  @BeforeAll
  static void loadContext() throws Exception {
    try {
      NpmPackage npm = new FilesystemPackageCacheManager.Builder().build().loadPackage("hl7.fhir.r5.core", "5.0.0");
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

  private static final String CS = "CodeSystem-colours.json";
  private static final String VS = "ValueSet-colours.json";
  private static final String PATIENT = "Patient-p.json";
  private static final String OTHER = "Patient-q.json";

  /** the sources of an IG: a value set of all the codes of a code system, and two patients */
  private static Map<String, String> sources(String codeSystemTitle) {
    Map<String, String> s = new LinkedHashMap<>();
    s.put(CS, "{\"resourceType\":\"CodeSystem\",\"id\":\"colours\",\"url\":\"http://example.org/ig/CodeSystem/colours\",\"version\":\"1.0.0\",\"name\":\"Colours\","
        + "\"title\":\"" + codeSystemTitle + "\",\"status\":\"active\",\"caseSensitive\":true,\"content\":\"complete\","
        + "\"concept\":[{\"code\":\"red\",\"display\":\"Red\"},{\"code\":\"blue\",\"display\":\"Blue\",\"definition\":\"\"}]}");
    s.put(VS, "{\"resourceType\":\"ValueSet\",\"id\":\"colours\",\"url\":\"http://example.org/ig/ValueSet/colours\",\"version\":\"1.0.0\",\"name\":\"ColourSet\","
        + "\"title\":\"All the colours\",\"status\":\"active\",\"compose\":{\"include\":[{\"system\":\"http://example.org/ig/CodeSystem/colours\"}]}}");
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
        byte[] cnt = s.getValue().getBytes(StandardCharsets.UTF_8);
        Element e = new JsonParser(context).parseSingle(new ByteArrayInputStream(cnt), null);
        FetchedResource r = f.addResource(s.getKey());
        r.setElement(e).setId(e.getIdBase());
        if (!"Patient".equals(e.fhirType())) {
          // a conformance resource is loaded as a resource as well, and is in the context
          Resource res = new org.hl7.fhir.model.core.formats.JsonParser(context.getModelContext()).parse(cnt);
          r.setResource(res);
          res.setWebPath(e.fhirType() + "-" + e.getIdBase() + ".html");
          context.cacheResource(res);
          cached.add(res);
        }
        loaded.accept(r);
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
      processor.generateNarratives(false);
      rendered = mockingDetails(pf.rendererFactory).getInvocations().size() / (1 + langs.size());
      return this;
    }

    /** the rest of the build, as far as Rapido goes: what each file made is recorded, and the state is saved */
    Build finish() throws Exception {
      pf.rapido.finishValidation();
      for (FetchedFile f : pf.fileList) {
        for (String phase : new String[] {RapidoTracker.PHASE_HTML, RapidoTracker.PHASE_SHEETS}) {
          if (!pf.rapido.canReplay(f, phase)) {
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

    FetchedResource resource(String file) {
      return pf.fileList.stream().filter(f -> f.getLoadPath().equals(file)).findFirst().orElseThrow().getResources().get(0);
    }

    String narrative(String file) throws Exception {
      return new XhtmlComposer(XhtmlComposer.XML).compose(resource(file).getElement().getNamedChild("text").getNamedChild("div").getXhtml());
    }

    /** every resource as the build left it, in every form it is written in */
    Map<String, String> state() throws Exception {
      Map<String, String> res = new LinkedHashMap<>();
      for (FetchedFile f : pf.fileList) {
        for (FetchedResource r : f.getResources()) {
          String k = f.getLoadPath() + " ";
          Element e = r.getElement();
          res.put(k + "element json", compose(new JsonParser(context), e));
          res.put(k + "element xml", compose(new org.hl7.fhir.services.elementmodel.XmlParser(context), e));
          RapidoDependencyAnalyser.Hasher h = new RapidoDependencyAnalyser.Hasher();
          RapidoNativeOutputs.contentKey(h, e);
          res.put(k + "element key", h.finish());
          Element div = e.getNamedChild("text").getNamedChild("div");
          res.put(k + "element narrative", tree(div.getXhtml()) + " source " + div.getXhtmlSource());
          res.put(k + "element data", new TreeSet<>(e.getUserDataNames()).toString());
          res.put(k + "element shape", shape(e));
          res.put(k + "generated", r.isGeneratedNarrative() + " " + r.isRegenAfterValidation());
          res.put(k + "messages", r.getErrors().size() + " " + f.getErrors().size());
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
        .append(x.isNoPretty()).append(" ").append(x.getEmptyExpanded());
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
    assertEquals(sources.size(), b.rendered);
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
      Map<String, String> sources = sources("Colours");
      Build first = new Build(root, sources).langs(langs).run();
      assertTrue(first.pf.rapido.isCompleteBuild());
      assertEquals(4, first.rendered);
      first.finish();

      Build second = new Build(root, sources).langs(langs).run();
      assertFalse(second.pf.rapido.isCompleteBuild(), second.pf.rapido.getReason());
      assertEquals(0, second.rendered, "no narrative is generated");
      assertTrue(second.narrative(VS).contains("Colours"));
      assertSameState(fresh(sources, langs), second);
      second.finish();

      // and again from what the second build kept, which wrote no record
      Build third = new Build(root, sources).langs(langs).run();
      assertEquals(0, third.rendered);
      assertSameState(fresh(sources, langs), third);
    }
  }

  /** the value set's narrative names the code system by its title: the value set's own file didn't change */
  @Test
  void aNarrativeThatShowsTheTitleOfAnotherResourceIsGeneratedWhenThatTitleChanges() throws Exception {
    build(sources("Colours")).run().finish();

    Map<String, String> sources = sources("Hues and shades");
    Build second = build(sources).run();
    assertFalse(second.pf.rapido.isCompleteBuild(), second.pf.rapido.getReason());
    assertEquals(2, second.rendered, "the code system and the value set; the patients' narratives are put back");
    assertTrue(second.narrative(VS).contains("Hues and shades"), second.narrative(VS));
    assertFalse(second.narrative(VS).contains("Colours"), second.narrative(VS));
    assertSameState(fresh(sources), second);
    second.finish();

    // the new narrative is the one the next build puts back
    Build third = build(sources).run();
    assertEquals(0, third.rendered);
    assertTrue(third.narrative(VS).contains("Hues and shades"));
    assertSameState(fresh(sources), third);
  }

  @Test
  void aChangedResourceGetsItsNarrativeGenerated() throws Exception {
    build(sources("Colours")).run().finish();

    Map<String, String> sources = sources("Colours");
    sources.put(PATIENT, sources.get(PATIENT).replace("female", "male"));
    Build second = build(sources).run();
    assertEquals(1, second.rendered);
    assertTrue(second.narrative(PATIENT).contains("Male"), second.narrative(PATIENT));
    assertSameState(fresh(sources), second);
  }

  /**
   * A build wrote records and then didn't finish, so the state is still the one of the build before it: what that
   * state says is unchanged is compared with that build, and the records are not that build's
   */
  @Test
  void theRecordOfABuildThatDidntFinishIsNotUsed() throws Exception {
    Map<String, String> sources = sources("Colours");
    build(sources).run().finish();

    Build failed = build(sources("Hues and shades")).run();
    assertEquals(2, failed.rendered);
    failed.endedWell(); // as far as the build tracker knows: the Rapido state wasn't saved

    Build third = build(sources).run();
    assertFalse(third.pf.rapido.isCompleteBuild(), third.pf.rapido.getReason());
    assertEquals(2, third.rendered, "the code system and the value set have the records of the build that didn't finish");
    assertTrue(third.narrative(VS).contains("Colours"), third.narrative(VS));
    assertSameState(fresh(sources), third);
  }

  /**
   * The loader changed the code system (it does: a publisher, a date, a version) without converting it to an element
   * again. Generating the narrative converts it, so the element then has the change; the element as loaded with the
   * narrative put back wouldn't
   */
  @Test
  void aNarrativeIsGeneratedWhenTheElementWouldNotBeTheOneGeneratingGives() throws Exception {
    Map<String, String> sources = sources("Colours");
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
    assertTrue(second.state().get(CS + " element json").contains("The loader"));
  }

  /** a narrative that came with the resource isn't generated, and isn't put back either: it is checked, every build */
  @Test
  void aNarrativeThatCameWithTheResourceIsLeftAlone() throws Exception {
    Map<String, String> sources = sources("Colours");
    sources.put(OTHER, sources.get(OTHER).replace("\"active\":true", "\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><p><b>Generated Narrative: Patient q</b></p></div>\"},\"active\":true"));
    Build first = build(sources).run();
    assertEquals(3, first.rendered);
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
    Build b = build(sources("Colours")).run();
    assertTrue(Files.isDirectory(dir.resolve("ig").resolve("temp").resolve("_rapido").resolve("narratives")));
    try (Stream<Path> s = Files.list(dir.resolve("ig").resolve("temp").resolve("_rapido").resolve("narratives"))) {
      assertEquals(4, s.count());
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
    div.para().tx("a").tx("b"); // two text nodes: read back from text they are one
    div.pre().notPretty().tx(" x é \n");
    div.addTag("br").setEmptyExpanded(true);
    div.addComment("note");
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
    div.getChildNodes().get(0).setLocation(new org.hl7.fhir.utilities.xhtml.XhtmlNode.Location(1, 2));
    assertNull(RapidoNarratives.copy(div));

    RapidoNarratives records = new RapidoNarratives(dir.toString());
    FetchedFile f = new FetchedFile("input/a.json");
    RapidoNarratives.FileRecord rec = new RapidoNarratives.FileRecord("id-1");
    rec.resources.add(new RapidoNarratives.ResourceRecord("Patient", "p", false, "generated", div, null, false, "key"));
    assertFalse(records.save(f, rec));
    assertNull(records.read(f, "id-1"));
  }
}
