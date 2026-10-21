package org.hl7.fhir.igtools.publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A map that tells the input cache what a cached step looked up in it, and when a step changes it.
 *
 * <p>It stands in for a memo the worker context keeps across resources (which code systems the terminology server
 * supports). A lookup is a read the step's result depends on. A step that changes the map is not cached: the change
 * comes from asking the server, so repeating the step's result without the change would leave the map, and the
 * server's record of what it was asked, different from a build that ran the step.
 */
public class InputCacheRecordingMap<V> extends HashMap<String, V> {

  /** what the map reports to */
  public interface Listener {
    void mapRead(String map, String key, boolean present, Object value);

    void mapOther(String map, String operation);
  }

  private final String name;
  private final Listener listener;

  public InputCacheRecordingMap(String name, Map<String, V> content, Listener listener) {
    super();
    this.name = name;
    this.listener = listener;
    for (Map.Entry<String, V> e : content.entrySet()) {
      super.put(e.getKey(), e.getValue());
    }
  }

  /** the value, without telling the listener (to check a recorded read) */
  public V getQuietly(String key) {
    return super.get(key);
  }

  public boolean containsKeyQuietly(String key) {
    return super.containsKey(key);
  }

  private void read(Object key) {
    if (key instanceof String) {
      listener.mapRead(name, (String) key, super.containsKey(key), super.get(key));
    } else {
      listener.mapOther(name, "lookup(" + (key == null ? "null" : key.getClass().getName()) + ")");
    }
  }

  @Override
  public boolean containsKey(Object key) {
    read(key);
    return super.containsKey(key);
  }

  @Override
  public V get(Object key) {
    read(key);
    return super.get(key);
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    read(key);
    return super.getOrDefault(key, defaultValue);
  }

  @Override
  public V put(String key, V value) {
    listener.mapOther(name, "put");
    return super.put(key, value);
  }

  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    listener.mapOther(name, "putAll");
    super.putAll(m);
  }

  @Override
  public V putIfAbsent(String key, V value) {
    listener.mapOther(name, "putIfAbsent");
    return super.putIfAbsent(key, value);
  }

  @Override
  public V remove(Object key) {
    listener.mapOther(name, "remove");
    return super.remove(key);
  }

  @Override
  public boolean remove(Object key, Object value) {
    listener.mapOther(name, "remove");
    return super.remove(key, value);
  }

  @Override
  public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
    listener.mapOther(name, "computeIfAbsent");
    return super.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
    listener.mapOther(name, "computeIfPresent");
    return super.computeIfPresent(key, remappingFunction);
  }

  @Override
  public V compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
    listener.mapOther(name, "compute");
    return super.compute(key, remappingFunction);
  }

  @Override
  public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    listener.mapOther(name, "merge");
    return super.merge(key, value, remappingFunction);
  }

  @Override
  public boolean replace(String key, V oldValue, V newValue) {
    listener.mapOther(name, "replace");
    return super.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(String key, V value) {
    listener.mapOther(name, "replace");
    return super.replace(key, value);
  }

  @Override
  public void replaceAll(BiFunction<? super String, ? super V, ? extends V> function) {
    listener.mapOther(name, "replaceAll");
    super.replaceAll(function);
  }

  @Override
  public void clear() {
    listener.mapOther(name, "clear");
    super.clear();
  }

  @Override
  public int size() {
    listener.mapOther(name, "size");
    return super.size();
  }

  @Override
  public boolean isEmpty() {
    listener.mapOther(name, "isEmpty");
    return super.isEmpty();
  }

  @Override
  public boolean containsValue(Object value) {
    listener.mapOther(name, "containsValue");
    return super.containsValue(value);
  }

  @Override
  public Set<String> keySet() {
    listener.mapOther(name, "keySet");
    return super.keySet();
  }

  @Override
  public Collection<V> values() {
    listener.mapOther(name, "values");
    return super.values();
  }

  @Override
  public Set<Map.Entry<String, V>> entrySet() {
    listener.mapOther(name, "entrySet");
    return super.entrySet();
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super V> action) {
    listener.mapOther(name, "forEach");
    super.forEach(action);
  }

  @Override
  public Object clone() {
    listener.mapOther(name, "clone");
    return new HashMap<>(this);
  }
}
