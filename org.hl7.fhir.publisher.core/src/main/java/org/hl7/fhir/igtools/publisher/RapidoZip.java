package org.hl7.fhir.igtools.publisher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.ZipGenerator;
import org.hl7.fhir.utilities.json.model.JsonObject;

/**
 * Writes one of the zips of the package step (the definitions, the examples, validator.pack, ...).
 *
 * <p>Without a zip from the last build to go on from, it is written by ZipGenerator, as it always was. In a
 * differential -rapido build that has to write the zip again (see {@link RapidoArchives}), most of its entries are
 * still what they were: each entry comes with a token that stands for its content, and an entry that had the same
 * token in the last build is copied from that build's zip as it is there, compressed. Only the others are made (the
 * resource composed, the file read) and compressed, the same way (deflate, level 9). So the zip holds the same
 * entries as the one ZipGenerator would write, with the same content as far as the tokens tell (a token leaves out
 * what only says when the content was made: see the tokens in PublisherGenerator). A copied entry keeps the time it
 * was first written, and the zip's own headers are laid out differently, which readers don't see.
 */
public class RapidoZip implements Closeable {

  /** what an entry holds: only asked for if the entry can't be copied from the last zip */
  public interface Content {
    byte[] get() throws Exception;
  }

  private final ZipGenerator plain;
  private final ZipArchiveOutputStream out;
  private final ZipFile last;
  private final JsonObject lastTokens;
  private JsonObject tokens = new JsonObject();
  private final Set<String> names = new HashSet<>();
  private int copied;

  /**
   * @param path the zip to write
   * @param previous the zip the last build wrote under this name with the tokens of its entries, or null
   */
  public RapidoZip(String path, RapidoArchives.Previous previous) throws IOException {
    ZipFile old = null;
    if (previous != null) {
      try {
        old = ZipFile.builder().setFile(previous.getFile().toFile()).get();
      } catch (IOException | RuntimeException e) {
        // it can't be read as a zip: the zip is written with nothing from it
      }
    }
    last = old;
    if (last == null) {
      plain = new ZipGenerator(path);
      out = null;
      lastTokens = null;
    } else {
      plain = null;
      lastTokens = previous.getEntries();
      try {
        out = new ZipArchiveOutputStream(new File(path));
      } catch (IOException | RuntimeException e) {
        last.close();
        throw e;
      }
      out.setLevel(Deflater.BEST_COMPRESSION);
    }
  }

  /**
   * Adds an entry.
   *
   * @param token stands for the content: the same as in the last build only if the content is. null if not known
   */
  public void add(String name, String token, Content content) throws Exception {
    if (!start(name, token)) {
      byte[] bytes = content.get();
      if (plain != null) {
        plain.addBytes(name, bytes, false);
      } else {
        write(name, bytes);
      }
    }
  }

  /** Adds a file as an entry (see {@link #add}) */
  public void addFile(String name, String token, String path) throws IOException {
    if (!start(name, token)) {
      if (plain != null) {
        plain.addFileName(name, path, false);
      } else {
        write(name, FileUtilities.fileToBytes(path));
      }
    }
  }

  /** true if the entry is in the zip already: copied from the last one */
  private boolean start(String name, String token) throws IOException {
    if (!names.add(name)) {
      throw new ZipException("duplicate entry: " + name); // as ZipGenerator fails
    }
    if (token == null) {
      tokens = null;
    } else if (tokens != null) {
      tokens.set(name, token);
    }
    if (plain != null || token == null || !token.equals(lastTokens.asString(name))) {
      return false;
    }
    ZipArchiveEntry old = last.getEntry(name);
    if (old == null) {
      return false;
    }
    try (InputStream raw = last.getRawInputStream(old)) {
      out.addRawArchiveEntry(old, raw);
    }
    copied++;
    return true;
  }

  private void write(String name, byte[] bytes) throws IOException {
    ZipArchiveEntry e = new ZipArchiveEntry(name);
    e.setTime(System.currentTimeMillis()); // as ZipOutputStream.putNextEntry() does
    out.putArchiveEntry(e);
    out.write(bytes);
    out.closeArchiveEntry();
  }

  @Override
  public void close() throws IOException {
    if (plain != null) {
      plain.close();
    } else {
      try {
        out.close();
      } finally {
        last.close();
      }
    }
  }

  /** the tokens of the entries, by name, for the next build. null if an entry had none */
  public JsonObject getTokens() {
    return tokens;
  }

  public int getEntries() {
    return names.size();
  }

  /** how many of the entries were copied from the last build's zip */
  public int getCopied() {
    return copied;
  }
}
