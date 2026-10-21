package org.hl7.fhir.igtools.publisher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The entries of the input cache on disk: one file per key, under a folder named for the format version.
 *
 * <p>An entry is a header (a magic number, the format version, the length and the SHA-256 of the compressed payload)
 * followed by the gzipped payload. An entry is written to a file of its own and then moved over the old one, so a
 * reader sees either the old entry or the new one, never part of one, even with several builds using the folder at
 * once. Anything that doesn't check out (a different format version, a short file, a checksum that doesn't match,
 * a payload that doesn't unzip) is treated as a miss.
 *
 * <p>A hit sets the entry's modification time, and {@link #evict()} deletes the entries used longest ago until the
 * folder is back under its size limit.
 */
public class InputCacheStore {

  public static final int FORMAT_VERSION = 1;
  private static final byte[] MAGIC = "FHIRIGPC".getBytes(StandardCharsets.US_ASCII);
  private static final int HEADER_LENGTH = MAGIC.length + 4 + 4 + 32;
  private static final String ENTRY_SUFFIX = ".entry";
  /** a temporary file older than this was left by a build that died before it could move it into place */
  private static final long STALE_TEMP_MILLIS = 60 * 60 * 1000L;

  private final File folder;
  private final long maxBytes;
  private int corrupt;

  /**
   * @param root the cache folder (the entries go in a subfolder for the format version)
   * @param maxBytes the size {@link #evict()} keeps the entries under
   */
  public InputCacheStore(File root, long maxBytes) {
    this.folder = new File(root, "v" + FORMAT_VERSION);
    this.maxBytes = maxBytes;
  }

  public File getFolder() {
    return folder;
  }

  /** how many entries were ignored because they couldn't be read */
  public int getCorrupt() {
    return corrupt;
  }

  private File entryFile(String key) {
    if (key.length() < 3 || !key.matches("[0-9a-f]+")) {
      throw new IllegalArgumentException("Not a cache key: " + key);
    }
    return new File(new File(folder, key.substring(0, 2)), key + ENTRY_SUFFIX);
  }

  /** the payload stored for the key, or null if there isn't one or it can't be read */
  public byte[] read(String key) {
    File f = entryFile(key);
    byte[] content;
    try {
      content = Files.readAllBytes(f.toPath());
    } catch (NoSuchFileException e) {
      return null;
    } catch (IOException e) {
      corrupt++;
      return null;
    }
    byte[] payload = unwrap(content);
    if (payload == null) {
      corrupt++;
      return null;
    }
    try {
      Files.setLastModifiedTime(f.toPath(), FileTime.fromMillis(System.currentTimeMillis()));
    } catch (IOException e) {
      // only the eviction order depends on it
    }
    return payload;
  }

  /** stores the payload for the key, replacing what was there */
  public void write(String key, byte[] payload) throws IOException {
    File f = entryFile(key);
    File dir = f.getParentFile();
    if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
      throw new IOException("Unable to create " + dir);
    }
    File tmp = new File(dir, key + "." + ProcessHandle.current().pid() + "." + Long.toHexString(ThreadLocalRandom.current().nextLong()) + ".tmp");
    try {
      Files.write(tmp.toPath(), wrap(payload));
      try {
        Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tmp.toPath());
    }
  }

  public void delete(String key) throws IOException {
    Files.deleteIfExists(entryFile(key).toPath());
  }

  static byte[] wrap(byte[] payload) throws IOException {
    ByteArrayOutputStream bz = new ByteArrayOutputStream();
    // the fastest level: the entries (mostly html) shrink almost as much as at the default level, in a fraction of the time
    try (GZIPOutputStream gz = new GZIPOutputStream(bz, 65536) {
      {
        def.setLevel(Deflater.BEST_SPEED);
      }
    }) {
      gz.write(payload);
    }
    byte[] z = bz.toByteArray();
    ByteBuffer b = ByteBuffer.allocate(HEADER_LENGTH + z.length);
    b.put(MAGIC).putInt(FORMAT_VERSION).putInt(z.length).put(sha256(z)).put(z);
    return b.array();
  }

  /** the payload, or null if the content isn't a complete entry of this format version */
  static byte[] unwrap(byte[] content) {
    if (content.length < HEADER_LENGTH || !Arrays.equals(Arrays.copyOf(content, MAGIC.length), MAGIC)) {
      return null;
    }
    ByteBuffer b = ByteBuffer.wrap(content);
    b.position(MAGIC.length);
    if (b.getInt() != FORMAT_VERSION) {
      return null;
    }
    int len = b.getInt();
    if (len != content.length - HEADER_LENGTH) {
      return null;
    }
    byte[] sum = new byte[32];
    b.get(sum);
    byte[] z = Arrays.copyOfRange(content, HEADER_LENGTH, content.length);
    if (!Arrays.equals(sum, sha256(z))) {
      return null;
    }
    if (z.length < 4) {
      return null;
    }
    // the gzip trailer ends with the payload's length (mod 2^32), so the payload is read into an array of its size
    long size = ByteBuffer.wrap(z, z.length - 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
    try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(z), 65536)) {
      if (size >= Integer.MAX_VALUE - 8) {
        return in.readAllBytes();
      }
      byte[] res = new byte[(int) size];
      if (in.readNBytes(res, 0, res.length) != res.length || in.read() != -1) {
        return null;
      }
      return res;
    } catch (IOException e) {
      return null;
    }
  }

  static byte[] sha256(byte[] b) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(b);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Deletes the entries used longest ago until the entries take up no more than the size limit, and the temporary
   * files left by builds that died. Another build may be deleting or writing entries at the same time, so files
   * that disappear are skipped. Returns the number of entries deleted
   */
  public int evict() throws IOException {
    if (!folder.isDirectory()) {
      return 0;
    }
    List<Path> entries = new ArrayList<>();
    List<Long> sizes = new ArrayList<>();
    List<Long> times = new ArrayList<>();
    long total = 0;
    long now = System.currentTimeMillis();
    try (Stream<Path> s = Files.walk(folder.toPath())) {
      for (Path p : (Iterable<Path>) s::iterator) {
        String n = p.getFileName().toString();
        try {
          if (n.endsWith(".tmp")) {
            if (now - Files.getLastModifiedTime(p).toMillis() > STALE_TEMP_MILLIS) {
              Files.deleteIfExists(p);
            }
          } else if (n.endsWith(ENTRY_SUFFIX)) {
            long sz = Files.size(p);
            entries.add(p);
            sizes.add(sz);
            times.add(Files.getLastModifiedTime(p).toMillis());
            total += sz;
          }
        } catch (NoSuchFileException e) {
          // deleted by another build
        }
      }
    }
    if (total <= maxBytes) {
      return 0;
    }
    Integer[] order = new Integer[entries.size()];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    Arrays.sort(order, Comparator.comparingLong(times::get));
    int deleted = 0;
    for (int i : order) {
      if (total <= maxBytes) {
        break;
      }
      Files.deleteIfExists(entries.get(i));
      total -= sizes.get(i);
      deleted++;
    }
    return deleted;
  }
}
