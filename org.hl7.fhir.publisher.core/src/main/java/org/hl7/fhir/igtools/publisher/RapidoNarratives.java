package org.hl7.fhir.igtools.publisher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.igtools.publisher.RapidoDependencyAnalyser.Hasher;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

/**
 * -rapido: the narrative that was generated for each resource of a source file, kept in the temp folder
 * (_rapido/narratives) so that a later build can put it back on a resource that hasn't changed instead of rendering
 * the resource and converting it again.
 *
 * <p>A {@link FileRecord} holds, for each resource of the file that a narrative was generated for, the narrative as
 * the renderer built it (the tree itself, node for node: reading xhtml back from text is not the same tree), and for
 * a resource that is converted to an element afterwards, the text of the narrative as that element holds it. With
 * them is a key of the whole element once it had its narrative (see {@link RapidoNativeOutputs#contentKey}): the
 * narrative is only put back if the element then has that key again.
 *
 * <p>Each record has an id of its own, which the Rapido state names once the build that wrote or used the record has
 * finished. A record that the state doesn't name was written by a build that didn't finish, and is not used.
 */
public class RapidoNarratives {

  private static final int VERSION = 1;
  /** deeper than any narrative: a tree like that is not recorded */
  private static final int MAX_DEPTH = 200;

  /** the narrative generated for one resource */
  public static class ResourceRecord {
    final String type;
    final String id;
    /** the narrative was generated on the resource, which was then converted to an element; else on the element */
    final boolean onResource;
    /** Narrative.status */
    final String status;
    /** the narrative as the renderer built it */
    final XhtmlNode div;
    /** onResource: the narrative as the text the element has for it */
    final String source;
    /** onResource: the narrative on the resource is marked as generated (it is when there is more than one language) */
    final boolean marked;
    /** the key of the element with its narrative */
    final String key;

    ResourceRecord(String type, String id, boolean onResource, String status, XhtmlNode div, String source, boolean marked, String key) {
      this.type = type;
      this.id = id;
      this.onResource = onResource;
      this.status = status;
      this.div = div;
      this.source = source;
      this.marked = marked;
      this.key = key;
    }
  }

  /** the narratives generated for the resources of one source file: one for each resource, null where none is kept */
  public static class FileRecord {
    final String id;
    final List<ResourceRecord> resources = new ArrayList<>();

    FileRecord(String id) {
      this.id = id;
    }

    boolean isEmpty() {
      for (ResourceRecord r : resources) {
        if (r != null) {
          return false;
        }
      }
      return true;
    }
  }

  private final File folder;

  public RapidoNarratives(String tempDir) throws IOException {
    this.folder = new File(Utilities.path(tempDir, "_rapido", "narratives"));
  }

  public static String newId() {
    return UUID.randomUUID().toString();
  }

  /** what the file's record goes by: where it was loaded from, as for RapidoTracker's state */
  private static String name(FetchedFile f) {
    return f.getLoadPath() != null ? f.getLoadPath() : f.getPath();
  }

  private File recordFile(FetchedFile f) {
    return new File(folder, new Hasher().add(name(f)).finish() + ".bin");
  }

  public void forget(FetchedFile f) throws IOException {
    Files.deleteIfExists(recordFile(f).toPath());
  }

  /**
   * Keeps the record for the next build.
   *
   * @return false if a narrative in it can't be kept as it is: then nothing is kept
   */
  public boolean save(FetchedFile f, FileRecord rec) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeInt(VERSION);
      string(out, name(f));
      string(out, rec.id);
      out.writeInt(rec.resources.size());
      for (ResourceRecord r : rec.resources) {
        out.writeBoolean(r != null);
        if (r != null) {
          string(out, r.type);
          string(out, r.id);
          out.writeBoolean(r.onResource);
          string(out, r.status);
          string(out, r.source);
          out.writeBoolean(r.marked);
          string(out, r.key);
          if (!writeNode(out, r.div, 0)) {
            forget(f);
            return false;
          }
        }
      }
      out.writeInt(VERSION); // the end: a record cut short doesn't have it
    }
    Files.createDirectories(folder.toPath());
    File dst = recordFile(f);
    Path tmp = new File(folder, dst.getName() + ".tmp").toPath();
    try {
      Files.write(tmp, bytes.toByteArray());
      Files.move(tmp, dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(tmp);
    }
    return true;
  }

  /** the file's record if it is the one with this id and can be read, else null */
  public FileRecord read(FetchedFile f, String id) {
    File src = recordFile(f);
    if (id == null || !src.isFile()) {
      return null;
    }
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(src.toPath()), 1 << 16))) {
      if (in.readInt() != VERSION || !name(f).equals(string(in)) || !id.equals(string(in))) {
        return null;
      }
      FileRecord rec = new FileRecord(id);
      int n = in.readInt();
      if (n < 0) {
        return null;
      }
      for (int i = 0; i < n; i++) {
        if (!in.readBoolean()) {
          rec.resources.add(null);
        } else {
          String type = string(in);
          String rid = string(in);
          boolean onResource = in.readBoolean();
          String status = string(in);
          String source = string(in);
          boolean marked = in.readBoolean();
          String key = string(in);
          rec.resources.add(new ResourceRecord(type, rid, onResource, status, readNode(in, 0), source, marked, key));
        }
      }
      return in.readInt() == VERSION && in.read() == -1 ? rec : null;
    } catch (Exception e) {
      // cut short, or not a record: the narratives are generated again
      return null;
    }
  }

  /**
   * A copy of the tree that is the same node for node, or null if the tree has something a record doesn't hold (then
   * the narrative it belongs to is not recorded)
   */
  public static XhtmlNode copy(XhtmlNode x) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(bytes, 1 << 14))) {
        if (!writeNode(out, x, 0)) {
          return null;
        }
      }
      XhtmlNode res = readNode(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 0);
      return same(x, res) ? res : null;
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  /** false if the node isn't one that can be read back as it is */
  private static boolean writeNode(DataOutputStream out, XhtmlNode x, int depth) throws IOException {
    if (x == null || x.getClass() != XhtmlNode.class || x.getNodeType() == null || depth > MAX_DEPTH
        || (x.getLocation() != null && x.getLocation().getClass() != XhtmlNode.Location.class)) {
      return false;
    }
    out.writeByte(x.getNodeType().ordinal());
    string(out, x.getName());
    string(out, x.getContent());
    out.writeBoolean(x.isNoPretty());
    out.writeByte(x.getEmptyExpanded() == null ? 0 : x.getEmptyExpanded() ? 1 : 2);
    // where it was in the text it was read from (markdown, say), if it was read
    out.writeBoolean(x.getLocation() != null);
    if (x.getLocation() != null) {
      out.writeInt(x.getLocation().getLine());
      out.writeInt(x.getLocation().getColumn());
    }
    if (!x.hasAttributes()) {
      out.writeInt(0);
    } else {
      out.writeInt(x.getAttributes().size());
      for (Map.Entry<String, String> a : x.getAttributes().entrySet()) {
        string(out, a.getKey());
        string(out, a.getValue());
      }
    }
    if (!x.hasChildren()) {
      out.writeInt(0);
    } else {
      out.writeInt(x.getChildNodes().size());
      for (XhtmlNode c : x.getChildNodes()) {
        if (!writeNode(out, c, depth + 1)) {
          return false;
        }
      }
    }
    return true;
  }

  private static XhtmlNode readNode(DataInputStream in, int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException("too deep");
    }
    int type = in.readByte();
    if (type < 0 || type >= NodeType.values().length) {
      throw new IOException("not a node type: " + type);
    }
    XhtmlNode x = new XhtmlNode(NodeType.values()[type], string(in));
    String content = string(in);
    if (content != null) {
      x.setContent(content);
    }
    if (in.readBoolean()) {
      x.notPretty();
    }
    int expanded = in.readByte();
    if (expanded != 0) {
      x.setEmptyExpanded(expanded == 1);
    }
    if (in.readBoolean()) {
      int line = in.readInt();
      x.setLocation(new XhtmlNode.Location(line, in.readInt()));
    }
    int attributes = count(in);
    for (int i = 0; i < attributes; i++) {
      String name = string(in);
      x.getAttributes().put(name, string(in));
    }
    int children = count(in);
    for (int i = 0; i < children; i++) {
      x.getChildNodes().add(readNode(in, depth + 1));
    }
    return x;
  }

  /** whether the two trees are the same in all that a record holds, the order of the attributes included */
  static boolean same(XhtmlNode a, XhtmlNode b) {
    if (a.getNodeType() != b.getNodeType() || !Objects.equals(a.getName(), b.getName()) || !Objects.equals(a.getContent(), b.getContent())
        || a.isNoPretty() != b.isNoPretty() || !Objects.equals(a.getEmptyExpanded(), b.getEmptyExpanded())
        || a.hasAttributes() != b.hasAttributes() || a.hasChildren() != b.hasChildren() || (a.getLocation() == null) != (b.getLocation() == null)
        || (a.getLocation() != null && (a.getLocation().getLine() != b.getLocation().getLine() || a.getLocation().getColumn() != b.getLocation().getColumn()))) {
      return false;
    }
    if (a.hasAttributes()) {
      if (a.getAttributes().size() != b.getAttributes().size()) {
        return false;
      }
      Iterator<Map.Entry<String, String>> ia = a.getAttributes().entrySet().iterator();
      Iterator<Map.Entry<String, String>> ib = b.getAttributes().entrySet().iterator();
      while (ia.hasNext()) {
        Map.Entry<String, String> ea = ia.next();
        Map.Entry<String, String> eb = ib.next();
        if (!Objects.equals(ea.getKey(), eb.getKey()) || !Objects.equals(ea.getValue(), eb.getValue())) {
          return false;
        }
      }
    }
    if (a.hasChildren()) {
      if (a.getChildNodes().size() != b.getChildNodes().size()) {
        return false;
      }
      for (int i = 0; i < a.getChildNodes().size(); i++) {
        if (!same(a.getChildNodes().get(i), b.getChildNodes().get(i))) {
          return false;
        }
      }
    }
    return true;
  }

  private static int count(DataInputStream in) throws IOException {
    int n = in.readInt();
    if (n < 0) {
      throw new IOException("not a count: " + n);
    }
    return n;
  }

  private static void string(DataOutputStream out, String s) throws IOException {
    if (s == null) {
      out.writeInt(-1);
    } else {
      byte[] b = s.getBytes(StandardCharsets.UTF_8);
      out.writeInt(b.length);
      out.write(b);
    }
  }

  private static String string(DataInputStream in) throws IOException {
    int n = in.readInt();
    if (n < 0) {
      return null;
    }
    if (n > in.available()) {
      throw new IOException("cut short");
    }
    byte[] b = new byte[n];
    in.readFully(b);
    return new String(b, StandardCharsets.UTF_8);
  }
}
