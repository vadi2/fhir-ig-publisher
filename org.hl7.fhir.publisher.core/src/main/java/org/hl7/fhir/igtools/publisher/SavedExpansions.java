package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.igtools.publisher.RapidoDependencyAnalyser.Hasher;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.hl7.fhir.model.core.ValueSet;

/**
 * The expansions that the build saves in expansions.json and expansions.xml, in the order that the files they come
 * from were generated.
 *
 * <p>In a -rapido build, the expansions of a file that isn't generated again come from the journal of the last build
 * (see {@link RapidoTracker}), as JSON. They are only parsed if the expansion files are written
 * ({@link #resolve}): whether they have to be is known from {@link #key}, which is made from the hashes that the
 * journals hold of their expansions, not from the expansions.
 */
public class SavedExpansions {

  /** a ValueSet, or the JSON of one from a journal (see RapidoTracker.saveExpansion()) */
  private final List<Object> items = new ArrayList<>();
  /** the hashes that the journals have of their expansions, in order */
  private final List<String> hashes = new ArrayList<>();
  /** how many of the items the hashes cover */
  private int hashed;
  private boolean gap;

  public void add(ValueSet vs) {
    items.add(vs);
  }

  public int size() {
    return items.size();
  }

  /** the expansions added since there were start of them: none of them from a journal */
  public List<ValueSet> since(int start) {
    List<ValueSet> res = new ArrayList<>();
    for (Object o : items.subList(start, items.size())) {
      res.add((ValueSet) o);
    }
    return res;
  }

  /** the expansions since there were start of them are in a journal now, where their hash is this */
  public void journaled(int start, String hash) {
    if (start != hashed) {
      gap = true;
    }
    hashes.add(hash);
    hashed = items.size();
  }

  /** adds the expansions of a journal, which has this hash for them */
  public void addJournaled(List<String> json, String hash) {
    if (hash == null) {
      gap = true;
    }
    int start = items.size();
    items.addAll(json);
    journaled(start, hash);
  }

  /**
   * Stands for the content of every expansion, in order: the same in two builds only if the expansions are. null
   * if some of the expansions aren't in a journal, so there is no hash of them
   */
  public String key() {
    if (gap || hashed != items.size()) {
      return null;
    }
    Hasher h = new Hasher().add(Integer.toString(items.size()));
    for (String s : hashes) {
      h.add(s);
    }
    return h.finish();
  }

  /** the hash that a journal keeps of the expansions it holds */
  public static String hash(List<String> json) {
    Hasher h = new Hasher();
    for (String s : json) {
      h.add(s);
    }
    return h.finish();
  }

  /** every expansion, parsing the ones that came from a journal */
  public List<ValueSet> resolve(JsonParser parser) throws IOException {
    List<ValueSet> res = new ArrayList<>(items.size());
    for (int i = 0; i < items.size(); i++) {
      Object o = items.get(i);
      if (o instanceof String) {
        o = RapidoTracker.readExpansion(parser, (String) o);
        items.set(i, o);
      }
      res.add((ValueSet) o);
    }
    return res;
  }
}
