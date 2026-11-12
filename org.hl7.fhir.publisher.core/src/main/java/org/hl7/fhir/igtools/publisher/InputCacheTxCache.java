package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.model.core.CapabilityStatement;
import org.hl7.fhir.model.core.CodeableConcept;
import org.hl7.fhir.model.core.Coding;
import org.hl7.fhir.model.core.Parameters;
import org.hl7.fhir.model.core.TerminologyCapabilities;
import org.hl7.fhir.model.core.ValueSet;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.hl7.fhir.services.context.IWorkerContext;
import org.hl7.fhir.services.terminology.CacheToken;
import org.hl7.fhir.services.terminology.ExpansionOptions;
import org.hl7.fhir.services.terminology.ValidationResult;
import org.hl7.fhir.services.terminology.ValueSetExpansionOutcome;
import org.hl7.fhir.standalone.terminology.utilities.TerminologyCache;
import org.hl7.fhir.utilities.validation.ValidationOptions;

/**
 * The worker context's terminology cache, with the lookups and stores of a cached step reported to the input cache.
 *
 * <p>Every call goes to the real cache. The answers the step got (a validation, an expansion, a subsumption, a value
 * set or code system the server supplied) are reads its result depends on; a store tells the input cache whether the
 * step computed an answer itself (transient) or got it from the server (permanent).
 *
 * <p>This is a memory-only {@link TerminologyCache} of its own only so that it can stand in for the real one: none of
 * its own state is used.
 */
public class InputCacheTxCache extends TerminologyCache {

  /** what the cache reports to */
  public interface Listener {
    /** kind: v (validation), e (expansion), s (subsumes); result null for a miss */
    void txRead(String kind, CacheToken token, Object result);

    void txStore(String kind, CacheToken token, Object result, boolean persistent);

    /** a lookup of a resource the server supplied: kind hvs, gvs, hcs, gcs */
    void txResourceRead(String kind, String canonical, Object result);

    /** anything else that changes the cache */
    void txOther(String operation);
  }

  private final TerminologyCache real;
  private final Listener listener;

  public InputCacheTxCache(Object lock, TerminologyCache real, IWorkerContext context, Listener listener) throws IOException, FHIRException {
    super(lock, null, context);
    this.real = real;
    this.listener = listener;
  }

  public TerminologyCache getReal() {
    return real;
  }

  // ---- the answers: reported ------------------------------------------------------------------------------------------

  @Override
  public ValidationResult getValidation(CacheToken cacheToken) {
    ValidationResult res = real.getValidation(cacheToken);
    listener.txRead("v", cacheToken, res);
    return res;
  }

  @Override
  public void cacheValidation(CacheToken cacheToken, ValidationResult res, boolean persistent) {
    listener.txStore("v", cacheToken, res, persistent);
    real.cacheValidation(cacheToken, res, persistent);
  }

  @Override
  public ValueSetExpansionOutcome getExpansion(CacheToken cacheToken) {
    ValueSetExpansionOutcome res = real.getExpansion(cacheToken);
    listener.txRead("e", cacheToken, res);
    return res;
  }

  @Override
  public void cacheExpansion(CacheToken cacheToken, ValueSetExpansionOutcome res, boolean persistent) {
    listener.txStore("e", cacheToken, res, persistent);
    real.cacheExpansion(cacheToken, res, persistent);
  }

  @Override
  public Boolean getSubsumes(CacheToken cacheToken) {
    Boolean res = real.getSubsumes(cacheToken);
    listener.txRead("s", cacheToken, res);
    return res;
  }

  @Override
  public void cacheSubsumes(CacheToken cacheToken, Boolean b, boolean persistent) {
    listener.txStore("s", cacheToken, b, persistent);
    real.cacheSubsumes(cacheToken, b, persistent);
  }

  @Override
  public boolean hasValueSet(String canonical) {
    boolean res = real.hasValueSet(canonical);
    listener.txResourceRead("hvs", canonical, res);
    return res;
  }

  @Override
  public SourcedValueSet getValueSet(String canonical) {
    SourcedValueSet res = real.getValueSet(canonical);
    listener.txResourceRead("gvs", canonical, res);
    return res;
  }

  @Override
  public boolean hasCodeSystem(String canonical) {
    boolean res = real.hasCodeSystem(canonical);
    listener.txResourceRead("hcs", canonical, res);
    return res;
  }

  @Override
  public SourcedCodeSystem getCodeSystem(String canonical) {
    SourcedCodeSystem res = real.getCodeSystem(canonical);
    listener.txResourceRead("gcs", canonical, res);
    return res;
  }

  @Override
  public void cacheValueSet(String canonical, SourcedValueSet svs) {
    listener.txOther("cacheValueSet");
    real.cacheValueSet(canonical, svs);
  }

  @Override
  public void cacheCodeSystem(String canonical, SourcedCodeSystem scs) {
    listener.txOther("cacheCodeSystem");
    real.cacheCodeSystem(canonical, scs);
  }

  @Override
  public void removeCS(String url) {
    listener.txOther("removeCS");
    real.removeCS(url);
  }

  @Override
  public void clear() throws IOException {
    listener.txOther("clear");
    real.clear();
  }

  @Override
  public void unload() {
    listener.txOther("unload");
    real.unload();
  }

  @Override
  public void cacheCapabilityStatement(String address, CapabilityStatement capabilityStatement) throws IOException {
    listener.txOther("cacheCapabilityStatement");
    real.cacheCapabilityStatement(address, capabilityStatement);
  }

  @Override
  public void cacheTerminologyCapabilities(String address, TerminologyCapabilities terminologyCapabilities) throws IOException {
    listener.txOther("cacheTerminologyCapabilities");
    real.cacheTerminologyCapabilities(address, terminologyCapabilities);
  }

  // ---- everything else: passed through ------------------------------------------------------------------------------

  @Override
  public String getServerId(String address) throws IOException {
    return real.getServerId(address);
  }

  @Override
  public boolean hasCapabilityStatement(String address) {
    return real.hasCapabilityStatement(address);
  }

  @Override
  public CapabilityStatement getCapabilityStatement(String address) {
    return real.getCapabilityStatement(address);
  }

  @Override
  public boolean hasTerminologyCapabilities(String address) {
    return real.hasTerminologyCapabilities(address);
  }

  @Override
  public TerminologyCapabilities getTerminologyCapabilities(String address) {
    return real.getTerminologyCapabilities(address);
  }

  @Override
  public CacheToken generateValidationToken(ValidationOptions options, Coding code, ValueSet vs, Parameters expParameters) {
    return real.generateValidationToken(options, code, vs, expParameters);
  }

  @Override
  public CacheToken generateValidationToken(ValidationOptions options, Coding code, ValueSet vs, Parameters expParameters, String path) {
    return real.generateValidationToken(options, code, vs, expParameters, path);
  }

  @Override
  public CacheToken generateValidationToken(ValidationOptions options, Coding code, String vsUrl, Parameters expParameters) {
    return real.generateValidationToken(options, code, vsUrl, expParameters);
  }

  @Override
  public String extracted(JsonParser json, ValueSet vsc) throws IOException {
    return real.extracted(json, vsc);
  }

  @Override
  public CacheToken generateValidationToken(ValidationOptions options, CodeableConcept code, ValueSet vs, Parameters expParameters) {
    return real.generateValidationToken(options, code, vs, expParameters);
  }

  @Override
  public ValueSet getVSEssense(ValueSet vs) {
    return real.getVSEssense(vs);
  }

  @Override
  public CacheToken generateExpandToken(ValueSet vs, ExpansionOptions options) {
    return real.generateExpandToken(vs, options);
  }

  @Override
  public CacheToken generateExpandToken(String url, ExpansionOptions options) {
    return real.generateExpandToken(url, options);
  }

  @Override
  public void nameCacheToken(ValueSet vs, CacheToken ct) {
    real.nameCacheToken(vs, ct);
  }

  @Override
  public void save() {
    real.save();
  }

  @Override
  public void setSaveDelayMs(long saveDelayMs) {
    real.setSaveDelayMs(saveDelayMs);
  }

  @Override
  public String hashJson(String s) {
    return real.hashJson(s);
  }

  @Override
  public String summary(ValueSet vs) {
    return real.summary(vs);
  }

  @Override
  public String summary(Coding code) {
    return real.summary(code);
  }

  @Override
  public String summary(CodeableConcept code) {
    return real.summary(code);
  }

  @Override
  public String getFolder() {
    return real.getFolder();
  }

  @Override
  public Map<String, String> servers() {
    return real.servers();
  }

  @Override
  public CacheToken generateSubsumesToken(ValidationOptions options, Coding parent, Coding child, Parameters expParameters) {
    return real.generateSubsumesToken(options, parent, child, expParameters);
  }

  @Override
  public String getReport() {
    return real.getReport();
  }

  @Override
  public int getRequestCount() {
    return real.getRequestCount();
  }

  @Override
  public int getHitCount() {
    return real.getHitCount();
  }

  @Override
  public int getNetworkCount() {
    return real.getNetworkCount();
  }
}
