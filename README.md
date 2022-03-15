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
  DUDE_LINGUIST_FILE: ${instrument}/languages.yml # a file containing languages to extension mappings according to [GitHub Linguist](https://github.com/github/linguist/blob/master/lib/linguist/languages.yml)
  DUDE_IGNORE_FILE: ${instrument}/.ignore # a file containing the patterns that DuDe should ignore in the analysis.
  DUDE_WHITELIST_FILE: ${instrument}/.whitelist # a file containing the names (one per line) of files relative to the root folder that will be taken into consideration for the analysis. When used, ignores any other filters.

```
