package org.hl7.fhir.igtools.publisher;

/**
 * Tells a listener when a {@link FetchedResource} or {@link FetchedFile} is read: the input cache uses it to work out
 * which of the IG's resources each cached step depended on. The getters call it; with no listener set, that costs one
 * volatile read.
 *
 * <p>Only what can change from one build to the next without changing the IG's set of files and resources is
 * reported: a resource's content, its messages and what validation found, and a file's content, messages and outputs.
 * The rest (a file's path, a resource's type and id) is part of that set, which every cached step reads as a whole.
 */
public final class FetchedReads {

  /** the parts of a resource that are reported as read */
  public enum ResourcePart {
    /** the element, the resource, and everything loaded with them (title, configuration, IG entry, history) */
    CONTENT,
    /** the messages about it */
    ERRORS,
    /** the profiles it was found to conform to, and the examples, test plans and test scripts found for it */
    FOUND,
    /** whether it has been validated */
    VALIDATED
  }

  /** the parts of a file that are reported as read */
  public enum FilePart {
    /** the source, and what was loaded with it (title, value sets to load, hashes, translations) */
    CONTENT,
    /** the messages about it */
    ERRORS,
    /** the names of the files generated from it */
    OUTPUTS
  }

  /** told about each read, on the thread that made it */
  public interface Listener {
    void resourceRead(FetchedResource r, ResourcePart part);

    void fileRead(FetchedFile f, FilePart part);
  }

  private static volatile Listener listener;

  private FetchedReads() {
  }

  /** sets (or, with null, clears) the listener; there's one at a time */
  static void setListener(Listener l) {
    listener = l;
  }

  static Listener getListener() {
    return listener;
  }

  static void read(FetchedResource r, ResourcePart part) {
    Listener l = listener;
    if (l != null) {
      l.resourceRead(r, part);
    }
  }

  static void read(FetchedFile f, FilePart part) {
    Listener l = listener;
    if (l != null) {
      l.fileRead(f, part);
    }
  }
}
