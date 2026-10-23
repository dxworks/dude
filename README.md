# DuDe
 This repository is a CLI adaptation of the [original DuDe project](https://wettel.github.io/dude.html) developed by [Richard Wettel](https://wettel.github.io/index.html).
 
## Installation
Please download one of our [Releases from Github](https://github.com/dxworks/dude/releases), or use as a Voyager instrument.

### Use in Voyenv:

```yaml
instruments:
  - name: dxworks/dude
    asset: dude-voyager.zip
```

## Summary Artifacts

DuDe extraction writes duplication outputs in `results/`:

- `<project>-internal_duplication.json`
- `<project>-external_duplication.csv`

Use the Python helper to generate Voyager summary artifacts from these files:

```bash
python3 dude-summary.py results
```

On Windows you can use:

```bash
py -3 dude-summary.py results
```

This command creates:

- `results/summary.md`
- `results/summary.html`

If both DuDe duplication outputs are missing, the script generates missing
summary artifacts.

### Configure in Voyager

To configure DuDe as a Voyager instrument you can add the following parameters and environment variables in the `mission.yml` file:

```yaml
# A map of instrument names to commands and parameters.
# When 'runsAll' is false the mission will run only the instruments
# with the commands declared here, in this order.
instruments:
  dude:
    # A map of parameter name to value
    parameters:
      max-heap: 8g # will configure the maximum heap space the jvm process will get. For large process may be needed to be set to 16g or higher

# A map of environment variables, name to value, for voyager missions
# overwrites the variables from global config, instrument and command
# Only set the environment variables you need. They will override the default values set here.
environment:
  DUDE_MIN_CHUNK: 10 # the minimum number of uninterrupted lines of duplicated code
  DUDE_MAX_LINEBIAS: 2 # the maximum number of uninterrupted non-duplicated lines that separate two chunks of duplicated lines
  DUDE_MIN_LENGTH: 30 # the minimum lines of code involved in a duplication chain (including “gaps”)
  DUDE_MAX_LINESIZE: 500 # dude will ignore lines longer than this value
  DUDE_MAX_FILESIZE: 10000 # dude will ignore files containing a number of lines larger than this value
  DUDE_MIN_FILESIZE: 50 # dude will ignore files containing a number of lines smaller than this value
  DUDE_EXTENSIONS: ".java,.js,.ts,.php,.c,.cc,.cpp,.h,.hh,.hpp,.cs,.sql,.lua,.groovy" # the default extensions to analyse 
  DUDE_LANGUAGES: "java,groovy,kotlin,javascript,typescript,vue,c,c++,c#,php,python,ruby,rust,dart,perl,lua,cobol,sql" # the default languages to analyse
  DUDE_LINGUIST_FILE: /absolute/path/to/languages.yml # a file containing languages to extension mappings according to [GitHub Linguist](https://github.com/github/linguist/blob/master/lib/linguist/languages.yml), see "Use your own languages.yml" below
  DUDE_IGNORE_FILE: ${instrument}/.ignore # a file containing the patterns that DuDe should ignore in the analysis.
  DUDE_WHITELIST_FILE: ${instrument}/.whitelist # a file containing the names (one per line) of files relative to the root folder that will be taken into consideration for the analysis. When used, ignores any other filters.

```

### Use your own languages.yml

DuDe uses a `languages.yml` file to decide which files belong to the languages it analyses (`DUDE_LANGUAGES`). The file is in the [GitHub Linguist](https://github.com/github/linguist/blob/master/lib/linguist/languages.yml) format: each language with its extensions and file names. By default DuDe uses the `languages.yml` shipped next to `dude.jar`, which comes from [dx-linguist](https://github.com/dxworks/dx-linguist).

To use your own file in Voyager (V1 and V2), set `DUDE_LINGUIST_FILE` in the `environment:` section of `mission.yml`, with an absolute path. The analysis and the DuDe summary (Voyager V2) then both use it:

```yaml
environment:
  DUDE_LINGUIST_FILE: /absolute/path/to/languages.yml
```

In Voyager V2 you can also set it for DuDe only. Set it on both steps, `start` (the analysis) and `summary`: a variable set on one step does not reach the other.

```yaml
instruments:
  dude:
    actions:
      start:
        environment:
          DUDE_LINGUIST_FILE: /absolute/path/to/languages.yml
      summary:
        environment:
          DUDE_LINGUIST_FILE: /absolute/path/to/languages.yml
```

- Use an absolute path. In the mission, `${instrument}` is not the DuDe folder: Voyager V1 passes it on as plain text, and Voyager V2 replaces it with the instrument name. In Voyager V2 only, `${instrumentPath}/languages.yml` points to a file placed in the DuDe instrument folder.
- If the file does not exist, or its name does not end in `.yml` or `.yaml`, DuDe silently uses `~/.linguist/languages.yml` instead (downloaded from GitHub Linguist the first time). If the results do not change, check the path.
- When an extension is listed under several languages, the analysis takes the file if any of them is in `DUDE_LANGUAGES`. The summary shows the first of them in the file.

Without Voyager, DuDe takes the first one set of: the `-linguistFile=<path>` argument, the `DUDE_LINGUIST_FILE` environment variable, `linguist.file=<path>` in `config.txt`. Otherwise it uses `languages.yml` in the working folder. Relative paths are relative to the working folder. `dude-summary.py` reads `DUDE_LINGUIST_FILE` and `config.txt` the same way, but it cannot see the `-linguistFile` argument: if you run it yourself, set the file with one of the other two.
