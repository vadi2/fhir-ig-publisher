package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/** SUSHI on a thread of its own: what it logs is logged once it has finished, and its failure is the build's */
class SushiAlongsideTest {

  private final List<String> logged = new ArrayList<>();

  @Test
  void whatItLoggedIsLoggedInOrderOnceItHasFinished() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    SushiAlongside s = new SushiAlongside(logger -> {
      logger.logMessage("Sushi: one");
      logger.logMessage("Sushi: two");
      assertTrue(release.await(30, TimeUnit.SECONDS));
      logger.logMessage("Sushi: three");
    });
    s.start();
    release.countDown();

    s.join(logged::add);

    assertEquals(List.of("Sushi: one", "Sushi: two", "Sushi: three"), logged);
  }

  @Test
  void itsExceptionIsThrown() {
    Exception failure = new java.io.IOException("Sushi failed with errors");
    SushiAlongside s = new SushiAlongside(logger -> {
      logger.logMessage("Sushi: error");
      throw failure;
    });
    s.start();

    assertSame(failure, assertThrows(Exception.class, () -> s.join(logged::add)));
    assertEquals(List.of("Sushi: error"), logged);
  }

  @Test
  void itsErrorIsThrown() {
    Error failure = new OutOfMemoryError();
    SushiAlongside s = new SushiAlongside(logger -> {
      throw failure;
    });
    s.start();

    assertSame(failure, assertThrows(Error.class, () -> s.join(logged::add)));
  }

  @Test
  void anInterruptedWaitStillWaitsForItToFinish() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    AtomicBoolean finished = new AtomicBoolean();
    SushiAlongside s = new SushiAlongside(logger -> {
      started.countDown();
      Thread.sleep(500);
      finished.set(true);
    });
    s.start();
    assertTrue(started.await(30, TimeUnit.SECONDS));

    Thread.currentThread().interrupt();
    try {
      s.join(logged::add);
      assertTrue(finished.get(), "it still writes fsh-generated until it has finished");
      assertTrue(Thread.currentThread().isInterrupted(), "the interrupt is kept");
    } finally {
      Thread.interrupted();
    }
  }
}
