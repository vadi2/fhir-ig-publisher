package org.hl7.fhir.igtools.publisher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.utilities.validation.ValidationMessage;

/**
 * The binary form of what the input cache stores: strings, byte arrays, numbers and validation messages.
 *
 * <p>A message is stored field by field, so it comes back exactly as it was, including the fields that have no
 * setter. A message with a field of a type this doesn't know can't be stored ({@link Unstorable}), and the step
 * that produced it isn't cached.
 */
public class InputCacheCodec {

  /** thrown for content that can't be stored faithfully */
  public static class Unstorable extends Exception {
    public Unstorable(String message) {
      super(message);
    }
  }

  private static final List<Field> MESSAGE_FIELDS;
  private static final String MESSAGE_FIELDS_PROBLEM;

  static {
    List<Field> list = new ArrayList<>();
    String problem = null;
    for (Field fld : ValidationMessage.class.getDeclaredFields()) {
      if (Modifier.isStatic(fld.getModifiers())) {
        continue;
      }
      Class<?> t = fld.getType();
      if (!(t == String.class || t == int.class || t == boolean.class || t.isEnum() || t == Date.class || t == List.class)) {
        problem = "ValidationMessage." + fld.getName() + " has a type the cache can't store (" + t.getName() + ")";
      }
      try {
        fld.setAccessible(true);
      } catch (RuntimeException e) {
        problem = "ValidationMessage." + fld.getName() + " can't be read: " + e.getMessage();
      }
      list.add(fld);
    }
    MESSAGE_FIELDS = Collections.unmodifiableList(list);
    MESSAGE_FIELDS_PROBLEM = problem;
  }

  public static class Out {
    private final ByteArrayOutputStream bytes;
    private final DataOutputStream out;

    /** writes to an array: see {@link #toBytes()} */
    public Out() {
      bytes = new ByteArrayOutputStream();
      out = new DataOutputStream(bytes);
    }

    /** writes to the stream, as it goes: call {@link #flush()} at the end */
    public Out(OutputStream os) {
      bytes = null;
      out = new DataOutputStream(os);
    }

    /** the length {@link #str(String)} writes */
    public static int strLength(String s) {
      return s == null ? 4 : 4 + s.getBytes(StandardCharsets.UTF_8).length;
    }

    /** the length {@link #bytes(byte[])} writes */
    public static int bytesLength(byte[] b) {
      return b == null ? 4 : 4 + b.length;
    }

    public Out str(String s) throws IOException {
      if (s == null) {
        out.writeInt(-1);
      } else {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
      }
      return this;
    }

    public Out bytes(byte[] b) throws IOException {
      if (b == null) {
        out.writeInt(-1);
      } else {
        out.writeInt(b.length);
        out.write(b);
      }
      return this;
    }

    public Out integer(int i) throws IOException {
      out.writeInt(i);
      return this;
    }

    public Out lng(long l) throws IOException {
      out.writeLong(l);
      return this;
    }

    public Out bool(boolean b) throws IOException {
      out.writeBoolean(b);
      return this;
    }

    public Out strings(List<String> list) throws IOException {
      out.writeInt(list.size());
      for (String s : list) {
        str(s);
      }
      return this;
    }

    public Out messages(List<ValidationMessage> list) throws IOException, Unstorable {
      out.writeInt(list.size());
      for (ValidationMessage vm : list) {
        message(vm);
      }
      return this;
    }

    public Out message(ValidationMessage vm) throws IOException, Unstorable {
      if (MESSAGE_FIELDS_PROBLEM != null) {
        throw new Unstorable(MESSAGE_FIELDS_PROBLEM);
      }
      try {
        for (Field fld : MESSAGE_FIELDS) {
          Class<?> t = fld.getType();
          Object v = fld.get(vm);
          if (t == int.class) {
            out.writeInt(fld.getInt(vm));
          } else if (t == boolean.class) {
            out.writeBoolean(fld.getBoolean(vm));
          } else if (v == null) {
            out.writeByte(0);
          } else {
            out.writeByte(1);
            if (t == String.class) {
              str((String) v);
            } else if (t.isEnum()) {
              str(((Enum<?>) v).name());
            } else if (t == Date.class) {
              out.writeLong(((Date) v).getTime());
            } else {
              List<?> l = (List<?>) v;
              out.writeInt(l.size());
              for (Object o : l) {
                if (!(o instanceof ValidationMessage)) {
                  throw new Unstorable("ValidationMessage." + fld.getName() + " holds a " + (o == null ? "null" : o.getClass().getName()));
                }
                message((ValidationMessage) o);
              }
            }
          }
        }
      } catch (IllegalAccessException e) {
        throw new Unstorable("ValidationMessage can't be read: " + e.getMessage());
      }
      return this;
    }

    public byte[] toBytes() throws IOException {
      if (bytes == null) {
        throw new IllegalStateException("this Out writes to a stream");
      }
      out.flush();
      return bytes.toByteArray();
    }

    public void flush() throws IOException {
      out.flush();
    }
  }

  public static class In {
    private final DataInputStream in;

    public In(byte[] b) {
      in = new DataInputStream(new ByteArrayInputStream(b));
    }

    public String str() throws IOException {
      int len = in.readInt();
      if (len < 0) {
        return null;
      }
      return new String(in.readNBytes(checkLength(len)), StandardCharsets.UTF_8);
    }

    public byte[] bytes() throws IOException {
      int len = in.readInt();
      if (len < 0) {
        return null;
      }
      return in.readNBytes(checkLength(len));
    }

    private int checkLength(int len) throws IOException {
      if (len > in.available()) {
        throw new IOException("Truncated entry");
      }
      return len;
    }

    public int integer() throws IOException {
      return in.readInt();
    }

    public long lng() throws IOException {
      return in.readLong();
    }

    public boolean bool() throws IOException {
      return in.readBoolean();
    }

    public List<String> strings() throws IOException {
      int n = checkLength(in.readInt());
      List<String> res = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        res.add(str());
      }
      return res;
    }

    public List<ValidationMessage> messages() throws IOException {
      int n = checkLength(in.readInt());
      List<ValidationMessage> res = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        res.add(message());
      }
      return res;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ValidationMessage message() throws IOException {
      if (MESSAGE_FIELDS_PROBLEM != null) {
        throw new IOException(MESSAGE_FIELDS_PROBLEM);
      }
      ValidationMessage vm = new ValidationMessage();
      try {
        for (Field fld : MESSAGE_FIELDS) {
          Class<?> t = fld.getType();
          if (t == int.class) {
            fld.setInt(vm, in.readInt());
          } else if (t == boolean.class) {
            fld.setBoolean(vm, in.readBoolean());
          } else if (in.readByte() == 0) {
            fld.set(vm, null);
          } else if (t == String.class) {
            fld.set(vm, str());
          } else if (t.isEnum()) {
            fld.set(vm, Enum.valueOf((Class<Enum>) t, str()));
          } else if (t == Date.class) {
            fld.set(vm, new Date(in.readLong()));
          } else {
            int n = checkLength(in.readInt());
            List<ValidationMessage> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
              l.add(message());
            }
            fld.set(vm, l);
          }
        }
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new IOException("Unable to restore a validation message: " + e.getMessage(), e);
      }
      return vm;
    }

    public boolean atEnd() throws IOException {
      return in.available() == 0;
    }
  }
}
