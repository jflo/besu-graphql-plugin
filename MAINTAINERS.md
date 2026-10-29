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

Besu releases about monthly. For each Besu release, a maintainer:

1. bumps `besu` in `gradle/libs.versions.toml` and the compatibility table in README.md;
2. runs `./gradlew build` and the integration tests against that Besu;
3. adds a CHANGELOG entry and, if anything changed, releases a new plugin version.

While Besu bundles the plugin, the Besu release engineer picks the plugin version to bundle, so
plugin releases should be made ahead of the Besu release freeze.

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
   delete `besuSnapshotRepository`/`besuSnapshotRef` from `gradle.properties` together with the
   `mavenLocal()` repository in `build.gradle`.

## Becoming a maintainer

Contributors with a track record of reviews and merged pull requests can be nominated by an active
maintainer in a pull request against this file; it is merged when a majority of the active
maintainers approve.
