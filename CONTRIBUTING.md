# Contributing to the Besu GraphQL plugin

Thanks for taking the time to contribute! This plugin serves Besu's
[EIP-1767](https://eips.ethereum.org/EIPS/eip-1767) GraphQL API. It was extracted from
[Besu](https://github.com/besu-eth/besu), and contributions follow the same practices as Besu's.

## Ground rules

- **Issues**: report bugs and suggest enhancements in the
  [issues tab](https://github.com/jflo/besu-graphql-plugin/issues). Security issues: see
  [SECURITY.md](SECURITY.md), never a public issue.
- **DCO**: every commit must be signed off (`git commit -s`), see [DCO.md](DCO.md).
- **Code of conduct**: see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
- **Changelog**: user-visible changes get an entry under `## Unreleased` in
  [CHANGELOG.md](CHANGELOG.md).

## Development

You need git and a JDK 17 or newer to run Gradle. The build compiles with a Java 25 toolchain,
which Gradle downloads if it is not installed. While the plugin targets an unreleased Besu, the
first step below builds Besu, which needs a JDK 25 (`JAVA_HOME`, or `java` on the `PATH`).

```sh
./gradlew build            # spotless check, errorprone, javadoc, all tests, distribution checks
./gradlew spotlessApply    # format the code (google-java-format, as Besu)
./gradlew integrationTest -PbesuHome=<Besu installation>   # see README.md, "Testing"
```

The plugin is built against the Besu version in `gradle/libs.versions.toml`. While that is an
unreleased `-SNAPSHOT` version, run `scripts/prepare-besu.sh` once to build and publish it locally
(README.md, "Building against an unreleased Besu").

### What reviewers look for

- **Compatibility first.** Users switch between Besu's former core GraphQL service and this plugin
  without noticing. The spec fixtures in `src/test/resources/org/hyperledger/besu/plugins/graphql/`
  are byte-identical to Besu's former fixtures, and `graphqlextra/` holds responses recorded from
  Besu's core service. Do not edit a fixture to make a test pass; a changed response is a
  behaviour change and needs a changelog entry.
- **Plugin API only.** The main source set may only use Besu's plugin API
  (`org.hyperledger.besu.plugin.*`), `org.hyperledger.besu.datatypes`/`evm` types exposed by it,
  and third-party libraries; `./gradlew check` fails otherwise (`verifyPluginApiOnly` in
  `build.gradle` lists the allowed packages). Besu internals are for tests only. If the plugin
  needs something the API lacks, propose the API change to Besu first.
- **Dependencies.** Libraries Besu ships are compileOnly (the besu-plugin-distribution Gradle plugin
  takes care of this). Anything else ends up in the plugin distribution zip; keep that list short.
- **Option names.** New options are named `--plugin-graphql-*` only. The deprecated `--graphql-*`
  aliases exist for compatibility and are not extended.

### Pull requests

Keep pull requests focused, with a description of the change and how it was tested. CI runs
`./gradlew build` and the integration tests on every pull request; the DCO check must pass.
Maintainers merge with squash merges.

## Other useful links

- [Besu contribution guidelines](https://github.com/besu-eth/besu/blob/main/CONTRIBUTING.md)
- [Besu plugin API](https://github.com/besu-eth/besu/tree/main/plugin-api)
- [Besu Discord](https://discord.com/invite/hyperledger)
