package org.hl7.fhir.igtools.templates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Whether the template's check (onCheck) may look at the output: watch mode writes the output files the html check
 * changed again, as Jekyll wrote them, for a check that may
 */
class TemplateOnCheckOutputTest {

  @TempDir
  Path dir;

  /** the base templates' onCheck: it gathers the outcomes in the template folder, and nothing else */
  private static final String BASE_ON_CHECK = "<extension-point name=\"onCheck.extend\"/>"
      + "<target name=\"onCheck\" depends=\"onCheck.extend\">"
      + "<echo file=\"${ig.template}/onCheck-validation.json\" message=\"{&quot;resourceType&quot;:&quot;OperationOutcome&quot;,&quot;issue&quot;:[&#x0a;\"/>"
      + "<concat destfile=\"${ig.template}/onCheck-validation.json\" encoding=\"UTF-8\" outputencoding=\"UTF-8\" append=\"yes\">"
      + "<fileset dir=\"${ig.template}\" includes=\"oncheck-validation*.json\"/>"
      + "<fileset dir=\"${ig.template}\" includes=\"**/*.oncheck-validation.json\"/>"
      + "<filterchain><tailfilter lines=\"-1\" skip=\"0\"/></filterchain></concat>"
      + "<echo file=\"${ig.template}/onCheck-validation.json\" message=\"]}&#x0a;\" append=\"yes\"/>"
      + "<replaceregexp file=\"${ig.template}/onCheck-validation.json\" match=\",(\\s*\\]\\}\\s*)$\" replace=\"\\1\" flags=\"s\"/>"
      + "</target>";

  private void write(String path, String content) throws IOException {
    Path p = dir.resolve(path);
    Files.createDirectories(p.getParent());
    Files.write(p, content.getBytes(StandardCharsets.UTF_8));
  }

  private String why(String project) throws IOException {
    return why(project, dir.resolve("output").toString());
  }

  private String why(String project, String output) throws IOException {
    write("template/config.json", "{\"script\": \"scripts/ant.xml\", \"otherScripts\": [], \"targets\": {\"onGenerate\": \"onGenerate\", \"onCheck\": \"onCheck\"}}");
    write("template/scripts/ant.xml", "<project name=\"t\">" + project + "</project>");
    return new Template(dir.toString(), true, null, null, false, false, false).whyOnCheckMaySeeOutput(output);
  }

  @Test
  void theBaseTemplatesCheckDoesNot() throws IOException {
    // what the other targets do doesn't matter
    assertNull(why("<target name=\"onGenerate\"><copy todir=\"${ig.root}/output\"><fileset dir=\"${ig.root}/temp\"/></copy></target>" + BASE_ON_CHECK));
    // nor a target that extends another extension point
    assertNull(why("<extension-point name=\"onGenerate.extend\"/><target name=\"x\" extensionOf=\"onGenerate.extend\"><xslt in=\"a\" out=\"b\"/></target>" + BASE_ON_CHECK));
  }

  @Test
  void noCheckDoesNot() throws IOException {
    write("template/config.json", "{}");
    assertNull(new Template(dir.toString(), true, null, null, false, false, false).whyOnCheckMaySeeOutput(dir.resolve("output").toString()));
  }

  @Test
  void aTargetThatExtendsTheCheckMay() throws IOException {
    String why = why("<target name=\"qa\" extensionOf=\"onCheck.extend\"><copy todir=\"x\"><fileset dir=\"${ig.root}/output\"/></copy></target>" + BASE_ON_CHECK);
    assertTrue(why != null && why.contains("'qa'") && why.contains("copy"), why);
    // in a file the script imports
    write("template/scripts/qa.xml", "<project name=\"qa\"><target name=\"qa\" extensionOf=\"onCheck.extend\"><echo message=\"x\"/>"
        + "<concat destfile=\"${ig.template}/x.txt\"><fileset dir=\"${ig.root}/output\" includes=\"*.html\"/></concat></target></project>");
    why = why("<import file=\"qa.xml\"/>" + BASE_ON_CHECK);
    assertTrue(why != null && why.contains("output"), why);
    // a target the check depends on
    why = why("<target name=\"pre\"><xslt in=\"${ig.root}/output/index.html\" out=\"${ig.template}/x\"/></target>"
        + BASE_ON_CHECK.replace("depends=\"onCheck.extend\"", "depends=\"pre,onCheck.extend\""));
    assertTrue(why != null && why.contains("'pre'"), why);
  }

  @Test
  void aTaskOfTheTemplatesOwnUnderAnAntName() throws IOException {
    // defined when the script loads, Ant's own are put back after (Project.init()): the check runs Ant's
    write("output/index.html", "x");
    String macro = "<macrodef name=\"echo\"><attribute name=\"file\"/><attribute name=\"message\"/><sequential>"
        + "<copy file=\"${ig.root}/output/index.html\" tofile=\"${ig.root}/copied.html\"/></sequential></macrodef>";
    assertNull(why(macro + BASE_ON_CHECK));
    new Template(dir.toString(), true, null, null, false, false, false).onCheckEvent(null, null);
    assertFalse(Files.exists(dir.resolve("copied.html")));
    assertTrue(Files.exists(dir.resolve("template/onCheck-validation.json")));
    // defined by the check
    String why = why("<target name=\"defs\">" + macro + "</target>" + BASE_ON_CHECK.replace("depends=\"onCheck.extend\"", "depends=\"defs,onCheck.extend\""));
    assertTrue(why != null && why.contains("macrodef"), why);
  }

  @Test
  void filesOutsideTheTemplateFolder() throws IOException {
    for (String fs : new String[] {"<fileset dir=\"${ig.root}/output\"/>", "<fileset dir=\"output\"/>", "<fileset dir=\"${ig.template}/..\"/>",
        "<fileset dir=\"${ig.template}\" includes=\"../output/**\"/>", "<fileset dir=\"${ig.template}\" includes=\"a.json, /output/**\"/>",
        "<fileset dir=\"${no.such.property}\"/>", "<fileset dir=\"${ig.template}\"><include name=\"**\"/></fileset>",
        "<fileset dir=\"${ig.template}\" followsymlinks=\"true\"/>", "<path><pathelement location=\"${ig.root}/output\"/></path>"}) {
      String why = why("<target name=\"onCheck\"><concat destfile=\"${ig.template}/x.json\">" + fs + "</concat></target>");
      assertNotNull(why, fs);
    }
    assertNotNull(why("<target name=\"onCheck\"><replaceregexp file=\"${ig.root}/output/index.html\" match=\"a\" replace=\"b\"/></target>"));
    assertNotNull(why("<target name=\"onCheck\"><echo file=\"../x.txt\" message=\"a\"/></target>"));
    assertNull(why("<target name=\"onCheck\"><echo file=\"${ig.template}/sub/../x.txt\" message=\"a\"/></target>"));
  }

  @Test
  void theOutputInTheTemplateFolder() throws IOException {
    assertNotNull(why(BASE_ON_CHECK, dir.resolve("template/output").toString()));
  }

  @Test
  void aCheckThatCantBeOrdered() throws IOException {
    assertNotNull(why("<target name=\"onCheck\" depends=\"missing\"/>"));
  }
}
