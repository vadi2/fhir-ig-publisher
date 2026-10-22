package org.hl7.fhir.igtools.publisher;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A memo (a map of answers worked out before) that tells the input cache when an answer is used again: the step that
 * uses it depends on what the answer was worked out from, which the step that worked it out read and this one doesn't.
 * The sources function names those: resources, or the canonical URLs of resources. Using the memo in any other way
 * than looking up one key (walking it, asking its size) is reported too.
 *
 * <p>Every call goes to the map it stands in for. It is not synchronized, as the maps it stands in for aren't.
 */
public class InputCacheMemoMap<K, V> implements Map<K, V> {

  /** what the memo reports to */
  public interface Listener {
    /** an answer worked out before was used again: what it was worked out from (resources, and canonical URLs) */
    void memoHit(String memo, Collection<?> sources);

    /** the memo was used in some other way than looking up a key */
    void memoOther(String memo, String operation);
  }

  /** what an answer was worked out from */
  public interface Sources<K, V> {
    Collection<?> of(K key, V value);
  }

  private final String name;
  private final Map<K, V> map;
  private final Sources<K, V> sources;
  private final Listener listener;

  public InputCacheMemoMap(String name, Map<K, V> map, Sources<K, V> sources, Listener listener) {
    this.name = name;
    this.map = map;
    this.sources = sources;
    this.listener = listener;
  }

  public String getName() {
    return name;
  }

  /** the map this stands in for */
  public Map<K, V> getMap() {
    return map;
  }

  @SuppressWarnings("unchecked")
  private void hit(Object key, V value) {
    listener.memoHit(name, sources.of((K) key, value));
  }

  @Override
  public V get(Object key) {
    V v = map.get(key);
    if (v != null || map.containsKey(key)) {
      hit(key, v);
    }
    return v;
  }

  @Override
  public boolean containsKey(Object key) {
    boolean res = map.containsKey(key);
    if (res) {
      hit(key, map.get(key));
    }
    return res;
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public int size() {
    listener.memoOther(name, "size");
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    listener.memoOther(name, "isEmpty");
    return map.isEmpty();
  }

  @Override
  public boolean containsValue(Object value) {
    listener.memoOther(name, "containsValue");
    return map.containsValue(value);
  }

  @Override
  public Set<K> keySet() {
    listener.memoOther(name, "keySet");
    return map.keySet();
  }

  @Override
  public Collection<V> values() {
    listener.memoOther(name, "values");
    return map.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    listener.memoOther(name, "entrySet");
    return map.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    return o == this || map.equals(o);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
