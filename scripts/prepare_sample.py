#!/usr/bin/env python3
"""Prepare a labelling set from collected answers: one attempt per student, random IDs, balanced sample.

What it does, in order:
  0. Drops test entries: any answer containing the text given by --exclude-text (default "SYNTHETIC QA").
  1. Keeps the first attempt per (participant, exercise, language); repeats are dropped.
  2. Replaces every participant ID with a random code (S001, S002, ...), so student numbers never
     reach the labelling page, the models or the paper. The private mapping goes to a separate file
     that you keep off shared drives and delete when your ethics approval says so.
  3. Builds a sample per language: every answer for the exercises you list as "keep all", plus a
     random number of answers from the exercises you list as "sample" (so one very common exercise
     does not swamp the labelled set).
  4. Writes the sample in the same format as answers.jsonl. The original file is never changed.

Usage:
    python3 scripts/prepare_sample.py study-data/answers.jsonl --sample-per-language 25 --sample-tasks list-insert
Then stop the app, move the original answers.jsonl to a backup folder, copy the new file in as
study-data/answers.jsonl, and start the app again. Label ids stay valid because answer ids are kept.
"""
import argparse
import json
import random
import sys
from collections import Counter, defaultdict
from pathlib import Path


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("answers", type=Path)
    p.add_argument("--out", type=Path, default=Path("study-data/answers-sample.jsonl"))
    p.add_argument("--map-out", type=Path, default=Path("study-data/id-map.private.json"))
    p.add_argument("--sample-tasks", nargs="*", default=[], help="exercise ids to down-sample (very common ones)")
    p.add_argument("--sample-per-language", type=int, default=25, help="how many answers to keep from the down-sampled exercises, per language")
    p.add_argument("--seed", type=int, default=2026)
    p.add_argument("--exclude-text", default="SYNTHETIC QA", help="drop answers containing this text (test entries)")
    args = p.parse_args()

    rows = [json.loads(line) for line in args.answers.read_text(encoding="utf-8").splitlines() if line.strip()]
    before = len(rows)
    rows = [r for r in rows if not args.exclude_text or args.exclude_text not in json.dumps(r, ensure_ascii=False)]
    if before != len(rows):
        print(f"Dropped {before - len(rows)} test entries containing '{args.exclude_text}'.")
    rows.sort(key=lambda r: r.get("collectedAt", ""))
    seen, first = set(), []
    for r in rows:
        key = (r["participantId"], r["taskId"], r["language"])
        if key not in seen:
            seen.add(key)
            first.append(r)

    rng = random.Random(args.seed)
    kept = []
    for language in sorted({r["language"] for r in first}):
        of_lang = [r for r in first if r["language"] == language]
        keep_all = [r for r in of_lang if r["taskId"] not in args.sample_tasks]
        pool = [r for r in of_lang if r["taskId"] in args.sample_tasks]
        kept += keep_all + rng.sample(pool, min(args.sample_per_language, len(pool)))

    ids = sorted({r["participantId"] for r in rows})
    rng.shuffle(ids)
    mapping = {old: f"S{n:03d}" for n, old in enumerate(ids, start=1)}
    kept.sort(key=lambda r: r.get("collectedAt", ""))
    out_rows = [{**r, "participantId": mapping[r["participantId"]]} for r in kept]

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("".join(json.dumps(r, ensure_ascii=False) + "\n" for r in out_rows), encoding="utf-8")
    args.map_out.write_text(json.dumps(mapping, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"Read {len(rows)} answers from {len(ids)} participants; {len(first)} first attempts.")
    for (language, task), n in sorted(Counter((r["language"], r["taskId"]) for r in out_rows).items()):
        print(f"  {n:3d}  {language:7s} {task}")
    print(f"Wrote {len(out_rows)} answers to {args.out} and the private ID map to {args.map_out}.")
    print("Keep the map private; the original answers file was not changed.")


if __name__ == "__main__":
    main()
