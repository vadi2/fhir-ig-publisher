package org.hl7.fhir.igtools.publisher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * A set of strings that tells the input cache what a cached step asked of it and what it added.
 *
 * <p>It stands in for the warn-once and seen-it sets that the worker context and the validator keep across
 * resources, so a step's result can depend on them (a status warning given once, a code system known to be
 * unsupported) and a step can add to them. A membership test is a read the step's result depends on; an addition is
 * replayed when the step is a hit. Anything else (a removal, a look at the whole set) is recorded as a read of the
 * whole content, or makes the step uncacheable.
 */
public class InputCacheRecordingSet extends HashSet<String> {

  /** what the set reports to */
  public interface Listener {
    void setContains(String set, String value, boolean result);

    void setAdd(String set, String value, boolean added);

    /** something read the whole set (its size, or its members): the listener reads it with forEachQuietly if it needs to */
    void setWhole(InputCacheRecordingSet set);

    void setOther(String set, String operation);
  }

  private final String name;
  private final Listener listener;

  public InputCacheRecordingSet(String name, Collection<String> content, Listener listener) {
    super();
    this.name = name;
    this.listener = listener;
    for (String s : content) {
      super.add(s);
    }
  }

  public String getName() {
    return name;
  }

  /** the set's content, without telling the listener */
  public boolean containsQuietly(String value) {
    return super.contains(value);
  }

  /** adds without telling the listener (when a step's additions are replayed) */
  public boolean addQuietly(String value) {
    return super.add(value);
  }

  /** each member, without telling the listener (to check a recorded read) */
  public void forEachQuietly(Consumer<? super String> action) {
    Iterator<String> it = super.iterator();
    while (it.hasNext()) {
      action.accept(it.next());
    }
  }

  private void whole() {
    listener.setWhole(this);
  }

  private Collection<String> quietCopy() {
    HashSet<String> res = new HashSet<>();
    Iterator<String> it = super.iterator();
    while (it.hasNext()) {
      res.add(it.next());
    }
    return res;
  }

  @Override
  public boolean contains(Object o) {
    boolean res = super.contains(o);
    if (o instanceof String) {
      listener.setContains(name, (String) o, res);
    } else {
      listener.setOther(name, "contains(" + (o == null ? "null" : o.getClass().getName()) + ")");
    }
    return res;
  }

  @Override
  public boolean add(String e) {
    boolean res = super.add(e);
    listener.setAdd(name, e, res);
    return res;
  }

  @Override
  public boolean addAll(Collection<? extends String> c) {
    boolean res = false;
    for (String s : c) {
      res = add(s) || res;
    }
    return res;
  }

  @Override
  public boolean remove(Object o) {
    listener.setOther(name, "remove");
    return super.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    listener.setOther(name, "removeAll");
    return super.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    listener.setOther(name, "retainAll");
    return super.retainAll(c);
  }

  @Override
  public boolean removeIf(Predicate<? super String> filter) {
    listener.setOther(name, "removeIf");
    return super.removeIf(filter);
  }

  @Override
  public void clear() {
    listener.setOther(name, "clear");
    super.clear();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    whole();
    return super.containsAll(c);
  }

  @Override
  public int size() {
    whole();
    return super.size();
  }

  @Override
  public boolean isEmpty() {
    whole();
    return super.isEmpty();
  }

  @Override
  public Iterator<String> iterator() {
    whole();
    // a removal through the iterator goes through remove(), so it is reported too
    Iterator<String> it = quietCopy().iterator();
    return new Iterator<String>() {
      private String last;

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public String next() {
        last = it.next();
        return last;
      }

      @Override
      public void remove() {
        InputCacheRecordingSet.this.remove(last);
      }
    };
  }

  @Override
  public Spliterator<String> spliterator() {
    whole();
    return quietCopy().spliterator();
  }

  @Override
  public void forEach(Consumer<? super String> action) {
    whole();
    quietCopy().forEach(action);
  }

  @Override
  public Object[] toArray() {
    whole();
    return quietCopy().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    whole();
    return quietCopy().toArray(a);
  }

  @Override
  public <T> T[] toArray(IntFunction<T[]> generator) {
    whole();
    return quietCopy().toArray(generator);
  }

  @Override
  public boolean equals(Object o) {
    whole();
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    whole();
    return super.hashCode();
  }

  @Override
  public String toString() {
    whole();
    return quietCopy().toString();
  }

  @Override
  public Object clone() {
    whole();
    return new HashSet<>(quietCopy());
  }
}
