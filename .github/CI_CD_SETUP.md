# CI/CD Setup

GitHub Actions configuration for the OpenESPI-GreenButton-Java monorepo (Java 25, Spring Boot 4,
Maven reactor with five modules: `openespi-handoff`, `openespi-common`, `openespi-datacustodian`,
`openespi-thirdparty`, `openespi-authserver`). Releasing is described in [`RELEASING.md`](../RELEASING.md).

## Workflows

| Workflow | Triggers | What it does |
|---|---|---|
| [`ci.yml`](workflows/ci.yml) | every `pull_request` to `main` (no paths filter), `push` to `main`, `merge_group` | `test` matrix, `quality` (SonarCloud), `ci-ok` aggregator |
| [`pr-title.yml`](workflows/pr-title.yml) | `pull_request` opened / edited / synchronize / reopened, `merge_group` | `pr-title`: Conventional-Commits PR title check |
| [`sonarcloud-fork-pr.yml`](workflows/sonarcloud-fork-pr.yml) | `workflow_run` after **CI** succeeds for a fork PR | SonarCloud PR analysis for forks (which get no secrets in `ci.yml`) |
| [`security.yml`](workflows/security.yml) | `pull_request`, `push` to `main`, weekly cron (Mon 05:17 UTC), manual | OWASP dependency-check (advisory) and CodeQL |
| [`release.yml`](workflows/release.yml) | push of a tag `<module>-v<X.Y.Z>` | test, GitHub Release with jar, ECR image (datacustodian), bump PR |

Every job sets up Java through the composite action
[`.github/actions/setup-java-maven`](actions/setup-java-maven/action.yml): Temurin 25, the
`setup-java` `cache: maven` cache of `~/.m2/repository` and `MAVEN_ARGS=-B -ntp`. All workflows
default to `permissions: contents: read`; jobs add only what they need.

### `ci.yml`

- **`test (<module>)`**: matrix over the five modules, `fail-fast: false`. Each entry runs
  `mvn install -DskipTests -pl <module> -am` (it only compiles the upstream modules, about 30 s)
  and then `mvn verify -pl <module>`. Each test suite runs exactly once, in its own entry, and the
  entries run in parallel. Wall-clock time is that of the slowest module, not the sum.
  An install-once job that shares `~/.m2/repository/org/greenbuttonalliance` as an artifact was
  rejected. It would add a serial job, plus artifact upload and download, that cost more than the
  30 s compile it saves.
  - `openespi-authserver` runs only `-Dgroups=testcontainers-it` until **#129** fixes the
    unit tests on the H2 profiles.
  - Testcontainers suites use the Docker daemon on `ubuntu-latest`.
  - On failure the job uploads `surefire-reports-<module>`. It always uploads that module's
    `jacoco.xml` for the `quality` job.
- **`quality`**: one SonarCloud analysis. It runs on pushes to `main` and same-repo PRs. It
  compiles the code (`-DskipTests`) and downloads the matrix's JaCoCo reports into each
  module's `target/site/jacoco/`, then runs `sonar:sonar`. The project key, organization, host
  and coverage paths come from the root `pom.xml` `sonar.*` properties. `openespi-authserver` sets
  `sonar.skip=true`.
  - Before #193, the surefire `<argLine>` in the root pom dropped the JaCoCo agent, so no
    `jacoco.exec` was written and SonarCloud got no coverage. It now starts with `@{argLine}`.
  - The 50 %-per-package `jacoco:check` rule is now evaluated for the first time, so it is
    advisory for now (`haltOnFailure=false`). Most `openespi-thirdparty` packages, for example,
    are below 50 %. Flip it to `true` when the modules meet the rule.
- **`ci-ok`**: `if: always()`, `needs: [test, quality]`. It fails if any needed job failed or was
  cancelled. A skipped job is fine: `quality` is skipped on fork PRs and in the merge queue.
  **This is the single required check from this workflow.**

`pr-title` is a separate workflow on purpose. It must re-run on `edited` when someone fixes the
title. Inside `ci.yml`, an `edited` run would either cancel the real build (same concurrency
group) or publish a second, green `ci-ok` for the same commit.

Concurrency: superseded PR runs are cancelled. Pushes to `main` are never cancelled, because each
one feeds SonarCloud.

### `sonarcloud-fork-pr.yml`

This workflow runs in the base-repository context after a successful CI run of a fork PR. It
resolves the PR number and branch, checks out the fork's head SHA without persisted credentials,
and compiles it. It reuses the CI run's JaCoCo artifacts (`actions: read`) and runs
`sonar:sonar` with the `sonar.pullrequest.*` parameters. Untrusted values reach the shell only
through `env:`.

> Security note: this job builds fork code while `SONAR_TOKEN` is available to a later step.
> Keep `SONAR_TOKEN` an analysis-scoped SonarCloud token. No other secret is exposed. To give
> up fork-PR decoration, delete the workflow; CI is unaffected.

### `security.yml`

- **OWASP dependency-check**. It runs `mvn install -DskipTests` and then
  `mvn org.owasp:dependency-check-maven:aggregate`, which produces one report for the reactor.
  The plugin version is managed in the root pom (`owasp-dependency-check.version`). The NVD
  database is cached in `~/.m2/repository/org/owasp/dependency-check-data`. Reports (HTML, JSON,
  SARIF) are uploaded as `dependency-check-report`.
  - **Advisory until #131 is cleared:** the step has `continue-on-error: true` and no CVSS
    threshold.
  - **To make it a gate:** remove `continue-on-error` and add `-DfailBuildOnCVSS=8`.
  - The optional `NVD_API_KEY` secret makes NVD updates much faster.
- **CodeQL** (`java-kotlin`, `build-mode: none`). It extracts the source without compiling, so
  it does not depend on JDK 25 support in the CodeQL autobuilder. Results go to *Security → Code
  scanning*. It does not run on fork PRs, because they cannot upload results.

### `release.yml`

See [`RELEASING.md`](../RELEASING.md).

### Dependabot ([`dependabot.yml`](dependabot.yml))

- **Maven**: one weekly entry at the root covers the whole reactor. Updates are grouped:
  - `spring-boot`: Spring Boot, Spring Framework, Security, Data and springdoc;
  - `test-libraries`: JUnit, Mockito, AssertJ, Testcontainers, Datafaker and XMLUnit;
  - `maven-plugins`.
  - `org.greenbuttonalliance.espi:*` is ignored, because the release flow versions the modules.
- **GitHub Actions**: weekly, as one group.

Actions are pinned to **major version tags** throughout, and Dependabot keeps them current.

## Secrets and variables

| Name | Kind | Required | Used by |
|---|---|---|---|
| `SONAR_TOKEN` | secret | yes (for `quality`, fork-PR Sonar) | SonarCloud analysis token for project `GreenButtonAlliance_OpenESPI-GreenButton-Java` |
| `NVD_API_KEY` | secret | optional | dependency-check NVD updates ([request a key](https://nvd.nist.gov/developers/request-an-api-key)) |
| `AWS_ECR_ROLE_ARN` | variable | optional\* | IAM role assumed via GitHub OIDC to push the datacustodian image |
| `AWS_REGION` | variable | optional\* | Region of the ECR repository, e.g. `us-east-1` |
| `ECR_REPOSITORY` | variable | optional\* | ECR repository name, e.g. `openespi-datacustodian` |

\* If any of the three `AWS_*`/`ECR_*` variables is unset, the release workflow skips the ECR
push with a notice instead of failing. `GITHUB_TOKEN` is provided automatically.

### AWS: IAM role for GitHub OIDC → ECR push

1. Create the ECR repository, for example `openespi-datacustodian`, with tag immutability
   **off**, because `latest` is re-pushed on each release. Scan on push is recommended.
2. Add the GitHub OIDC identity provider to the AWS account, once per account:
   - provider URL `https://token.actions.githubusercontent.com`;
   - audience `sts.amazonaws.com`.
3. Create a role with the trust policy below. It allows only release-tag runs of this repository:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com" },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
        "StringLike": { "token.actions.githubusercontent.com:sub": "repo:GreenButtonAlliance/OpenESPI-GreenButton-Java:ref:refs/tags/datacustodian-v*" }
      }
    }
  ]
}
```

4. Attach a permissions policy that allows only pushing to that repository:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "ecr:GetAuthorizationToken", "Resource": "*" },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:BatchGetImage",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeImages",
        "ecr:DescribeRepositories",
        "ecr:GetDownloadUrlForLayer",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:<REGION>:<ACCOUNT_ID>:repository/openespi-datacustodian"
    }
  ]
}
```

5. Set the repository variables (*Settings → Secrets and variables → Actions → Variables*):
   - `AWS_ECR_ROLE_ARN` = the role ARN;
   - `AWS_REGION`;
   - `ECR_REPOSITORY`.

## Recommended branch ruleset for `main`

*Settings → Rules → Rulesets → New branch ruleset*, target `main`:

- Require a pull request before merging, with at least one approval. Dismiss stale approvals.
- Require status checks to pass:
  - **`ci-ok`**
  - **`pr-title`**

  Do not list the individual `test (...)` matrix jobs. `ci-ok` covers them, and the matrix can
  change without touching the ruleset.
- Require branches to be up to date before merging, or enable the merge queue. `ci.yml` and
  `pr-title.yml` already listen to `merge_group`.
- Block force pushes and deletions.
- Optional: require code scanning results (CodeQL).

Also set *Settings → Actions → General → Workflow permissions* to **Allow GitHub Actions to
create and approve pull requests**. The release workflow needs this to open its version-bump PR.

## Running the same checks locally

```bash
mvn install -DskipTests -pl openespi-datacustodian -am && mvn verify -pl openespi-datacustodian
mvn verify -pl openespi-authserver -Dgroups=testcontainers-it -DfailIfNoTests=false   # until #129
mvn install -DskipTests && mvn org.owasp:dependency-check-maven:aggregate
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest -color             # lint workflows
```
