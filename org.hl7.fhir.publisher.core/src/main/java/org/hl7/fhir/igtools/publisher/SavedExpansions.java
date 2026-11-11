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

  /**
   * The JSON of a value set without what only says when its expansion was made: the identifier and the timestamp
   * that the expansion gets each time it is generated. So an expansion that is generated again with the same content
   * doesn't change a key, and what was made from it stays, with the expansion as it was generated before. The rest
   * of the JSON is returned as it is (and all of it, if it can't be read)
   */
  public static String withoutRunDetails(String json) {
    StringBuilder out = null;
    int copied = 0;
    int depth = 0;
    // the name read last at the top of the value set is "expansion", and whether its object is the one being read
    boolean named = false;
    boolean inExpansion = false;
    int n = json.length();
    int i = 0;
    while (i < n) {
      char c = json.charAt(i);
      if (c == '"') {
        int end = endOfString(json, i);
        if (end < 0) {
          return json;
        }
        int next = skipSpace(json, end + 1);
        if (next < n && json.charAt(next) == ':') {
          if (depth == 1) {
            named = is(json, i, end, "expansion");
          } else if (depth == 2 && inExpansion && (is(json, i, end, "identifier") || is(json, i, end, "timestamp"))) {
            int value = skipSpace(json, next + 1);
            if (value < n && json.charAt(value) == '"') {
              int valueEnd = endOfString(json, value);
              if (valueEnd < 0) {
                return json;
              }
              if (out == null) {
                out = new StringBuilder(n);
              }
              out.append(json, copied, value + 1);
              copied = valueEnd;
              end = valueEnd;
            }
          }
        }
        i = end + 1;
        continue;
      }
      if (c == '{' || c == '[') {
        inExpansion = inExpansion || (depth == 1 && c == '{' && named);
        named = false;
        depth++;
      } else if (c == '}' || c == ']') {
        depth--;
        if (depth <= 1) {
          inExpansion = false;
        }
      }
      i++;
    }
    return out == null ? json : out.append(json, copied, n).toString();
  }

  /** where the string that starts at start (a quote) ends: its closing quote. -1 if it doesn't */
  private static int endOfString(String json, int start) {
    for (int i = start + 1; i < json.length(); i++) {
      char c = json.charAt(i);
      if (c == '\\') {
        i++;
      } else if (c == '"') {
        return i;
      }
    }
    return -1;
  }

  private static int skipSpace(String json, int i) {
    while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
      i++;
    }
    return i;
  }

  /** whether the string between the quotes at start and end is name */
  private static boolean is(String json, int start, int end, String name) {
    return end - start - 1 == name.length() && json.startsWith(name, start + 1);
  }

  /** the hash that a journal keeps of the expansions it holds (as withoutRunDetails() gives them) */
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
