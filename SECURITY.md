# Security Policy

## Reporting a Vulnerability

If you believe you have found a security vulnerability in the Besu GraphQL plugin, please report it
privately. **Do not open a public issue.**

### Preferred method: GitHub private vulnerability reporting

Use the **[Report a vulnerability](https://github.com/jflo/besu-graphql-plugin/security/advisories/new)**
button in the Security tab of this repository. This opens a private draft advisory visible only to
the maintainers listed in [MAINTAINERS.md](MAINTAINERS.md).

### If the vulnerability is in Besu itself

The plugin reads all of its data through Besu's plugin API. If the issue lies in Besu (including
the plugin API service implementations the plugin calls), report it to Besu instead, following
[Besu's security policy](https://github.com/besu-eth/besu/security/policy): the
[Report a vulnerability](https://github.com/besu-eth/besu/security/advisories/new) button of
besu-eth/besu, or email [security-besu@lists.hyperledger.org](mailto:security-besu@lists.hyperledger.org).
If you are not sure, report it to Besu: Besu currently bundles this plugin in its distribution, so
the Besu security team needs to know about vulnerabilities in either.

When reporting, include a description of the vulnerability and any relevant detail: reproduction
steps (for example a GraphQL query), affected plugin and Besu versions, and any known active
exploitation.

### How we handle reports

- Acknowledgement: we aim to acknowledge receipt within 5 business days.
- Fix development: fixes are developed privately, in coordination with the Besu security team when
  the fix must ship in a Besu release that bundles the plugin.
- Release: the fix ships in a plugin release and, while Besu bundles the plugin, in a Besu release.
- Disclosure: after a fixed version has been available for a reasonable period, we publish a GitHub
  Security Advisory with the details, affected versions, fixed version and reporter credit (with
  your consent).
