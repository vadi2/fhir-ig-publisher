package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hl7.fhir.igtools.templates.Template;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.services.context.IWorkerContext;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.terminology.ExpansionOptions;
import org.hl7.fhir.services.terminology.ValueSetExpansionOutcome;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.standalone.terminology.utilities.TerminologyCache;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.NamedItemList;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.hl7.fhir.utilities.xhtml.HierarchicalTableGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The terminology cache keys a ValueSet that has a version by url|version, and an IG's ValueSets keep their version
 * while they're edited. So before a build expands or validates anything, what the cache holds for the ValueSets and
 * CodeSystems that may have changed is removed: in a differential build, the ones validated again (they changed, or
 * an included ValueSet, a CodeSystem or a supplement they use did); in a complete build or a build without -rapido,
 * all of the IG's own. Both also remove the urls the IG defined in the last build and no longer does.
 *
 * <p>A -watch rebuild runs the same checkDependencies with a new context, whose cache is read again from disk, so these
 * cover it too; the scrub on disk is tested in core's TerminologyCacheTests.
 */
class RapidoTxCacheScrubTest {

  private static final String B = "http://example.org/ig";
  private static final String SCT = "http://snomed.info/sct";
  private static final String CS_A = B+"/CodeSystem/cs-a";
  private static final String SUPP = B+"/CodeSystem/sct-supp";
  private static final String VS_A = B+"/ValueSet/vs-a";
  private static final String VS_B = B+"/ValueSet/vs-b";
  private static final String VS_SCT = B+"/ValueSet/vs-sct";
  private static final String VS_OTHER = B+"/ValueSet/vs-other";
  private static final String EXTERNAL_VS = "http://hl7.org/fhir/ValueSet/administrative-gender";

  /** every url the cache holds an expansion for */
  private static final List<String> ALL = List.of(CS_A, SUPP, VS_A, VS_B, VS_SCT, VS_OTHER, SCT, EXTERNAL_VS, VS_A+"x");

  @TempDir
  Path dir;

  private final String uuid = HierarchicalTableGenerator.uuid;
  private TerminologyCache cache;
  /** the content of each file of the IG: file name -> its resource's (child name, value) pairs */
  private final Map<String, List<String[]>> ig = new LinkedHashMap<>();

  @BeforeEach
  void setUp() throws Exception {
    cache = new TerminologyCache(new Object(), null, mock(IWorkerContext.class));
    ig.put("CodeSystem-cs-a.json", content("url", CS_A, "concept", "a1"));
    // a supplement to an external code system, which vs-sct includes
    ig.put("CodeSystem-sct-supp.json", content("url", SUPP, "supplements", SCT, "designation", "uz display"));
    ig.put("ValueSet-vs-a.json", content("url", VS_A, "system", CS_A));
    ig.put("ValueSet-vs-b.json", content("url", VS_B, "valueSet", VS_A));
    ig.put("ValueSet-vs-sct.json", content("url", VS_SCT, "system", SCT));
    ig.put("ValueSet-vs-other.json", content("url", VS_OTHER, "system", "http://loinc.org"));
  }

  @AfterEach
  void restoreUuid() {
    HierarchicalTableGenerator.uuid = uuid;
  }

  private static List<String[]> content(String... pairs) {
    List<String[]> l = new ArrayList<>();
    for (int i = 0; i < pairs.length; i += 2) {
      l.add(new String[] {pairs[i], pairs[i+1]});
    }
    return l;
  }

  private void edit(String file, String name, String value) {
    ig.get(file).add(new String[] {name, value});
  }

  private void set(String file, String name, String value) {
    for (String[] c : ig.get(file)) {
      if (c[0].equals(name)) {
        c[1] = value;
      }
    }
  }

  private void fillCache() {
    for (String url : ALL) {
      cache.cacheExpansion(cache.generateExpandToken(url, new ExpansionOptions()), new ValueSetExpansionOutcome(new ValueSet().setUrl(url)), true);
    }
  }

  /** the urls the cache still has an expansion for */
  private Set<String> cached() {
    return ALL.stream().filter(u -> cache.getExpansion(cache.generateExpandToken(u, new ExpansionOptions())) != null)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  private Set<String> allBut(String... urls) {
    Set<String> res = new TreeSet<>(ALL);
    res.removeAll(List.of(urls));
    return res;
  }

  /** a first build, finished, and the cache filled as it would leave it */
  private void firstBuild() throws Exception {
    fillCache();
    PublisherFields pf = build(true);
    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals(Set.of(SCT, EXTERNAL_VS, VS_A+"x"), cached(), "a complete build removes the entries for every ValueSet and CodeSystem of the IG");
    finish(pf);
    fillCache();
  }

  @Test
  void aChangedValueSetAndTheValueSetsThatIncludeItAreScrubbed() throws Exception {
    firstBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    // vs-b includes vs-a; the code system vs-a includes didn't change; vs-ax only starts with vs-a's url
    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aChangedCodeSystemAndTheValueSetsThatUseItAreScrubbed() throws Exception {
    firstBuild();
    edit("CodeSystem-cs-a.json", "concept", "a2");

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());

    assertEquals(allBut(CS_A, VS_A, VS_B), cached());
  }

  @Test
  void aChangedSupplementScrubsItsCodeSystemAndTheValueSetsThatIncludeIt() throws Exception {
    firstBuild();
    edit("CodeSystem-sct-supp.json", "designation", "new uz display");

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());

    // vs-sct doesn't name the supplement, but core sends it with every expansion and validation that uses SNOMED CT
    assertEquals(allBut(SUPP, SCT, VS_SCT), cached());
  }

  @Test
  void theOldUrlOfACodeSystemWhoseUrlChangedIsScrubbed() throws Exception {
    firstBuild();
    set("CodeSystem-cs-a.json", "url", CS_A+"-new");

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    // what was cached for cs-a while the IG defined it isn't what the server says now
    assertEquals(allBut(CS_A, VS_A, VS_B), cached());
  }

  @Test
  void theUrlOfADeletedValueSetIsScrubbed() throws Exception {
    firstBuild();
    ig.remove("ValueSet-vs-other.json");

    PublisherFields pf = build(true);

    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals(Set.of(SCT, EXTERNAL_VS, VS_A+"x"), cached());
  }

  @Test
  void nothingChangedKeepsEveryEntry() throws Exception {
    firstBuild();

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    assertEquals(new TreeSet<>(ALL), cached());
  }

  @Test
  void aBuildWithoutRapidoScrubsEveryValueSetAndCodeSystemOfTheIg() throws Exception {
    fillCache();

    build(false);

    assertEquals(Set.of(SCT, EXTERNAL_VS, VS_A+"x"), cached());
  }

  /** what the rest of a rapido build does that checkDependencies needs next time */
  private void finish(PublisherFields pf) throws Exception {
    pf.rapido.finishValidation();
    for (FetchedFile f : pf.fileList) {
      pf.rapido.startRecording(f, RapidoTracker.PHASE_HTML);
      pf.rapido.stopRecording();
      pf.rapido.startRecording(f, RapidoTracker.PHASE_SHEETS);
      pf.rapido.stopRecording();
    }
    new RapidoOutputs(pf.vsCache, pf.tempDir).update(pf.fileList, pf.changeList);
    pf.rapido.save();
    IniFile buildTracker = new IniFile(dir.resolve(".build-tracker.ini").toString());
    buildTracker.setBooleanProperty("status", "complete", true, null);
    buildTracker.save();
  }

  private PublisherFields build(boolean rapido) throws Exception {
    FetchedFile igf = file("ig.json", "{}", List.of());
    Publisher publisher = new Publisher();
    publisher.getSettings().setRapidoMode(rapido);
    PublisherFields pf = new PublisherFields();
    pf.logger = mock(ILoggingService.class);
    pf.rootDir = dir.toString();
    pf.vsCache = dir.toString();
    pf.tempDir = dir.resolve("temp").toString();
    pf.igpkp = mock(IGKnowledgeProvider.class);
    when(pf.igpkp.getCanonical()).thenReturn(B);
    pf.template = mock(Template.class);
    pf.context = mock(SimpleWorkerContext.class);
    when(pf.context.getTxCache()).thenReturn(cache);
    pf.fileList.add(igf);
    for (Map.Entry<String, List<String[]>> e : ig.entrySet()) {
      String type = e.getKey().substring(0, e.getKey().indexOf('-'));
      String id = e.getKey().substring(e.getKey().indexOf('-')+1, e.getKey().length()-5);
      StringBuilder src = new StringBuilder();
      for (String[] c : e.getValue()) {
        src.append(c[0]).append('=').append(c[1]).append('\n');
      }
      pf.fileList.add(file(e.getKey(), src.toString(), List.of(resource(type, id, e.getValue()))));
    }
    pf.changeList.addAll(pf.fileList);
    publisher.setPf(pf);
    Field loader = Publisher.class.getDeclaredField("loader");
    loader.setAccessible(true);
    ((PublisherIGLoader) loader.get(publisher)).setPf(pf);

    publisher.checkDependencies(igf, new IniFile(dir.resolve(".build-tracker.ini").toString()));
    return pf;
  }

  private FetchedFile file(String name, String content, List<FetchedResource> resources) {
    FetchedFile f = new FetchedFile(name);
    f.setPath(dir.resolve("input").resolve(name).toString());
    f.setSource(content.getBytes(StandardCharsets.UTF_8));
    f.getResources().addAll(resources);
    return f;
  }

  private static FetchedResource resource(String type, String id, List<String[]> content) {
    NamedItemList<Element> children = new NamedItemList<>();
    for (String[] c : content) {
      children.add(primitive(c[0], c[1]));
    }
    Element element = mock(Element.class);
    when(element.getChildList()).thenReturn(children);
    when(element.hasChildren()).thenReturn(true);
    when(element.getChildrenByName(anyString())).thenAnswer(inv -> {
      List<Element> l = new ArrayList<>();
      for (Element c : children) {
        if (c.getName().equals(inv.getArgument(0))) {
          l.add(c);
        }
      }
      return l;
    });
    when(element.getName()).thenReturn(type);
    when(element.fhirType()).thenReturn(type);
    when(element.getComments()).thenReturn(new ArrayList<>());
    FetchedResource resource = mock(FetchedResource.class);
    when(resource.fhirType()).thenReturn(type);
    when(resource.getId()).thenReturn(id);
    when(resource.getElement()).thenReturn(element);
    return resource;
  }

  private static Element primitive(String name, String value) {
    Element element = mock(Element.class);
    when(element.isPrimitive()).thenReturn(true);
    when(element.getName()).thenReturn(name);
    when(element.fhirType()).thenReturn("string");
    when(element.hasValue()).thenReturn(true);
    when(element.getValue()).thenReturn(value);
    when(element.primitiveValue()).thenReturn(value);
    when(element.getComments()).thenReturn(new ArrayList<>());
    return element;
  }
}
