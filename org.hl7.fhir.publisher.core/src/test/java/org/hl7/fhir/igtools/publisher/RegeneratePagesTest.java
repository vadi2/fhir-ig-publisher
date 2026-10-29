package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.utilities.logging.ILoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What a page-only rebuild writes to the temp folder for a page in a translated IG: the page in each language, from
 * its translation if it has one, as the build does
 */
class RegeneratePagesTest {

  @TempDir
  Path dir;

  private PublisherFields pf;
  private PublisherGenerator gen;
  private FetchedFile page;
  private final List<String> log = new ArrayList<>();

  private void write(String path, String content) throws IOException {
    Path p = dir.resolve(path);
    Files.createDirectories(p.getParent());
    Files.write(p, content.getBytes(StandardCharsets.UTF_8));
  }

  private String read(String path) throws IOException {
    return new String(Files.readAllBytes(dir.resolve(path)), StandardCharsets.UTF_8);
  }

  private String temp(String path) {
    return dir.resolve("temp").resolve(path).toString();
  }

  /** the files the build wrote for the page */
  private List<String> outputs() {
    return Arrays.asList(temp("_includes/en/index.md"), temp("_includes/ru/index.md"), temp("_includes/uz/index.md"),
        temp("_includes/index.md"));
  }

  @BeforeEach
  void setUp() throws IOException {
    write("input/pagecontent/index.md", "# Home");
    write("input/translations/ru/pagecontent/index.md", "# Главная");
    PublisherSettings settings = new PublisherSettings();
    settings.setNewMultiLangTemplateFormat(true);
    gen = new PublisherGenerator(settings);
    pf = new PublisherFields();
    pf.rootDir = dir.toString();
    pf.tempDir = dir.resolve("temp").toString();
    pf.outputDir = dir.resolve("output").toString();
    Files.createDirectories(dir.resolve("output"));
    pf.logger = new ILoggingService() {
      public void logMessage(String message) {
        log.add(message);
      }

      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    pf.defaultTranslationLang = "en";
    pf.translationLangs.addAll(Arrays.asList("ru", "uz"));
    pf.translationSources.addAll(Arrays.asList("input/translations/ru", "input/translations/uz"));
    gen.setPf(pf);
    page = new FetchedFile("input/pagecontent/index.md", "pagecontent/index.md");
    page.setPath(dir.resolve("input/pagecontent/index.md").toString());
    page.setRelativePath("_includes/index.md");
    page.setProcessMode(FetchedFile.PROCESS_NONE);
    // what the build recorded when it wrote the page (uz has no translation, so it's the English page)
    for (String s : outputs()) {
      pf.allOutputs.add(s.toLowerCase());
      page.getOutputNames().add(s);
    }
    write("temp/_includes/en/index.md", "# Home");
    write("temp/_includes/ru/index.md", "# Главная");
    write("temp/_includes/uz/index.md", "# Home");
    write("temp/_includes/index.md", "# Home");
  }

  private List<String> regenerate() throws Exception {
    List<String> res = gen.regeneratePages(Collections.singletonList(page));
    assertEquals(4, pf.allOutputs.size());
    return res;
  }

  @Test
  void anEditToTheEnglishPage() throws Exception {
    write("input/pagecontent/index.md", "# Home, edited");
    // and the page without a translation in uz
    assertEquals(Arrays.asList(temp("_includes/en/index.md"), temp("_includes/uz/index.md"), temp("_includes/index.md")), regenerate());
    assertEquals("# Home, edited", read("temp/_includes/en/index.md"));
    assertEquals("# Главная", read("temp/_includes/ru/index.md"));
    assertEquals("# Home, edited", read("temp/_includes/uz/index.md"));
    assertEquals("# Home, edited", read("temp/_includes/index.md"));
  }

  @Test
  void anEditToATranslation() throws Exception {
    write("input/translations/ru/pagecontent/index.md", "# Главная страница");
    assertEquals(Collections.singletonList(temp("_includes/ru/index.md")), regenerate());
    assertEquals("# Главная страница", read("temp/_includes/ru/index.md"));
    assertEquals("# Home", read("temp/_includes/en/index.md"));
  }

  @Test
  void theFrontMatterIsTakenOut() throws Exception {
    write("input/translations/ru/pagecontent/index.md", "---\ntitle: x\n---\n# Главная страница");
    regenerate();
    assertEquals("\n# Главная страница", read("temp/_includes/ru/index.md"));
  }

  @Test
  void aLangFragmentIsProcessedAsInTheBuild() throws Exception {
    // the build processes it while it builds the database (and then with the language of the page, which is none)
    write("input/pagecontent/index.md", "{% lang-fragment ip-statements.xhtml %}");
    regenerate();
    assertEquals("{% include ip-statements-en.xhtml %}", read("temp/_includes/uz/index.md"));
    assertEquals("{% include ip-statements-en.xhtml %}", read("temp/_includes/index.md"));
    assertEquals(Collections.emptySet(), pf.pagesWithCustomContent);

    pf.generatingDatabase = false;
    write("input/pagecontent/index.md", "{% lang-fragment ip-statements.xhtml %} ");
    regenerate();
    assertEquals("{% lang-fragment ip-statements.xhtml %} ", read("temp/_includes/index.md"));
  }

  @Test
  void notIfTheBuildWroteThePageElsewhere() {
    page.getOutputNames().remove(temp("_includes/uz/index.md"));
    assertThrows(IOException.class, this::regenerate);
  }

  @Test
  void notIfTheBuildDidntRecordItsOutput() {
    pf.allOutputs.remove(temp("_includes/ru/index.md").toLowerCase());
    assertThrows(IOException.class, this::regenerate);
  }

  @Test
  void notIfThePageCantBeWritten() throws Exception {
    // the last file the page is written to
    Files.delete(dir.resolve("temp/_includes/index.md"));
    Files.createDirectories(dir.resolve("temp/_includes/index.md"));
    write("input/pagecontent/index.md", "# Home, edited");
    IOException e = assertThrows(IOException.class, () -> gen.regeneratePages(Collections.singletonList(page)));
    assertTrue(e.getMessage().contains("could not be written to " + temp("_includes/index.md")), e.getMessage());
    assertTrue(log.get(0).startsWith("Exception generating page " + temp("_includes/index.md")), log.toString());
  }

  @Test
  void aPageThatIsntAnInclude() throws Exception {
    // written to the language's folder (as the pages of a path-pages folder are)
    page.setRelativePath("index.md");
    pf.allOutputs.clear();
    page.getOutputNames().clear();
    for (String s : Arrays.asList(temp("en/index.md"), temp("ru/index.md"), temp("uz/index.md"), temp("index.md"))) {
      pf.allOutputs.add(s.toLowerCase());
      page.getOutputNames().add(s);
    }
    write("temp/en/index.md", "# Home");
    write("temp/ru/index.md", "# Главная");
    write("temp/uz/index.md", "# Home");
    write("temp/index.md", "# Home");
    write("input/translations/ru/pagecontent/index.md", "# Главная страница");
    assertEquals(Collections.singletonList(temp("ru/index.md")), regenerate());
    assertEquals("# Главная страница", read("temp/ru/index.md"));
  }

  @Test
  void anEditToATranslationThroughTheRebuild() throws Exception {
    // what the watch session does with it: find the page, write it again, and put the temp folder back for Jekyll
    page.setPath(dir.resolve("input/pagecontent/index.md").toFile().getCanonicalPath());
    pf.altMap.put("page/" + page.getPath(), page);
    pf.fileList.add(page);
    PageRebuild state = new PageRebuild();
    state.setPf(pf);
    state.setPageLanguages(Arrays.asList("ru", "uz"));
    try {
      state.beforeJekyll();
      state.afterJekyll();
      state.afterPackage();
      state.finished();
      List<Path> roots = Collections.singletonList(dir.resolve("input"));
      SourceSnapshot before = SourceSnapshot.take(roots, null);
      write("input/translations/ru/pagecontent/index.md", "# Главная страница");
      List<String> reason = new ArrayList<>();
      List<FetchedFile> pages = state.pagesFor(SourceSnapshot.take(roots, before).changesSince(before), reason);
      assertEquals(Collections.singletonList(page), pages, reason.toString());
      List<String> written = gen.regeneratePages(pages);
      assertEquals(Collections.singletonList(temp("_includes/ru/index.md")), written);
      // the other files the page is written to are as they were
      state.prepareTemp(written);
      assertEquals("# Главная страница", read("temp/_includes/ru/index.md"));
    } finally {
      state.discard();
    }
  }

  /** the rebuild state that the build sets up, from what it did */
  private PageRebuild checked() throws IOException {
    PageRebuild state = new PageRebuild();
    state.setPf(pf);
    gen.setPageRebuild(state);
    pf.changeList.add(page);
    gen.checkPageRebuild();
    return state;
  }

  @Test
  void theBuildTellsTheRebuildTheLanguagesOfThePages() throws Exception {
    page.setPath(dir.resolve("input/pagecontent/index.md").toFile().getCanonicalPath());
    pf.altMap.put("page/" + page.getPath(), page);
    pf.fileList.add(page);
    PageRebuild state = checked();
    try {
      state.beforeJekyll();
      state.afterJekyll();
      state.afterPackage();
      state.finished();
      assertEquals(null, state.getNotReusable());
      List<Path> roots = Collections.singletonList(dir.resolve("input"));
      SourceSnapshot before = SourceSnapshot.take(roots, null);
      write("input/translations/ru/pagecontent/index.md", "# Главная страница");
      List<String> reason = new ArrayList<>();
      assertEquals(Collections.singletonList(page), state.pagesFor(SourceSnapshot.take(roots, before).changesSince(before), reason), reason.toString());
    } finally {
      state.discard();
    }
  }

  @Test
  void notForAnIgTranslatedTheOldWay() throws Exception {
    gen = new PublisherGenerator(new PublisherSettings());
    gen.setPf(pf);
    pf.hasTranslations = true;
    assertEquals("the IG is translated, but not in the multi-language template format", checked().getNotReusable());
  }

  @Test
  void anIgThatIsntTranslated() throws Exception {
    PublisherSettings settings = new PublisherSettings();
    gen = new PublisherGenerator(settings);
    gen.setPf(pf);
    pf.allOutputs.clear();
    page.getOutputNames().clear();
    pf.allOutputs.add(temp("_includes/index.md").toLowerCase());
    page.getOutputNames().add(temp("_includes/index.md"));
    write("input/pagecontent/index.md", "# Home, edited");
    assertEquals(Collections.singletonList(temp("_includes/index.md")), gen.regeneratePages(Collections.singletonList(page)));
    assertEquals("# Home, edited", read("temp/_includes/index.md"));
    assertEquals(new ArrayList<>(Collections.singletonList(temp("_includes/index.md").toLowerCase())), new ArrayList<>(pf.allOutputs));
  }
}
