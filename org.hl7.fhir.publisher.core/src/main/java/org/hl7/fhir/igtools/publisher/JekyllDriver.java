package org.hl7.fhir.igtools.publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.lang3.SystemUtils;
import org.hl7.fhir.utilities.json.model.JsonArray;
import org.hl7.fhir.utilities.json.model.JsonElement;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.parser.JsonParser;
import org.hl7.fhir.utilities.settings.FhirSettings;

/**
 * Watch mode: a Jekyll that keeps running between the builds of a watch session (see jekyll-driver.rb), so that when
 * only some pages changed, it renders those pages (and the pages that include what changed) instead of the whole site,
 * and the steps after Jekyll know which output files are new.
 *
 * <p>It runs the Jekyll that `jekyll build` would run: the Ruby that the jekyll command on the path (after the
 * configured Ruby path) starts. It isn't used on Windows, when the IG asks for another Jekyll command, or when the
 * system property {@value #DISABLE_PROPERTY} is "false"; and it gives up (so the build runs `jekyll build` as before)
 * when it can't load Jekyll or the site has a Gemfile.
 */
public class JekyllDriver {

  public static final String DISABLE_PROPERTY = "ig.jekyll.driver";
  private static final String MARK = "@@ig-jekyll-driver@@ ";
  private static final String EOF = "\u0000eof";

  /** What a command did */
  public static class Result {
    private final boolean ok;
    private final boolean full;
    private final String reason;
    private final String error;
    private final List<String> written;
    private final List<String> unknown;

    Result(JsonObject json) {
      ok = json.asBoolean("ok");
      full = json.has("full") && json.asBoolean("full");
      reason = json.has("reason") && json.get("reason").isJsonPrimitive() ? json.asString("reason") : null;
      error = json.has("error") ? json.asString("error") : null;
      written = list(json, "written");
      unknown = list(json, "unknown");
    }

    private static List<String> list(JsonObject json, String name) {
      if (!json.has(name) || !json.get(name).isJsonArray()) {
        return null;
      }
      List<String> res = new ArrayList<>();
      for (JsonElement e : json.getJsonArray(name)) {
        res.add(e.asJsonPrimitive().asString());
      }
      return res;
    }

    public boolean isOk() {
      return ok;
    }

    /** Whether the whole site was built (so every output file is new) */
    public boolean isFull() {
      return full;
    }

    /** Why an update built the whole site, if it did */
    public String getReason() {
      return reason;
    }

    public String getError() {
      return error;
    }

    /** The output files written (relative to the output folder), when not the whole site */
    public List<String> getWritten() {
      return written == null ? Collections.emptyList() : written;
    }

    /** For a rewrite: the files asked for that Jekyll doesn't write */
    public List<String> getUnknown() {
      return unknown == null ? Collections.emptyList() : unknown;
    }
  }

  private final List<String> command;
  private final Map<String, String> env;
  private Process process;
  private OutputStream stdin;
  private BlockingQueue<String> lines;
  private String source;
  private String destination;
  private String unavailable;
  private Path script;

  JekyllDriver(List<String> command, Map<String, String> env) {
    this.command = command;
    this.env = env;
  }

  /**
   * A driver for this Jekyll command, or null if it can't be used for it (see class doc). Nothing is started until the
   * first command
   */
  public static JekyllDriver forCommand(String jekyllCommand) {
    if (SystemUtils.IS_OS_WINDOWS || !"jekyll".equals(jekyllCommand) || "false".equals(System.getProperty(DISABLE_PROPERTY))) {
      return null;
    }
    Map<String, String> env = new java.util.HashMap<>(System.getenv());
    if (FhirSettings.hasRubyPath()) {
      env.put("PATH", FhirSettings.getRubyPath() + File.pathSeparator + env.getOrDefault("PATH", ""));
      if (FhirSettings.getGemPath() != null) {
        env.put("GEM_PATH", FhirSettings.getGemPath());
      }
    }
    return new JekyllDriver(rubyFor(env.get("PATH")), env);
  }

  /**
   * How to start the Ruby that runs the jekyll command found on this path: the interpreter its #! line names (for a
   * RubyGems binstub), otherwise the ruby on the path
   */
  static List<String> rubyFor(String path) {
    List<String> res = new ArrayList<>();
    for (String dir : path == null ? new String[0] : path.split(File.pathSeparator)) {
      File f = new File(dir.isEmpty() ? "." : dir, "jekyll");
      if (f.isFile() && f.canExecute()) {
        String line = firstLine(f);
        if (line != null && line.startsWith("#!") && line.contains("ruby")) {
          String[] parts = line.substring(2).trim().split("\\s+");
          int i = 0;
          if (parts[0].endsWith("/env")) {
            i = 1;
            while (i < parts.length && parts[i].startsWith("-")) {
              i++;
            }
          }
          for (; i < parts.length; i++) {
            res.add(parts[i]);
          }
          if (!res.isEmpty()) {
            return res;
          }
        }
        break;
      }
    }
    res.add("ruby");
    return res;
  }

  private static String firstLine(File f) {
    try (BufferedReader r = Files.newBufferedReader(f.toPath(), StandardCharsets.ISO_8859_1)) {
      return r.readLine();
    } catch (IOException e) {
      return null;
    }
  }

  /** Why the driver can't be used, once it has found that it can't (then the build runs `jekyll build`) */
  public String getUnavailable() {
    return unavailable;
  }

  /** Builds the whole site from source into destination */
  public Result build(String source, String destination, long timeout, Consumer<String> log) throws IOException {
    return send(source, destination, "{\"cmd\":\"build\"}", timeout, log);
  }

  /**
   * Brings the output up to date after the files changed (relative to source) changed: either by rendering what
   * depends on them, or (as the result says) by building the whole site
   */
  public Result update(String source, String destination, Collection<String> changed, long timeout, Consumer<String> log) throws IOException {
    JsonObject cmd = new JsonObject();
    cmd.add("cmd", "update");
    JsonArray a = new JsonArray();
    for (String s : changed) {
      a.add(s);
    }
    cmd.add("changed", a);
    return send(source, destination, JsonParser.compose(cmd, false), timeout, log);
  }

  /** Writes these output files (relative to destination) again as the last build or update wrote them */
  public Result rewrite(String source, String destination, Collection<String> files, long timeout, Consumer<String> log) throws IOException {
    JsonObject cmd = new JsonObject();
    cmd.add("cmd", "rewrite");
    JsonArray a = new JsonArray();
    for (String s : files) {
      a.add(s);
    }
    cmd.add("files", a);
    return send(source, destination, JsonParser.compose(cmd, false), timeout, log);
  }

  /** Whether the running Jekyll has built this site (so that an update can go on from it) */
  public boolean isRunningFor(String source, String destination) {
    return process != null && process.isAlive() && source.equals(this.source) && destination.equals(this.destination);
  }

  private synchronized Result send(String source, String destination, String cmd, long timeout, Consumer<String> log) throws IOException {
    if (unavailable != null) {
      throw new IOException(unavailable);
    }
    if (!isRunningFor(source, destination)) {
      stop();
      start(source, destination);
    }
    try {
      stdin.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
      stdin.flush();
    } catch (IOException e) {
      stop();
      throw new IOException("Jekyll stopped running: " + e.getMessage(), e);
    }
    long end = System.currentTimeMillis() + timeout;
    while (true) {
      String line;
      try {
        long wait = end - System.currentTimeMillis();
        line = wait <= 0 ? null : lines.poll(wait, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        stop();
        throw new IOException("Interrupted while Jekyll was running");
      }
      if (line == null) {
        stop();
        throw new IOException("Jekyll timeout exceeded: " + (timeout / 1000) + " seconds");
      }
      if (EOF.equals(line)) {
        stop();
        throw new IOException("Jekyll stopped running");
      }
      if (line.startsWith(MARK)) {
        JsonObject json = JsonParser.parseObject(line.substring(MARK.length()));
        if (json.has("unavailable")) {
          unavailable = json.asString("unavailable");
          stop();
          throw new IOException(unavailable);
        }
        return new Result(json);
      }
      log.accept(line);
    }
  }

  private void start(String source, String destination) throws IOException {
    if (script == null) {
      script = Files.createTempFile("ig-jekyll-driver", ".rb");
      script.toFile().deleteOnExit();
      try (InputStream is = JekyllDriver.class.getResourceAsStream("/jekyll/jekyll-driver.rb")) {
        if (is == null) {
          throw new IOException("jekyll-driver.rb is missing");
        }
        Files.copy(is, script, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    List<String> cmd = new ArrayList<>(command);
    cmd.add(script.toString());
    cmd.add(destination);
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(new File(source));
    pb.redirectErrorStream(true);
    pb.environment().clear();
    pb.environment().putAll(env);
    try {
      process = pb.start();
    } catch (IOException e) {
      unavailable = "Ruby could not be started (" + String.join(" ", command) + "): " + e.getMessage();
      throw new IOException(unavailable, e);
    }
    Process p = process;
    Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
    stdin = process.getOutputStream();
    BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    lines = queue;
    Thread reader = new Thread(() -> {
      try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = r.readLine()) != null) {
          queue.add(line);
        }
      } catch (IOException e) {
        // the process has gone: the EOF below says so
      }
      queue.add(EOF);
    }, "jekyll-driver-output");
    reader.setDaemon(true);
    reader.start();
    this.source = source;
    this.destination = destination;
  }

  /** Stops the Jekyll process, if it's running */
  public synchronized void stop() {
    if (process != null) {
      try {
        stdin.close();
      } catch (IOException e) {
        // it's being stopped anyway
      }
      process.destroy();
      try {
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
          process.destroyForcibly();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
      }
      process = null;
      source = null;
      destination = null;
    }
  }
}
