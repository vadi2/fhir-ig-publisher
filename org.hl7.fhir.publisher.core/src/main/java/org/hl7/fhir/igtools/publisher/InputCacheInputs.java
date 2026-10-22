package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.hl7.fhir.model.core.BaseDateTimeType;
import org.hl7.fhir.model.core.Bundle;
import org.hl7.fhir.model.core.CanonicalResource;
import org.hl7.fhir.model.core.ImplementationGuide;
import org.hl7.fhir.model.core.PackageInformation;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.services.context.IWorkerContext;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.elementmodel.Property;
import org.hl7.fhir.services.renderers.utils.RenderingContext;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;

/**
 * The IG's inputs as a phase starts, as the reads a cached step records see them.
 *
 * <p>Every step reads the IG's shape: which files and resources there are, and what each one is (its path, type, id
 * and canonical URL, whether it's an example, the profiles stated for it), the build's messages, and the state the
 * publisher worked out from the IG before the phase (everything in {@link PublisherFields} but the services, memos
 * and the things steps add to, which are recorded or journaled on their own). A change to any of these is a miss for
 * every step. Beyond that, a step reads the files and resources it uses: its own, and the ones it looks at (through
 * {@link FetchedReads}) or that the worker context returns to it. Each is read at a digest of its content as the phase
 * started; a resource's messages, what validation found for it and whether it's been validated are read as they stand
 * when the step reads them.
 *
 * <p>The keys: {@code shape:files}, {@code shape:errors}, {@code shape:pf}; {@code r:}, {@code re:}, {@code rf:} and
 * {@code rv:} then the resource's locator (its file's path, '#', its index in the file); {@code f:}, {@code fe:} and
 * {@code fo:} then the file's path.
 */
final class InputCacheInputs {

  static final String SHAPE_FILES = "shape:files";
  static final String SHAPE_ERRORS = "shape:errors";
  static final String SHAPE_PF = "shape:pf";
  static final List<String> SHAPES = Collections.unmodifiableList(Arrays.asList(SHAPE_FILES, SHAPE_ERRORS, SHAPE_PF));

  static final String CONTENT = "r:";
  static final String ERRORS = "re:";
  static final String FOUND = "rf:";
  static final String VALIDATED = "rv:";
  static final String FILE = "f:";
  static final String FILE_ERRORS = "fe:";
  static final String FILE_OUTPUTS = "fo:";

  /** what an input's value starts with when it can't be digested: a step that reads it isn't cached */
  static final String UNDIGESTIBLE = "?undigestible: ";

  /**
   * The fields of {@link PublisherFields} left out of {@code shape:pf}. Each is one of: what a step adds to, which the
   * cache journals or records (errors, outputs, fragments, expansions, the npm packages, the memos); a service that
   * works from the build's settings and the state that is in the digest (the worker context and validators, whose
   * lookups are recorded, and the renderers, loggers and writers); or something that is different in every build
   * and that no step's result depends on (timers, the build's time, the package cache)
   */
  static final Set<String> NOT_INPUTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
    // journaled or recorded on their own
    "errors", "allOutputs", "otherFilesRun", "trackedFragments", "fragmentUses", "expansions", "npm", "vnpms", "lnpms", "sdMapCache", "sdUsageCache",
    "inputCache", "sqlIndex",
    // services, whose lookups are recorded (or that work from what's in the digest)
    "context", "validator", "pvalidator", "csvalidator", "validationFetcher", "fetcher", "resolver", "rc", "rcLangs", "dr", "bdr", "cu", "xver",
    "igpkp", "rendererFactory", "versionConvertorRegistry", "markdownEngine", "templateProvider", "templateManager", "realmRules", "module",
    "langUtils", "registry", "inspector", "validatorSession", "signer", "cql", "webTxServer", "previousVersionComparator", "ipaComparator",
    "ipsComparator", "dependentIgFinder", "pcm", "allProfilesCsv", "allProfilesXlsx", "logger",
    // gathered from every resource's content and read only once the steps are done, to write usage-stats.json
    "extensionTracker",
    // different in every build
    "execTime", "last", "tt", "filelog", "maxMemory", "txLog"
  )));

  private final PublisherFields pf;
  private final String buildDate;
  /** the locators of the IG's resources and files, and the reverse */
  private final Map<FetchedResource, String> resourceLocs = new IdentityHashMap<>();
  private final Map<FetchedFile, String> fileLocs = new IdentityHashMap<>();
  private final Map<String, FetchedResource> resources = new HashMap<>();
  private final Map<String, FetchedFile> files = new HashMap<>();
  /** the resource objects and elements of the IG's resources (and the resources in its bundles), by identity */
  private final Map<Object, String> objectLocs = new IdentityHashMap<>();
  /** the IG's canonical resources by URL, and by URL|version */
  private final Map<String, String> urlLocs = new HashMap<>();
  /** the digest of each resource's and file's content, by key */
  private final Map<String, String> content = new HashMap<>();
  private final Map<String, String> shapes = new HashMap<>();
  /** the digest of each field in shape:pf */
  private final Map<String, String> pfFields = new TreeMap<>();
  /** message list digests, by list: worked out again when the list has grown */
  private final Map<List<ValidationMessage>, Object[]> messageDigests = new IdentityHashMap<>();

  private InputCacheInputs(PublisherFields pf, String buildDate) {
    this.pf = pf;
    this.buildDate = buildDate;
  }

  /** the inputs as they are now */
  static InputCacheInputs of(PublisherFields pf, String buildDate) throws IOException {
    InputCacheInputs res = new InputCacheInputs(pf, buildDate);
    res.locateAll();
    res.digestContent();
    res.digestShapes();
    return res;
  }

  // ---- locators -------------------------------------------------------------------------------------------------------

  private void locateAll() {
    int n = 0;
    for (FetchedFile f : pf.fileList) {
      String floc = f.getPath() == null ? "@" + n : f.getPath();
      n++;
      fileLocs.put(f, floc);
      files.put(floc, f);
      for (int i = 0; i < f.getResources().size(); i++) {
        FetchedResource r = f.getResources().get(i);
        String loc = floc + "#" + i;
        resourceLocs.put(r, loc);
        resources.put(loc, r);
        if (r.getElement() != null) {
          objectLocs.put(r.getElement(), loc);
        }
        Resource res = r.getResource();
        if (res != null) {
          objectLocs.put(res, loc);
          addUrl(res, loc);
          if (res instanceof Bundle) {
            for (Bundle.BundleEntryComponent be : ((Bundle) res).getEntryList()) {
              if (be.hasResource()) {
                objectLocs.putIfAbsent(be.getResource(), loc);
                addUrl(be.getResource(), loc);
              }
            }
          }
        }
      }
    }
    for (Map.Entry<String, FetchedResource> e : pf.canonicalResources.entrySet()) {
      String loc = resourceLocs.get(e.getValue());
      if (loc != null) {
        // keyed by url, and by url#version
        urlLocs.putIfAbsent(e.getKey().replace('#', '|'), loc);
      }
    }
  }

  private void addUrl(Resource res, String loc) {
    if (res instanceof CanonicalResource && ((CanonicalResource) res).hasUrl()) {
      CanonicalResource cr = (CanonicalResource) res;
      urlLocs.putIfAbsent(cr.getUrl(), loc);
      if (cr.hasVersion()) {
        urlLocs.putIfAbsent(cr.getUrl() + "|" + cr.getVersion(), loc);
      }
    }
  }

  /** the resource's locator, or null if it isn't one of the IG's */
  String locate(FetchedResource r) {
    return resourceLocs.get(r);
  }

  String locate(FetchedFile f) {
    return fileLocs.get(f);
  }

  /** the resource with the locator, or null */
  FetchedResource resource(String loc) {
    return resources.get(loc);
  }

  /** the locator of the IG resource that the resource object or element is (or is in), or null */
  String locateObject(Object o) {
    return objectLocs.get(o);
  }

  /** the locator of the IG's canonical resource with the URL (optionally |version), or null */
  String locateUrl(String url) {
    return url == null ? null : urlLocs.get(url);
  }

  // ---- content ------------------------------------------------------------------------------------------------------

  private void digestContent() throws IOException {
    for (Map.Entry<FetchedFile, String> e : fileLocs.entrySet()) {
      content.put(FILE + e.getValue(), fileDigest(e.getKey()));
    }
    for (Map.Entry<FetchedResource, String> e : resourceLocs.entrySet()) {
      content.put(CONTENT + e.getValue(), resourceDigest(e.getKey()));
    }
  }

  private String fileDigest(FetchedFile f) {
    InputCacheDigest d = new InputCacheDigest();
    if (!f.isFolder()) {
      try {
        d.bytes(f.getSource());
      } catch (Error e) {
        d.add("dropped");
      }
    }
    d.add(f.hasTitle()).add(f.getTitle()).add(f.getSize()).add(f.getHash()).add(f.getCalcHash());
    d.add(new TreeMap<>(f.getValuesetsToLoad()).toString());
    return d.finish();
  }

  private String resourceDigest(FetchedResource r) {
    InputCacheDigest d = new InputCacheDigest();
    d.add(r.hasTitle()).add(r.getTitle()).add(r.getResourceName()).add(r.getResourceDescription()).add(r.getConfig() == null ? null : r.getConfig().toString());
    d.add(r.isSnapshotted()).add(r.isHasTranslations()).add(r.isRegenAfterValidation()).add(r.isUmlGenerated()).add(r.isGeneratedNarrative()).add(r.getProvenance());
    String[] parts = {deep(r.getResEntry()), deep(r.getAudits()), r.hasOtherVersions() ? deep(r.getOtherVersions()) : null, deep(r.getResource())};
    for (String v : parts) {
      if (v != null && v.startsWith(UNDIGESTIBLE)) {
        return v;
      }
      d.add(v);
    }
    element(r.getElement(), d);
    element(r.getLogicalElement(), d);
    return d.finish();
  }

  private void element(Element e, InputCacheDigest d) {
    if (e == null) {
      d.add("no element");
      return;
    }
    d.add("(").add(e.getName()).add(e.fhirType()).add(String.valueOf(e.getFormat()));
    for (String c : e.getComments()) {
      d.add("c").add(c);
    }
    for (Element c : e.getChildList()) {
      if ("date".equals(c.getName()) && buildDate.equals(c.getValue())) {
        // stamped by the loader because the source has no date: the same input every build
        d.add("(build date)");
      } else {
        scan(c, d);
      }
    }
    d.add(")");
  }

  private static void scan(Element e, InputCacheDigest d) {
    d.add("(").add(e.getName()).add(e.fhirType());
    if (e.isXhtml()) {
      d.add(e.getXhtml() == null ? e.getValue() : new XhtmlComposer(XhtmlComposer.XML).compose(e.getXhtml()));
    } else if (e.hasValue()) {
      d.add(e.getValue());
    }
    for (String c : e.getComments()) {
      d.add("c").add(c);
    }
    if (e.hasChildren()) {
      for (Element c : e.getChildList()) {
        scan(c, d);
      }
    }
    d.add(")");
  }

  /** a deep digest of part of a resource: a package is digested by its id and version (its date is the build's) */
  private String deep(Object o) {
    if (o == null) {
      return null;
    }
    try {
      return InputCacheDigest.deep(o, this::resourcePart);
    } catch (InputCacheDigest.TooLarge e) {
      return UNDIGESTIBLE + e.getMessage();
    }
  }

  /**
   * In a resource, a package is its id and version (its date is the build's), and a date that is the build's (stamped
   * by the loader because the source has none) is the same input every build
   */
  private String resourcePart(Object x) {
    if (x instanceof PackageInformation) {
      return "package " + ((PackageInformation) x).getVID();
    } else if (x instanceof BaseDateTimeType && buildDate.equals(((BaseDateTimeType) x).asStringValue())) {
      return "(build date)";
    }
    return null;
  }

  // ---- shape --------------------------------------------------------------------------------------------------------

  private void digestShapes() throws IOException {
    InputCacheDigest d = new InputCacheDigest();
    for (FetchedFile f : pf.fileList) {
      d.add("F").add(fileLocs.get(f)).add(f.getName()).add(f.getRelativePath()).add(f.getStatedPath()).add(f.getLoadPath()).add(f.getProcessMode())
        .add(f.getContentType()).add(f.getLogical()).add(f.isFolder()).add(String.valueOf(f.getBundleType())).add(locateOrNull(f.getBundle()));
      for (FetchedFile.Output o : f.getOutputs()) {
        d.add("O").add(o.getRelativePath()).add(o.getProcessMode()).bytes(o.getXslt());
      }
      if (f.hasAdditionalPaths()) {
        d.add("P").add(f.getAdditionalPaths().toString());
      }
      if (f.isFolder()) {
        d.add("D").add(f.getFiles().toString());
      }
      if (f.getDependencies() != null) {
        for (FetchedFile dep : f.getDependencies()) {
          d.add("d").add(fileLocs.get(dep));
        }
      }
      for (FetchedResource r : f.getResources()) {
        d.add("R").add(r.fhirType()).add(r.getId()).add(r.getPath()).add(r.getExampleUri()).add(r.isExample()).add(r.isValidateAsResource())
          .add(r.getNameForErrors()).add(r.getStatedProfiles().toString()).add(r.getTestArtifacts().toString()).add(canonical(r));
      }
    }
    shapes.put(SHAPE_FILES, d.finish());
    shapes.put(SHAPE_ERRORS, messagesDigest(pf.errors));
    InputCacheDigest p = new InputCacheDigest();
    List<String> undigestible = new ArrayList<>();
    for (Field f : publisherFields()) {
      String v;
      try {
        v = fieldDigest(f.getName(), f.get(pf));
      } catch (IllegalAccessException | RuntimeException e) {
        v = UNDIGESTIBLE + "unreadable: " + e;
      }
      if (v.startsWith(UNDIGESTIBLE)) {
        undigestible.add(f.getName() + ": " + v.substring(UNDIGESTIBLE.length()));
      }
      pfFields.put(f.getName(), v);
      p.add(f.getName()).add(v);
    }
    shapes.put(SHAPE_PF, undigestible.isEmpty() ? p.finish() : UNDIGESTIBLE + String.join("; ", undigestible));
  }

  private String locateOrNull(FetchedResource r) {
    return r == null ? null : resourceLocs.get(r);
  }

  /** the canonical URL and version of the resource, from what was loaded */
  private static String canonical(FetchedResource r) {
    Resource res = r.getResource();
    if (res instanceof CanonicalResource) {
      return ((CanonicalResource) res).getVUrl();
    }
    Element e = r.getElement();
    if (e != null && e.hasChild("url")) {
      return e.getChildValue("url") + "|" + e.getChildValue("version");
    }
    return null;
  }

  private static List<Field> publisherFields() {
    List<Field> res = new ArrayList<>();
    for (Field f : PublisherFields.class.getDeclaredFields()) {
      if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && !NOT_INPUTS.contains(f.getName())) {
        f.setAccessible(true);
        res.add(f);
      }
    }
    res.sort((a, b) -> a.getName().compareTo(b.getName()));
    return res;
  }

  private String fieldDigest(String name, Object v) {
    if (v instanceof ImplementationGuide && ("publishedIg".equals(name) || "sourceIg".equals(name))) {
      // what each resource is called and how it's described comes from the resource, and is read with it
      ImplementationGuide ig = ((ImplementationGuide) v).copy(org.hl7.fhir.model.Base.COPY_ALL);
      for (ImplementationGuide.ImplementationGuideDefinitionResourceComponent rc : ig.getDefinition().getResourceList()) {
        rc.setNameElement(null);
        rc.setDescriptionElement(null);
      }
      v = ig;
    }
    try {
      return InputCacheDigest.deep(v, this::substitute);
    } catch (InputCacheDigest.TooLarge e) {
      return UNDIGESTIBLE + e.getMessage();
    }
  }

  /**
   * In shape:pf, the IG's own files and resources are digested by locator (and resource objects with their content
   * digest), packages and spec maps by id and version, a package's resources by package and id, the build date as such, and the worker context not at all
   */
  private String substitute(Object o) {
    if (o instanceof FetchedResource) {
      String loc = resourceLocs.get(o);
      return loc == null ? null : "resource " + loc;
    } else if (o instanceof FetchedFile) {
      String loc = fileLocs.get(o);
      return loc == null ? otherFile((FetchedFile) o) : "file " + loc;
    } else if (o instanceof IWorkerContext) {
      return "context";
    } else if (o instanceof RenderingContext) {
      // a service, set up from the build's settings
      return "rendering context";
    } else if (o instanceof PackageInformation) {
      return "package " + ((PackageInformation) o).getVID();
    } else if (o instanceof NpmPackage) {
      // what's in a package is in K0
      return "npm " + ((NpmPackage) o).vid();
    } else if (o instanceof SpecMapManager) {
      // what a spec map lists comes from its package, which is in K0; the rest is what it's called and where it's published
      SpecMapManager m = (SpecMapManager) o;
      return "spec map " + m + " " + m.getKey() + " " + m.getAuth() + " " + m.getRealm() + " " + m.getSpecial();
    } else if (o instanceof BaseDateTimeType) {
      return resourcePart(o);
    }
    String loc = objectLocs.get(o);
    if (loc != null) {
      return "content " + loc + " " + content.get(CONTENT + loc);
    }
    if (o instanceof Resource) {
      Resource r = (Resource) o;
      if (r.getSourcePackage() != null && r.getSourcePackage() != pf.packageInfo) {
        return "package resource " + r.getSourcePackage().getVID() + " " + r.fhirType() + "/" + r.getIdBase() + (r instanceof CanonicalResource ? " " + ((CanonicalResource) r).getVUrl() : "");
      }
    } else if (o instanceof Property) {
      Property p = (Property) o;
      return "property " + (p.getStructure() == null ? null : p.getStructure().getVUrl()) + "#" + (p.getDefinition() == null ? null : p.getDefinition().getId());
    }
    return null;
  }

  /**
   * A file that isn't one of the IG's (a folder of pages, or a file of one that was loaded before): what it is, and not
   * when it was last written or how long processing it took, which are different every build
   */
  private static String otherFile(FetchedFile f) {
    return "other file " + f.getPath() + " " + f.getRelativePath() + " " + f.getProcessMode()
      + (f.isFolder() ? " folder " + new TreeSet<>(f.getFiles()) : " " + f.getSize() + " " + f.getHash());
  }

  /** the digest of each field in shape:pf, by name (to say which changed) */
  Map<String, String> getPublisherFieldDigests() {
    return Collections.unmodifiableMap(pfFields);
  }

  /**
   * For the calibrate setting, to say what changed: the digest of each field in shape:pf, and of its parts (a map's
   * entries, a list's elements, an object's fields), and each file's and resource's content digest
   */
  Map<String, String> calibration() {
    Map<String, String> res = new TreeMap<>(pfFields);
    for (Field f : publisherFields()) {
      try {
        parts(f.getName(), f.get(pf), res);
      } catch (IllegalAccessException | RuntimeException e) {
        res.put(f.getName() + ".*", "unreadable: " + e.getClass().getName());
      }
    }
    res.putAll(content);
    for (Map.Entry<FetchedResource, String> e : resourceLocs.entrySet()) {
      resourceParts(e.getValue(), e.getKey(), res);
    }
    return res;
  }

  /** what a resource's content digest is made of, each part on its own (to say which part changed) */
  private void resourceParts(String loc, FetchedResource r, Map<String, String> res) {
    String k = CONTENT + loc + " ";
    res.put(k + "about", InputCacheDigest.of(String.valueOf(r.hasTitle()), r.getTitle(), r.getResourceName(), r.getResourceDescription(),
      r.getConfig() == null ? null : r.getConfig().toString(), String.valueOf(r.isSnapshotted()), String.valueOf(r.isHasTranslations()),
      String.valueOf(r.isRegenAfterValidation()), String.valueOf(r.isUmlGenerated()), String.valueOf(r.isGeneratedNarrative()), String.valueOf(r.getProvenance())));
    res.put(k + "resEntry", deep(r.getResEntry()));
    res.put(k + "audits", deep(r.getAudits()));
    InputCacheDigest d = new InputCacheDigest();
    element(r.getElement(), d);
    res.put(k + "element", d.finish());
    d = new InputCacheDigest();
    element(r.getLogicalElement(), d);
    res.put(k + "logicalElement", d.finish());
    Resource rr = r.getResource();
    if (rr != null) {
      for (Class<?> c = rr.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
        for (Field f : c.getDeclaredFields()) {
          if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && !"userData".equals(f.getName())) {
            try {
              f.setAccessible(true);
              res.put(k + "resource." + f.getName(), deep(f.get(rr)));
            } catch (IllegalAccessException | RuntimeException e) {
              res.put(k + "resource." + f.getName(), "unreadable: " + e.getClass().getName());
            }
          }
        }
      }
    }
  }

  private static final int MAX_PARTS = 5000;

  private void parts(String name, Object v, Map<String, String> res) throws IllegalAccessException {
    int n = 0;
    if (v instanceof Map) {
      for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
        if (n++ == MAX_PARTS) {
          break;
        }
        res.put(name + "[" + e.getKey() + "]", fieldDigest(name, e.getValue()));
      }
    } else if (v instanceof Set) {
      // in no order: each element by its digest
      for (Object o : (Set<?>) v) {
        if (n++ == MAX_PARTS) {
          break;
        }
        String dg = fieldDigest(name, o);
        res.put(name + "{" + dg + "}", dg);
      }
    } else if (v instanceof Collection) {
      for (Object o : (Collection<?>) v) {
        if (n == MAX_PARTS) {
          break;
        }
        res.put(name + "[" + (n++) + "]", fieldDigest(name, o));
      }
    } else if (v != null && v.getClass().getName().startsWith("org.hl7.fhir.") && substitute(v) == null) {
      for (Class<?> c = v.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
        for (Field f : c.getDeclaredFields()) {
          if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
            f.setAccessible(true);
            res.put(name + "." + f.getName(), fieldDigest(name, f.get(v)));
          }
        }
      }
    }
  }

  // ---- evaluation ---------------------------------------------------------------------------------------------------

  /** the read's value now, or null if the key isn't one of these */
  String evaluate(String key) throws IOException {
    String v = shapes.get(key);
    if (v != null) {
      return v;
    }
    if (key.startsWith(CONTENT) || key.startsWith(FILE)) {
      v = content.get(key);
      return v == null ? InputCache.ABSENT : v;
    }
    int i = key.indexOf(':');
    if (i < 0) {
      return null;
    }
    String prefix = key.substring(0, i + 1);
    String loc = key.substring(i + 1);
    switch (prefix) {
      case ERRORS:
      case FOUND:
      case VALIDATED: {
        FetchedResource r = resources.get(loc);
        if (r == null) {
          return InputCache.ABSENT;
        }
        if (ERRORS.equals(prefix)) {
          return messagesDigest(r.getErrors());
        } else if (VALIDATED.equals(prefix)) {
          return r.isValidated() ? "1" : "0";
        }
        InputCacheDigest d = new InputCacheDigest();
        d.add(new java.util.TreeSet<>(r.getFoundProfiles()).toString());
        for (Collection<FetchedResource> c : Arrays.asList(r.getStatedExamples(), r.getFoundExamples(), r.getFoundTestPlans(), r.getFoundTestScripts())) {
          d.add(locators(c));
        }
        return d.finish();
      }
      case FILE_ERRORS:
      case FILE_OUTPUTS: {
        FetchedFile f = files.get(loc);
        if (f == null) {
          return InputCache.ABSENT;
        }
        if (FILE_OUTPUTS.equals(prefix)) {
          return InputCacheDigest.of(new java.util.TreeSet<>(f.getOutputNames()).toArray(new String[0]));
        }
        return messagesDigest(f.getErrors()) + (f.getFilteredMessages() == null ? "" : "/" + messagesDigest(f.getFilteredMessages()));
      }
      default:
        return null;
    }
  }

  private String locators(Collection<FetchedResource> c) {
    List<String> l = new ArrayList<>();
    for (FetchedResource r : c) {
      String loc = resourceLocs.get(r);
      l.add(loc == null ? "?" + r : loc);
    }
    Collections.sort(l);
    return l.toString();
  }

  /** a digest of the messages; the build date in them is replaced, since it's the same input every build */
  String messagesDigest(List<ValidationMessage> list) throws IOException {
    Object[] m = messageDigests.get(list);
    if (m != null && (Integer) m[0] == list.size() && (list.isEmpty() || m[1] == list.get(list.size() - 1))) {
      return (String) m[2];
    }
    InputCacheCodec.Out out = new InputCacheCodec.Out();
    try {
      out.messages(list);
    } catch (InputCacheCodec.Unstorable e) {
      throw new IOException(e.getMessage());
    }
    String res = InputCacheDigest.of(InputCache.replace(out.toBytes(), buildDate, "(build date)"));
    messageDigests.put(list, new Object[] {list.size(), list.isEmpty() ? null : list.get(list.size() - 1), res});
    return res;
  }

  /** the keys whose digests are different in the two (as {@link #getPublisherFieldDigests()} or {@link #calibration()}) */
  static List<String> changed(Map<String, String> before, Map<String, String> now) {
    List<String> res = new ArrayList<>();
    Map<String, String> all = new LinkedHashMap<>(before);
    for (String k : now.keySet()) {
      all.putIfAbsent(k, null);
    }
    for (String k : all.keySet()) {
      String a = before.get(k);
      String b = now.get(k);
      if (a == null ? b != null : !a.equals(b)) {
        res.add(k);
      }
    }
    Collections.sort(res);
    return res;
  }

  /** a function that leaves objects as they are (for tests) */
  static Function<Object, String> none() {
    return x -> null;
  }
}
