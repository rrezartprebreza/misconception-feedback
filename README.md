# misconception-feedback

Research code for the study *Misconception-aware LLM feedback in undergraduate data structures and algorithms: a classroom study*.

A Spring Boot + Spring AI service with two feedback conditions:

- **GENERIC**: the model sees the task, the student's Java code and explanation.
- **INFORMED**: the same, plus the misconception a detector found, with its false belief and corrective explanation.

Both conditions use the same model and settings. The generic condition never calls the detector.

## How it fits together

| Piece | Where | What it does |
| --- | --- | --- |
| Catalogue | `src/main/resources/catalogue.json` | Eight candidate misconceptions (id, topic, belief, pattern, correction). Validate with instructors before use. |
| Detector | `detect/LlmDetector` | Asks the model which catalogue entries a submission shows; drops low-confidence matches and ids not in the catalogue. |
| Prompts | `feedback/PromptFactory` | Every prompt used in the study, in one place so it can be published with the paper. |
| Anonymiser | `feedback/Anonymiser` | Strips emails and `@author` lines before anything reaches a model. Not a guarantee; review samples by hand. |
| Service | `feedback/FeedbackService` | Runs one submission under one condition. `detectOnly` supports the RQ1 accuracy evaluation. |
| Metrics | `eval/AgreementMetrics` | Micro precision, recall, F1 and Cohen's kappa between detector and instructor labels. |
| API | `web/FeedbackController` | `POST /api/feedback` |

## Run

```bash
export OPENAI_API_KEY=...        # or swap the starter for spring-ai-starter-model-ollama
export LLM_MODEL=gpt-4o-mini     # fix the model for the whole study and report it
mvn test
mvn spring-boot:run
```

```bash
curl -X POST localhost:8080/api/feedback -H 'Content-Type: application/json' -d '{
  "id": "P01",
  "task": "Implement binary search on a sorted int array.",
  "code": "int search(int[] a, int k) { int lo = 0, hi = a.length - 1; while (lo < hi) { int mid = (lo + hi) / 2; if (a[mid] < k) lo = mid; else hi = mid; } return a[lo] == k ? lo : -1; }",
  "explanation": "It halves the range each time.",
  "condition": "INFORMED"
}'
```

## Status

Early scaffold. The catalogue is a starting list from common DSA teaching patterns, not validated against course data.

- The core logic (catalogue, prompts, anonymiser, detector, service, metrics) was compiled and checked with a plain-Java harness.
- The Spring wiring (`SpringAiLlmClient`, `AppConfig`, controller) and the JUnit tests have not been run yet: run `mvn test` first and fix anything that fails.
- Versions used: Spring Boot 3.5.0, Spring AI 1.0.0, Java 21.
- Student data and study logs must stay out of git (`study-data/` is ignored).

## Ethics

Get ethics approval and consent before using real student work. Check the model provider's data-retention terms, or use a local model.
