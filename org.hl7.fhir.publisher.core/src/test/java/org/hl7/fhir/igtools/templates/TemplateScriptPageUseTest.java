package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Which template scripts stop a watch mode rebuild from treating a page edit as a page-only change */
class TemplateScriptPageUseTest {

  @Test
  void aScriptThatOnlyReadsGeneratedContentIsFine() {
    assertNull(Template.scriptPageUse("onGenerate.xml",
        "<project><target name=\"onGenerate\"><copy todir=\"${ig.temp}/_data\"><fileset dir=\"${ig.temp}/_includes\"/></copy></target></project>"));
  }

  @Test
  void definingAPropertyFromTheInputFolderIsFine() {
    assertNull(Template.scriptPageUse("onGenerate.xml",
        "<project><property name=\"in\" value=\"${folder.input}\"/><target name=\"onGenerate\"/></project>"));
  }

  @Test
  void aScriptThatNamesAPageFolderIsNot() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/pagecontent\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"input/intro-notes\"/>"));
  }

  @Test
  void aScriptThatReadsTheInputFolderIsNot() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${folder.input}\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"${dir.input}/images/logo.png\" todir=\"x\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<copy file=\"${dir.input}/data/x.json\" todir=\"x\"/>"));
  }

  @Test
  void aScriptThatReadsTheInputFolderFromTheRootIsNot() {
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${ig.root}/input\"/>"));
    assertNotNull(Template.scriptPageUse("onGenerate.xml", "<fileset dir=\"${basedir}/input/includes\"/>"));
    assertNull(Template.scriptPageUse("onGenerate.xml", "<property name=\"x\" value=\"${ig.root}/input-cache/schemas\"/><copy todir=\"${ig.root}/input-cache\"/>"));
  }
}
