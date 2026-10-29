package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Which template scripts stop a watch mode rebuild from treating a page edit as a page-only change: all of them, or
 * those in the folders the scripts read
 */
class TemplateScriptPageUseTest {

  @TempDir
  Path dir;

  private static Set<String> folders(String src) {
    return Template.scriptFolderUse("onGenerate.xml", src).keySet();
  }

  @Test
  void aScriptThatOnlyReadsGeneratedContentIsFine() {
    String src = "<project><target name=\"onGenerate\"><copy todir=\"${ig.temp}/_data\"><fileset dir=\"${ig.temp}/_includes\"/></copy></target></project>";
    assertNull(Template.scriptPageUse("onGenerate.xml", src));
    assertTrue(folders(src).isEmpty());
  }

  @Test
  void definingAPropertyFromTheInputFolderIsFine() {
    String src = "<project><property name=\"in\" value=\"${folder.input}\"/><target name=\"onGenerate\"/></project>";
    assertNull(Template.scriptPageUse("onGenerate.xml", src));
    assertTrue(folders(src).isEmpty());
  }

  @Test
  void aScriptThatNamesAPageFolderReadsIt() {
    assertNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/pagecontent\"/>"));
    assertEquals(Set.of("pagecontent"), folders("<fileset dir=\"input/pagecontent\"/>"));
    assertEquals(Set.of("intro-notes"), folders("<fileset dir=\"${x}/intro-notes\"/>"));
    assertEquals(Set.of("translations"), folders("<fileset dir=\"input/translations\"/>"));
  }

  @Test
  void aScriptThatReadsTheInputFolderBlocksAll() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${folder.input}\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"${dir.input}/images/logo.png\" todir=\"x\"/>"));
    assertEquals(Set.of("images"), folders("<copy file=\"${dir.input}/images/logo.png\" todir=\"x\"/>"));
    assertEquals(Set.of("data"), folders("<copy file=\"${dir.input}/data/x.json\" todir=\"x\"/>"));
  }

  @Test
  void aScriptThatReadsTheInputFolderFromTheRoot() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.root}/input\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${basedir}/input/includes\"/>"));
    assertEquals(Set.of("includes"), folders("<fileset dir=\"${basedir}/input/includes\"/>"));
    String cache = "<property name=\"x\" value=\"${ig.root}/input-cache/schemas\"/><copy todir=\"${ig.root}/input-cache\"/>";
    assertNull(Template.scriptPageUse("onGenerate.xml", cache));
    assertTrue(folders(cache).isEmpty());
  }

  @Test
  void aScriptThatReadsTheInputFolderThroughAProperty() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml",
        "<project><property name=\"in\" value=\"${folder.input}\"/><fileset dir=\"${in}\"/></project>"));
    String menu = "<project><property name=\"in\" location=\"${ig.root}/input\"/><copy file=\"${in}/includes/menu.xml\" todir=\"x\"/></project>";
    assertNull(Template.scriptPageUse("onGenerate.xml", menu));
    assertEquals(Set.of("includes"), folders(menu));
    String data = "<project><property name=\"in\" value=\"${folder.input}\"/><copy file=\"${in}/data/x.json\" todir=\"x\"/></project>";
    assertNull(Template.scriptPageUse("onGenerate.xml", data));
    assertEquals(Set.of("data"), folders(data));
  }

  @Test
  void dotsAreTheInputFolderOrTheIgsFolder() {
    for (String src : new String[] {"<fileset dir=\"${dir.input}/.\"/>", "<fileset dir=\"${dir.input}/../pages\"/>",
        "<fileset dir=\"${ig.root}/input/..\"/>", "<fileset dir=\"input/../pages\"/>", "<property name=\"r\" value=\"input/..\"/>",
        "<copy file=\"${dir.input}/includes/../images/logo.png\" todir=\"x\"/>", "<fileset dir=\"input/includes/./..\"/>"}) {
      assertNotNull(Template.scriptPageUse("onGenerate.xml", src), src);
    }
    assertTrue(folders("<fileset dir=\"${dir.input}/.\"/>").isEmpty());
    // through a property defined as a folder in the input folder
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"inc\" value=\"${dir.input}/includes\"/><fileset dir=\"${inc}/..\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"inc\" location=\"input/includes\"/><copy file=\"${inc}/../pagecontent/a.md\" todir=\"x\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"inc\" value=\"${dir.input}/includes\"/><fileset dir=\"${inc}/menus\"/>"));
  }

  @Test
  void theFolderEndsAtAQuoteOrABracket() {
    // the base template's onLoad transform names the folders it adds to the IG
    assertEquals(Set.of("images"), folders("<xsl:with-param name=\"value\" select=\"'input/images'\"/>"));
    assertEquals(Set.of("data"), folders("<available file=\"${basedir}/input/data\" type=\"dir\"/>(input/data)"));
  }

  @Test
  void aFolderThatCantBeToldBlocksAll() {
    // a property or a wildcard could be any folder
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${dir.input}/${folder.pages}\"/>"));
    assertTrue(folders("<fileset dir=\"${dir.input}/${folder.pages}\"/>").isEmpty());
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${dir.input}/inc*\"/>"));
    assertTrue(folders("<fileset dir=\"${dir.input}/inc*\"/>").isEmpty());
    assertNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"${dir.input}/data/x.json;${dir.input}/data/y.json\" todir=\"x\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${dir.input}/incl?des\"/>"));
    assertTrue(folders("<fileset dir=\"${dir.input}/incl?des\"/>").isEmpty());
    assertEquals("the template's script onGenerate.xml reads input/inc*", Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/inc*\"/>"));
    assertTrue(folders("<fileset dir=\"input/inc*\"/>").isEmpty());
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/incl?des\"/>"));
  }

  @Test
  void theInputFolderByItsPath() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\".\" includes=\"input/**\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/${x}\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input-cache\"/>"));
    // the base template defines it
    String base = "<property name=\"folder.input\" value=\"input\"/><copy file=\"${folder.input}/data/x.json\" todir=\"x\"/>";
    assertNull(Template.scriptPageUse("onGenerate.xml", base));
    assertEquals(Set.of("data"), folders(base));
    String in = "<property name=\"in\" value=\"input\"/><copy file=\"${in}/includes/menu.xml\" todir=\"x\"/>";
    assertNull(Template.scriptPageUse("onGenerate.xml", in));
    assertEquals(Set.of("includes"), folders(in));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"in\" value=\"input\"/><fileset dir=\"${in}\"/>"));
  }

  private Template template(String targets) throws IOException {
    write("template/config.json", "{\"script\": \"scripts/ant.xml\", \"targets\": {" + targets + "}}");
    write("template/scripts/ant.xml", "<project name=\"t\"><target name=\"onGenerate\"><copy file=\"${dir.input}/includes/menu.xml\" todir=\"x\"/></target>"
        + "<target name=\"onJekyll\"/></project>");
    return new Template(dir.toString(), true, null, null, false, false, false);
  }

  private void write(String path, String content) throws IOException {
    Path p = dir.resolve(path);
    Files.createDirectories(p.getParent());
    Files.write(p, content.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void theScriptsInFoldersAndTheAntFileWhereverItIs() throws IOException {
    write("template/config.json", "{\"script\": \"build/ant.xml\", \"targets\": {\"onGenerate\": \"onGenerate\"}}");
    write("template/build/ant.xml", "<project name=\"t\"><target name=\"onGenerate\"><import file=\"../scripts/more/x.xml\"/>"
        + "<copy file=\"${dir.input}/includes/menu.xml\" todir=\"x\"/></target></project>");
    write("template/scripts/more/x.xml", "<project name=\"x\"><copy file=\"${dir.input}/images/logo.png\" todir=\"x\"/></project>");
    Template t = new Template(dir.toString(), true, null, null, false, false, false);
    assertEquals(Set.of("includes", "images"), t.pageFolderBlockers().keySet());
    assertNull(t.pageOnlyRebuildBlocker());
    // an ant file that isn't read
    write("template/build/ant.xml", "<project name=\"t\"><import file=\"common.xml\"/><target name=\"onGenerate\"/></project>");
    write("template/build/common.xml", "<project name=\"c\"/>");
    t = new Template(dir.toString(), true, null, null, false, false, false);
    assertEquals("the template's script build/ant.xml runs common.xml, which isn't read", t.pageOnlyRebuildBlocker());
    // other builds, which aren't read either
    for (String run : new String[] {"<ant antfile=\"x.xml\"/>", "<ant dir=\"x\"/>", "<subant><fileset dir=\"x\"/></subant>",
        "<import resource=\"x.xml\"/>", "<exec executable=\"python\"><arg value=\"menu.py\"/></exec>", "<apply executable=\"x\"/>",
        "<taskdef name=\"x\" classname=\"y\"/>"}) {
      write("template/build/ant.xml", "<project name=\"t\"><target name=\"onGenerate\">" + run + "</target></project>");
      t = new Template(dir.toString(), true, null, null, false, false, false);
      assertTrue(t.pageOnlyRebuildBlocker().startsWith("the template's script build/ant.xml runs another build or a program"), run);
    }
  }

  @Test
  void theStyleSheetsAndScriptsThatTheAntFileUses() throws IOException {
    write("template/config.json", "{\"script\": \"scripts/ant.xml\", \"targets\": {\"onGenerate\": \"onGenerate\"}}");
    write("template/scripts/s.xslt", "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>");
    write("template/xslt/x.xslt", "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>");
    String[][] uses = {{"${ig.scripts}/s.xslt", null}, {"${ig.template}/xslt/x.xslt", "uses ${ig.template}/xslt/x.xslt"},
        {"${ig.scripts}/gone.xslt", "uses ${ig.scripts}/gone.xslt"},
        // a path in the IG's folder: nothing there reads nothing, but a file there isn't read
        {"{$ig.scripts}/s.xslt", null}, {"template/xslt/x.xslt", "uses template/xslt/x.xslt"}};
    for (String[] u : uses) {
      write("template/scripts/ant.xml", "<project name=\"t\"><target name=\"onGenerate\"><xslt in=\"a.xml\" out=\"b.xml\" style=\"" + u[0]
          + "\"/></target></project>");
      Template t = new Template(dir.toString(), true, null, null, false, false, false);
      assertEquals(u[1] == null ? null : "the template's script scripts/ant.xml " + u[1] + ", which isn't read", t.pageOnlyRebuildBlocker(), u[0]);
    }
  }

  @Test
  void aPathWithBackslashes() throws IOException {
    write("template/config.json", "{\"script\": \"ant.xml\", \"targets\": {\"onLoad\": \"onLoad\"}}");
    write("template/ant.xml", "<project name=\"t\"><target name=\"onLoad\"><copy file=\"input\\includes\\menu.xml\" todir=\"x\"/></target></project>");
    Template t = new Template(dir.toString(), true, null, null, false, false, false);
    assertNull(t.pageOnlyRebuildBlocker());
    assertEquals(Set.of("includes"), t.pageFolderBlockers().keySet());
  }

  @Test
  void theIgFolderByAnotherName() {
    // a property defined as the IG's folder
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"root\" value=\"${ig.root}\"/><fileset dir=\"${root}\" includes=\"**/*.md\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"root\" location=\".\"/><property name=\"r\" value=\"${root}/\"/>"
        + "<fileset dir=\"${r}\"/>"));
    String menu = "<property name=\"root\" value=\"${ig.root}\"/><copy file=\"${root}/input/includes/menu.xml\" todir=\"x\"/>";
    assertNull(Template.scriptPageUse("onGenerate.xml", menu));
    assertEquals(Set.of("includes"), folders(menu));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"root\" value=\"${ig.root}\"/><fileset dir=\"${root}/input\"/>"));
    // the template's folder is in the IG's folder
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.template}/..\" includes=\"**/*.md\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"${ig.scripts}/../../input/includes/menu.xml\" todir=\"x\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"${ig.scripts}/..x/menu.xml\" todir=\"x\"/>"));
  }

  @Test
  void twoScriptsWithTheSameFileName() throws IOException {
    write("template/config.json", "{\"script\": \"ant.xml\", \"targets\": {\"onGenerate\": \"onGenerate\"}}");
    write("template/ant.xml", "<project name=\"t\"><target name=\"onGenerate\"><copy file=\"${dir.input}/includes/menu.xml\" todir=\"x\"/></target></project>");
    write("template/scripts/ant.xml", "<project name=\"x\"/>");
    Template t = new Template(dir.toString(), true, null, null, false, false, false);
    assertEquals(Map.of("includes", "the template's script ant.xml reads (${dir.input}/includes)"), t.pageFolderBlockers());
  }

  @Test
  void aPropertyDefinedInAnotherScript() throws IOException {
    write("template/config.json", "{\"script\": \"scripts/ant.xml\", \"targets\": {\"onGenerate\": \"onGenerate\"}}");
    write("template/scripts/ant.xml", "<project name=\"t\"><property name=\"in\" value=\"${folder.input}\"/><import file=\"x.xml\"/>"
        + "<target name=\"onGenerate\"/></project>");
    write("template/scripts/x.xml", "<project name=\"x\"><target name=\"x\"><copy file=\"${in}/includes/menu.xml\" todir=\"x\"/></target></project>");
    Template t = new Template(dir.toString(), true, null, null, false, false, false);
    assertNull(t.pageOnlyRebuildBlocker());
    assertEquals(Map.of("includes", "the template's script scripts/x.xml reads (${in}/includes)"), t.pageFolderBlockers());
    write("template/scripts/x.xml", "<project name=\"x\"><target name=\"x\"><fileset dir=\"${in}\"/></target></project>");
    t = new Template(dir.toString(), true, null, null, false, false, false);
    assertEquals("the template's script scripts/x.xml reads ${in}", t.pageOnlyRebuildBlocker());
  }

  @Test
  void theAntFileWithoutAFolderOfScripts() throws IOException {
    write("template/config.json", "{\"script\": \"ant.xml\", \"targets\": {\"onLoad\": \"onLoad\"}}");
    write("template/ant.xml", "<project name=\"t\"><target name=\"onLoad\"><copy file=\"${dir.input}/includes/menu.xml\" todir=\"x\"/></target></project>");
    Template t = new Template(dir.toString(), true, null, null, false, false, false);
    assertNull(t.pageOnlyRebuildBlocker());
    assertEquals(Map.of("includes", "the template's script ant.xml reads (${dir.input}/includes)"), t.pageFolderBlockers());
  }

  @Test
  void theFoldersTheTemplatesScriptsRead() throws IOException {
    Template t = template("\"onGenerate\": \"onGenerate\"");
    assertNull(t.pageOnlyRebuildBlocker());
    assertEquals(Map.of("includes", "the template's script scripts/ant.xml reads (${dir.input}/includes)"), t.pageFolderBlockers());
    // onLoad runs before Jekyll too
    t = template("\"onLoad\": \"onGenerate\"");
    assertNull(t.pageOnlyRebuildBlocker());
    assertEquals(Map.of("includes", "the template's script scripts/ant.xml reads (${dir.input}/includes)"), t.pageFolderBlockers());
    // nothing runs before Jekyll
    t = template("\"onCheck\": \"onGenerate\"");
    assertNull(t.pageOnlyRebuildBlocker());
    assertTrue(t.pageFolderBlockers().isEmpty());
    // something runs just before Jekyll: that blocks all
    t = template("\"onGenerate\": \"onGenerate\", \"onJekyll\": \"onJekyll\"");
    assertNotNull(t.pageOnlyRebuildBlocker());
    assertTrue(t.pageFolderBlockers().isEmpty());
  }

  @Test
  void howTheScriptReadsTheFolder() {
    // the translated base template reads the menu for its QA check, and its own translations
    Map<String, String> use = Template.scriptFolderUse("ant.xml",
        "<property name=\"dir.input\" value=\"${ig.root}/${folder.input}\"/>"
            + "<xslt in=\"${dir.input}/includes/menu.xml\" out=\"${ig.template}/x.json\"/>"
            + "<property name=\"dir.template.translations\" value=\"${ig.template}/translations\"/>");
    assertEquals(Set.of("includes"), use.keySet());
    assertEquals("the template's script ant.xml reads (${dir.input}/includes)", use.get("includes"));
  }

  @Test
  void aScriptThatReadsEverythingUnderTheRootBlocksAll() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.root}\" includes=\"**/*.md\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${basedir}\"><include name=\"**/*.md\"/></fileset>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<copy todir=\"x\"><fileset dir=\"${ig.root}\"/></copy>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"x\" todir=\"${ig.root}\"/>"));
    // a wildcard first folder may be the input folder
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.root}\"><include name=\"*/includes/menu.xml\"/></fileset>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.root}\" includes=\"a.xml, in?ut/includes/menu.xml\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.root}\"><include name=\"${dir.temp.includes}/*.svg\"/></fileset>"));
    // an xslt task's basedir is a folder it reads
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<xslt basedir=\"${ig.root}\" destdir=\"x\" style=\"s.xslt\"/>"));
    String menu = "<xslt basedir=\"${ig.root}\" destdir=\"x\" style=\"s.xslt\"><include name=\"input/includes/menu.xml\"/></xslt>";
    assertNull(Template.scriptPageUse("onGenerate.xml", menu));
    assertEquals(Set.of("includes"), folders(menu));
    // but not the project's
    assertNull(Template.scriptPageUse("onGenerate.xml", "<project name=\"t\" basedir=\".\"><target name=\"x\"/></project>"));
    // the scripts run in the IG's folder
    for (String d : new String[] {".", "./", "", "${ig.root}/.", "${basedir}/./"}) {
      assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"" + d + "\" includes=\"**/*.md\"/>"), d);
    }
    // the base template's spreadsheet search
    assertNull(Template.scriptPageUse("onLoad.xml",
        "<fileset id=\"s\" dir=\"${ig.root}\"><include name=\"${folder.input}/examples/*spreadsheet.xml\"/></fileset>"));
  }
}
