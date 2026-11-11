package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
 * CodeSystems that may have changed is removed: those that changed since the cache was last scrubbed (a record next to
 * the cache), with those that name them and the code systems their supplements add to, and the urls the IG no longer
 * defines; all of the IG's own when there is no record. That happens before each snapshot generation, for what is
 * loaded by then, and once everything is loaded. The record marks what is being scrubbed first, so a build that stops
 * leaves it to the next. Each IG has its own record, as IGs can share a cache folder.
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
  /** the files whose resource has no element model, only a resource */
  private final Set<String> noElement = new HashSet<>();

  private interface Snapshots {
    void run(boolean first) throws Exception;
  }

  private interface BeforeBuild {
    void accept(PublisherFields pf) throws Exception;
  }

  /** what the loader adds to a file's resource (as pinning a version does) */
  private final Map<String, List<String[]>> addedByLoad = new HashMap<>();
  private String packageId;
  private BeforeBuild beforeBuild = pf -> {};

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
      cacheExpansion(url);
    }
  }

  private void cacheExpansion(String url) {
    cache.cacheExpansion(cache.generateExpandToken(url, new ExpansionOptions()), new ValueSetExpansionOutcome(new ValueSet().setUrl(url)), true);
  }

  private boolean isCached(String url) {
    return cache.getExpansion(cache.generateExpandToken(url, new ExpansionOptions())) != null;
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
    assertEquals(Set.of(EXTERNAL_VS, VS_A+"x"), cached(), "a complete build removes the entries for every ValueSet and CodeSystem of the IG, and what their supplements add to");
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

    // a file less makes it a complete build, which still keeps the answers for what didn't change
    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals(allBut(VS_OTHER), cached());
  }

  @Test
  void theUrlOfAValueSetDeletedAfterABuildWithTxFailuresIsScrubbed() throws Exception {
    fillCache();
    PublisherFields first = build(true);
    // the server didn't answer some calls: the state keeps nothing for the next build, apart from the urls
    first.txFailures = mock(RapidoTracker.TxFailures.class);
    when(first.txFailures.count()).thenReturn(1);
    finish(first);
    fillCache();
    ig.remove("ValueSet-vs-other.json");

    PublisherFields pf = build(true);

    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals(allBut(VS_OTHER), cached());
  }

  @Test
  void aCompleteBuildWithNothingChangedKeepsEveryEntry() throws Exception {
    firstBuild();
    // the next build starts while this one never finished
    build(true);
    fillCache();

    PublisherFields pf = build(true);

    assertTrue(pf.rapido.isCompleteBuild());
    assertEquals(new TreeSet<>(ALL), cached());
  }

  @Test
  void nothingChangedKeepsEveryEntry() throws Exception {
    firstBuild();

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    assertEquals(new TreeSet<>(ALL), cached());
  }

  @Test
  void aBuildWithoutRapidoOrARecordScrubsEveryValueSetAndCodeSystemOfTheIg() throws Exception {
    fillCache();

    build(false);

    assertEquals(Set.of(EXTERNAL_VS, VS_A+"x"), cached());
  }

  /** a build without -rapido, which scrubs everything and records the IG's ValueSets and CodeSystems */
  private void firstPlainBuild() throws Exception {
    fillCache();
    build(false);
    fillCache();
  }

  @Test
  void aBuildWithoutRapidoWithNothingChangedKeepsEveryEntry() throws Exception {
    firstPlainBuild();

    PublisherFields pf = build(false);

    assertEquals(new TreeSet<>(ALL), cached());
    assertTrue(pf.errors.isEmpty(), "nothing in the QA");
  }

  @Test
  void aBuildWithoutRapidoScrubsAChangedValueSetAndTheValueSetsThatIncludeIt() throws Exception {
    firstPlainBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");

    build(false);

    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aBuildWithoutRapidoScrubsAChangedCodeSystemAndTheValueSetsThatUseIt() throws Exception {
    firstPlainBuild();
    edit("CodeSystem-cs-a.json", "concept", "a2");

    build(false);

    assertEquals(allBut(CS_A, VS_A, VS_B), cached());
  }

  @Test
  void aBuildWithoutRapidoScrubsTheCodeSystemAChangedSupplementAddsTo() throws Exception {
    firstPlainBuild();
    edit("CodeSystem-sct-supp.json", "designation", "new uz display");

    build(false);

    assertEquals(allBut(SUPP, SCT, VS_SCT), cached());
  }

  @Test
  void aBuildWithoutRapidoScrubsADeletedValueSetAndTheValueSetsThatIncludedIt() throws Exception {
    firstPlainBuild();
    ig.remove("ValueSet-vs-a.json");

    build(false);

    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aValueSetThatNamesAChangedOneWithAVersionOrAsAnImplicitValueSetIsScrubbed() throws Exception {
    String vsC = B+"/ValueSet/vs-c";
    String vsD = B+"/ValueSet/vs-d";
    ig.put("ValueSet-vs-c.json", content("url", vsC, "valueSet", VS_A+"|0.1.0"));
    ig.put("ValueSet-vs-d.json", content("url", vsD, "valueSet", CS_A+"?fhir_vs=isa/a1"));
    firstPlainBuild();
    cacheExpansion(vsC);
    cacheExpansion(vsD);
    edit("CodeSystem-cs-a.json", "concept", "a2");

    build(false);

    assertEquals(allBut(CS_A, VS_A, VS_B), cached());
    assertFalse(isCached(vsC), "vs-c includes vs-a|0.1.0");
    assertFalse(isCached(vsD), "vs-d includes an implicit value set of cs-a");
  }

  @Test
  void aRecordEntryWithoutAHashScrubsEverything() throws Exception {
    firstPlainBuild();
    Files.writeString(dir.resolve(".tx-cache-scrub.json"), "{\"terminology\":[{\"url\":\""+VS_A+"\"}]}");

    build(false);

    assertEquals(Set.of(EXTERNAL_VS, VS_A+"x"), cached());
  }

  @Test
  void aRapidoBuildAfterABuildWithoutRapidoOfOtherContentScrubsWhatDiffers() throws Exception {
    firstBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");
    build(false);
    fillCache();
    // back to what the rapido state has: rapido sees no change, but the cache has answers for the edited vs-a
    ig.get("ValueSet-vs-a.json").remove(ig.get("ValueSet-vs-a.json").size()-1);

    PublisherFields pf = build(true);

    assertFalse(pf.rapido.isCompleteBuild(), pf.rapido.getReason());
    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void anUnreadableRecordScrubsEverything() throws Exception {
    firstPlainBuild();
    Files.writeString(dir.resolve(".tx-cache-scrub.json"), "{not json");

    build(false);

    assertEquals(Set.of(EXTERNAL_VS, VS_A+"x"), cached());
  }

  @Test
  void aDeletedSupplementScrubsTheCodeSystemItAddedTo() throws Exception {
    firstPlainBuild();
    ig.remove("CodeSystem-sct-supp.json");

    build(false);

    // core sent the supplement with the SNOMED CT answers
    assertEquals(allBut(SUPP, SCT, VS_SCT), cached());
  }

  @Test
  void aSupplementMovedToAnotherCodeSystemScrubsBoth() throws Exception {
    firstPlainBuild();
    set("CodeSystem-sct-supp.json", "supplements", "http://loinc.org");

    build(false);

    assertEquals(allBut(SUPP, SCT, VS_SCT, VS_OTHER), cached());
  }

  @Test
  void whatChangedIsScrubbedBeforeTheSnapshotsAndNotAgainAfterThem() throws Exception {
    firstPlainBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");
    edit("ValueSet-vs-other.json", "concept", "o2");

    build(false, Set.of("ValueSet-vs-other.json"), first -> {
      if (first) {
        // vs-other isn't loaded yet, which doesn't make it deleted
        assertEquals(allBut(VS_A, VS_B), cached(), "before the first snapshots");
        // the snapshots expand vs-a again, from its new content
        cacheExpansion(VS_A);
      } else {
        assertFalse(isCached(VS_OTHER), "vs-other is scrubbed before the snapshots of what was loaded late");
        assertTrue(isCached(VS_A), "vs-a isn't scrubbed again");
      }
    });

    assertTrue(isCached(VS_A), "vs-a's new expansion is kept");
    assertEquals(allBut(VS_B, VS_OTHER), cached());
  }

  @Test
  void aScrubWhoseWriteFailedIsTriedAgainNextBuild() throws Exception {
    firstPlainBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");
    TerminologyCache.ScrubResult failed = mock(TerminologyCache.ScrubResult.class);
    when(failed.getFailed()).thenReturn(List.of("cs-a"));
    TerminologyCache working = cache;
    cache = mock(TerminologyCache.class);
    when(cache.scrub(any())).thenReturn(failed);

    PublisherFields pf = build(false);

    assertEquals(1, pf.errors.size(), "the failure is in the QA");
    cache = working;
    fillCache();
    build(false);
    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aRecordThatCannotBeWrittenDoesntStopTheBuild() throws Exception {
    firstPlainBuild();
    Path record = dir.resolve(".tx-cache-scrub.json");
    Files.delete(record);
    Files.createDirectory(record);

    PublisherFields pf = build(false);

    assertEquals(1, pf.errors.size(), "the failure is in the QA");
  }

  @Test
  void aValueSetWithoutAnElementIsScrubbedEveryBuild() throws Exception {
    noElement.add("ValueSet-vs-a.json");
    firstPlainBuild();

    build(false);

    // nothing to compare it by, so it is taken as changed, with what includes it
    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aValueSetWithoutAUrlIsTrackedByTheUrlTheLoaderGivesIt() throws Exception {
    String vsNoUrl = B+"/ValueSet/vs-no-url";
    ig.put("ValueSet-vs-no-url.json", content("system", CS_A));
    firstPlainBuild();
    cacheExpansion(vsNoUrl);
    edit("ValueSet-vs-no-url.json", "concept", "a2");

    build(false);

    assertFalse(isCached(vsNoUrl));
    assertEquals(new TreeSet<>(ALL), cached());
  }

  @Test
  void aChangeTheLoaderMakesIsScrubbed() throws Exception {
    firstPlainBuild();
    // as pinning the version of the code system vs-a includes does, after a dependency's version changed
    addedByLoad.put("ValueSet-vs-a.json", content("version", "2.0.0"));

    build(false, Set.of(), first -> {
      if (first) {
        assertEquals(allBut(VS_A, VS_B), cached(), "before the snapshots");
      }
    });

    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aBuildThatStopsAfterAScrubLeavesItToTheNextBuildEvenIfTheEditIsUndone() throws Exception {
    firstPlainBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");
    assertThrows(Exception.class, () -> build(false, Set.of(), first -> {
      // the snapshots expand the edited vs-a, and then the build fails
      cacheExpansion(VS_A);
      throw new Exception("snapshot generation failed");
    }));
    assertTrue(isCached(VS_A));
    ig.get("ValueSet-vs-a.json").remove(ig.get("ValueSet-vs-a.json").size()-1);

    build(false);

    assertEquals(allBut(VS_A, VS_B), cached());
  }

  @Test
  void aRecordThatCannotBeUpdatedBeforeAScrubIsDeleted() throws Exception {
    firstPlainBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");
    beforeBuild = pf -> {
      pf.txCacheScrub = spy(new RapidoTracker(pf));
      doThrow(new IOException("locked")).when(pf.txCacheScrub).writeTxCacheRecord(any());
    };
    PublisherFields pf = build(false);
    assertEquals(2, pf.errors.size(), "before the scrub and when the record is saved: both in the QA");
    assertFalse(Files.exists(dir.resolve(".tx-cache-scrub.json")));
    beforeBuild = p -> {};
    fillCache();

    build(false);

    assertEquals(Set.of(EXTERNAL_VS, VS_A+"x"), cached(), "no record: everything is scrubbed");
  }

  @Test
  void aRecordThatCannotBeUpdatedOrDeletedBeforeAScrubIsMarkedStale() throws Exception {
    firstPlainBuild();
    edit("ValueSet-vs-a.json", "concept", "a2");
    beforeBuild = pf -> {
      pf.txCacheScrub = spy(new RapidoTracker(pf));
      doThrow(new IOException("locked")).when(pf.txCacheScrub).writeTxCacheRecord(any());
      doReturn(false).when(pf.txCacheScrub).deleteTxCacheRecord();
    };
    PublisherFields pf = build(false);
    assertEquals(2, pf.errors.size(), "before the scrub and when the record is saved: both in the QA");
    assertTrue(Files.exists(dir.resolve(".tx-cache-scrub.json.stale")));
    beforeBuild = p -> {};
    fillCache();
    // vs-a back as recorded
    ig.get("ValueSet-vs-a.json").remove(ig.get("ValueSet-vs-a.json").size()-1);

    build(false);

    assertEquals(Set.of(EXTERNAL_VS, VS_A+"x"), cached(), "the record is stale: everything is scrubbed");
    assertFalse(Files.exists(dir.resolve(".tx-cache-scrub.json.stale")), "and the record saved again");
    fillCache();
    build(false);
    assertEquals(new TreeSet<>(ALL), cached());
  }

  @Test
  void aChangedVersionedSupplementScrubsItsCodeSystemAndTheValueSetsThatIncludeIt() throws Exception {
    set("CodeSystem-sct-supp.json", "supplements", SCT+"|http://snomed.info/sct/45991000052106");
    firstPlainBuild();
    edit("CodeSystem-sct-supp.json", "designation", "new uz display");

    build(false);

    assertEquals(allBut(SUPP, SCT, VS_SCT), cached());
  }

  @Test
  void aDeletedVersionedSupplementScrubsItsCodeSystemAndTheValueSetsThatIncludeIt() throws Exception {
    set("CodeSystem-sct-supp.json", "supplements", SCT+"|http://snomed.info/sct/45991000052106");
    firstPlainBuild();
    ig.remove("CodeSystem-sct-supp.json");

    build(false);

    assertEquals(allBut(SUPP, SCT, VS_SCT), cached());
  }

  @Test
  void aRecordWhoseSupplementsIsntAStringScrubsEverything() throws Exception {
    firstPlainBuild();
    Path record = dir.resolve(".tx-cache-scrub.json");
    String recorded = Files.readString(record);
    assertTrue(recorded.contains("\"supplements\":\""+SCT+"\""), recorded);
    Files.writeString(record, recorded.replace("\"supplements\":\""+SCT+"\"", "\"supplements\":{}"));
    // what the supplement added to is only in the record now
    ig.remove("CodeSystem-sct-supp.json");

    build(false);

    assertFalse(isCached(VS_A), "the record isn't as written, so it isn't read: every ValueSet and CodeSystem of the IG is scrubbed");
  }

  @Test
  void aCacheFolderSharedWithAnotherIgKeepsTheRecordOfEach() throws Exception {
    packageId = "example.a";
    firstPlainBuild();
    Map<String, List<String[]>> igA = new LinkedHashMap<>(ig);
    packageId = "@scope/example.b";
    ig.remove("ValueSet-vs-other.json");
    build(false);
    fillCache();
    packageId = "example.a";
    ig.clear();
    ig.putAll(igA);

    build(false);

    assertEquals(new TreeSet<>(ALL), cached(), "nothing changed in example.a since its last build");
    assertTrue(Files.exists(dir.resolve(".tx-cache-scrub-_scope_example.b.json")));
  }

  @Test
  void trimmingTheCacheKeepsTheRecords() throws Exception {
    Files.writeString(dir.resolve(".tx-cache-scrub-example.a.json"), "{}");
    Files.writeString(dir.resolve("cs-a.cache"), "");

    assertEquals(Set.of("snomed.cache", ".tx-cache-scrub-example.a.json"), Set.of(RapidoTracker.keepWithTxCacheRecords(dir.toString(), "snomed.cache")));
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
    return build(rapido, Set.of(), first -> {});
  }

  /**
   * a build, through the loader's loadConformance1, whose files in late are loaded after the first snapshot generation
   * (as the IG's resource list and test data factories are); beforeSnapshots runs where each snapshot generation would
   */
  private PublisherFields build(boolean rapido, Set<String> late, Snapshots beforeSnapshots) throws Exception {
    FetchedFile igf = file("ig.json", "{}", List.of());
    Publisher publisher = new Publisher();
    publisher.getSettings().setRapidoMode(rapido);
    PublisherFields pf = new PublisherFields();
    pf.logger = mock(ILoggingService.class);
    pf.rootDir = dir.toString();
    pf.vsCache = dir.toString();
    pf.tempDir = dir.resolve("temp").toString();
    pf.npmName = packageId;
    pf.igpkp = mock(IGKnowledgeProvider.class);
    when(pf.igpkp.getCanonical()).thenReturn(B);
    pf.template = mock(Template.class);
    pf.context = mock(SimpleWorkerContext.class);
    when(pf.context.getTxCache()).thenReturn(cache);
    pf.fileList.add(igf);
    Set<FetchedResource> loaded = Collections.newSetFromMap(new IdentityHashMap<>());
    List<FetchedFile> lateFiles = new ArrayList<>();
    for (Map.Entry<String, List<String[]>> e : ig.entrySet()) {
      String type = e.getKey().substring(0, e.getKey().indexOf('-'));
      String id = e.getKey().substring(e.getKey().indexOf('-')+1, e.getKey().length()-5);
      StringBuilder src = new StringBuilder();
      for (String[] c : e.getValue()) {
        src.append(c[0]).append('=').append(c[1]).append('\n');
      }
      FetchedResource r = noElement.contains(e.getKey()) ? resourceWithoutElement(type, id, e.getValue())
        : resource(type, id, e.getValue(), addedByLoad.getOrDefault(e.getKey(), List.of()), loaded);
      (late.contains(e.getKey()) ? lateFiles : pf.fileList).add(file(e.getKey(), src.toString(), List.of(r)));
    }
    publisher.setPf(pf);
    beforeBuild.accept(pf);
    Field field = Publisher.class.getDeclaredField("loader");
    field.setAccessible(true);
    PublisherIGLoader loader = spy((PublisherIGLoader) field.get(publisher));
    loader.setPf(pf);
    field.set(publisher, loader);
    doAnswer(inv -> {
      for (FetchedFile f : pf.fileList) {
        if (!f.isLoaded()) {
          loaded.addAll(f.getResources());
        }
      }
      return null;
    }).when(loader).load(anyString(), anyBoolean());
    boolean[] first = {true};
    doAnswer(inv -> {
      beforeSnapshots.run(first[0]);
      first[0] = false;
      return null;
    }).when(loader).generateSnapshots();

    loader.loadConformance1(true);
    pf.fileList.addAll(lateFiles);
    loader.loadConformance1(false);
    pf.changeList.addAll(pf.fileList);
    publisher.checkDependencies(igf, new IniFile(dir.resolve(".build-tracker.ini").toString()));
    return pf;
  }

  private static FetchedResource resourceWithoutElement(String type, String id, List<String[]> content) {
    FetchedResource resource = mock(FetchedResource.class);
    when(resource.fhirType()).thenReturn(type);
    when(resource.getId()).thenReturn(id);
    when(resource.getResource()).thenReturn(new ValueSet().setUrl(content.get(0)[1]));
    return resource;
  }

  private FetchedFile file(String name, String content, List<FetchedResource> resources) {
    FetchedFile f = new FetchedFile(name);
    f.setPath(dir.resolve("input").resolve(name).toString());
    f.setSource(content.getBytes(StandardCharsets.UTF_8));
    f.getResources().addAll(resources);
    return f;
  }

  /**
   * a resource whose element has content as read, and extra too once loaded; once loaded, it has a resource with the
   * url the loader gives it (canonical/Type/id if content has none)
   */
  private static FetchedResource resource(String type, String id, List<String[]> content, List<String[]> extra, Set<FetchedResource> loaded) {
    NamedItemList<Element> children = new NamedItemList<>();
    for (String[] c : content) {
      children.add(primitive(c[0], c[1]));
    }
    NamedItemList<Element> loadedChildren = new NamedItemList<>();
    loadedChildren.addAll(children);
    for (String[] c : extra) {
      loadedChildren.add(primitive(c[0], c[1]));
    }
    FetchedResource resource = mock(FetchedResource.class);
    Element element = mock(Element.class);
    when(element.getChildList()).thenAnswer(inv -> loaded.contains(resource) ? loadedChildren : children);
    when(element.hasChildren()).thenReturn(true);
    when(element.getChildrenByName(anyString())).thenAnswer(inv -> {
      List<Element> l = new ArrayList<>();
      for (Element c : element.getChildList()) {
        if (c.getName().equals(inv.getArgument(0))) {
          l.add(c);
        }
      }
      return l;
    });
    when(element.getName()).thenReturn(type);
    when(element.fhirType()).thenReturn(type);
    when(element.getComments()).thenReturn(new ArrayList<>());
    String url = content.stream().filter(c -> c[0].equals("url")).map(c -> c[1]).findFirst().orElse(B+"/"+type+"/"+id);
    when(resource.fhirType()).thenReturn(type);
    when(resource.getId()).thenReturn(id);
    when(resource.getElement()).thenReturn(element);
    when(resource.getResource()).thenAnswer(inv -> loaded.contains(resource) ? new ValueSet().setUrl(url) : null);
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
