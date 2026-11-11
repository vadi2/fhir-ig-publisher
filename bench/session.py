#!/usr/bin/env python3
"""One -rapido -watch session of the IG Publisher on uz.dhp.core, with timed edits.

  python3 bench/session.py --jar <publisher.jar> --seed <warm IG copy> --work <dir> --pcf <package cache dir>
      [--heap 7g] [--threads N] [--steps first,prof1,...] [--label base] [--jvm "<extra jvm args>"] [--keep-running]

Copies --seed to --work (without output/, temp/, template/, fsh-generated/), starts the publisher there, waits for the
first build, then for each step makes the edit, waits for "Finished @" and the next "Watching for changes", and
checks that the edited text is in output/. Writes into --work/../<label>-<time>/ : publisher.log (every line with
seconds since java started), gc.log, result.json and summary.txt (also printed).

Per build: wall seconds from the edit to "Finished @" (finished_s), to the moment the checked output file first has
the edited text (page_s, polled every 50 ms), the Rapido line, every phase line of the publisher with the seconds
until the next phase line, GC pause seconds, and peak RSS of java and of its children (Jekyll, SUSHI) during that
build (rss.log has them every 2 s), swap in use at its peak and MB read back from swap (swapin: a build that reads
much back from swap was slowed down by it).
"""
import argparse, json, os, re, shutil, signal, subprocess, sys, threading, time

PROF = ("input/fsh/UZCoreSpecimen.fsh",
        'Current lifecycle status of the specimen (e.g., available, unavailable).',
        "output/en/StructureDefinition-uz-core-specimen.html")
INST = ("input/fsh/UZCoreObservation.fsh",
        "* component[=].valueQuantity = 120 'mm[Hg]'",
        "output/Observation-blood-pressure-example.json")
PAGE = ("input/pagecontent/index.md",
        "It is intended for testing and feedback purposes only",
        "output/en/index.html")

# step -> (file, original text, replacement, output file, text expected in output, text that must be absent)
STEPS = {
    "prof1": (PROF[0], PROF[1], PROF[1] + " [bench prof1]", PROF[2], "[bench prof1]", None),
    "prof2": (PROF[0], PROF[1], PROF[1] + " [bench prof2]", PROF[2], "[bench prof2]", None),
    "prof3": (PROF[0], PROF[1], PROF[1] + " [bench prof3]", PROF[2], "[bench prof3]", None),
    "restore-prof": (PROF[0], PROF[1], PROF[1], PROF[2], "unavailable).", "[bench prof"),
    "inst1": (INST[0], INST[1], INST[1].replace("120", "131"), INST[2], '"value" : 131', None),
    "inst2": (INST[0], INST[1], INST[1].replace("120", "132"), INST[2], '"value" : 132', None),
    "inst3": (INST[0], INST[1], INST[1].replace("120", "133"), INST[2], '"value" : 133', None),
    "restore-inst": (INST[0], INST[1], INST[1], INST[2], '"value" : 120', None),
    "page1": (PAGE[0], PAGE[1], PAGE[1] + " (bench page1)", PAGE[2], "(bench page1)", None),
    "page2": (PAGE[0], PAGE[1], PAGE[1] + " (bench page2)", PAGE[2], "(bench page2)", None),
    "page3": (PAGE[0], PAGE[1], PAGE[1] + " (bench page3)", PAGE[2], "(bench page3)", None),
    "restore-page": (PAGE[0], PAGE[1], PAGE[1], PAGE[2], "feedback purposes only", "(bench page"),
}
DEFAULT_STEPS = "first,prof1,prof2,restore-prof,inst1,inst2,restore-inst,page1,restore-page"
# the publisher's timed log lines end like "(00:12.345 / 01:02.345, 1Gb)" or "(12.3sec / 01:02, 1Gb)"
ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")
PHASE_RE = re.compile(r"^(?P<msg>.*?)\s+\((?P<d>[^()/]+) / (?P<t>[^(),]+), (?P<mem>[^()]+)\)\s*$")


class Session:
    def __init__(self, a):
        self.a = a
        self.lines = []          # (t, text)
        self.cv = threading.Condition()
        self.t0 = None
        self.peak = {"java": 0, "children": 0}
        self.done = False

    def start(self):
        a = self.a
        cmd = ["java", "-Xmx" + a.heap, "-Xlog:gc:file=" + os.path.join(a.out, "gc.log")]
        cmd += a.jvm.split() if a.jvm else []
        cmd += ["-jar", a.jar, "-ig", ".", "-rapido", "-watch", "-tx", a.tx, "-package-cache-folder", a.pcf]
        if a.threads is not None:
            cmd += ["-threads", str(a.threads)]
        self.cmd = cmd
        env = dict(os.environ, IG_GATE_OFF="1", MALLOC_ARENA_MAX="2")
        self.t0 = time.time()
        self.p = subprocess.Popen(cmd, cwd=a.work, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                  start_new_session=True, text=True, errors="replace", bufsize=1)
        threading.Thread(target=self._read, daemon=True).start()
        threading.Thread(target=self._rss, daemon=True).start()

    def _read(self):
        with open(os.path.join(self.a.out, "publisher.log"), "w") as log:
            for line in self.p.stdout:
                t = time.time() - self.t0
                line = ANSI_RE.sub("", line.rstrip("\n"))  # the publisher colours what the libraries log
                log.write("%9.3f %s\n" % (t, line))
                log.flush()
                with self.cv:
                    self.lines.append((t, line))
                    self.cv.notify_all()
        with self.cv:
            self.done = True
            self.cv.notify_all()

    def _rss(self):
        def rss(pid):
            try:
                with open("/proc/%d/statm" % pid) as f:
                    return int(f.read().split()[1]) * os.sysconf("SC_PAGE_SIZE")
            except (OSError, ValueError, IndexError):
                return 0

        def kids(pid):
            res = []
            try:
                for tid in os.listdir("/proc/%d/task" % pid):
                    with open("/proc/%d/task/%s/children" % (pid, tid)) as f:
                        for c in f.read().split():
                            res.append(int(c))
                            res.extend(kids(int(c)))
            except OSError:
                pass
            return res

        with open(os.path.join(self.a.out, "rss.log"), "w") as log:  # seconds, java MB, children MB
            n = 0
            while self.p.poll() is None:
                j, c = rss(self.p.pid), sum(rss(c) for c in kids(self.p.pid))
                self.peak["java"] = max(self.peak["java"], j)
                self.peak["children"] = max(self.peak["children"], c)
                self.peak["sum"] = max(self.peak.get("sum", 0), j + c)
                self.peak["swap"] = max(self.peak.get("swap", 0), swap_used())
                n += 1
                if n % 4 == 0:
                    log.write("%.0f %d %d\n" % (time.time() - self.t0, j >> 20, c >> 20))
                    log.flush()
                time.sleep(0.5)

    def wait_line(self, pattern, start, timeout):
        """index of the first line at or after index start that contains pattern"""
        end = time.time() + timeout
        i = start
        with self.cv:
            while True:
                while i < len(self.lines):
                    if pattern in self.lines[i][1]:
                        return i
                    i += 1
                if self.done:
                    raise RuntimeError("the publisher exited while waiting for '%s'" % pattern)
                left = end - time.time()
                if left <= 0:
                    raise RuntimeError("timed out waiting for '%s'" % pattern)
                self.cv.wait(min(left, 1.0))

    def stop(self):
        try:
            os.killpg(self.p.pid, signal.SIGTERM)
            try:
                self.p.wait(60)  # (a flight recording is written on the way out)
            except subprocess.TimeoutExpired:
                pass
            os.killpg(self.p.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass


def swap_used():
    """swap in use on the machine, bytes (0 if there is none): a build that swaps isn't a fair measurement"""
    try:
        with open("/proc/meminfo") as f:
            v = dict((l.split(":")[0], int(l.split()[1])) for l in f if l.startswith("Swap"))
        return (v["SwapTotal"] - v["SwapFree"]) * 1024
    except (OSError, KeyError, ValueError, IndexError):
        return 0


def swapped_in():
    """bytes read back from swap since boot"""
    try:
        with open("/proc/vmstat") as f:
            return next(int(l.split()[1]) for l in f if l.startswith("pswpin ")) * os.sysconf("SC_PAGE_SIZE")
    except (OSError, StopIteration, ValueError):
        return 0


def gc_pauses(path):
    """(uptime seconds, pause seconds) of every GC pause in a -Xlog:gc file"""
    res = []
    try:
        with open(path) as f:
            for line in f:
                m = re.match(r"\[(\d+\.\d+)s\].*Pause.* (\d+\.\d+)ms", line)
                if m:
                    res.append((float(m.group(1)), float(m.group(2)) / 1000))
    except OSError:
        pass
    return res


def phases(lines, t_from):
    """the publisher's timed log lines of one build: (seconds since t_from, seconds until the next one, text)"""
    timed = [(t, PHASE_RE.match(s).group("msg").strip()) for t, s in lines if PHASE_RE.match(s)]
    res = []
    for i, (t, msg) in enumerate(timed):
        nxt = timed[i + 1][0] if i + 1 < len(timed) else lines[-1][0]
        res.append((round(t - t_from, 2), round(nxt - t, 2), msg))
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jar", required=True)
    ap.add_argument("--seed", required=True, help="IG checkout with a warm input-cache/txcache")
    ap.add_argument("--work", required=True, help="folder for the fresh copy of the IG (deleted first)")
    ap.add_argument("--pcf", required=True, help="package cache folder (warm)")
    ap.add_argument("--heap", default="7g")
    ap.add_argument("--threads", type=int, default=None, help="value for -threads (not passed if absent)")
    ap.add_argument("--tx", default="https://tx.fhir.org")
    ap.add_argument("--steps", default=DEFAULT_STEPS)
    ap.add_argument("--label", default="session")
    ap.add_argument("--jvm", default="", help="extra JVM arguments, e.g. a -XX:StartFlightRecording=...")
    ap.add_argument("--first-timeout", type=int, default=3600)
    ap.add_argument("--timeout", type=int, default=1200)
    ap.add_argument("--settle", type=float, default=3.0, help="seconds between 'Watching for changes' and the edit")
    ap.add_argument("--keep-running", action="store_true", help="leave the publisher running at the end")
    a = ap.parse_args()
    a.jar, a.seed, a.work, a.pcf = (os.path.abspath(x) for x in (a.jar, a.seed, a.work, a.pcf))
    a.out = os.path.join(os.path.dirname(a.work), "%s-%s" % (a.label, time.strftime("%H%M%S")))
    os.makedirs(a.out)

    if os.path.exists(a.work):
        shutil.rmtree(a.work)
    shutil.copytree(a.seed, a.work, symlinks=True,
                    ignore=lambda d, names: [n for n in names if d == a.seed and n in
                                             ("output", "temp", "template", "fsh-generated")])
    # Rapido's records of an earlier build travel with the tx cache: without them the first build is always a
    # complete build with no state
    for n in (".build-tracker.ini", ".build-outputs.json"):
        try:
            os.remove(os.path.join(a.work, "input-cache", "txcache", n))
        except OSError:
            pass
    originals = {}
    for step in STEPS.values():
        with open(os.path.join(a.work, step[0])) as f:
            originals[step[0]] = f.read()
        if step[1] not in originals[step[0]]:
            sys.exit("the text to edit is not in %s: is the IG at 2c39cd0a?" % step[0])

    s = Session(a)
    s.start()
    builds = []
    ok = True
    try:
        pos = 0
        for name in a.steps.split(","):
            name = name.strip()
            first = name == "first"
            swapin0 = swapped_in()
            check_at = None
            if first:
                t_edit = 0.0
                start = 0
            else:
                path, old, new, outfile, want, absent = STEPS[name]
                time.sleep(a.settle)
                start = len(s.lines)
                with open(os.path.join(a.work, path), "w") as f:
                    f.write(originals[path].replace(old, new))
                t_edit = time.time() - s.t0
                # when does the output file first have the edit
                def poll(outfile=outfile, want=want, absent=absent, res=[]):
                    full = os.path.join(a.work, outfile)
                    while not stop_poll.is_set():
                        try:
                            with open(full, errors="replace") as f:
                                txt = f.read()
                            if want in txt and (absent is None or absent not in txt):
                                return time.time() - s.t0
                        except OSError:
                            pass
                        time.sleep(0.05)
                    return None
                stop_poll = threading.Event()
                holder = {}
                th = threading.Thread(target=lambda: holder.setdefault("t", poll()), daemon=True)
                th.start()
            fin = s.wait_line("Finished @", start, a.first_timeout if first else a.timeout)
            t_fin = s.lines[fin][0]
            pos = s.wait_line("Watching for changes", fin, 120)
            b = {"step": name, "edit_at": round(t_edit, 2), "finished_s": round(t_fin - t_edit, 2)}
            lines = s.lines[start:pos + 1]
            if not first:
                stop_poll.set()
                th.join()
                check_at = holder.get("t")
                b["page_s"] = round(check_at - t_edit, 2) if check_at else None
                b["output_ok"] = check_at is not None
                ok = ok and b["output_ok"]
                det = [t for t, l in lines if "File Changes Detected" in l]
                b["detected_s"] = round(det[0] - t_edit, 2) if det else None
            rap = [PHASE_RE.sub(r"\g<msg>", l).strip() for t, l in lines if "Rapido Mode:" in l and " Build (" in l + " "
                   or "Rapido Mode: Page Rebuild" in l]
            # (a file that isn't a page gives "Complete Build (... is not a page)" and then the Differential Build line)
            b["rapido"] = rap[-1] if rap else None
            b["built"] = next((l.strip() for t, l in lines if l.startswith("Built. ")), None)
            b["finished_line"] = s.lines[fin][1].strip()
            b["errors"] = next((l.strip() for t, l in lines if re.search(r"Errors: \d+", l)), None)
            b["phases"] = phases(lines, t_edit)
            b["gc_pause_s"] = round(sum(p for t, p in gc_pauses(os.path.join(a.out, "gc.log")) if t_edit <= t <= t_fin), 2)
            b["rss_java_gb"] = round(s.peak["java"] / 2**30, 2)
            b["rss_children_gb"] = round(s.peak["children"] / 2**30, 2)
            b["rss_sum_gb"] = round(s.peak.get("sum", 0) / 2**30, 2)
            b["swap_gb"] = round(s.peak.get("swap", 0) / 2**30, 2)
            b["swapin_mb"] = (swapped_in() - swapin0) >> 20
            s.peak = {"java": 0, "children": 0}
            builds.append(b)
            print("%-13s finished %7.1fs  page %7s  gc %5.1fs  %s" % (
                name, b["finished_s"], "-" if first else ("%.1fs" % b["page_s"] if b.get("page_s") else "MISSING"),
                b["gc_pause_s"], b["rapido"] or ""), flush=True)
    except Exception as e:
        ok = False
        print("FAILED: %s (peak RSS: java %.1f GB, children %.1f GB)" % (e, s.peak["java"] / 2**30, s.peak["children"] / 2**30), flush=True)
        print("\n".join(l for t, l in s.lines[-25:]), flush=True)
    finally:
        if not a.keep_running or not ok:
            s.stop()

    res = {"label": a.label, "cmd": " ".join(s.cmd), "ok": ok, "t0_epoch": s.t0, "builds": builds}
    with open(os.path.join(a.out, "result.json"), "w") as f:
        json.dump(res, f, indent=1)
    with open(os.path.join(a.out, "summary.txt"), "w") as f:
        f.write(res["cmd"] + "\n\n")
        f.write("%-13s %9s %9s %9s %7s %6s %6s %6s %7s  %s\n" % ("step", "finished", "page", "detected", "gc", "java", "child", "swap", "swapin", "rapido"))
        for b in builds:
            f.write("%-13s %8.1fs %9s %9s %6.1fs %5.1fG %5.1fG %5.1fG %6dM  %s\n" % (
                b["step"], b["finished_s"], "%.1fs" % b["page_s"] if b.get("page_s") else "-",
                "%.1fs" % b["detected_s"] if b.get("detected_s") is not None else "-", b["gc_pause_s"],
                b["rss_java_gb"], b["rss_children_gb"], b["swap_gb"], b["swapin_mb"], (b["rapido"] or "").split("  ")[0]))
        for b in builds:
            f.write("\n== %s: %s\n   %s\n" % (b["step"], b["finished_line"], b["built"] or ""))
            for at, dur, msg in b["phases"]:
                f.write("  %8.2f %8.2f  %s\n" % (at, dur, msg[:150]))
    print(open(os.path.join(a.out, "summary.txt")).read().split("\n==")[0])
    print("results in " + a.out)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
