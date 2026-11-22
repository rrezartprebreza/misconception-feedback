#!/usr/bin/env python3
"""Load past student answers from a folder into a running instance, ready for labelling.

Folder layout (one folder per exercise id, one file per student answer; the extension sets the language):

    past-answers/
      binary-search/    s1.java  s2.java  s3.cs  s4.py
      list-insert/      ...
      remove-negatives/ ...
      loop-complexity/  ...

Answers are anonymised by the server (emails, @author lines) and get generated participant IDs
(P001, P002, ...), so file names and student names never reach the data. Remove names from the code
yourself too if they appear in comments or strings.

Usage:
    python3 scripts/import_answers.py past-answers --permission              # import
    python3 scripts/import_answers.py past-answers --dry-run                  # only list what would be sent

Use --permission only when you have permission to use these answers for research (ethics approval or
the institution's rules for teaching data). It stands in for the consent box on the collect page.
"""
import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

LANGUAGES = {".java": "java", ".py": "python", ".cs": "csharp"}


def collect(folder: Path):
    items = []
    for task_dir in sorted(p for p in folder.iterdir() if p.is_dir()):
        for file in sorted(task_dir.iterdir()):
            language = LANGUAGES.get(file.suffix.lower())
            if file.is_file() and language:
                items.append((task_dir.name, language, file))
    return items


def post(url: str, body: dict):
    request = urllib.request.Request(url, data=json.dumps(body).encode("utf-8"),
                                     headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.status


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("folder", type=Path)
    parser.add_argument("--url", default="http://localhost:8080", help="address of the running app")
    parser.add_argument("--permission", action="store_true", help="you may use these answers for research")
    parser.add_argument("--dry-run", action="store_true", help="list the answers without sending them")
    args = parser.parse_args()

    if not args.folder.is_dir():
        sys.exit(f"{args.folder} is not a folder")
    items = collect(args.folder)
    if not items:
        sys.exit("No .java, .py or .cs files found inside exercise folders.")

    counts = {}
    for task, language, _ in items:
        counts[(task, language)] = counts.get((task, language), 0) + 1
    for (task, language), n in sorted(counts.items()):
        print(f"{n:3d}  {language:7s} {task}")
    print(f"Total {len(items)} answers.")
    if args.dry_run:
        return
    if not args.permission:
        sys.exit("Add --permission to confirm you may use these answers for research (see --help).")

    sent = 0
    for number, (task, language, file) in enumerate(items, start=1):
        code = file.read_text(encoding="utf-8", errors="replace")
        body = {"participantId": f"P{number:03d}", "taskId": task, "code": code, "explanation": "",
                "consent": True, "language": language}
        try:
            post(args.url.rstrip("/") + "/api/collect/submit", body)
            sent += 1
        except urllib.error.HTTPError as e:
            print(f"Skipped {file} ({e.code}): {e.read().decode('utf-8', 'replace')[:120]}")
        except urllib.error.URLError as e:
            sys.exit(f"Cannot reach {args.url}: {e.reason}. Is the app running?")
    print(f"Imported {sent} of {len(items)}. Open /label.html to label them.")


if __name__ == "__main__":
    main()
