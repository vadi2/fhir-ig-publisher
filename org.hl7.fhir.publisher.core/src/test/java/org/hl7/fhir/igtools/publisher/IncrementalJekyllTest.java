package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IncrementalJekyllTest {

  @TempDir
  Path temp;

  private final Map<String, String> env = new HashMap<>();
  private final IncrementalJekyll jekyll = new IncrementalJekyll(env::get);
  private static final FileTime OLD = FileTime.from(Instant.parse("2026-01-01T00:00:00Z"));

  @BeforeEach
  void site() throws IOException {
    write("_config.yml", "kramdown:\n  toc_levels: 1..3\n");
    write("_data/pages.json", "{}");
    write("_includes/index.md", "the page");
    write("_includes/fragment.xhtml", "<div/>");
    write("index.html", "---\n---\n{% include index.md %}");
    write("_rapido/state.json", "1");
  }

  /** a first run that succeeded, as Jekyll leaves it */
  private void ranOnce() throws IOException {
    assertEquals("no earlier Jekyll run in this session", jekyll.prepare(temp));
    ran();
  }

  /** what Jekyll writes, then the run succeeded */
  private void ran() throws IOException {
    write(IncrementalJekyll.METADATA, "jekyll's record");
    write(IncrementalJekyll.SITE + "/index.html", "rendered");
    jekyll.succeeded(temp);
  }

  /** (Windows needs a privilege to make one) */
  private static void symlink(Path link, Path target) {
    try {
      Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | IOException e) {
      assumeTrue(false, "symbolic links not supported here: " + e);
    }
  }

  private Path write(String rel, String content) throws IOException {
    Path p = temp.resolve(rel);
    Files.createDirectories(p.getParent());
    Files.writeString(p, content);
    // as if written in an earlier build: a rewrite always gets a later time
    Files.setLastModifiedTime(p, OLD);
    return p;
  }

  private Path rewrite(String rel, String content) throws IOException {
    Path p = temp.resolve(rel);
    Files.createDirectories(p.getParent());
    Files.writeString(p, content);
    Files.setLastModifiedTime(p, FileTime.from(Instant.now()));
    return p;
  }

  @Test
  void theFirstRunRendersEveryPage() throws IOException {
    write(IncrementalJekyll.STATE + "/" + IncrementalJekyll.METADATA, "from another session");
    assertEquals("no earlier Jekyll run in this session", jekyll.prepare(temp));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void nothingChanged() throws IOException {
    ranOnce();
    assertNull(jekyll.prepare(temp));
    assertEquals(0, jekyll.getChanged());
    jekyll.beforeRun(temp);
    assertEquals("jekyll's record", Files.readString(temp.resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void jekyllsRecordIsKeptOutOfTheWayBetweenRuns() throws IOException {
    ranOnce();
    // the build deletes the files at the top of the temp folder that it didn't write
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    assertNull(jekyll.prepare(temp));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    jekyll.beforeRun(temp);
    assertTrue(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void withoutPrepareJekyllRendersEveryPage() throws IOException {
    ranOnce();
    jekyll.beforeRun(temp);
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void aCompleteRunDeletesJekyllsRecord() throws IOException {
    ranOnce();
    rewrite("_data/pages.json", "{\"a\":1}");
    assertEquals("changed: _data/pages.json", jekyll.prepare(temp));
    jekyll.beforeRun(temp);
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void anIncludeChanged() throws IOException {
    ranOnce();
    rewrite("_includes/index.md", "the page, edited");
    rewrite("_includes/new.md", "new");
    Files.delete(temp.resolve("_includes/fragment.xhtml"));
    assertNull(jekyll.prepare(temp));
    assertEquals(3, jekyll.getChanged());
  }

  @Test
  void aFileWrittenAgainWithTheSameContentKeepsItsTime() throws IOException {
    ranOnce();
    Path page = rewrite("index.html", "---\n---\n{% include index.md %}");
    Path data = rewrite("_data/pages.json", "{}");
    assertNull(jekyll.prepare(temp));
    assertEquals(0, jekyll.getChanged());
    assertEquals(OLD, Files.getLastModifiedTime(page));
    assertEquals(OLD, Files.getLastModifiedTime(data));
  }

  @Test
  void aChangedFileKeepsItsNewTime() throws IOException {
    ranOnce();
    Path include = rewrite("_includes/index.md", "the page, edited");
    assertNull(jekyll.prepare(temp));
    assertNotEquals(OLD, Files.getLastModifiedTime(include));
    // and the next run compares with it
    ran();
    assertNull(jekyll.prepare(temp));
    assertEquals(0, jekyll.getChanged());
  }

  @Test
  void aDataFileChanged() throws IOException {
    ranOnce();
    rewrite("_data/pages.json", "{\"a\":1}");
    assertEquals("changed: _data/pages.json", jekyll.prepare(temp));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void theConfigChanged() throws IOException {
    ranOnce();
    rewrite("_config.yml", "kramdown:\n  toc_levels: 1..4\n");
    assertEquals("changed: _config.yml", jekyll.prepare(temp));
  }

  @Test
  void aPageChangedWasAddedOrDeleted() throws IOException {
    ranOnce();
    rewrite("index.html", "---\n---\nother");
    assertEquals("changed: index.html", jekyll.prepare(temp));
    ran();
    rewrite("en/new.html", "---\n---\n");
    assertEquals("added: en/new.html", jekyll.prepare(temp));
    ran();
    Files.delete(temp.resolve("en/new.html"));
    assertEquals("deleted: en/new.html", jekyll.prepare(temp));
  }

  @Test
  void foldersThatJekyllDoesntReadAreNotFollowed() throws IOException {
    ranOnce();
    rewrite("_rapido/state.json", "2");
    rewrite(".jekyll-cache/x", "cache");
    rewrite(PublisherIGLoader.WATCH_OUTPUT_STAGING + "/index.html", "staged");
    rewrite(IncrementalJekyll.SITE + "/index.html", "rendered again");
    assertNull(jekyll.prepare(temp));
    assertEquals(0, jekyll.getChanged());
  }

  @Test
  void pluginsMakeEveryRunComplete() throws IOException {
    ranOnce();
    write("_plugins/p.rb", "");
    assertEquals("the site can have plugins", jekyll.prepare(temp));
  }

  @Test
  void aGemfileMakesEveryRunComplete() throws IOException {
    ranOnce();
    write("Gemfile", "source 'https://rubygems.org'");
    assertEquals("the site can have plugins", jekyll.prepare(temp));
  }

  @Test
  void bundlerMakesEveryRunComplete() throws IOException {
    ranOnce();
    env.put("BUNDLE_GEMFILE", "/somewhere/Gemfile");
    assertEquals("the site can have plugins", jekyll.prepare(temp));
  }

  @Test
  void onlyConfigThatJekyllsIncrementalBuildFollows() throws IOException {
    assertNull(IncrementalJekyll.unknownConfig(temp));
    write("_config.yml", "---\n# the template's\nkramdown:\n  toc_levels: 1..3\n\nexclude: [x]\r\ntitle: An IG\n");
    assertNull(IncrementalJekyll.unknownConfig(temp));
    for (String config : new String[] {"plugins:\n  - jekyll-sitemap\n", "theme: minima\n", "collections:\n  rapido:\n    output: true\n",
        "keep_files: [.git, extra]\n", "includes_dir: x\n", "gems: [x]\n", "\"plugins\": [x]\n", "'theme': x\n", "\uFEFFplugins: [x]\n",
        "{plugins: [x]}\n", "kramdown:\n  toc_levels: 1..3\nplugins_dir: x\n", "  plugins: [x]\n  title: y\n",
        "---\n  plugins: [x]\n", "kramdown:\n  x: 1\rplugins: [x]\n", "kramdown:\n  x: 1\u0085plugins: [x]\n", "kramdown:\n  x: 1\nplugins:\n  - x\n"}) {
      write("_config.yml", config);
      assertNotNull(IncrementalJekyll.unknownConfig(temp), config);
    }
  }

  @Test
  void theConfigCanBeYamlButNotToml() throws IOException {
    Files.delete(temp.resolve("_config.yml"));
    write("_config.yaml", "theme: minima\n");
    assertEquals("_config.yaml: theme: minima", IncrementalJekyll.unknownConfig(temp));
    write("_config.yaml", "kramdown:\r\n  toc_levels: 1..3\r\n");
    assertNull(IncrementalJekyll.unknownConfig(temp));
    Files.delete(temp.resolve("_config.yaml"));
    write("_config.toml", "title = \"An IG\"\n");
    assertEquals("_config.toml", IncrementalJekyll.unknownConfig(temp));
  }

  @Test
  void somethingThatIsntAFileMakesTheRunComplete() throws IOException {
    ranOnce();
    symlink(temp.resolve("_includes/link.md"), temp.resolve("_includes/index.md"));
    assertTrue(jekyll.prepare(temp).startsWith("Jekyll's source folder has something that isn't a file or a folder"));
  }

  @Test
  void aListingThatStoppedEarlyIsntWhatTheNextRunComparesWith() throws IOException {
    ranOnce();
    symlink(temp.resolve("_includes/link.md"), temp.resolve("_includes/index.md"));
    assertNotNull(jekyll.prepare(temp));
    ran();
    Files.delete(temp.resolve("_includes/link.md"));
    assertEquals("no earlier Jekyll run in this session", jekyll.prepare(temp));
  }

  @Test
  void aFolderInTheStagingFolderThatIsNowAFile() throws IOException {
    Path site = temp.resolve("site");
    Path staging = temp.resolve("staging");
    Files.createDirectories(site);
    Files.writeString(site.resolve("x"), "a file now");
    Files.createDirectories(staging.resolve("x/y"));
    Files.writeString(staging.resolve("x/y/z.html"), "was in a folder");
    assertEquals(1, jekyll.copySite(site, staging));
    assertEquals("a file now", Files.readString(staging.resolve("x")));
  }

  @Test
  void theCopyKeepsTheTimeJekyllGaveTheFile() throws IOException {
    Path site = temp.resolve("site");
    Files.createDirectories(site);
    Files.writeString(site.resolve("logo.png"), "a static file");
    Files.setLastModifiedTime(site.resolve("logo.png"), OLD);
    Path staging = temp.resolve("staging");
    jekyll.copySite(site, staging);
    assertEquals(OLD, Files.getLastModifiedTime(staging.resolve("logo.png")));
  }

  @Test
  void whenJekyllSucceeds() throws IOException {
    ranOnce();
    assertNull(jekyll.prepare(temp));
    jekyll.beforeRun(temp);
    Path staging = temp.resolve(PublisherIGLoader.WATCH_OUTPUT_STAGING);
    assertEquals(1, jekyll.finished(temp, true, staging));
    assertEquals("rendered", Files.readString(staging.resolve("index.html")));
    assertTrue(Files.exists(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA)));
    assertNull(jekyll.prepare(temp));
  }

  @Test
  void whenJekyllReturnsAnErrorButTheBuildGoesOn() throws IOException {
    ranOnce();
    assertNull(jekyll.prepare(temp));
    jekyll.beforeRun(temp);
    Path staging = temp.resolve(PublisherIGLoader.WATCH_OUTPUT_STAGING);
    assertEquals(1, jekyll.finished(temp, false, staging));
    assertEquals("rendered", Files.readString(staging.resolve("index.html")));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA)));
    assertEquals("no earlier Jekyll run in this session", jekyll.prepare(temp));
  }

  @Test
  void withoutJekyllsRecordOrOutputEveryPageIsRendered() throws IOException {
    ranOnce();
    Files.delete(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA));
    assertEquals("no " + IncrementalJekyll.METADATA, jekyll.prepare(temp));
    ran();
    Files.delete(temp.resolve(IncrementalJekyll.SITE + "/index.html"));
    Files.delete(temp.resolve(IncrementalJekyll.SITE));
    assertEquals("no " + IncrementalJekyll.SITE, jekyll.prepare(temp));
  }

  @Test
  void afterAFailureEveryPageIsRendered() throws IOException {
    ranOnce();
    assertNull(jekyll.prepare(temp));
    jekyll.failed(temp);
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    assertEquals("no earlier Jekyll run in this session", jekyll.prepare(temp));
    ran();
    assertNull(jekyll.prepare(temp));
    ran();
    // what an earlier run kept goes too
    jekyll.failed(temp);
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.STATE).resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void theStagingFolderGetsWhatJekyllWrote() throws IOException {
    Path site = temp.resolve("site");
    Path staging = temp.resolve("staging");
    Files.createDirectories(site.resolve("en"));
    Files.writeString(site.resolve("index.html"), "jekyll index");
    Files.writeString(site.resolve("en/a.html"), "jekyll a");
    Files.writeString(site.resolve("a.json"), "{}");
    Files.createDirectories(staging.resolve("old/deeper"));
    Files.writeString(staging.resolve("old/deeper/x.html"), "left by the last build");
    Files.writeString(staging.resolve("qa.html"), "written after Jekyll by the last build");
    Files.createDirectories(staging.resolve(".git"));
    Files.writeString(staging.resolve(".git/HEAD"), "kept by Jekyll");

    assertEquals(3, jekyll.copySite(site, staging));
    assertEquals("jekyll index", Files.readString(staging.resolve("index.html")));
    assertEquals("jekyll a", Files.readString(staging.resolve("en/a.html")));
    assertFalse(Files.exists(staging.resolve("qa.html")));
    assertFalse(Files.exists(staging.resolve("old")));
    assertTrue(Files.exists(staging.resolve(".git/HEAD")));

    // the html check fills in a page, the sync moves one out, Jekyll renders one again
    Files.writeString(staging.resolve("index.html"), "jekyll index, with the publish box");
    Files.setLastModifiedTime(staging.resolve("index.html"), FileTime.from(Instant.now().plusSeconds(5)));
    Files.delete(staging.resolve("a.json"));
    Files.writeString(site.resolve("en/a.html"), "jekyll a, rendered again");
    Files.setLastModifiedTime(site.resolve("en/a.html"), FileTime.from(Instant.now().plusSeconds(5)));
    assertEquals(3, jekyll.copySite(site, staging));
    assertEquals("jekyll index", Files.readString(staging.resolve("index.html")));
    assertEquals("jekyll a, rendered again", Files.readString(staging.resolve("en/a.html")));
    assertEquals("{}", Files.readString(staging.resolve("a.json")));

    // nothing touched since
    assertEquals(0, jekyll.copySite(site, staging));
  }
}
