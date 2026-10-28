package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

// macOS uses the polling monitor
@DisabledOnOs(OS.MAC)
class FileChangeMonitorTest {

  @TempDir
  Path dir;

  private Path page;
  private Path fshGenerated;
  private FileChangeMonitor monitor;

  @BeforeEach
  void setUp() throws Exception {
    Files.writeString(dir.resolve("ig.ini"), "[IG]");
    Files.createDirectories(dir.resolve("input/pagecontent"));
    page = dir.resolve("input/pagecontent/index.md");
    Files.writeString(page, "one");
    fshGenerated = dir.resolve("fsh-generated");
    Files.createDirectories(fshGenerated.resolve("resources"));
    // as the publisher's watch mode does: some files in the IG folder, the input folder, and SUSHI's output
    monitor = new FileChangeMonitor(List.of(
        FileChangeMonitor.MonitoredPath.file(dir.resolve("ig.ini")),
        FileChangeMonitor.MonitoredPath.recursive(dir.resolve("input")),
        FileChangeMonitor.MonitoredPath.recursive(fshGenerated)));
  }

  @AfterEach
  void tearDown() throws Exception {
    monitor.close();
  }

  @Test
  void anEditInAWatchedFolderIsReportedWhenFilesAreWatchedToo() throws Exception {
    monitor.startMonitoring();
    Files.writeString(page, "two");

    FileChangeMonitor.FileChangeEvent event = monitor.waitForChange(10, TimeUnit.SECONDS);

    assertNotNull(event, "the edit to index.md wasn't reported");
    assertEquals(page.toAbsolutePath(), event.getPath());
  }

  @Test
  void otherFilesNextToAWatchedFileAreNotReported() throws Exception {
    monitor.startMonitoring();
    Files.writeString(dir.resolve("notes.txt"), "not watched");

    assertNull(monitor.waitForChange(2, TimeUnit.SECONDS));
  }

  @Test
  void changesAreReportedAfterMonitoringIsStartedAgain() throws Exception {
    monitor.close();
    // only the folder, so that this doesn't depend on the files being watched too
    monitor = new FileChangeMonitor(List.of(FileChangeMonitor.MonitoredPath.recursive(dir.resolve("input"))));
    monitor.startMonitoring();
    Files.writeString(page, "two");
    assertNotNull(monitor.waitForChange(10, TimeUnit.SECONDS));
    monitor.clearChanges();
    monitor.stopMonitoring();

    monitor.startMonitoring();
    Files.writeString(page, "three");

    assertNotNull(monitor.waitForChange(10, TimeUnit.SECONDS), "nothing was reported after the restart");
  }

  @Test
  void whatTheBuildWroteIsNotReported() throws Exception {
    // what SUSHI writes while the build runs, between the monitor being made (or stopped) and started
    Files.writeString(fshGenerated.resolve("resources/StructureDefinition-example.json"), "{}");
    Thread.sleep(500);

    monitor.startMonitoring(List.of(fshGenerated));

    assertNull(monitor.waitForChange(2, TimeUnit.SECONDS));
  }

  @Test
  void anEditMadeWhileABuildRanIsReportedWhenMonitoringStartsAgain() throws Exception {
    monitor.startMonitoring();
    monitor.stopMonitoring();
    // the build runs: SUSHI writes its output, and the user saves a page
    Files.writeString(fshGenerated.resolve("resources/StructureDefinition-example.json"), "{}");
    Files.writeString(page, "edited during the build");
    Thread.sleep(500);

    monitor.startMonitoring(List.of(fshGenerated));

    FileChangeMonitor.FileChangeEvent event = monitor.waitForChange(10, TimeUnit.SECONDS);
    assertNotNull(event, "the edit made during the build wasn't reported");
    assertEquals(page.toAbsolutePath(), event.getPath());
    for (FileChangeMonitor.FileChangeEvent other : monitor.getChanges()) {
      assertEquals(page.toAbsolutePath(), other.getPath());
    }
  }

  @Test
  void lostEventsAreReportedAsAChange() throws Exception {
    // more events than the watch service keeps for a folder: it then reports an overflow, and the change to
    // ig.ini is lost in it
    for (int i = 0; i < 600; i++) {
      Files.writeString(dir.resolve("other-" + i + ".txt"), "not watched");
    }
    Files.writeString(dir.resolve("ig.ini"), "[IG]\nchanged");
    Thread.sleep(500);

    monitor.startMonitoring();

    assertNotNull(monitor.waitForChange(10, TimeUnit.SECONDS), "the lost change wasn't reported");
  }
}
