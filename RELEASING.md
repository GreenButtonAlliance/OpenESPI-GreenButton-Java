# Releasing

Each module is versioned and released **independently**. Every module restarted at `4.0.0`, so
on `main` each module is at `4.0.0-SNAPSHOT` until its first release.

| Module id (tag prefix) | Directory | Artifact | Kind | Release asset |
|---|---|---|---|---|
| `handoff` | `openespi-handoff` | `OpenESPI-Handoff` | library | jar |
| `common` | `openespi-common` | `OpenESPI-Common` | library | jar |
| `datacustodian` | `openespi-datacustodian` | `OpenESPI-DataCustodian` | application | Spring Boot jar + ECR image |
| `thirdparty` | `openespi-thirdparty` | `OpenESPI-ThirdParty` | application | Spring Boot jar |
| `authserver` | `openespi-authserver` | `OpenESPI-AuthorizationServer` | application | Spring Boot jar |

The parent pom, `openespi-parent`, is not released on its own.

## Versions on `main`

- Every module pom keeps its own `<version>X.Y.Z-SNAPSHOT</version>`.
- The library versions that other modules use are set in **one place**, the parent `pom.xml`:
  `openespi-handoff.version` and `openespi-common.version`, used through
  `<dependencyManagement>`. Module poms declare `OpenESPI-Handoff` and `OpenESPI-Common` without
  a version. When a library version changes, its module `<version>` and the parent property
  change together; the release workflow does both.
- Libraries are **not published** to any Maven repository. Applications are always built from
  the in-tree library sources, and their GitHub Release notes record which library versions
  they contain.

## Cutting a release

1. Make sure `main` is green and that the module's pom is at the version you want to release,
   as a SNAPSHOT. For example, `openespi-datacustodian/pom.xml` should be at `4.0.0-SNAPSHOT`.
   For a minor or major release, first merge a PR that changes the SNAPSHOT version: the module
   `<version>`, plus the parent property for a library. The workflow only ever bumps the patch.
2. Tag the commit on `main` and push the tag:

   ```bash
   git switch main && git pull
   git tag -a datacustodian-v4.0.0 -m "datacustodian 4.0.0"
   git push origin datacustodian-v4.0.0
   ```

3. [`release.yml`](.github/workflows/release.yml) then runs these jobs:
   1. **prepare**:
      - parses `<module>-v<X.Y.Z>`;
      - fails if the module is unknown, the tagged commit is not on `main`, or the module's pom
        version at that commit is not `<X.Y.Z>-SNAPSHOT`;
      - finds the previous tag of the same module.
   2. **build**:
      - sets the release version with `versions:set -pl <module>`, **in CI only**; nothing is
        committed;
      - runs the upstream modules' tests and then `mvn verify -pl <module>`. This is equivalent
        to `-am verify`. Every module, `authserver` included, runs its full test suite;
      - collects the jar. For applications this is the Spring Boot fat jar.
   3. **github-release**: creates the GitHub Release `<tag>` with:
      - generated notes, starting from the previous tag of the same module;
      - the jar, named `<Artifact>-<X.Y.Z>.jar`;
      - `SHA256SUMS`.
   4. **ecr-image** (datacustodian only):
      - builds the OCI image with `spring-boot:build-image`, using Paketo buildpacks with
        `BP_JVM_VERSION=25`;
      - pushes it to Amazon ECR as `X.Y.Z` and as `latest`, through GitHub OIDC;
      - is skipped with a notice if the `AWS_ECR_ROLE_ARN`, `AWS_REGION` or `ECR_REPOSITORY`
        variable is unset. See [`.github/CI_CD_SETUP.md`](.github/CI_CD_SETUP.md).
   5. **bump**: opens a PR `release/<module>-<X.Y.Z+1>-SNAPSHOT` that sets the module to
      `<X.Y.Z+1>-SNAPSHOT` on `main`. For a library, the PR also sets the parent property.
4. Merge the bump PR (see the next section).

Nothing is deployed to EC2 automatically.

### The bump PR does not run CI by itself

The workflow opens the PR with the built-in `GITHUB_TOKEN`. Events created with that token
**do not trigger other workflows**, so `ci-ok` and `pr-title` never report and the PR cannot be
merged under the ruleset. Use one of these:

- **Close and reopen** the PR, or push an empty commit to its branch
  (`git commit --allow-empty -m "ci: trigger"`). Either action comes from a user, so CI runs.
- Or, as a permanent fix, change the `bump` job to use a GitHub App installation token
  (`actions/create-github-app-token`) or a fine-grained PAT stored as a secret instead of
  `github.token`. PRs opened with that token trigger CI normally.

The repository setting *Actions → General → Allow GitHub Actions to create and approve pull
requests* must be enabled for the bump job to open the PR.

## Notes and limitations

- **Release notes are repository-wide.** GitHub lists every PR merged between the two tags, not
  only the PRs that touched the module. On a module's first release there is no earlier tag of
  that module, so GitHub starts from the latest release of *any* module.
- **Failed release.** Delete the GitHub Release, if one was created, and the tag. Fix the
  problem on `main`, then tag again. Because the release version is never committed, nothing
  needs to be reverted.
- **Release order.** A library release does not trigger application releases. To ship a library
  fix in an application, release the application after the fix is on `main`.
