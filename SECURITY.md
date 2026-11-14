# Security Policy

## Supported versions

OpenESPI-GreenButton-Java is **pre-release**: there is no released version yet, and nothing in
this repository should be deployed with real customer data. Security fixes are made on `main`.

| Version | Supported |
|---|---|
| `main` (4.0.0-SNAPSHOT) | Yes: fixes land here |
| Any earlier tag or fork | No |

## Reporting a vulnerability

**Please do not report security vulnerabilities in public GitHub issues, discussions or pull
requests.**

Report them privately through GitHub:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability**
   ([direct link](https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/security/advisories/new)).
3. Describe the issue, the affected module (`openespi-authserver`, `openespi-datacustodian`,
   `openespi-thirdparty`, `openespi-common` or `openespi-handoff`), the steps to reproduce it,
   and its impact.

Your report is visible only to you and the maintainers.

## What to expect

- We aim to acknowledge a report within **5 business days**.
- We will confirm or rule out the issue, and keep you informed while we work on a fix.
- Fixes are developed in a private advisory fork. The advisory is published, with a CVE where
  appropriate, once a fix is on `main`.
- We are happy to credit reporters in the advisory. Tell us if you prefer to stay anonymous.

## Scope

In scope: the code in this repository, including its OAuth2 flows (authorization server,
resource server, third-party client), ESPI XML handling and the build and CI configuration.

Out of scope: vulnerabilities in third-party dependencies that are already publicly known
(Dependabot tracks these), and deployments of this code operated by others.
