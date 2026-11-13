package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IncrementalJekyllTest {

  @TempDir
  Path temp;

  private final IncrementalJekyll jekyll = new IncrementalJekyll();
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
    assertEquals("jekyll's record", Files.readString(temp.resolve(IncrementalJekyll.METADATA)));
  }

  @Test
  void jekyllsRecordIsKeptOutOfTheWayBetweenRuns() throws IOException {
    ranOnce();
    // the build deletes the files at the top of the temp folder that it didn't write
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
    assertNull(jekyll.prepare(temp));
    assertTrue(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
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
    assertFalse(Files.exists(temp.resolve(IncrementalJekyll.METADATA)));
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
  void configThatLoadsPluginsMakesEveryRunComplete() throws IOException {
    write("_config.yml", "plugins:\n  - jekyll-sitemap\n");
    ranOnce();
    assertEquals("_config.yml has settings that Jekyll's incremental build doesn't follow", jekyll.prepare(temp));
  }

  @Test
  void collectionsMakeEveryRunComplete() throws IOException {
    write("_config.yml", "collections:\n  rapido:\n    output: true\n");
    ranOnce();
    assertEquals("_config.yml has settings that Jekyll's incremental build doesn't follow", jekyll.prepare(temp));
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
    // a failure between runs (the build failed before Jekyll, say) forgets what Jekyll kept
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
