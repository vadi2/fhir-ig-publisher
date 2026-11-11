package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hl7.fhir.igtools.publisher.RapidoDependencyAnalyser.Hasher;
import org.hl7.fhir.utilities.json.model.JsonObject;

/**
 * -rapido: the archives that the package step writes to the output folder once Jekyll has run (the example and
 * definition zips, the expansions, validator.pack, the csv, excel and schematron zips), kept from the last build when
 * what goes into them is the same.
 *
 * <p>Each archive (or group of archives made from the same content) has a key: a hash of everything that ends up in
 * it. The build works the key out before it does the work, and asks {@link #keep} whether the files of the last build
 * can stay: they can if that build recorded the same key for them, and each file still has the size and the digest
 * that were recorded when it was written. If not, the build writes them again, and tells {@link #written}. A key of
 * null means the build can't tell what goes into the archive, and it is written again.
 *
 * <p>A zip that is written again can still take the entries that didn't change from the zip of the last build
 * ({@link #previous}, see {@link RapidoZip}), if that build recorded what stands for the content of each entry.
 *
 * <p>Jekyll removes from the output folder what it didn't put there, so the files are moved aside before it runs
 * ({@link #beforeJekyll}) and moved back when they are kept. What is left over when the package step is done isn't
 * made any more, and is deleted ({@link #finish}).
 *
 * <p>The records are part of the Rapido state (see {@link RapidoTracker#save()}), which is only written by a build
 * that finished: after a build that failed, the next one is a complete build, which has no records and writes
 * everything again.
 */
public class RapidoArchives {

  /** changes when what the archives hold for the same key changes */
  private static final String FORMAT = "1";
  private static final int BUFFER = 1 << 16;

  private final String outputDir;
  private final String asideDir;
  /** the records of the last build by group (the name of its first file), or null in a complete build */
  private final JsonObject last;
  private final JsonObject now = new JsonObject();
  /** the names of the files moved aside, and not put back since */
  private final Set<String> movedAside = new TreeSet<>();
  private final List<String> kept = new ArrayList<>();
  /** the groups written again, with the reason */
  private final Map<String, String> rewritten = new LinkedHashMap<>();

  /**
   * @param outputDir the folder the archives are written to
   * @param asideDir where they are while Jekyll runs: not a folder Jekyll reads, and on the same drive if possible
   * @param last what {@link #getRecords()} returned in the last build, or null if nothing can be kept from it
   */
  public RapidoArchives(String outputDir, String asideDir, JsonObject last) {
    this.outputDir = outputDir;
    this.asideDir = asideDir;
    this.last = last;
  }

  /** A hasher for the key of an archive of this kind. Add everything that ends up in the archive to it */
  public static Hasher key(String kind) {
    return new Hasher().add(FORMAT).add(IGVersionUtil.getVersion()).add(kind);
  }

  /** Before Jekyll runs: moves the archives of the last build out of the output folder */
  public void beforeJekyll() {
    if (last == null) {
      return;
    }
    for (String group : last.getNames()) {
      JsonObject files = last.getJsonObject(group).getJsonObject("files");
      if (files == null) {
        continue;
      }
      for (String name : files.getNames()) {
        if (!isName(name)) {
          continue;
        }
        Path from = output(name);
        try {
          if (Files.isRegularFile(from)) {
            Files.createDirectories(Paths.get(asideDir));
            Files.move(from, aside(name), StandardCopyOption.REPLACE_EXISTING);
            movedAside.add(name);
          }
        } catch (IOException e) {
          // then it isn't there to be kept, and is written again
        }
      }
    }
  }

  /**
   * Whether the files (one archive, or several made from the same content) are those of the last build, for the same
   * key. If so they are in the output folder again, and are recorded for the next build. If not, the caller writes
   * them, and then calls {@link #written}
   *
   * @param key from {@link #key}, or null if what goes into the files isn't known
   * @param names the names of the files in the output folder
   */
  public boolean keep(String key, String... names) {
    String group = names[0];
    JsonObject rec = last == null ? null : last.getJsonObject(group);
    String why = notUsable(rec, key, names);
    if (why == null) {
      why = putBack(names);
    }
    if (why != null) {
      rewritten.put(group, why);
      return false;
    }
    now.set(group, rec);
    kept.add(group);
    return true;
  }

  private Path output(String name) {
    return Paths.get(outputDir, name);
  }

  private Path aside(String name) {
    return Paths.get(asideDir, name);
  }

  private String notUsable(JsonObject rec, String key, String[] names) {
    if (key == null) {
      return "its content isn't known before it's written";
    }
    if (rec == null) {
      return "not kept from the last build";
    }
    if (!key.equals(rec.asString("key"))) {
      return "its content changed";
    }
    JsonObject files = rec.getJsonObject("files");
    if (files == null || files.getNames().size() != names.length) {
      return "the last build wrote other files";
    }
    for (String name : names) {
      String why = intact(name, files.getJsonObject(name));
      if (why != null) {
        return why;
      }
    }
    return null;
  }

  /** null if the file moved aside is what the last build recorded (f), else what is wrong with it */
  private String intact(String name, JsonObject f) {
    if (f == null) {
      return "the last build wrote other files";
    }
    Path p = aside(name);
    if (!movedAside.contains(name) || !Files.isRegularFile(p)) {
      return name + " is missing";
    }
    try {
      Long size = f.asLong("size");
      if (size == null || Files.size(p) != size || !digest(p).equals(f.asString("digest"))) {
        return name + " isn't as the last build wrote it";
      }
    } catch (IOException e) {
      return name + " can't be read (" + e.getMessage() + ")";
    }
    return null;
  }

  private String putBack(String[] names) {
    try {
      for (String name : names) {
        Files.move(aside(name), output(name), StandardCopyOption.REPLACE_EXISTING);
        movedAside.remove(name);
      }
      return null;
    } catch (IOException e) {
      return "it can't be put back (" + e.getMessage() + ")";
    }
  }

  /** A zip of the last build, as that build wrote it, with what it recorded for each of its entries */
  public static class Previous {
    private final Path file;
    private final JsonObject entries;

    Previous(Path file, JsonObject entries) {
      this.file = file;
      this.entries = entries;
    }

    public Path getFile() {
      return file;
    }

    /** by entry name: what stood for the content of the entry */
    public JsonObject getEntries() {
      return entries;
    }
  }

  /**
   * The zip that the last build wrote under this name (the first of its group), to take entries from while it is
   * written again. null if there is none, it isn't as that build wrote it, or its entries weren't recorded
   */
  public Previous previous(String name) {
    JsonObject rec = last == null ? null : last.getJsonObject(name);
    JsonObject entries = rec == null ? null : rec.getJsonObject("entries");
    JsonObject files = rec == null ? null : rec.getJsonObject("files");
    if (entries == null || files == null || intact(name, files.getJsonObject(name)) != null) {
      return null;
    }
    return new Previous(aside(name), entries);
  }

  /** As {@link #written(String, String...)}, for a zip: entries is what stands for the content of each entry, by name (or null) */
  public void written(String key, JsonObject entries, String... names) {
    written(key, names);
    JsonObject rec = now.getJsonObject(names[0]);
    if (rec != null && key != null && key.equals(rec.asString("key")) && entries != null) {
      rec.set("entries", entries);
    }
  }

  /** adds to what the log says about an archive that was written again */
  public void note(String name, String note) {
    rewritten.computeIfPresent(name, (k, v) -> v + ", " + note);
  }

  /**
   * Records the files that the build has just written to the output folder, for the next build to keep. Nothing is
   * recorded if the key is null, or a file isn't there
   */
  public void written(String key, String... names) {
    now.remove(names[0]);
    if (key == null) {
      return;
    }
    JsonObject files = new JsonObject();
    try {
      for (String name : names) {
        Path p = output(name);
        if (!isName(name) || !Files.isRegularFile(p)) {
          return;
        }
        files.add(name, new JsonObject().add("size", Files.size(p)).add("digest", digest(p)));
      }
    } catch (IOException e) {
      return;
    }
    now.set(names[0], new JsonObject().add("key", key).add("files", files));
  }

  /**
   * When the package step is over: deletes what was moved aside and not kept. If the package step didn't run to its
   * end, the files it didn't get to are put back as the last build left them instead
   */
  public void finish(boolean packageStepDone) {
    for (String name : movedAside) {
      try {
        Path to = output(name);
        if (packageStepDone || Files.exists(to)) {
          Files.deleteIfExists(aside(name));
        } else {
          Files.move(aside(name), to);
        }
      } catch (IOException e) {
        // it stays where it is until the next complete build clears the temp folder
      }
    }
    movedAside.clear();
  }

  /** what to save for the next build: the archives this build kept or wrote, with their keys */
  public JsonObject getRecords() {
    return now;
  }

  public List<String> getKept() {
    return kept;
  }

  /** the groups that were written again (by the name of their first file), with the reason */
  public Map<String, String> getRewritten() {
    return rewritten;
  }

  /** for the log: how many archives were kept, and which were written again, and why */
  public String summary() {
    StringBuilder b = new StringBuilder();
    b.append(kept.size()).append(" kept, ").append(rewritten.size()).append(" written again");
    if (!rewritten.isEmpty()) {
      List<String> list = new ArrayList<>();
      for (Map.Entry<String, String> e : rewritten.entrySet()) {
        list.add(e.getKey() + ": " + e.getValue());
      }
      b.append(" (").append(String.join("; ", list)).append(")");
    }
    return b.toString();
  }

  /** a file name, not a path: the records come from a file */
  private static boolean isName(String name) {
    return !name.isEmpty() && !name.contains("/") && !name.contains("\\") && !name.equals("..") && !name.equals(".");
  }

  /** the SHA-256 of the file's content */
  static String digest(Path p) throws IOException {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new Error(e);
    }
    byte[] buf = new byte[BUFFER];
    try (InputStream in = Files.newInputStream(p)) {
      int n;
      while ((n = in.read(buf)) > 0) {
        md.update(buf, 0, n);
      }
    }
    StringBuilder b = new StringBuilder();
    for (byte x : md.digest()) {
      b.append(String.format("%02x", x));
    }
    return b.toString();
  }
}
