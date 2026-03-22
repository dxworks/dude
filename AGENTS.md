# DuDe — Duplication Detector

## Project Overview

DuDe is a CLI tool for detecting code duplication across source files. It is a CLI adaptation of the [original DuDe project](https://wettel.github.io/dude.html) by Richard Wettel. It uses a suffix-tree-based algorithm to find exact, modified, and composed duplication patterns.

The tool is also packaged as a [Voyager](https://github.com/dxworks/voyager) instrument (`dude-voyager.zip`).

## Build & Run

- **Language:** Java 21
- **Build tool:** Maven (wrapper included: `./mvnw`)
- **Build:** `./mvnw clean package`
- **Output:** `target/dude.jar` (fat JAR with all dependencies)
- **Run:** `java -Xmx8g -jar target/dude.jar project.name=myproject project.folder=/path/to/src`
- **Main class:** `lrg.dude.duplication.DuDe`

### Configuration

Parameters can be set via CLI args, environment variables (prefixed `DUDE_`), or `config.txt`:

| Parameter | Default | Description |
|---|---|---|
| `project.name` | (required) | Name for output files |
| `project.folder` | `.` | Root folder to scan |
| `results.folder` | `results` | Output directory |
| `min.length` | `30` | Minimum lines in a duplication chain |
| `min.chunk` | `10` | Minimum uninterrupted duplicated lines |
| `max.linebias` | `2` | Max gap lines between chunks |
| `max.linesize` | `500` | Ignore lines longer than this |
| `max.filesize` | `10000` | Ignore files with more lines |
| `min.filesize` | `50` | Ignore files with fewer lines |
| `extensions` | `.java,.js,.ts,...` | File extensions to analyse |
| `languages` | `java,groovy,...` | Language names for linguist matching |

## Project Structure

```
src/main/java/lrg/dude/duplication/   — Core duplication detection logic
  DuDe.java                           — CLI entry point, configuration
  SuffixTreeProcessor.java            — Main algorithm (suffix trie matching)
  Processor.java                      — Base processor class
  Entity.java / SourceFile.java       — Represent analysed files
  Duplication.java                    — Duplication result model
  MatrixLine.java / VirtualMatrix.java — Dot-matrix data structures
  DuplicationUtil.java               — Code cleaning utilities

src/main/java/org/ardverk/collection/ — Embedded Patricia trie implementation

scripts/
  build.sh                            — Build script (wraps mvnw)
  prepare-release.sh                  — Package standard release ZIP
  prepare-release-voyager.sh          — Package Voyager instrument ZIP
  regression-test.sh                  — Compare output against latest release

config.txt                            — Default configuration file
languages.yml                         — GitHub Linguist language definitions
instrument.yml                        — Voyager instrument descriptor
.ignore                               — Patterns to exclude from analysis
```

## Dependencies

- `jackson-databind` — JSON serialization for output
- `dx-ignore` (`org.dxworks.utils`) — Gitignore-style file filtering
- `dx-linguist` (`org.dxworks.utils`) — Language detection by file extension
- `argumenthor` (`org.dxworks.utils`) — CLI argument/config/env parsing

## Docker

- **Image:** `dxworks/dude` on Docker Hub
- **Base:** `eclipse-temurin:21-jre-alpine`
- **Build:** `docker build -t dude-test .` (requires `target/dude.jar` — run `./mvnw clean package` first)
- **Run:** `docker run -v /path/to/project:/project -e DUDE_PROJECT_NAME=myproject -e DUDE_PROJECT_FOLDER=/project dude-test`

## CI/CD

- **Release:** Tag `v*` triggers `release.yml` — gate → archive + docker (parallel) → GitHub Release
- **Voyager release:** Tag `v*-voyager` triggers `release-voyager.yml` — same flow plus dispatch to `dxworks/voyager`
- **Security:** `trivy-security-scan.yml` runs on PRs to `dev` (filesystem + Docker image scan); `trivy-daily-scan.yml` runs daily
- **Regression:** `regression-test.yml` runs on PRs (non-blocking) and manual dispatch

All release workflows use reusable pipelines from `dxworks/pipelines@v1`.

## Branching Strategy

- Main development branch: `dev`
- Release tags: `v<semver>` (standard) and `v<semver>-voyager` (Voyager instrument)

## Output Format

DuDe produces two output files in the results folder:

- `<project>-internal_duplication.json` — JSON array of per-file internal duplication metrics
- `<project>-external_duplication.csv` — CSV of inter-file duplication pairs (`file1,file2,length`)
