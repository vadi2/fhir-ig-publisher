package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Which template scripts stop a watch mode rebuild from treating a page edit as a page-only change: all of them, or
 * those in the folders the scripts read
 */
class TemplateScriptPageUseTest {

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
    // the base template's spreadsheet search
    assertNull(Template.scriptPageUse("onLoad.xml",
        "<fileset id=\"s\" dir=\"${ig.root}\"><include name=\"${folder.input}/examples/*spreadsheet.xml\"/></fileset>"));
  }
}
