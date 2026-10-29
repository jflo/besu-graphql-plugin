# Maintainers

## Active maintainers

| Name | GitHub | Role |
| --- | --- | --- |
| Justin Florentine | [@jflo](https://github.com/jflo) | Maintainer (interim, until a maintenance team adopts the repository) |

## Responsibilities

Maintainers:

- review and merge pull requests (squash merges, DCO check green, CI green);
- triage issues and security reports ([SECURITY.md](SECURITY.md));
- keep the plugin building against the latest Besu release (see "Release cadence");
- cut releases (README.md, "Releasing");
- coordinate with the Besu maintainers while Besu bundles the plugin in its distribution.

## Release cadence

Besu releases about monthly. Besu warns about (or, with `--plugins-verification-mode=FULL`, refuses)
an installed plugin built against another Besu version (README.md, "Compatibility"), so there is a
plugin release for every Besu release, even when nothing else changed. Once Besu X.Y.Z is
released, a maintainer:

1. bumps `besu` in `gradle/libs.versions.toml` to X.Y.Z, and adds a row for the new plugin version
   at the top of the compatibility table in README.md (`./gradlew check` verifies the first row);
2. fixes what the bump breaks: usually only the test harness (see "Besu internals in the tests");
3. runs `./gradlew build` and the integration tests against that Besu
   (`./gradlew integrationTest -PbesuHome=<Besu X.Y.Z>`);
4. adds a CHANGELOG entry ("Built against Besu X.Y.Z") and releases a new plugin version.

### Bundled releases

While Besu bundles the plugin, Besu's build fetches the version named by `graphqlPluginVersion` in
Besu's `gradle.properties`, with checksums in Besu's `gradle/verification-metadata.xml`. A plugin
release can only be built against a released Besu, so Besu X.Y.Z bundles a plugin release built
against an earlier Besu (normally the latest plugin release, built against the previous Besu
release). That is fine: Besu does not verify the Besu version of a bundled plugin, and its Docker
image tests check that the bundled plugin serves GraphQL. Before the Besu release freeze, tell the
Besu release engineer which plugin version to bundle, with the sha256 of its jar and POM from the
release notes; they are only needed again when the bundled version changes.

## Contracts with Besu

Besides the plugin API, the plugin relies on the following Besu behaviour. None of it is part of
the plugin API, so check it when Besu changes the code named here; the plugin's tests cover each
point against the Besu it is built with, and the `asBundledWithBesuOffersTheDeprecatedNames`
integration test covers them against a bundling Besu distribution.

| The plugin relies on | Plugin code | Besu code |
| --- | --- | --- |
| Besu loads bundled plugins from a directory named `bundled-plugins` (the plugin offers the deprecated names only from there) | `GraphQLCliOptions.BUNDLED_PLUGINS_DIRECTORY` | `PluginConfiguration.getBundledPluginsDir()` (ethereum/core) |
| Besu accepts the `--graphql-` prefix for the `graphql` namespace from a bundled plugin, and checks every name before it adds any option | `GraphQLCliOptions.register` | `PicoCLIOptionsImpl.LEGACY_OPTION_PREFIXES` and `addPicoCLIOptions` (app) |
| The options added through `PicoCLIOptions` become a mixin of Besu's root command, reachable through `OptionSpec.command()` | `GraphQLCliOptions.getCommandLine` | `PicoCLIOptionsImpl.addPicoCLIOptions` |
| Besu's default value provider reads the environment, then the configuration file, trying each name of an option in turn; environment variables are named `BESU_` + the option name in upper case with `_` | `GraphQLCliOptions.legacyUse`, `ignoredLegacyValue`, `envVarName` | `EnvironmentVariableDefaultProvider`, `TomlConfigurationDefaultProvider`, `CascadingDefaultProvider` (app, `cli.util`) |
| A `ParameterException` from `beforeExternalServices` fails Besu's command line | `GraphQLPlugin.validateOptions` | `BesuPluginContextImpl.beforeExternalServices` |

The longer-term fix is a plugin API for these needs (option aliases with deprecation, or telling a
plugin that it is bundled); propose it to Besu before relying on more of Besu's internals.

## Besu internals in the tests

The main code may only use Besu's plugin API (`./gradlew check` enforces it, see
`verifyPluginApiOnly`), but the tests build a real Besu chain with Besu internals:
`TestChainServices` (blockchain, world state, transaction simulator and pool services) and
`BesuHost`/`LegacyPrefixPicoCLIOptions` (Besu's option handling). Their constructors change often,
so a Besu bump regularly breaks the test compilation while the main code still compiles: fix it in
those classes, which keep all of that wiring in one place, without touching the tests themselves.

## Adopting the repository (maintenance team hand-over)

The repository is currently `jflo/besu-graphql-plugin`. A team adopts it as follows:

1. **Transfer** the repository to the team's organisation (for example `besu-eth`) with GitHub's
   *Transfer ownership*; GitHub redirects the old URLs.
2. **Owners**: replace the placeholder in [.github/CODEOWNERS](.github/CODEOWNERS) with the team,
   update the table above, and protect `main` (required reviews from code owners, required checks
   `CI / build` and `DCO / DCO`).
3. **Coordinates**: update the Maven group. `group` in `gradle.properties` is
   `com.github.jflo` (JitPack's group for this repository); after a transfer it becomes
   `com.github.<org>` on JitPack, or the organisation's own group (for example
   `org.hyperledger.besu.plugins`) once releases go to Maven Central or Besu's Maven repository.
   Update the README, `jitpack.yml` comment and the `pom` URLs in `build.gradle`, and note the
   change in the CHANGELOG.
4. **Links**: replace `jflo/besu-graphql-plugin` in README.md, CONTRIBUTING.md, SECURITY.md,
   `.github/pull_request_template.md` and `build.gradle`.
5. **Security**: enable private vulnerability reporting and Dependabot alerts in the repository
   settings, and make sure the Besu security team knows who to contact.
6. **Besu**: tell the Besu maintainers the new coordinates, so Besu's build fetches the bundled
   plugin from the new location.
7. **Snapshot source**: once a Besu release contains besu-eth/besu#11744, point `besu` at it and
   delete `besuSnapshotRepository`/`besuSnapshotCommit` from `gradle.properties` together with the
   `mavenLocal()` repository in `build.gradle`. Until then, a new commit of besu-eth/besu#11744 is
   picked up by changing `besuSnapshotCommit`.
8. **Maven repository**: publish releases to Maven Central or Besu's Maven repository (for example
   under `org.hyperledger.besu.plugins`) instead of JitPack, so that Besu's build does not depend
   on JitPack building the artifacts on demand.

## Becoming a maintainer

Contributors with a track record of reviews and merged pull requests can be nominated by an active
maintainer in a pull request against this file; it is merged when a majority of the active
maintainers approve.
