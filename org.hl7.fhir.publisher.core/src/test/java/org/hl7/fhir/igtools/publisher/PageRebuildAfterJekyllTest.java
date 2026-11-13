package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.igtools.templates.Template;
import org.hl7.fhir.utilities.TimeTracker;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the build and a page rebuild do once Jekyll has run: the template's check, and keeping (or putting back) the
 * package step's files
 */
class PageRebuildAfterJekyllTest {

  @TempDir
  Path dir;

  private PublisherFields pf;
  private PublisherGenerator gen;
  private PageRebuild state;
  // what the template's check reports, and writes to the output folder, each time it runs
  private String finding = "something";
  private String checkPage = "<p>checked</p>";

  private void write(String path, String content) throws IOException {
    Path p = dir.resolve(path);
    Files.createDirectories(p.getParent());
    Files.write(p, content.getBytes(StandardCharsets.UTF_8));
  }

  private String read(String path) throws IOException {
    return new String(Files.readAllBytes(dir.resolve(path)), StandardCharsets.UTF_8);
  }

  private Map<String, List<ValidationMessage>> check() throws IOException {
    write("output/qa-template.html", checkPage);
    Map<String, List<ValidationMessage>> res = new HashMap<>();
    res.put("index.html", new ArrayList<>(Collections.singletonList(
        new ValidationMessage(Source.Template, IssueType.BUSINESSRULE, "x", finding, IssueSeverity.WARNING))));
    return res;
  }

  @BeforeEach
  void setUp() throws IOException {
    write("temp/index.md", "# Home");
    write("output/index.html", "<html/>");
    pf = new PublisherFields();
    pf.rootDir = dir.toString();
    pf.tempDir = dir.resolve("temp").toString();
    pf.outputDir = dir.resolve("output").toString();
    pf.tt = new TimeTracker();
    pf.template = mock(Template.class);
    when(pf.template.onCheckEvent(any(), any())).thenAnswer(i -> check());
    gen = new PublisherGenerator(new PublisherSettings());
    gen.setPf(pf);
    state = new PageRebuild();
    state.setPf(pf);
    gen.setPageRebuild(state);
  }

  @AfterEach
  void tearDown() {
    state.discard();
  }

  /** what the build does from Jekyll to the package step */
  private void build() throws IOException {
    state.beforeJekyll();
    gen.afterJekyll(null);
    write("output/package.tgz", "tgz");
    state.afterPackage();
    state.finished();
  }

  /** what Jekyll does when it runs again: the output folder has only what it writes */
  private void jekyllRuns() throws IOException {
    Files.delete(dir.resolve("output/package.tgz"));
    Files.delete(dir.resolve("output/qa-template.html"));
  }

  @Test
  void whatTheTemplatesCheckWritesIsNotKeptAsThePackageStepsOutput() throws IOException {
    build();
    assertEquals(Collections.singletonList(Paths.get("package.tgz")), state.getStashed());
  }

  @Test
  void aPageRebuildRunsTheTemplatesCheckAgainAndPutsBackThePackage() throws IOException {
    build();
    jekyllRuns();
    checkPage = "<p>checked again</p>";
    gen.setPageRebuild(null);
    gen.afterJekyll(state);
    assertEquals("tgz", read("output/package.tgz"));
    assertEquals("<p>checked again</p>", read("output/qa-template.html"));
  }

  @Test
  void aPageRebuildFailsIfTheTemplatesCheckReportsSomethingElse() throws IOException {
    build();
    jekyllRuns();
    finding = "something else";
    gen.setPageRebuild(null);
    assertThrows(IOException.class, () -> gen.afterJekyll(state));
  }

  @Test
  void nothingIsKeptOnceTheBuildKnowsAPageRebuildCantFollowIt() throws IOException {
    state.notReusable("the template doesn't declare what its scripts read");
    gen.afterJekyll(null);
    write("output/package.tgz", "tgz");
    gen.keepForPageRebuild(() -> state.afterPackage());
    assertEquals(null, state.getStashFolder());
    gen.keepForPageRebuild(() -> {
      throw new AssertionError("not expected");
    });
  }
}
