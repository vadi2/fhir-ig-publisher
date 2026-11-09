package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.hl7.fhir.utilities.logging.ILoggingService;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * In watch mode a #current template is checked against its CI build once an hour, not in every build: loading it from
 * the package cache manager (pcm.loadPackage) is what makes the check
 */
class TemplateCurrencyChecksTest {

  private final AtomicLong now = new AtomicLong(1_000_000);
  private FilesystemPackageCacheManager pcm;
  private NpmPackage npm;

  @BeforeEach
  void setUp() throws Exception {
    pcm = Mockito.mock(FilesystemPackageCacheManager.class);
    npm = Mockito.mock(NpmPackage.class);
    when(pcm.loadPackage("x.template", "current")).thenReturn(npm);
    when(pcm.loadPackageFromCacheOnly("x.template", "current")).thenReturn(npm);
  }

  private TemplateManager manager(TemplateManager.CurrencyChecks checks) {
    TemplateManager tm = new TemplateManager(pcm, Mockito.mock(ILoggingService.class));
    tm.setCurrencyChecks(checks);
    return tm;
  }

  @Test
  void aCurrentTemplateIsCheckedOnceAnHourAcrossBuilds() throws Exception {
    TemplateManager.CurrencyChecks checks = new TemplateManager.CurrencyChecks(now::get);
    assertSame(npm, manager(checks).loadPackage("x.template#current", "/ig"));
    now.addAndGet(TemplateManager.CurrencyChecks.INTERVAL - 1);
    // the next build makes a new template manager, with the same checks
    assertSame(npm, manager(checks).loadPackage("x.template#current", "/ig"));
    verify(pcm, times(1)).loadPackage("x.template", "current");
    verify(pcm, times(1)).loadPackageFromCacheOnly("x.template", "current");

    now.addAndGet(1);
    manager(checks).loadPackage("x.template#current", "/ig");
    verify(pcm, times(2)).loadPackage("x.template", "current");
  }

  @Test
  void aCurrentTemplateThatIsNoLongerInTheCacheIsFetchedAgain() throws Exception {
    TemplateManager.CurrencyChecks checks = new TemplateManager.CurrencyChecks(now::get);
    manager(checks).loadPackage("x.template#current", "/ig");
    when(pcm.loadPackageFromCacheOnly("x.template", "current")).thenReturn(null);
    assertSame(npm, manager(checks).loadPackage("x.template#current", "/ig"));
    verify(pcm, times(2)).loadPackage("x.template", "current");
  }

  @Test
  void aFailedLoadIsNotTakenAsACheck() throws Exception {
    TemplateManager.CurrencyChecks checks = new TemplateManager.CurrencyChecks(now::get);
    when(pcm.loadPackage("x.template", "current")).thenThrow(new RuntimeException("offline")).thenReturn(npm);
    assertThrows(RuntimeException.class, () -> manager(checks).loadPackage("x.template#current", "/ig"));
    assertSame(npm, manager(checks).loadPackage("x.template#current", "/ig"));
    verify(pcm, never()).loadPackageFromCacheOnly(anyString(), anyString());
  }

  @Test
  void withoutChecksEveryLoadChecks() throws Exception {
    manager(null).loadPackage("x.template#current", "/ig");
    manager(null).loadPackage("x.template#current", "/ig");
    verify(pcm, times(2)).loadPackage("x.template", "current");
    verify(pcm, never()).loadPackageFromCacheOnly(anyString(), anyString());
  }

  @Test
  void otherVersionsAreLoadedAsBefore() throws Exception {
    when(pcm.loadPackage("x.template", "1.0.0")).thenReturn(npm);
    TemplateManager.CurrencyChecks checks = new TemplateManager.CurrencyChecks(now::get);
    manager(checks).loadPackage("x.template#1.0.0", "/ig");
    manager(checks).loadPackage("x.template#1.0.0", "/ig");
    verify(pcm, times(2)).loadPackage("x.template", "1.0.0");
    verify(pcm, never()).loadPackageFromCacheOnly(anyString(), anyString());
  }
}
