# selectcaragent

A multi-agent **car-news pipeline** built with **Java 17**, **Spring Boot**, **Spring AI** and a
local **Ollama** model. It reads a list of car-brand and news homepages, scans them for recent
articles, keeps only car-related and not-yet-processed links, extracts a title + summary, and then
uses Ollama to generate a long-form news article, a social-media short story and a short-video
script for each one.

## Pipeline (agents)

| # | Agent | Responsibility |
|---|-------|----------------|
| 1 | `SourceReaderAgent` | Read `data/website.json` (brand + news homepages). |
| 2 | `WebScannerAgent` | Scan each homepage (Jsoup) and collect candidate article links. |
| 3 | `CarRelevanceFilterAgent` | Drop links that are not car-related (keyword heuristics). |
| 4 | `DeduplicationAgent` | Remove links already in `data/processed.json`. |
| 5 | `ContentExtractorAgent` | Fetch each page, extract title + summary, enforce the *last-week* recency window, and write `data/articlelist.json`. |
| 6 | `ArticleWriterAgent` | Ollama: generate a **1200+ word** news article per link. |
| 7 | `SocialStoryAgent` | Ollama: generate a short social-media story. |
| 8 | `VideoScriptAgent` | Ollama: generate a short-form video script. |

`NewsPipelineOrchestrator` wires the agents together; results are written to `data/articles.json`
and the processed URLs are appended to `data/processed.json`.

## Data files (in `data/`)

- **`website.json`** — input list of sources: `{ "sources": [ { "name", "url", "type": "BRAND|NEWS" } ] }`.
- **`processed.json`** — list of URLs already handled (used for dedupe; updated after each run).
- **`articlelist.json`** — intermediate output (title + summary of fresh car articles).
- **`articles.json`** — final output (article + social story + video script per link).

## Prerequisites

- JDK 17+
- [Ollama](https://ollama.com) running locally with a chat model pulled:
  ```bash
  ollama serve
  ollama pull llama3.1
  ```

## Build & run

```bash
mvn clean package
java -jar target/selectcaragent-0.1.0.jar
```

### Stage selection

```bash
# full pipeline (default)
java -jar target/selectcaragent-0.1.0.jar --stage=all

# only scan/extract -> writes articlelist.json (no Ollama needed)
java -jar target/selectcaragent-0.1.0.jar --stage=scan

# only generate from an existing articlelist.json (needs Ollama)
java -jar target/selectcaragent-0.1.0.jar --stage=generate
```

## Configuration

Override via environment variables or `--flags` (see `src/main/resources/application.yml`):

| Property | Env var | Default |
|----------|---------|---------|
| `spring.ai.ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `spring.ai.ollama.chat.options.model` | `OLLAMA_MODEL` | `llama3.1` |
| `selectcar.data-dir` | `SELECTCAR_DATA_DIR` | `data` |
| `selectcar.lookback-days` | | `7` |
| `selectcar.max-links-per-source` | | `25` |
| `selectcar.min-article-words` | | `1200` |
