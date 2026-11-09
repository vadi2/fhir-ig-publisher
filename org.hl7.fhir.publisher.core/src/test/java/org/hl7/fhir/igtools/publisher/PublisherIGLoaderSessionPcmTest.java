package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** In watch mode every build uses the same package cache manager, which reads the list of CI builds once an hour */
class PublisherIGLoaderSessionPcmTest {

  @TempDir
  Path dir;

  private PublisherIGLoader loader(boolean watch, String packagesFolder) throws Exception {
    Files.createDirectories(dir.resolve("packages"));
    PublisherSettings settings = new PublisherSettings();
    settings.setPackageCacheFolder(dir.resolve("packages").toString());
    settings.setPackagesFolder(packagesFolder);
    PublisherIGLoader loader = new PublisherIGLoader(settings);
    loader.setPf(new PublisherFields());
    if (watch) {
      loader.setWatchSession(new WatchSession(dir, Collections.singletonList(dir.resolve("input"))));
    }
    return loader;
  }

  @Test
  void watchBuildsShareThePackageCacheManager() throws Exception {
    PublisherIGLoader loader = loader(true, null);
    assertSame(loader.packageCacheManager(), loader.packageCacheManager());
  }

  @Test
  void withoutWatchEachBuildHasItsOwn() throws Exception {
    PublisherIGLoader loader = loader(false, null);
    assertNotSame(loader.packageCacheManager(), loader.packageCacheManager());
  }

  @Test
  void packagesLoadedFromAFolderInEachBuildGoIntoANewOne() throws Exception {
    PublisherIGLoader loader = loader(true, dir.resolve("local-packages").toString());
    assertNotSame(loader.packageCacheManager(), loader.packageCacheManager());
  }
}
