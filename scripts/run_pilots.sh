#!/usr/bin/env bash
# Runs the pilot evaluation for each language and each model and saves the results in study-data/results/.
#
#   scripts/run_pilots.sh java csharp
#   MODELS="openai/gpt-oss-20b llama-3.3-70b-versatile" scripts/run_pilots.sh java
#
# Needs a labelled file per language: study-data/gold-<language>.json (the file you download from /label.html).
# Provider, address and key come from .env. The automatic fallback is switched off so each run uses one model.
set -uo pipefail

MODELS=${MODELS:-"openai/gpt-oss-20b openai/gpt-oss-120b llama-3.3-70b-versatile llama-3.1-8b-instant"}

if [ "$#" -eq 0 ]; then
  echo "Usage: scripts/run_pilots.sh <language> [<language> ...]   (java, python, csharp)"
  exit 1
fi

failed=0
for lang in "$@"; do
  gold="study-data/gold-${lang}.json"
  if [ ! -f "$gold" ]; then
    echo "Missing $gold. Download the labelled set for $lang from /label.html and save it there."
    failed=1
    continue
  fi
  for model in $MODELS; do
    echo "== $lang with $model"
    if ! STUDY_LANGUAGE="$lang" OPENAI_MODEL="$model" LLM_FALLBACK_MODELS="" \
        mvn -q -Popenai spring-boot:run \
        -Dspring-boot.run.profiles=pilot \
        -Dspring-boot.run.arguments="--study.pilot.gold-file=$gold"; then
      echo "FAILED: $lang with $model (rate limit or a parsing error; run it again later)"
      failed=1
    fi
  done
done
echo "Done. Results are in study-data/results/."
exit $failed
