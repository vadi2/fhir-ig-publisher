package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.hl7.fhir.utilities.ZipGenerator;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A zip of the package step that is written again takes the entries that didn't change from the zip of the last
 * build, and holds what ZipGenerator would have written.
 */
class RapidoZipTest {

  @TempDir
  Path dir;

  /** how many times the content of an entry was asked for */
  private final AtomicInteger made = new AtomicInteger();

  private static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  /** what a zip holds: name to content, in the order of the entries */
  private static Map<String, String> read(Path zip) throws Exception {
    Map<String, String> res = new LinkedHashMap<>();
    try (ZipFile z = new ZipFile(zip.toFile())) {
      for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
        ZipEntry ze = e.nextElement();
        try (InputStream in = z.getInputStream(ze)) {
          assertNull(res.put(ze.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8)), ze.getName());
        }
      }
    }
    return res;
  }

  /** writes a zip of these entries (name, content, name, content ...), each with its content as its token */
  private RapidoZip write(Path zip, RapidoArchives.Previous previous, String... entries) throws Exception {
    RapidoZip z = new RapidoZip(zip.toString(), previous);
    for (int i = 0; i < entries.length; i += 2) {
      String content = entries[i + 1];
      z.add(entries[i], "token of " + content, () -> {
        made.incrementAndGet();
        return bytes(content);
      });
    }
    z.close();
    return z;
  }

  private static RapidoArchives.Previous previous(Path zip, RapidoZip written) {
    return new RapidoArchives.Previous(zip, written.getTokens());
  }

  @Test
  void withoutAPreviousZipItIsTheZipThatZipGeneratorWrites() throws Exception {
    Path a = dir.resolve("a.zip");
    RapidoZip z = write(a, null, "one.json", "{\"a\":1}", "two.json", "second ".repeat(500));
    assertEquals(0, z.getCopied());
    assertEquals(2, z.getEntries());

    Path b = dir.resolve("b.zip");
    ZipGenerator gen = new ZipGenerator(b.toString());
    gen.addBytes("one.json", bytes("{\"a\":1}"), false);
    gen.addBytes("two.json", bytes("second ".repeat(500)), false);
    gen.close();
    assertEquals(read(b), read(a));
    assertEquals(Files.size(b), Files.size(a));
  }

  @Test
  void unchangedEntriesAreCopiedAndTheOthersMade() throws Exception {
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = write(first, null, "one.json", "1", "two.json", "second ".repeat(500), "three.json", "3");
    assertEquals(3, made.getAndSet(0));

    // two.json is the same; one.json changed, three.json is gone, four.json is new
    Path second = dir.resolve("second.zip");
    RapidoZip z2 = write(second, previous(first, z1), "one.json", "1 changed", "two.json", "second ".repeat(500), "four.json", "4");
    assertEquals(1, z2.getCopied());
    assertEquals(2, made.getAndSet(0), "only what isn't copied is made");
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("one.json", "1 changed");
    expected.put("two.json", "second ".repeat(500));
    expected.put("four.json", "4");
    assertEquals(expected, read(second));

    // and it is a zip the next build can go on from
    Path third = dir.resolve("third.zip");
    RapidoZip z3 = write(third, previous(second, z2), "one.json", "1 changed", "two.json", "second ".repeat(500), "four.json", "4!");
    assertEquals(2, z3.getCopied());
    expected.put("four.json", "4!");
    assertEquals(expected, read(third));
  }

  @Test
  void aCopiedEntryIsCompressedAsItWas() throws Exception {
    Path first = dir.resolve("first.zip");
    String big = "a line of a resource that compresses well\n".repeat(2000);
    RapidoZip z1 = write(first, null, "big.json", big, "other.json", "x");
    Path second = dir.resolve("second.zip");
    write(second, previous(first, z1), "big.json", big, "other.json", "y");
    try (ZipFile a = new ZipFile(first.toFile()); ZipFile b = new ZipFile(second.toFile())) {
      assertEquals(a.getEntry("big.json").getCompressedSize(), b.getEntry("big.json").getCompressedSize());
      assertEquals(a.getEntry("big.json").getCrc(), b.getEntry("big.json").getCrc());
      assertEquals(ZipEntry.DEFLATED, b.getEntry("other.json").getMethod());
      assertTrue(b.getEntry("big.json").getCompressedSize() < big.length() / 20);
    }
  }

  @Test
  void anEntryWithNoTokenIsMadeAndNoTokensAreKept() throws Exception {
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = write(first, null, "one.json", "1");
    RapidoZip z = new RapidoZip(dir.resolve("second.zip").toString(), previous(first, z1));
    z.add("one.json", null, () -> bytes("not what it was"));
    z.close();
    assertEquals(0, z.getCopied());
    assertNull(z.getTokens());
    assertEquals("not what it was", read(dir.resolve("second.zip")).get("one.json"));
  }

  @Test
  void anEntryTheLastZipDoesNotHaveIsMade() throws Exception {
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = write(first, null, "one.json", "1");
    // the record says there was a two.json with this token, but the zip has none
    JsonObject tokens = z1.getTokens();
    tokens.set("two.json", "token of 2");
    Path second = dir.resolve("second.zip");
    RapidoZip z2 = write(second, new RapidoArchives.Previous(first, tokens), "one.json", "1", "two.json", "2");
    assertEquals(1, z2.getCopied());
    assertEquals("2", read(second).get("two.json"));
  }

  @Test
  void aFileIsAddedAsItsBytes() throws Exception {
    Path file = dir.resolve("spec.internals");
    Files.write(file, bytes("the spec"));
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = new RapidoZip(first.toString(), null);
    z1.addFile("spec.internals", "t1", file.toString());
    z1.close();
    assertEquals("the spec", read(first).get("spec.internals"));

    Files.write(file, bytes("the spec, changed"));
    Path second = dir.resolve("second.zip");
    RapidoZip z2 = new RapidoZip(second.toString(), previous(first, z1));
    z2.addFile("spec.internals", "t2", file.toString());
    z2.close();
    assertEquals("the spec, changed", read(second).get("spec.internals"));

    Path third = dir.resolve("third.zip");
    RapidoZip z3 = new RapidoZip(third.toString(), previous(second, z2));
    z3.addFile("spec.internals", "t2", dir.resolve("not read").toString());
    z3.close();
    assertEquals(1, z3.getCopied());
    assertEquals("the spec, changed", read(third).get("spec.internals"));
  }

  @Test
  void aNameTwiceFailsAsItDoesInZipGenerator() throws Exception {
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = write(first, null, "one.json", "1");
    for (RapidoArchives.Previous p : new RapidoArchives.Previous[] {null, previous(first, z1)}) {
      RapidoZip z = new RapidoZip(dir.resolve("dup.zip").toString(), p);
      z.add("one.json", "token of 1", () -> bytes("1"));
      assertThrows(ZipException.class, () -> z.add("one.json", "token of 1", () -> bytes("1")));
      z.close();
    }
    assertNotNull(read(dir.resolve("dup.zip")).get("one.json"));
    assertFalse(read(dir.resolve("dup.zip")).size() > 1);
  }

  @Test
  void theBytesOfAnEntryAreWhatWasGiven() throws Exception {
    byte[] binary = new byte[70000];
    new java.util.Random(7).nextBytes(binary);
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = new RapidoZip(first.toString(), null);
    z1.add("bin", "b", () -> binary);
    z1.add("text", "t", () -> bytes("t"));
    z1.close();
    Path second = dir.resolve("second.zip");
    RapidoZip z2 = new RapidoZip(second.toString(), previous(first, z1));
    z2.add("bin", "b", () -> {
      throw new IllegalStateException("not needed");
    });
    z2.add("text", "t2", () -> bytes("t2"));
    z2.close();
    try (ZipFile z = new ZipFile(second.toFile()); InputStream in = z.getInputStream(z.getEntry("bin"))) {
      assertArrayEquals(binary, in.readAllBytes());
    }
  }

  @Test
  void aPreviousZipThatCannotBeReadIsNotUsed() throws Exception {
    Path broken = dir.resolve("broken.zip");
    Files.write(broken, bytes("this is not a zip"));
    Path second = dir.resolve("second.zip");
    RapidoZip z = write(second, new RapidoArchives.Previous(broken, new JsonObject().add("one.json", "token of 1")), "one.json", "1");
    assertEquals(0, z.getCopied());
    assertEquals("1", read(second).get("one.json"));
  }

  @Test
  void aZipWithCopiedEntriesReadsAsAStreamToo() throws Exception {
    Path first = dir.resolve("first.zip");
    RapidoZip z1 = write(first, null, "one.json", "1", "two.json", "second ".repeat(500));
    Path second = dir.resolve("second.zip");
    write(second, previous(first, z1), "one.json", "1 changed", "two.json", "second ".repeat(500));
    Map<String, String> res = new LinkedHashMap<>();
    try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(Files.newInputStream(second))) {
      for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        res.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    assertEquals(read(second), res);
  }
}
