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
  private FileChangeMonitor monitor;

  @BeforeEach
  void setUp() throws Exception {
    Files.writeString(dir.resolve("ig.ini"), "[IG]");
    Files.createDirectories(dir.resolve("input/pagecontent"));
    page = dir.resolve("input/pagecontent/index.md");
    Files.writeString(page, "one");
    // as the publisher's watch mode does: some files in the IG folder, and the input folder
    monitor = new FileChangeMonitor(List.of(
        FileChangeMonitor.MonitoredPath.file(dir.resolve("ig.ini")),
        FileChangeMonitor.MonitoredPath.recursive(dir.resolve("input"))));
  }

  @AfterEach
  void tearDown() {
    monitor.stopMonitoring();
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
    monitor.stopMonitoring();
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
  void changesMadeWhileNotMonitoringAreNotReported() throws Exception {
    // what a build writes, between the monitor being made (or stopped) and started
    Files.writeString(page, "written by the build");
    Thread.sleep(500);

    monitor.startMonitoring();

    assertNull(monitor.waitForChange(2, TimeUnit.SECONDS));
  }
}
