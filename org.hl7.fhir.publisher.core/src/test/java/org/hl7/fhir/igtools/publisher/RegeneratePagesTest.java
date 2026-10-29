package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
