package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InputCacheTest {

  @TempDir
  File tmp;

  private static String key(String s) {
    return InputCacheDigest.of(s);
  }

  // ---- the store ----------------------------------------------------------------------------------------------------

  @Test
  void store_roundTripsAndReplaces() throws Exception {
    InputCacheStore store = new InputCacheStore(tmp, 1 << 20);
    assertNull(store.read(key("a")));
    store.write(key("a"), "one".getBytes(StandardCharsets.UTF_8));
    assertArrayEquals("one".getBytes(StandardCharsets.UTF_8), store.read(key("a")));
    store.write(key("a"), "two".getBytes(StandardCharsets.UTF_8));
    assertArrayEquals("two".getBytes(StandardCharsets.UTF_8), store.read(key("a")));
    store.delete(key("a"));
    assertNull(store.read(key("a")));
    assertEquals(0, store.getCorrupt());
  }

  private File entryFile(InputCacheStore store, String key) {
    return new File(new File(store.getFolder(), key.substring(0, 2)), key + ".entry");
  }

  @Test
  void store_ignoresCorruptTruncatedAndOtherFormatEntries() throws Exception {
    InputCacheStore store = new InputCacheStore(tmp, 1 << 20);
    byte[] payload = new byte[5000];
    Arrays.fill(payload, (byte) 7);

    store.write(key("flip"), payload);
    File f = entryFile(store, key("flip"));
    byte[] b = Files.readAllBytes(f.toPath());
    b[b.length - 3] ^= 0x55;
    Files.write(f.toPath(), b);
    assertNull(store.read(key("flip")));

    store.write(key("short"), payload);
    f = entryFile(store, key("short"));
    b = Files.readAllBytes(f.toPath());
    Files.write(f.toPath(), Arrays.copyOf(b, b.length / 2));
    assertNull(store.read(key("short")));

    store.write(key("version"), payload);
    f = entryFile(store, key("version"));
    b = Files.readAllBytes(f.toPath());
    b[8 + 3] = 99; // the format version, after the magic number
    Files.write(f.toPath(), b);
    assertNull(store.read(key("version")));

    store.write(key("empty"), payload);
    Files.write(entryFile(store, key("empty")).toPath(), new byte[0]);
    assertNull(store.read(key("empty")));

    assertEquals(4, store.getCorrupt());
  }

  @Test
  void store_entriesOfAnotherFormatVersionAreInAnotherFolder() throws Exception {
    InputCacheStore store = new InputCacheStore(tmp, 1 << 20);
    store.write(key("a"), new byte[] {1});
    assertEquals("v" + InputCacheStore.FORMAT_VERSION, store.getFolder().getName());
    assertEquals(tmp, store.getFolder().getParentFile());
  }

  @Test
  void store_evictsTheEntriesUsedLongestAgo() throws Exception {
    byte[] payload = new byte[20000];
    new java.util.Random(1).nextBytes(payload); // doesn't compress
    InputCacheStore store = new InputCacheStore(tmp, 50000);
    long t = System.currentTimeMillis() - 100000;
    String[] keys = {key("1"), key("2"), key("3"), key("4")};
    for (int i = 0; i < keys.length; i++) {
      store.write(keys[i], payload);
      Files.setLastModifiedTime(entryFile(store, keys[i]).toPath(), FileTime.fromMillis(t + i * 1000));
    }
    // a hit makes the oldest the newest
    assertNotNull(store.read(keys[0]));
    assertEquals(2, store.evict());
    assertNotNull(store.read(keys[0]));
    assertNull(store.read(keys[1]));
    assertNull(store.read(keys[2]));
    assertNotNull(store.read(keys[3]));
    assertEquals(0, store.evict());
  }

  @Test
  void store_concurrentWritersNeverLeaveAPartialEntry() throws Exception {
    InputCacheStore store = new InputCacheStore(tmp, 1 << 30);
    String k = key("shared");
    ExecutorService ex = Executors.newFixedThreadPool(6);
    try {
      List<Future<?>> fs = new ArrayList<>();
      for (int w = 0; w < 4; w++) {
        final byte fill = (byte) w;
        fs.add(ex.submit(() -> {
          byte[] p = new byte[100000];
          Arrays.fill(p, fill);
          for (int i = 0; i < 50; i++) {
            store.write(k, p);
          }
          return null;
        }));
      }
      for (int r = 0; r < 2; r++) {
        fs.add(ex.submit(() -> {
          for (int i = 0; i < 200; i++) {
            byte[] p = store.read(k);
            if (p != null) {
              assertEquals(100000, p.length);
              for (byte x : p) {
                assertEquals(p[0], x);
              }
            }
          }
          return null;
        }));
      }
      for (Future<?> f : fs) {
        f.get();
      }
    } finally {
      ex.shutdown();
    }
    assertEquals(0, store.getCorrupt());
    String[] left = entryFile(store, k).getParentFile().list();
    assertEquals(1, left.length, Arrays.toString(left));
  }

  // ---- the codec and the journal ---------------------------------------------------------------------------------------

  private static ValidationMessage message(String text) {
    ValidationMessage vm = new ValidationMessage(Source.InstanceValidator, IssueType.INVALID, 3, 4, "Patient.name", text, IssueSeverity.ERROR);
    vm.setMessageId("ID_" + text.length());
    vm.setSliceInfo(null);
    return vm;
  }

  @Test
  void codec_roundTripsMessages() throws Exception {
    ValidationMessage vm = message("a message");
    vm.setLocationLink("link");
    vm.setIgnorableError(true);
    InputCacheCodec.Out out = new InputCacheCodec.Out();
    out.messages(Arrays.asList(vm, message("another")));
    out.str(null).str("x").lng(42).bool(true).bytes(null);
    InputCacheCodec.In in = new InputCacheCodec.In(out.toBytes());
    List<ValidationMessage> l = in.messages();
    assertEquals(2, l.size());
    assertEquals("a message", l.get(0).getMessage());
    assertEquals("link", l.get(0).getLocationLink());
    assertTrue(l.get(0).isIgnorableError());
    assertEquals(IssueSeverity.ERROR, l.get(0).getLevel());
    assertEquals(3, l.get(0).getLine());
    assertEquals("another", l.get(1).getMessage());
    assertNull(in.str());
    assertEquals("x", in.str());
    assertEquals(42, in.lng());
    assertTrue(in.bool());
    assertNull(in.bytes());
    assertTrue(in.atEnd());
    // the same message gives the same bytes, so a verify run can compare them
    InputCacheCodec.Out a = new InputCacheCodec.Out();
    a.message(l.get(0));
    InputCacheCodec.Out b = new InputCacheCodec.Out();
    b.message(vm);
    assertArrayEquals(a.toBytes(), b.toBytes());
  }

  @Test
  void journal_roundTripsAndKeepsSharedMessagesShared() throws Exception {
    InputCache.Journal j = new InputCache.Journal();
    j.millis = 12;
    ValidationMessage shared = message("shared");
    InputCacheCodec.Out o = new InputCacheCodec.Out();
    o.message(shared);
    j.add("msg", "0", o.toBytes());
    InputCacheCodec.Out refs = new InputCacheCodec.Out();
    refs.integer(2).integer(0).integer(0);
    j.add("list", "errs", refs.toBytes());
    j.add("set", "status", "x".getBytes(StandardCharsets.UTF_8));
    InputCache.Journal d = InputCache.Journal.decode(j.encode());
    assertEquals(12, d.millis);
    List<ValidationMessage> l = d.list("errs");
    assertEquals(2, l.size());
    assertSame(l.get(0), l.get(1));
    assertEquals("shared", l.get(0).getMessage());
    assertTrue(d.list("pferr").isEmpty());
    assertNull(j.difference(d));
  }

  @Test
  void journal_differenceSaysWhere() throws Exception {
    InputCache.Journal a = new InputCache.Journal();
    a.add("file", "/t/a.xhtml", "<div>one</div>".getBytes(StandardCharsets.UTF_8));
    InputCache.Journal b = new InputCache.Journal();
    b.add("file", "/t/a.xhtml", "<div>two</div>".getBytes(StandardCharsets.UTF_8));
    String diff = a.difference(b);
    assertNotNull(diff);
    assertTrue(diff.contains("at byte 5"), diff);
    b.recs.clear();
    b.add("file", "/t/a.xhtml", "<div>one</div>".getBytes(StandardCharsets.UTF_8));
    assertNull(a.difference(b));
    b.add("out", "/t/b.xhtml", null);
    assertTrue(a.difference(b).contains("first extra: out"));
  }

  @Test
  void journal_rejectsTrailingBytes() throws Exception {
    InputCache.Journal j = new InputCache.Journal();
    j.add("x", "y", null);
    byte[] b = j.encode();
    byte[] longer = Arrays.copyOf(b, b.length + 1);
    try {
      InputCache.Journal.decode(longer);
      throw new AssertionError("decoded a journal with trailing bytes");
    } catch (java.io.IOException e) {
      // expected
    }
  }

  @Test
  void replace_replacesEveryOccurrenceAndKeepsOtherBytes() {
    // UTF-8 text, with characters outside ASCII next to the value
    byte[] src = "a-DATE-é-DATE中".getBytes(StandardCharsets.UTF_8);
    assertEquals("a-X-é-X中", new String(InputCache.replace(src, "DATE", "X"), StandardCharsets.UTF_8));
    assertSame(src, InputCache.replace(src, "none", "X"));
    assertArrayEquals(src, InputCache.replace(InputCache.replace(src, "DATE", "\u0007M\u0007"), "\u0007M\u0007", "DATE"));
  }

  // ---- digests ----------------------------------------------------------------------------------------------------------

  @Test
  void digest_isLengthPrefixed() {
    assertNotEquals(InputCacheDigest.of("ab", "c"), InputCacheDigest.of("a", "bc"));
    assertNotEquals(InputCacheDigest.of((String) null), InputCacheDigest.of(""));
    assertEquals(InputCacheDigest.of("a", "b"), InputCacheDigest.of("a", "b"));
  }

  @Test
  void deepDigest_ignoresSetOrderButNotListOrder() {
    Set1 a = new Set1(new LinkedHashSet<>(Arrays.asList("x", "y", "z")));
    Set1 b = new Set1(new LinkedHashSet<>(Arrays.asList("z", "y", "x")));
    assertEquals(InputCacheDigest.deep(a.s), InputCacheDigest.deep(b.s));
    assertNotEquals(InputCacheDigest.deep(Arrays.asList("x", "y")), InputCacheDigest.deep(Arrays.asList("y", "x")));
    Map<String, Integer> m1 = new HashMap<>();
    m1.put("a", 1);
    m1.put("b", 2);
    Map<String, Integer> m2 = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
    m2.putAll(m1);
    assertEquals(InputCacheDigest.deep(m1), InputCacheDigest.deep(m2));
    m2.put("b", 3);
    assertNotEquals(InputCacheDigest.deep(m1), InputCacheDigest.deep(m2));
  }

  private static class Set1 {
    final Collection<String> s;

    Set1(Collection<String> s) {
      this.s = s;
    }
  }

  @Test
  void deepDigest_walksFhirObjectsAndCycles() {
    ValidationMessage a = message("one");
    ValidationMessage b = message("one");
    assertEquals(InputCacheDigest.deep(a), InputCacheDigest.deep(b));
    b.setMessage("two");
    assertNotEquals(InputCacheDigest.deep(a), InputCacheDigest.deep(b));
    List<Object> cyc = new ArrayList<>();
    cyc.add("x");
    cyc.add(cyc);
    assertNotNull(InputCacheDigest.deep(cyc));
  }

  @Test
  void deepDigest_emptyFieldCollectionIsNone() {
    // a model object whose list was created by a getter digests as one whose list was never asked for
    assertEquals(InputCacheDigest.deep(new Set1(null)), InputCacheDigest.deep(new Set1(new ArrayList<>())));
    assertNotEquals(InputCacheDigest.deep(new Set1(null)), InputCacheDigest.deep(new Set1(Arrays.asList("x"))));
  }

  // ---- the recording set and map -----------------------------------------------------------------------------------------

  static class SetEvents implements InputCacheRecordingSet.Listener {
    final List<String> events = new ArrayList<>();

    @Override
    public void setContains(String set, String value, boolean result) {
      events.add("contains " + value + " " + result);
    }

    @Override
    public void setAdd(String set, String value, boolean added) {
      events.add("add " + value + " " + added);
    }

    @Override
    public void setWhole(String set, Collection<String> content) {
      events.add("whole " + new java.util.TreeSet<>(content));
    }

    @Override
    public void setOther(String set, String operation) {
      events.add("other " + operation);
    }
  }

  @Test
  void recordingSet_reportsWhatIsAskedAndAdded() {
    SetEvents ev = new SetEvents();
    InputCacheRecordingSet s = new InputCacheRecordingSet("status", new HashSet<>(Arrays.asList("a")), ev);
    assertTrue(s.contains("a"));
    assertFalse(s.contains("b"));
    assertTrue(s.add("b"));
    assertFalse(s.add("b"));
    assertEquals(2, s.size());
    s.remove("a");
    assertEquals(Arrays.asList("contains a true", "contains b false", "add b true", "add b false", "whole [a, b]", "other remove"), ev.events);
    ev.events.clear();
    assertTrue(s.addQuietly("c"));
    assertTrue(s.containsQuietly("c"));
    List<String> l = new ArrayList<>();
    s.forEachQuietly(l::add);
    assertEquals(2, l.size());
    assertTrue(ev.events.isEmpty(), ev.events.toString());
    // iterating is a read of the whole set, and a removal through the iterator is reported
    java.util.Iterator<String> it = s.iterator();
    it.next();
    it.remove();
    assertEquals("whole [b, c]", ev.events.get(0));
    assertEquals("other remove", ev.events.get(1));
  }

  @Test
  void recordingMap_reportsLookupsAndChanges() {
    List<String> events = new ArrayList<>();
    InputCacheRecordingMap.Listener l = new InputCacheRecordingMap.Listener() {
      @Override
      public void mapRead(String map, String key, boolean present, Object value) {
        events.add("read " + key + " " + present + " " + value);
      }

      @Override
      public void mapOther(String map, String operation) {
        events.add("other " + operation);
      }
    };
    Map<String, String> content = new HashMap<>();
    content.put("cs", "yes");
    InputCacheRecordingMap<String> m = new InputCacheRecordingMap<>("supported", content, l);
    assertTrue(m.containsKey("cs"));
    assertEquals("yes", m.get("cs"));
    assertNull(m.get("other"));
    m.put("x", "y");
    m.size();
    assertEquals("y", m.getQuietly("x"));
    assertTrue(m.containsKeyQuietly("x"));
    assertEquals(Arrays.asList("read cs true yes", "read cs true yes", "read other false null", "other put", "other size"), events);
  }
}
