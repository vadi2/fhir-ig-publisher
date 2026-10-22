package org.hl7.fhir.igtools.publisher;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.utilities.xhtml.XhtmlNodeList;

/**
 * SHA-256 digests for the input cache: an incremental one over strings and bytes, and a deep one over an object graph.
 */
public class InputCacheDigest {

  private final MessageDigest md;

  public InputCacheDigest() {
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** each value is length-prefixed, so ("ab", "c") and ("a", "bc") digest differently; null differs from "" */

  public InputCacheDigest add(String s) {
    if (s == null) {
      intValue(-1);
    } else {
      bytes(s.getBytes(StandardCharsets.UTF_8));
    }
    return this;
  }

  public InputCacheDigest add(long l) {
    for (int i = 7; i >= 0; i--) {
      md.update((byte) (l >>> (i * 8)));
    }
    return this;
  }

  public InputCacheDigest add(boolean b) {
    md.update((byte) (b ? 1 : 0));
    return this;
  }

  public InputCacheDigest bytes(byte[] b) {
    if (b == null) {
      intValue(-1);
    } else {
      intValue(b.length);
      md.update(b);
    }
    return this;
  }

  private void intValue(int i) {
    md.update((byte) (i >>> 24));
    md.update((byte) (i >>> 16));
    md.update((byte) (i >>> 8));
    md.update((byte) i);
  }

  public String finish() {
    return hex(md.digest());
  }

  public static String hex(byte[] b) {
    StringBuilder s = new StringBuilder(b.length * 2);
    for (byte x : b) {
      s.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
    }
    return s.toString();
  }

  public static String of(String... values) {
    InputCacheDigest d = new InputCacheDigest();
    for (String s : values) {
      d.add(s);
    }
    return d.finish();
  }

  public static String of(byte[] b) {
    return new InputCacheDigest().bytes(b).finish();
  }

  // ---- deep digest ---------------------------------------------------------------------------------------------------

  private static final Map<Class<?>, List<Field>> FIELDS = new ConcurrentHashMap<>();

  /**
   * A digest of everything reachable from the object: values of the simple types, and the fields of objects, field by
   * field, recursively. Lists keep their order; sets and maps are digested as the sorted digests of their members,
   * since their iteration order can depend on identity hash codes. Shared and cyclic references are digested by their
   * position in the walk, so the same graph always gives the same digest. A field holding an empty collection or map
   * digests as one holding null, and so does one holding an empty element of the model. {@code userData} fields
   * (the model's annotations) and model contexts are left out. Objects of other
   * library classes (anything outside java.lang/java.util and org.hl7.fhir) are digested by class name only.
   */
  public static String deep(Object o) {
    return deep(o, null);
  }

  /**
   * The same, but the substitute function is asked first about each object of a org.hl7.fhir class: when it returns a
   * string, that is digested instead of the object (null: the object is walked as usual)
   */
  public static String deep(Object o, Function<Object, String> substitute) {
    return deep(o, substitute, MAX_NODES);
  }

  /** as {@link #deep(Object, Function)}, giving up (with {@link TooLarge}) after maxNodes objects */
  public static String deep(Object o, Function<Object, String> substitute, int maxNodes) {
    InputCacheDigest d = new InputCacheDigest();
    new Walker(d, substitute, maxNodes).walk(o);
    return d.finish();
  }

  /** thrown when an object graph is too large to digest (it may reach a whole worker context) */
  public static class TooLarge extends RuntimeException {
    public TooLarge(String message) {
      super(message);
    }
  }

  /** the limit for state that may reach far more than it means to */
  static final int MAX_NODES = 500000;

  private static class Walker {
    private final InputCacheDigest d;
    private final Function<Object, String> substitute;
    private final Map<Object, Integer> seen = new IdentityHashMap<>();
    private final int maxNodes;
    private int nodes;
    /** the fields the walk is in, so that a graph too large to digest says where it went */
    private ArrayDeque<String> path = new ArrayDeque<>();

    Walker(InputCacheDigest d, Function<Object, String> substitute, int maxNodes) {
      this.d = d;
      this.substitute = substitute;
      this.maxNodes = maxNodes;
    }

    void walk(Object o) {
      if (++nodes > maxNodes) {
        List<String> at = new ArrayList<>(path);
        Collections.reverse(at);
        throw new TooLarge("more than " + maxNodes + " objects, at " + String.join(" > ", at.subList(0, Math.min(at.size(), 40))));
      }
      if (o == null) {
        d.add("N");
        return;
      }
      Class<?> c = o.getClass();
      String instead = substitute != null && c.getName().startsWith("org.hl7.fhir.") ? substitute.apply(o) : null;
      if (instead != null) {
        d.add("U").add(instead);
      } else if (o instanceof String) {
        d.add("S").add((String) o);
      } else if (o instanceof Number || o instanceof Boolean || o instanceof Character) {
        d.add(c.getName()).add(o.toString());
      } else if (o instanceof Enum) {
        d.add(c.getName()).add(((Enum<?>) o).name());
      } else if (o instanceof Date) {
        d.add("D").add(((Date) o).getTime());
      } else if (o instanceof byte[]) {
        d.add("B").bytes((byte[]) o);
      } else if (c.isArray()) {
        if (ref(o)) {
          return;
        }
        int n = Array.getLength(o);
        d.add("A").add(n);
        for (int i = 0; i < n; i++) {
          walk(Array.get(o, i));
        }
      } else if (o instanceof List) {
        if (ref(o)) {
          return;
        }
        List<?> l = (List<?>) o;
        d.add("L").add(l.size());
        for (Object x : l) {
          walk(x);
        }
      } else if (o instanceof Set || o instanceof Collection) {
        if (ref(o)) {
          return;
        }
        List<String> parts = new ArrayList<>();
        for (Object x : (Collection<?>) o) {
          parts.add(sub(x));
        }
        Collections.sort(parts);
        d.add("C").add(parts.size());
        for (String s : parts) {
          d.add(s);
        }
      } else if (o instanceof Map) {
        if (ref(o)) {
          return;
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
          parts.add(sub(e.getKey()) + sub(e.getValue()));
        }
        Collections.sort(parts);
        d.add("M").add(parts.size());
        for (String s : parts) {
          d.add(s);
        }
      } else if (o instanceof XhtmlNode) {
        if (ref(o)) {
          return;
        }
        xhtml((XhtmlNode) o);
      } else if (o instanceof org.hl7.fhir.services.context.IWorkerContext) {
        throw new TooLarge("reaches a worker context");
      } else if (c.getName().startsWith("org.hl7.fhir.")) {
        if (ref(o)) {
          return;
        }
        d.add("O").add(c.getName());
        for (Field f : fields(c)) {
          try {
            Object v = f.get(o);
            if ((v instanceof Collection && ((Collection<?>) v).isEmpty()) || (v instanceof Map && ((Map<?, ?>) v).isEmpty())
              || (v instanceof org.hl7.fhir.model.core.Element && ((org.hl7.fhir.model.core.Element) v).isEmpty())) {
              // the model creates its lists and elements when they're first asked for: an empty one is the same as none
              v = null;
            }
            path.push(c.getSimpleName() + "." + f.getName());
            try {
              walk(v);
            } finally {
              path.pop();
            }
          } catch (IllegalAccessException e) {
            d.add("?");
          }
        }
      } else {
        d.add("X").add(c.getName());
      }
    }

    /**
     * a narrative, which can be tens of thousands of nodes: each node's fields are digested here, the attributes in
     * the order they're written out in, so a node counts once rather than once a field
     */
    private void xhtml(XhtmlNode x) {
      d.add("H");
      for (Field f : fields(XhtmlNode.class)) {
        Object v;
        try {
          v = f.get(x);
        } catch (IllegalAccessException e) {
          d.add("?");
          continue;
        }
        if (v instanceof XhtmlNodeList) {
          d.add("L");
          for (Field lf : fields(XhtmlNodeList.class)) {
            try {
              Object lv = lf.get(v);
              if (lv instanceof List) {
                d.add(((List<?>) lv).size());
                for (Object child : (List<?>) lv) {
                  path.push("XhtmlNode." + f.getName());
                  try {
                    walk(child);
                  } finally {
                    path.pop();
                  }
                }
              } else {
                primitive(lv);
              }
            } catch (IllegalAccessException e) {
              d.add("?");
            }
          }
        } else if (v instanceof Map && f.getName().equals("attributes")) {
          d.add("a").add(((Map<?, ?>) v).size());
          for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
            d.add(String.valueOf(e.getKey())).add(e.getValue() == null ? null : String.valueOf(e.getValue()));
          }
        } else if (!primitive(v)) {
          path.push("XhtmlNode." + f.getName());
          try {
            walk(v instanceof Map && ((Map<?, ?>) v).isEmpty() ? null : v);
          } finally {
            path.pop();
          }
        }
      }
    }

    /** digests a null, string, boolean, number or enum as walk would, without counting it; false if it's none of those */
    private boolean primitive(Object v) {
      if (v == null) {
        d.add("N");
      } else if (v instanceof String) {
        d.add("S").add((String) v);
      } else if (v instanceof Number || v instanceof Boolean || v instanceof Character) {
        d.add(v.getClass().getName()).add(v.toString());
      } else if (v instanceof Enum) {
        d.add(v.getClass().getName()).add(((Enum<?>) v).name());
      } else {
        return false;
      }
      return true;
    }

    /** a member of a set or map, digested on its own so the order of the members doesn't matter */
    private String sub(Object o) {
      InputCacheDigest sd = new InputCacheDigest();
      Walker w = new Walker(sd, substitute, maxNodes);
      w.nodes = nodes;
      w.path = path;
      w.walk(o);
      nodes = w.nodes;
      return sd.finish();
    }

    /** true if the object was digested already (and writes its position instead) */
    private boolean ref(Object o) {
      Integer i = seen.get(o);
      if (i != null) {
        d.add("R").add(i.longValue());
        return true;
      }
      seen.put(o, seen.size());
      return false;
    }
  }

  private static List<Field> fields(Class<?> c) {
    return FIELDS.computeIfAbsent(c, k -> {
      List<Field> res = new ArrayList<>();
      for (Class<?> t = k; t != null && t != Object.class; t = t.getSuperclass()) {
        for (Field f : t.getDeclaredFields()) {
          // an element's model context is set when the element is first used, and isn't its content
          if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()) || f.getName().equals("userData") || f.isSynthetic()
            || org.hl7.fhir.model.IModelContext.class.isAssignableFrom(f.getType())) {
            continue;
          }
          try {
            f.setAccessible(true);
            res.add(f);
          } catch (RuntimeException e) {
            // not readable: left out
          }
        }
      }
      res.sort((a, b) -> (a.getDeclaringClass().getName() + "." + a.getName()).compareTo(b.getDeclaringClass().getName() + "." + b.getName()));
      return res;
    });
  }
}
