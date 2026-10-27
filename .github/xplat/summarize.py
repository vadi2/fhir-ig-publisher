"""Summarises a job's surefire reports (and the scenario checks) for the log and the job summary.

usage: summarize.py <label> <surefire-reports dir> [checks.txt]
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

label, reports = sys.argv[1], sys.argv[2]
checks = sys.argv[3] if len(sys.argv) > 3 else None

tests = failures = errors = skipped = 0
bad = []
for f in sorted(glob.glob(os.path.join(reports, "TEST-*.xml"))):
    try:
        root = ET.parse(f).getroot()
    except ET.ParseError as e:
        bad.append((os.path.basename(f), "unparseable report", str(e)))
        continue
    for case in root.iter("testcase"):
        tests += 1
        name = case.get("classname", "") + "." + case.get("name", "")
        for kind in ("failure", "error"):
            for el in case.findall(kind):
                if kind == "failure":
                    failures += 1
                else:
                    errors += 1
                msg = (el.get("message") or el.get("type") or "").strip().splitlines()
                bad.append((name, kind, msg[0][:300] if msg else ""))
        if case.find("skipped") is not None:
            skipped += 1

line = "XPLAT-SUMMARY %s tests=%d failures=%d errors=%d skipped=%d passed=%d" % (
    label, tests, failures, errors, skipped, tests - failures - errors - skipped)
out = [line]
for name, kind, msg in bad:
    out.append("XPLAT-FAILED %s | %s | %s" % (name, kind, msg))
if checks and os.path.exists(checks):
    with open(checks, encoding="utf-8") as c:
        for l in c:
            out.append("XPLAT-CHECK " + l.rstrip("\n"))
print("\n".join(out))

summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with open(summary, "a", encoding="utf-8") as s:
        s.write("### %s\n\n" % label)
        s.write("| tests | passed | failures | errors | skipped |\n|---|---|---|---|---|\n")
        s.write("| %d | %d | %d | %d | %d |\n\n" % (tests, tests - failures - errors - skipped, failures, errors, skipped))
        if bad:
            s.write("Failed tests:\n\n")
            for name, kind, msg in bad:
                s.write("- `%s` (%s): %s\n" % (name, kind, msg.replace("|", "\\|")))
            s.write("\n")
        if checks and os.path.exists(checks):
            s.write("Scenario checks:\n\n```\n")
            with open(checks, encoding="utf-8") as c:
                for l in c:
                    s.write(l[:400].rstrip("\n") + "\n")
            s.write("```\n")
