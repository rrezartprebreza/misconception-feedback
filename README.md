# misconception-feedback

Research code for the study *Misconception-aware LLM feedback in undergraduate data structures and algorithms: a classroom study*.

A Spring Boot + Spring AI service with two feedback conditions:

- **GENERIC**: the model sees the task, the student's code and explanation.
- **INFORMED**: the same, plus the misconception a detector found, with its false belief and corrective explanation.

Both conditions use the same model and settings. The generic condition never calls the detector.

## How it fits together

| Piece | Where | What it does |
| --- | --- | --- |
| Catalogue | `src/main/resources/catalogue-<language>.json` | Eight candidate misconceptions per language (id, topic, belief, pattern, correction). Validate with instructors before use. |
| Detector | `detect/LlmDetector` | Asks the model which catalogue entries a submission shows; drops low-confidence matches and ids not in the catalogue. |
| Prompts | `feedback/PromptFactory` | Every prompt used in the study, in one place so it can be published with the paper. |
| Anonymiser | `feedback/Anonymiser` | Strips emails and `@author` lines before anything reaches a model. Not a guarantee; review samples by hand. |
| Service | `feedback/FeedbackService` | Runs one submission under one condition. `detectOnly` supports the RQ1 accuracy evaluation. |
| Metrics | `eval/AgreementMetrics` | Micro precision, recall, F1 and Cohen's kappa between detector and instructor labels. |
| API | `web/FeedbackController` | `POST /api/feedback` |

## Languages

Java, Python and C# are supported. Pick one with `STUDY_LANGUAGE` (`java` by default, or `python`, `csharp`):

```bash
STUDY_LANGUAGE=python mvn spring-boot:run
```

Each language has its own files: `catalogue-<language>.json`, `study/tasks-<language>.json` and `pilot/synthetic-gold-<language>.json`. The misconception ids (M01 to M08) and the exercise ids are the same in all three, so results can be compared across languages. The Python and C# entries were adapted from the Java ones by an AI assistant and have not been checked by instructors. Run one language per study and report which one.

## Run

Default: a free local model through [Ollama](https://ollama.com). Student data stays on your machine.

```bash
ollama pull qwen2.5-coder:7b      # about 8 GB RAM; try a larger model if your machine allows
export OLLAMA_MODEL=qwen2.5-coder:7b   # fix the model for the whole study and report it
mvn test
mvn spring-boot:run
```

Ollama in Docker instead of installing it (on a Mac there is no GPU access, so it is slower; try a smaller model such as `qwen2.5-coder:3b`):

```bash
docker compose up -d
docker compose exec ollama ollama pull qwen2.5-coder:7b
mvn spring-boot:run
```

Hosted OpenAI instead (the Maven profile `-Popenai` swaps the starter):

```bash
export OPENAI_API_KEY=...
export OPENAI_MODEL=gpt-4o-mini
mvn -Popenai spring-boot:run
```

Other OpenAI-compatible services (Grok, Gemini, Groq, OpenRouter and similar) use the same profile. Set the base URL, key and model from the provider's documentation:

```bash
export OPENAI_BASE_URL=https://api.x.ai          # example: Grok
export OPENAI_API_KEY=...
export OPENAI_MODEL=<model name from the provider>
mvn -Popenai spring-boot:run
```

Groq (free plan with rate limits):

```bash
export OPENAI_BASE_URL=https://api.groq.com/openai
export OPENAI_API_KEY=...                         # never commit keys
export OPENAI_MODEL=openai/gpt-oss-20b
export LLM_FALLBACK_MODELS=openai/gpt-oss-120b    # used automatically when the first model is rate limited
mvn -Popenai spring-boot:run
```

`LLM_FALLBACK_MODELS` takes a comma-separated list. A model that fails is skipped for 60 seconds (`LLM_FALLBACK_COOLDOWN_SECONDS`). Do not use fallback in a study: the model must stay fixed and be reported.

Some providers use a different path than `/v1/chat/completions`; set `OPENAI_COMPLETIONS_PATH` if so. Hosted services send student code to a third party, so for a real study check their data terms and get the use approved.


```bash
curl -X POST localhost:8080/api/feedback -H 'Content-Type: application/json' -d '{
  "id": "P01",
  "task": "Implement binary search on a sorted int array.",
  "code": "int search(int[] a, int k) { int lo = 0, hi = a.length - 1; while (lo < hi) { int mid = (lo + hi) / 2; if (a[mid] < k) lo = mid; else hi = mid; } return a[lo] == k ? lo : -1; }",
  "explanation": "It halves the range each time.",
  "condition": "INFORMED"
}'
```

## Web page

With the app running, open http://localhost:8080 . Paste a task, your Java code and an explanation, choose a feedback type and press the button. The page shows the feedback and, for the misconception-aware type, the misconception found with its corrective idea. It uses `POST /api/feedback` and `GET /api/catalogue`. The page is a first version: no accounts, no history.

## Study mode

For the classroom study, send participants to `http://<your-address>:8080/study.html`. The page walks them through consent, a participant ID, an exercise and feedback, and lets them resubmit.

What the server does behind the page:

- **Assignment.** Condition is decided by participant ID and exercise topic (a counterbalanced crossover). IDs ending in an odd digit (P01, P03...) get generic feedback on topic 1 and misconception-aware feedback on topic 2; even digits (P02, P04...) get the reverse. P01 to P72 splits exactly 36 and 36.
- **Blinding.** Participants never see their condition or the detector output. They only get the feedback text and the attempt number.
- **Logging.** Every submission is appended to `study-data/submissions.jsonl` (ignored by git): timestamp, participant ID, exercise, topic, attempt, condition, model label, anonymised code and explanation, feedback and detector matches. Attempt numbers continue after a restart. Compare attempt 1 with the last attempt to see whether the misconception was corrected.
- **Consent.** The consent text in `study.html` is a placeholder. Replace it with the text approved by your ethics committee before any student uses the page. The server refuses submissions without the consent flag.

Set your own exercises with `study.tasks-file=study-data/tasks.json` (same format as `src/main/resources/study/tasks-java.json`; every exercise has topic 1 or 2). Set `STUDY_MODEL_LABEL` when you use a hosted model so the log records which one.

Participant IDs must be assigned by you, not chosen by students: letters, digits, `-` and `_` only, up to 20 characters. Do not use names.

## Pilot run

Smoke-test the detector before touching real student data. This sends 12 synthetic submissions through the detector and prints precision, recall, F1 and kappa:

```bash
ollama serve &                   # if Ollama is not already running
mvn spring-boot:run -Dspring-boot.run.profiles=pilot
# hosted model instead: OPENAI_API_KEY=... mvn -Popenai spring-boot:run -Dspring-boot.run.profiles=pilot
```

Small local models sometimes return JSON the detector cannot parse. If the pilot fails with a parsing error, try a larger model before changing the code.

The bundled set (`src/main/resources/pilot/synthetic-gold-<language>.json`) was written and labelled by an AI assistant, not by instructors. Use it only to check that the pipeline works and to compare models. Do not report its numbers as study results. For the real RQ1 evaluation, point the runner at an instructor-labelled file in the same format (anonymised, kept outside git):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=pilot -Dspring-boot.run.arguments=--study.pilot.gold-file=study-data/gold.json
```

To compare models, run the pilot once per model (set `OLLAMA_MODEL` or `OPENAI_MODEL`) and record the scores.

## Status

Early scaffold. The catalogue is a starting list from common DSA teaching patterns, not validated against course data.

- The core logic (catalogue, prompts, anonymiser, detector, service, metrics) was compiled and checked with a plain-Java harness.
- The Spring wiring (`SpringAiLlmClient`, `AppConfig`, controller) and the JUnit tests have not been run yet: run `mvn test` first and fix anything that fails.
- Versions used: Spring Boot 3.5.0, Spring AI 1.0.0, Java 21.
- Student data and study logs must stay out of git (`study-data/` is ignored).

## Ethics

Get ethics approval and consent before using real student work. Check the model provider's data-retention terms, or use a local model.

## Licence

Free to use, change and share under the [MIT licence](LICENSE). Contributions are welcome, especially new misconceptions, exercises and languages, and instructor-labelled data (anonymised).
