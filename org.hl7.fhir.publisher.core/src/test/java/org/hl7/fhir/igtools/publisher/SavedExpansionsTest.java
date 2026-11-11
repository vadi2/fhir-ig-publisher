package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  private static ValueSet made(ValueSet vs, String identifier, String timestamp) {
    vs.getExpansion().setIdentifier(identifier);
    vs.getExpansion().setTimestampElement(new org.hl7.fhir.model.core.DateTimeType(timestamp));
    return vs;
  }

  private static String stable(ValueSet vs) throws Exception {
    return SavedExpansions.withoutRunDetails(RapidoTracker.saveExpansion(parser(), vs));
  }

  @Test
  void whenAnExpansionWasMadeIsNotInItsHash() throws Exception {
    ValueSet one = made(expansion("a", "1", "2"), "urn:uuid:9f0c2a55-0000-4000-8000-000000000001", "2026-03-04T10:00:00Z");
    ValueSet again = made(expansion("a", "1", "2"), "urn:uuid:9f0c2a55-0000-4000-8000-000000000002", "2026-03-04T11:30:00Z");
    assertNotEquals(RapidoTracker.saveExpansion(parser(), one), RapidoTracker.saveExpansion(parser(), again));
    assertEquals(stable(one), stable(again));
    assertFalse(stable(one).contains("9f0c2a55"));
    assertFalse(stable(one).contains("2026-03-04T10:00:00Z"));
    // what is left is the JSON it was, without the two values
    assertEquals(RapidoTracker.saveExpansion(parser(), one).replace("urn:uuid:9f0c2a55-0000-4000-8000-000000000001", "").replace("2026-03-04T10:00:00Z", ""), stable(one));

    // anything else that differs still shows
    assertNotEquals(stable(one), stable(made(expansion("a", "1", "2", "3"), "urn:uuid:9f0c2a55-0000-4000-8000-000000000001", "2026-03-04T10:00:00Z")));
    // an expansion that starts with something else (one that isn't complete has extensions)
    ValueSet unclosed = made(expansion("a", "1", "2"), "urn:uuid:9f0c2a55-0000-4000-8000-000000000001", "2026-03-04T10:00:00Z");
    unclosed.getExpansion().addExtension("http://hl7.org/fhir/StructureDefinition/valueset-unclosed", new org.hl7.fhir.model.core.BooleanType(true));
    ValueSet unclosedAgain = made(expansion("a", "1", "2"), "urn:uuid:9f0c2a55-0000-4000-8000-000000000002", "2026-03-04T11:30:00Z");
    unclosedAgain.getExpansion().addExtension("http://hl7.org/fhir/StructureDefinition/valueset-unclosed", new org.hl7.fhir.model.core.BooleanType(true));
    assertTrue(RapidoTracker.saveExpansion(parser(), unclosed).contains("\"expansion\":{\"extension\""));
    assertEquals(stable(unclosed), stable(unclosedAgain));
    assertNotEquals(stable(one), stable(unclosed));
  }

  @Test
  void onlyTheExpansionsOwnIdentifierAndTimestampAreLeftOut() throws Exception {
    // the same values, and the same names, anywhere else stay
    ValueSet vs = made(expansion("a", "1"), "urn:uuid:1", "2026-03-04T10:00:00Z");
    vs.setName("2026-03-04T10:00:00Z");
    vs.addIdentifier().setValue("urn:uuid:1");
    vs.getExpansion().addParameter().setName("identifier").setValue(new org.hl7.fhir.model.core.StringType("urn:uuid:1"));
    vs.getExpansion().getContainsFirstRep().setDisplay("a \"timestamp\": \"x\" in text, and a \\ too");
    String json = RapidoTracker.saveExpansion(parser(), vs);
    String stable = SavedExpansions.withoutRunDetails(json);
    assertEquals(json.replace("{\"identifier\":\"urn:uuid:1\",\"timestamp\":\"2026-03-04T10:00:00Z\"", "{\"identifier\":\"\",\"timestamp\":\"\""), stable);
    assertTrue(stable.contains("\"name\":\"2026-03-04T10:00:00Z\""));
    assertTrue(stable.contains("\"valueString\":\"urn:uuid:1\""));
    assertTrue(stable.contains("\"identifier\":[{\"value\":\"urn:uuid:1\"}]"));
  }

  @Test
  void whenAnExpansionWasMadeIsTakenOutHoweverTheJsonIsLaidOut() {
    String pretty = "{\n  \"resourceType\" : \"ValueSet\",\n  \"expansion\" : {\n    \"identifier\" : \"urn:uuid:1\",\n    \"timestamp\" : \"2026-03-04T10:00:00Z\",\n    \"total\" : 2\n  }\n}";
    assertEquals(pretty.replace("urn:uuid:1", "").replace("2026-03-04T10:00:00Z", ""), SavedExpansions.withoutRunDetails(pretty));
    // not a value set's expansion, or not JSON that can be read: as it is
    String elsewhere = "{\"identifier\":\"urn:uuid:1\",\"timestamp\":\"2026-03-04T10:00:00Z\",\"contained\":[{\"expansion\":{\"identifier\":\"urn:uuid:1\"}}]}";
    assertEquals(elsewhere, SavedExpansions.withoutRunDetails(elsewhere));
    assertEquals("{}", SavedExpansions.withoutRunDetails("{}"));
    assertEquals("{\"expansion\":{\"identifier\":\"never ends", SavedExpansions.withoutRunDetails("{\"expansion\":{\"identifier\":\"never ends"));
    assertEquals("this is not a ValueSet", SavedExpansions.withoutRunDetails("this is not a ValueSet"));
  }
}
