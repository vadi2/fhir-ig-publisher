package org.hl7.fhir.igtools.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hl7.fhir.utilities.logging.ILoggingService;

/**
 * SUSHI, run on a thread of its own while the start-up of a watch build goes on (see PublisherIGLoader). What it logs is
 * kept, and logged by the start-up's thread once it has waited for SUSHI, so that the two don't log in between each
 * other
 */
class SushiAlongside {

  /** runs SUSHI, logging to the logger it is given */
  interface Runner {
    void run(ILoggingService logger) throws Exception;
  }

  private final List<String> messages = new ArrayList<>();
  private final Thread thread;
  private Throwable failure;

  SushiAlongside(Runner runner) {
    ILoggingService logger = new ILoggingService() {
      @Override
      public void logMessage(String message) {
        synchronized (messages) {
          messages.add(message);
        }
      }

      @Override
      public void logDebugMessage(LogCategory category, String message) {
        // SUSHI's runner has none worth keeping
      }
    };
    thread = new Thread(() -> {
      try {
        runner.run(logger);
      } catch (Throwable e) {
        failure = e;
      }
    }, "sushi");
    // a JVM that is exiting (a fatal error) doesn't wait for it
    thread.setDaemon(true);
  }

  void start() {
    thread.start();
  }

  /**
   * Waits for SUSHI to finish, logs what it logged, and throws what it failed with, if it did
   */
  void join(Consumer<String> log) throws Exception {
    boolean interrupted = false;
    while (true) {
      try {
        thread.join();
        break;
      } catch (InterruptedException e) {
        // SUSHI is writing fsh-generated: the build can't go on until it has finished
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    List<String> logged;
    synchronized (messages) {
      logged = new ArrayList<>(messages);
    }
    for (String s : logged) {
      log.accept(s);
    }
    if (failure instanceof Exception) {
      throw (Exception) failure;
    }
    if (failure instanceof Error) {
      throw (Error) failure;
    }
    if (failure != null) {
      throw new Exception(failure);
    }
  }
}
