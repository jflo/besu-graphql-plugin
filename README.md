# Besu GraphQL Plugin
 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
 [![CI](https://github.com/jflo/besu-graphql-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/jflo/besu-graphql-plugin/actions/workflows/ci.yml)

The [EIP-1767](https://eips.ethereum.org/EIPS/eip-1767) GraphQL API for
[Besu](https://github.com/besu-eth/besu), as a Besu plugin.

GraphQL used to be part of Besu's core. It now lives in this plugin, which reads all of its data
through Besu's plugin API and serves the same endpoint, schema and responses as Besu's former core
GraphQL service. Besu bundles the plugin in its distribution for now, so existing users see no
change; installing it from this repository and using its own option names is the way forward (see
[Relationship to Besu](#relationship-to-besu-and-deprecation-plan)).

## Compatibility

The plugin needs the plugin API added to Besu by
[besu-eth/besu#11744](https://github.com/besu-eth/besu/pull/11744) (chain query, call simulation
and network endpoint services). No Besu release contains it yet.

| Plugin version | Built and tested against Besu | Works with Besu |
| --- | --- | --- |
| Unreleased (`0.1.0-SNAPSHOT`) | `26.11.0-graphqlapi-SNAPSHOT` (besu-eth/besu#11744 at `f417ae0e6d`, built locally) | the first Besu release containing besu-eth/besu#11744, and later |

The Besu version the plugin is built against is `besu` in
[gradle/libs.versions.toml](gradle/libs.versions.toml); `./gradlew check` verifies that the table
above names it. It must be bumped to a real Besu release as soon as one contains
besu-eth/besu#11744.

## Install

1. Download `besu-graphql-plugin-<version>.zip` from the
   [releases](https://github.com/jflo/besu-graphql-plugin/releases) (or build it, see
   [Building](#building)).
2. Unzip its jars into Besu's `plugins` directory:

   ```sh
   unzip -j besu-graphql-plugin-<version>.zip -d "$BESU_HOME/plugins"
   ```

   With Docker, mount or copy them into `/opt/besu/plugins`. Another directory can be used with
   `BESU_OPTS="-Dbesu.plugins.dir=<dir>"`.
3. Restart Besu and enable the service with `--plugin-graphql-http-enabled`. Besu logs
   `Registered plugin of type org.hyperledger.besu.plugins.graphql.GraphQLPlugin` at startup.

If you select plugins with Besu's `--plugins` option, include `GraphQLPlugin`.

The zip contains the plugin jar and the libraries Besu does not provide (graphql-java,
java-dataloader, reactive-streams). Everything else (Vert.x, Jackson, Guava, picocli, Tuweni,
SLF4J and the plugin API) comes from Besu.

## Usage

```sh
besu --plugin-graphql-http-enabled --plugin-graphql-http-port=8547 ...
```

Like every Besu option, each option can also be set in the TOML configuration file
(`plugin-graphql-http-enabled=true`) or as an environment variable
(`BESU_PLUGIN_GRAPHQL_HTTP_ENABLED=true`).

### Configuration

| Option | Deprecated name (bundled Besu only) | Default | Description |
| --- | --- | --- | --- |
| `--plugin-graphql-http-enabled` | `--graphql-http-enabled` | `false` | Set to start the GraphQL HTTP service |
| `--plugin-graphql-http-host=<HOST>` | `--graphql-http-host` | Besu's default host address (`127.0.0.1`) | Host for GraphQL HTTP to listen on |
| `--plugin-graphql-http-port=<PORT>` | `--graphql-http-port` | `8547` | Port for GraphQL HTTP to listen on |
| `--plugin-graphql-http-cors-origins` | `--graphql-http-cors-origins` | none | Comma separated origin domain URLs for CORS validation (`all` or `*` for any) |
| `--plugin-graphql-tls-enabled` | `--graphql-tls-enabled` | `false` | Enable TLS for GraphQL HTTP service |
| `--plugin-graphql-tls-keystore-file` | `--graphql-tls-keystore-file` | | Path to the TLS keystore file for GraphQL HTTP service |
| `--plugin-graphql-tls-keystore-password-file` | `--graphql-tls-keystore-password-file` | | Path to the file containing the password for the TLS keystore |
| `--plugin-graphql-mtls-enabled` | `--graphql-mtls-enabled` | `false` | Enable mTLS for GraphQL HTTP service |
| `--plugin-graphql-tls-truststore-file` | `--graphql-tls-truststore-file` | | Path to the TLS truststore file for GraphQL HTTP service |
| `--plugin-graphql-tls-truststore-password-file` | `--graphql-tls-truststore-password-file` | | Path to the file containing the password for the TLS truststore |
| `--plugin-graphql-max-blocks-range` | `--graphql-max-blocks-range` | `5000` | Maximum number of blocks a single `blocks(from,to)` query may span; `0` for no limit |

The service also follows these Besu options, as Besu's core GraphQL service did:
`--host-allowlist` (allowed `Host` headers), `--Xhttp-timeout-seconds` (request timeout) and
`--rpc-max-log-filter-addresses` (addresses per `logs` filter).

### Endpoint

- `POST /graphql` with `Content-Type: application/json` (`{"query": ..., "variables": ...}`) or
  `application/graphql`, and `GET /graphql?query=...`.
- Schema: [schema.graphqls](src/main/resources/org/hyperledger/besu/plugins/graphql/schema.graphqls)
  (EIP-1767 with Besu's extensions).
- The service is reported as `graphQL` by the `net_services` JSON-RPC method and as `graphql-http`
  in the `besu.ports` file in the data directory, as before.

```sh
curl -s -X POST -H 'Content-Type: application/json' \
  --data '{"query":"{block{number hash}}"}' http://127.0.0.1:8547/graphql
```

### The deprecated `--graphql-*` names

When Besu bundles this plugin, it also accepts the option names of its former core GraphQL service
for it: `--graphql-http-enabled` and so on, the TOML keys `graphql-*` and the environment variables
`BESU_GRAPHQL_*`. Each of them sets the same option as its `--plugin-graphql-*` counterpart, and
using one logs a warning naming the replacement, for example:

```
WARN | GraphQLPlugin | Option --graphql-http-enabled is deprecated and will be removed in a future release, use --plugin-graphql-http-enabled instead
```

If both names are given, the command line wins over the environment, which wins over the
configuration file, as usual; within the configuration file or the environment the
`--plugin-graphql-*` name wins.

A Besu that does not bundle the plugin only accepts the `--plugin-graphql-*` names: the
deprecated names are unknown options on its command line and in its configuration file, and
`BESU_GRAPHQL_*` environment variables are ignored (the plugin logs a warning for each). The
plugin cannot ask Besu which names it accepts, so it offers both and falls back to the
`--plugin-graphql-*` names when Besu refuses; Besu logs an error for each refused name
(`Plugin option --graphql-http-enabled did not have the expected prefix of --plugin-graphql-`),
followed by an INFO line from the plugin saying they can be ignored. Start Besu with
`BESU_OPTS=-Dbesu.plugin.graphql.legacy-option-names=false` to skip the attempt.

## Relationship to Besu and deprecation plan

- The plugin was extracted from Besu (besu-eth/besu#11745) together with the history of Besu's
  core GraphQL sources, see [docs/HISTORY.md](docs/HISTORY.md). Besu then removed its own GraphQL
  service.
- **Bundled (deprecated).** To keep existing deployments working, Besu's distribution currently
  ships a release of this plugin and accepts the deprecated `--graphql-*` names for it. Both the
  bundling and the `--graphql-*` names are **deprecated** and will be removed in a future Besu
  release.
- **Recommended.** Install the plugin from this repository and use the `--plugin-graphql-*`
  names (and `plugin-graphql-*` TOML keys, `BESU_PLUGIN_GRAPHQL_*` variables). These work with
  bundled and unbundled Besu alike, so you can migrate now.
- Plugin releases follow Besu releases: each plugin release states the Besu version it was built
  and tested against (see [Compatibility](#compatibility) and [CHANGELOG.md](CHANGELOG.md)).

## Building

Requires git and a JDK 17+ to run Gradle; the code compiles with a Java 25 toolchain, which Gradle
downloads when needed.

```sh
./gradlew build          # format check, errorprone, javadoc, tests, distribution checks
./gradlew distZip        # build/distributions/besu-graphql-plugin-<version>.zip
./gradlew publishToMavenLocal
```

`-PbesuVersion=<version>` builds against another Besu version. The
[besu-plugin-distribution](https://github.com/Consensys-Incorporated/besu-plugin-gradle-plugin)
Gradle plugin resolves that version's BOM and artifact catalog from Besu's Maven repository
(`https://hyperledger.jfrog.io/hyperledger/besu-maven/`) and makes every library Besu ships
`compileOnly`.

### Building against an unreleased Besu

While `besu` in `gradle/libs.versions.toml` is a `-SNAPSHOT` version, its artifacts are taken
from the local Maven repository (`~/.m2`, Besu artifacts only). Publish them once with:

```sh
scripts/prepare-besu.sh
```

which clones Besu from `besuSnapshotRepository` at `besuSnapshotRef` (`gradle.properties`), runs
`./gradlew publishToMavenLocal installDist -Pversion=<besu version>` there and prints the path of the
installed Besu. CI does the same. Equivalently, from a Besu checkout of that branch:
`./gradlew publishToMavenLocal -Pversion=26.11.0-graphqlapi-SNAPSHOT`.

### Maven coordinates

`com.github.jflo:besu-graphql-plugin:<version>` (jar, sources, javadoc and the distribution zip as
`@zip`). Until the plugin is published to Maven Central, released versions are available from
[JitPack](https://jitpack.io/#jflo/besu-graphql-plugin), which builds them from the release tags:

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.github.jflo:besu-graphql-plugin:<version>' }
```

The group changes when the repository is transferred to its maintenance team (see
[MAINTAINERS.md](MAINTAINERS.md)).

## Testing

- `./gradlew test`: unit tests, and end-to-end tests that drive the plugin through its plugin
  lifecycle over Besu's real plugin service implementations and a real test chain (Besu's hive
  test chain, on Bonsai and Forest storage). They include Besu's GraphQL spec fixtures,
  byte-identical to the ones Besu's core service was tested with, plus responses recorded from
  Besu's core service (`graphqlextra/`). Option names are tested with Besu's own option parsing
  and configuration file/environment handling, as an external and as a bundled plugin.
- `./gradlew integrationTest -PbesuHome=<Besu installation>`: installs the distribution zip into
  that Besu, starts it and queries the plugin over HTTP. `scripts/prepare-besu.sh` prints a
  suitable installation. Skipped without `-PbesuHome`.

## Releasing

Plugin versions are semantic versions (`X.Y.Z`), independent of Besu's version.

1. Make sure `besu` in `gradle/libs.versions.toml` is a released Besu version (the release
   workflow refuses `-SNAPSHOT`s) and the compatibility table is up to date.
2. Rename `## Unreleased` in [CHANGELOG.md](CHANGELOG.md) to `## X.Y.Z`, set `version` in
   `gradle.properties` to the next `-SNAPSHOT`, and merge that.
3. Tag the merge commit `X.Y.Z` (signed) and push the tag. The
   [release workflow](.github/workflows/release.yml) builds and tests that version against Besu and
   creates the GitHub release with the distribution zip and jar and the changelog section as notes.
4. Request `com.github.jflo:besu-graphql-plugin:X.Y.Z` from JitPack once to have it built.
5. Tell the Besu release engineer, if Besu should bundle the new version.

## License

Apache 2.0, see [LICENSE](LICENSE).
