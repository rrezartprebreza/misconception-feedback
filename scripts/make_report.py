#!/usr/bin/env python3
"""Turn saved pilot results into the paper's tables and figures.

Reads study-data/results/<language>-<model>-<stamp>.json (written by the pilot runner), keeps the
newest file per language and model, and writes to report/:
  table1_overall.csv / .md      micro precision, recall, F1 per model and language
  table2_per_misconception.csv / .md   per-misconception precision, recall, F1, counts (best model per language)
  fig1_overall_f1.png           F1 per model, Java and C# side by side
  fig2_per_misconception_f1.png F1 per misconception for the best model in each language

Usage: python3 scripts/make_report.py [results_dir] [out_dir]
Needs matplotlib. It only reads ids and scores, never student text.
"""
import csv
import json
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

results_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "study-data/results")
out_dir = Path(sys.argv[2] if len(sys.argv) > 2 else "report")
LANG_NAMES = {"java": "Java", "csharp": "C#", "python": "Python"}

latest = {}
for f in sorted(results_dir.glob("*.json")):
    data = json.loads(f.read_text(encoding="utf-8"))
    key = (data["language"], data["model"])
    if key not in latest or data["createdAt"] > latest[key]["createdAt"]:
        latest[key] = data
if not latest:
    sys.exit(f"No result files in {results_dir}. Run scripts/run_pilots.sh first.")

out_dir.mkdir(parents=True, exist_ok=True)
languages = sorted({k[0] for k in latest})
models = sorted({k[1] for k in latest})


def lang(l):
    return LANG_NAMES.get(l, l)


def pct(x):
    return f"{x:.2f}"


# Table 1
rows = []
for (l, m), d in sorted(latest.items(), key=lambda kv: (kv[0][1], kv[0][0])):
    mi = d["micro"]
    rows.append([m, lang(l), d["items"], pct(mi["precision"]), pct(mi["recall"]), pct(mi["f1"]), d["createdAt"][:10]])
hdr1 = ["Model", "Language", "Answers", "Precision", "Recall", "F1", "Run date"]


def write_table(name, header, body):
    with open(out_dir / f"{name}.csv", "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(header)
        w.writerows(body)
    md = ["| " + " | ".join(header) + " |", "| " + " | ".join("---" for _ in header) + " |"]
    md += ["| " + " | ".join(str(c) for c in r) + " |" for r in body]
    (out_dir / f"{name}.md").write_text("\n".join(md) + "\n", encoding="utf-8")


write_table("table1_overall", hdr1, rows)

# Best model per language by micro F1
best = {l: max((d for (ll, _), d in latest.items() if ll == l), key=lambda d: d["micro"]["f1"]) for l in languages}

# Table 2
ids = sorted({i for d in best.values() for i in d["perMisconception"]})
hdr2 = ["Id"]
for l in languages:
    hdr2 += [f"{lang(l)} P", f"{lang(l)} R", f"{lang(l)} F1", f"{lang(l)} TP/FP/FN"]
body2 = []
for i in ids:
    r = [i]
    for l in languages:
        p = best[l]["perMisconception"].get(i)
        r += [pct(p["precision"]), pct(p["recall"]), pct(p["f1"]),
              f'{p["truePositives"]}/{p["falsePositives"]}/{p["falseNegatives"]}'] if p else ["", "", "", ""]
    body2.append(r)
write_table("table2_per_misconception", hdr2, body2)

# Figure 1
fig, ax = plt.subplots(figsize=(7, 4))
width = 0.8 / len(languages)
colors = ["#3a36d8", "#0b6b5e", "#a8321d"]
for n, l in enumerate(languages):
    vals = [latest[(l, m)]["micro"]["f1"] if (l, m) in latest else 0 for m in models]
    xs = [i + n * width for i in range(len(models))]
    bars = ax.bar(xs, vals, width, label=lang(l), color=colors[n % 3])
    for b, v in zip(bars, vals):
        ax.text(b.get_x() + b.get_width() / 2, v + 0.01, f"{v:.2f}", ha="center", fontsize=8)
ax.set_xticks([i + width * (len(languages) - 1) / 2 for i in range(len(models))])
ax.set_xticklabels(models, rotation=15, ha="right", fontsize=8)
ax.set_ylim(0, 1.05)
ax.set_ylabel("Micro F1")
ax.legend(frameon=False)
for s in ("top", "right"):
    ax.spines[s].set_visible(False)
fig.tight_layout()
fig.savefig(out_dir / "fig1_overall_f1.png", dpi=200)
plt.close(fig)

# Figure 2
fig, ax = plt.subplots(figsize=(7, 4))
for n, l in enumerate(languages):
    vals = [best[l]["perMisconception"][i]["f1"] if i in best[l]["perMisconception"] else 0 for i in ids]
    xs = [k + n * width for k in range(len(ids))]
    ax.bar(xs, vals, width, label=f"{lang(l)} ({best[l]['model']})", color=colors[n % 3])
ax.set_xticks([k + width * (len(languages) - 1) / 2 for k in range(len(ids))])
ax.set_xticklabels(ids)
ax.set_ylim(0, 1.05)
ax.set_ylabel("F1 per misconception")
ax.legend(frameon=False, fontsize=8)
for s in ("top", "right"):
    ax.spines[s].set_visible(False)
fig.tight_layout()
fig.savefig(out_dir / "fig2_per_misconception_f1.png", dpi=200)
plt.close(fig)

print(f"Wrote tables and figures to {out_dir}/ from {len(latest)} result files.")
