## PR description

## Fixed Issue(s)
<!-- Please link to fixed issue(s) here using format: fixes #<issue number> -->
<!-- Example: "fixes #2" -->


### Thanks for sending a pull request! Have you done the following?

- [ ] Checked out our [contribution guidelines](https://github.com/jflo/besu-graphql-plugin/blob/main/CONTRIBUTING.md)?
- [ ] Signed off every commit (DCO, `git commit -s`)?
- [ ] Considered the changelog and included an update if required.
- [ ] Kept GraphQL responses byte-identical to the spec fixtures, or explained why a fixture changes.

### Locally, you can run these tests to catch failures early:

- [ ] build, formatting and tests: `./gradlew build`
- [ ] integration tests: `./gradlew integrationTest -PbesuHome=$(scripts/prepare-besu.sh | tail -n 1)`
