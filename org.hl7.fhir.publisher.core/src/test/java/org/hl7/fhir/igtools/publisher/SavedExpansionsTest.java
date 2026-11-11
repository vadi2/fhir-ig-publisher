package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.junit.jupiter.api.Test;

/**
 * The expansions that come from the journals of the last build stand in the key of the expansion files by the
 * hashes the journals have of them, and are only parsed when the files are written.
 */
class SavedExpansionsTest {

  private static JsonParser parser() {
    return new JsonParser(ModelContext.fullCoreContext());
  }

  private static ValueSet expansion(String id, String... codes) {
    ValueSet vs = new ValueSet();
    vs.setId(id);
    vs.setUrl("http://example.org/ValueSet/" + id);
    for (String c : codes) {
      vs.getExpansion().addContains().setSystem("http://example.org/cs").setCode(c);
    }
    return vs;
  }

  private static List<String> json(ValueSet... list) throws Exception {
    List<String> res = new ArrayList<>();
    for (ValueSet vs : list) {
      res.add(RapidoTracker.saveExpansion(parser(), vs));
    }
    return res;
  }

  /** a build that takes the expansions of each file from its journal */
  private static SavedExpansions replayed(List<List<String>> journals) {
    SavedExpansions e = new SavedExpansions();
    for (List<String> j : journals) {
      e.addJournaled(j, SavedExpansions.hash(j));
    }
    return e;
  }

  @Test
  void sameExpansionsSameKey() throws Exception {
    List<List<String>> journals = Arrays.asList(json(expansion("a", "1", "2")), json(), json(expansion("b", "3"), expansion("c")));
    assertNotNull(replayed(journals).key());
    assertEquals(replayed(journals).key(), replayed(journals).key());
  }

  @Test
  void aChangedExpansionChangesTheKey() throws Exception {
    String before = replayed(Arrays.asList(json(expansion("a", "1", "2")), json(expansion("b", "3")))).key();
    // the ValueSet is the same, but its code system has another code
    String after = replayed(Arrays.asList(json(expansion("a", "1", "2", "2a")), json(expansion("b", "3")))).key();
    assertNotEquals(before, after);
  }

  @Test
  void anAddedOrRemovedExpansionChangesTheKey() throws Exception {
    String two = replayed(Arrays.asList(json(expansion("a", "1")), json(expansion("b", "3")))).key();
    String three = replayed(Arrays.asList(json(expansion("a", "1")), json(expansion("b", "3"), expansion("b", "3")))).key();
    String one = replayed(Arrays.asList(json(expansion("a", "1")), json())).key();
    assertNotEquals(two, three);
    assertNotEquals(two, one);
    assertNotEquals(one, replayed(Arrays.asList(json(), json(expansion("a", "1")), json())).key(), "another file made it");
  }

  @Test
  void theOrderChangesTheKey() throws Exception {
    String ab = replayed(Arrays.asList(json(expansion("a", "1")), json(expansion("b", "3")))).key();
    String ba = replayed(Arrays.asList(json(expansion("b", "3")), json(expansion("a", "1")))).key();
    assertNotEquals(ab, ba);
  }

  @Test
  void aGeneratedFileHasTheKeyOfItsJournal() throws Exception {
    List<String> a = json(expansion("a", "1"));
    List<String> b = json(expansion("b", "3"));
    String replayed = replayed(Arrays.asList(a, b)).key();

    // file b is generated again, and journaled when it's done
    SavedExpansions e = new SavedExpansions();
    e.addJournaled(a, SavedExpansions.hash(a));
    int start = e.size();
    e.add(expansion("b", "3"));
    assertNull(e.key(), "not in a journal yet");
    e.journaled(start, SavedExpansions.hash(b));
    assertEquals(replayed, e.key());
    assertEquals(1, e.since(start).size());
  }

  @Test
  void expansionsThatAreNotInAJournalHaveNoKey() throws Exception {
    List<String> a = json(expansion("a", "1"));
    SavedExpansions e = new SavedExpansions();
    e.add(expansion("x", "9")); // its file isn't journaled (generating it failed, say)
    e.addJournaled(a, SavedExpansions.hash(a));
    assertNull(e.key());

    SavedExpansions old = new SavedExpansions();
    old.addJournaled(a, null); // a journal from before the hash was kept
    assertNull(old.key());
  }

  @Test
  void theKeyDoesNotParseTheJournals() {
    List<String> notJson = Arrays.asList("this is not a ValueSet");
    SavedExpansions e = new SavedExpansions();
    e.addJournaled(notJson, SavedExpansions.hash(notJson));
    assertNotNull(e.key());
    assertThrows(Exception.class, () -> e.resolve(parser()));
  }

  @Test
  void resolveGivesEveryExpansionInOrder() throws Exception {
    SavedExpansions e = new SavedExpansions();
    List<String> a = json(expansion("a", "1"), expansion("b", "2"));
    e.addJournaled(a, SavedExpansions.hash(a));
    ValueSet c = expansion("c", "3");
    e.add(c);
    List<ValueSet> all = e.resolve(parser());
    assertEquals(3, all.size());
    assertEquals("a", all.get(0).getId());
    assertEquals("2", all.get(1).getExpansion().getContainsFirstRep().getCode());
    assertSame(c, all.get(2));
  }

  @Test
  void whenAnExpansionWasMadeIsNotInItsHash() throws Exception {
    ValueSet one = expansion("a", "1", "2");
    one.getExpansion().setIdentifier("urn:uuid:9f0c2a55-0000-4000-8000-000000000001");
    one.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType("2026-03-04T10:00:00Z"));
    ValueSet again = expansion("a", "1", "2");
    again.getExpansion().setIdentifier("urn:uuid:9f0c2a55-0000-4000-8000-000000000002");
    again.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType("2026-03-04T11:30:00Z"));
    String a = RapidoTracker.saveExpansion(parser(), one);
    String b = RapidoTracker.saveExpansion(parser(), again);
    assertNotEquals(a, b);
    assertEquals(SavedExpansions.withoutRunDetails(one, a), SavedExpansions.withoutRunDetails(again, b));
    assertNotEquals(a, SavedExpansions.withoutRunDetails(one, a));

    // anything else that differs still shows
    ValueSet other = expansion("a", "1", "2", "3");
    other.getExpansion().setIdentifier("urn:uuid:9f0c2a55-0000-4000-8000-000000000001");
    other.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType("2026-03-04T10:00:00Z"));
    assertNotEquals(SavedExpansions.withoutRunDetails(one, a), SavedExpansions.withoutRunDetails(other, RapidoTracker.saveExpansion(parser(), other)));
    // a value that only looks like the timestamp, somewhere else, stays
    ValueSet named = expansion("a", "1", "2");
    named.setName("2026-03-04T10:00:00Z");
    named.getExpansion().setIdentifier("urn:uuid:9f0c2a55-0000-4000-8000-000000000001");
    named.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType("2026-03-04T10:00:00Z"));
    assertTrue(SavedExpansions.withoutRunDetails(named, RapidoTracker.saveExpansion(parser(), named)).contains("\"name\":\"2026-03-04T10:00:00Z\""));
    // no expansion, or JSON that isn't the expansion's: nothing is taken out
    assertEquals("{}", SavedExpansions.withoutRunDetails(new ValueSet(), "{}"));
    assertEquals("{}", SavedExpansions.withoutRunDetails(one, "{}"));
  }

  @Test
  void whenAnExpansionWasMadeIsTakenOutOfAnyJson() {
    String compact = "{\"resourceType\":\"ValueSet\",\"expansion\":{\"identifier\":\"urn:uuid:1\",\"timestamp\":\"2026-03-04T10:00:00Z\",\"total\":2}}";
    String later = "{\"resourceType\":\"ValueSet\",\"expansion\":{\"identifier\":\"urn:uuid:2\",\"timestamp\":\"2026-03-04T11:00:00Z\",\"total\":2}}";
    String other = "{\"resourceType\":\"ValueSet\",\"expansion\":{\"identifier\":\"urn:uuid:1\",\"timestamp\":\"2026-03-04T10:00:00Z\",\"total\":3}}";
    assertEquals(SavedExpansions.withoutRunDetails(compact), SavedExpansions.withoutRunDetails(later));
    assertNotEquals(SavedExpansions.withoutRunDetails(compact), SavedExpansions.withoutRunDetails(other));
    assertTrue(SavedExpansions.withoutRunDetails(compact).contains("\"total\":2"));
    String pretty = "{\n  \"expansion\" : {\n    \"identifier\" : \"urn:uuid:1\",\n    \"timestamp\" : \"2026-03-04T10:00:00Z\",\n    \"total\" : 2\n  }\n}";
    assertEquals(pretty.replace("urn:uuid:1", "").replace("2026-03-04T10:00:00Z", ""), SavedExpansions.withoutRunDetails(pretty));
    // only the expansion's own: an identifier anywhere else stays
    String elsewhere = "{\"identifier\":\"urn:uuid:1\",\"timestamp\":\"2026-03-04T10:00:00Z\",\"expansion\":{\"total\":2}}";
    assertEquals(elsewhere, SavedExpansions.withoutRunDetails(elsewhere));
  }
}
