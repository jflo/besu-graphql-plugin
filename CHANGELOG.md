# Changelog

All notable changes to this plugin are documented here. Versions are semantic versions of the
plugin; each entry names the Besu version it was built and tested against.

## Unreleased

Built against Besu `26.11.0-graphqlapi-SNAPSHOT` (besu-eth/besu#11744); requires the first Besu
release that contains besu-eth/besu#11744.

### Added
- Initial extraction of Besu's GraphQL API (EIP-1767) from Besu into this standalone plugin
  repository, with the history of Besu's core GraphQL sources (see `docs/HISTORY.md`). The plugin
  serves the same endpoint, schema and responses as Besu's former core GraphQL service, reading
  all data through Besu's plugin API (besu-eth/besu#11744), and passes Besu's GraphQL spec
  fixtures byte for byte.
- Options `--plugin-graphql-http-enabled`, `--plugin-graphql-http-host`,
  `--plugin-graphql-http-port`, `--plugin-graphql-http-cors-origins`, `--plugin-graphql-tls-enabled`,
  `--plugin-graphql-tls-keystore-file`, `--plugin-graphql-tls-keystore-password-file`,
  `--plugin-graphql-mtls-enabled`, `--plugin-graphql-tls-truststore-file`,
  `--plugin-graphql-tls-truststore-password-file` and `--plugin-graphql-max-blocks-range`, with the
  defaults and validation of Besu's former `--graphql-*` options.
- The plugin is discovered from Besu's plugins directory; the distribution zip ships graphql-java
  and java-dataloader, which Besu no longer provides.
- Integration tests against a real Besu installation (`./gradlew integrationTest -PbesuHome=...`).

### Deprecated
- When Besu bundles the plugin, the former option names `--graphql-*` (TOML keys `graphql-*`,
  environment variables `BESU_GRAPHQL_*`) remain accepted as aliases of the `--plugin-graphql-*`
  options and log a deprecation warning. The plugin offers them only when Besu loads it from its
  `bundled-plugins` directory (or `-Dbesu.plugin.graphql.legacy-option-names=true` is set). They,
  and the bundling of the plugin in Besu's distribution, will be removed in a future release.
