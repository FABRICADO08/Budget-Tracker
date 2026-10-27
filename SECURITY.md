# Security Policy

## Supported Versions

Security updates and patches are actively maintained on the `main` branch and active release tags.

| Version | Supported          | Security Fix Delivery |
| ------- | ------------------ | --------------------- |
| 1.0.x   | :white_check_mark: | Automated Dependabot & CI |
| < 1.0   | :x:                | Deprecated |

---

## Automated Security & Quality Enforcement

1. **Dependabot tracking**: Dependabot checks Gradle, npm test dependencies, and GitHub Actions on a daily schedule.
2. **Human review for dependency updates**: Dependabot opens update pull requests, but the repository does not automatically approve or merge them. Review dependency changes and require passing CI/security checks before merging.
3. **CodeQL scanning**: GitHub's repository-level default CodeQL setup is enabled. There is no duplicate custom CodeQL workflow, because GitHub rejects advanced-configuration SARIF uploads while default setup is active.
4. **Pull Request Dependency Review**: Pull requests introducing or updating third-party dependencies are checked for known vulnerabilities.
5. **Pinned CI actions**: GitHub Actions workflows pin third-party actions to immutable commit SHAs to reduce supply-chain risk.
6. **Dependency constraints**: Security-sensitive transitive libraries are aligned to reviewed versions in Gradle. Dependency upgrades must be tested and reviewed before merging.

---

## Reporting a Vulnerability

If you discover a security vulnerability or potential threat within this application:

1. **Do not create a public GitHub issue.**
2. Report the vulnerability privately via [GitHub Private Vulnerability Reporting](https://github.com/FABRICADO08/Budget-Tracker/security/advisories/new) or contact the maintainers directly.
3. Include detailed steps to reproduce the issue, attack vectors, and any relevant logs or sample inputs.
4. Vulnerability disclosures receive initial triage and response within 48 hours, followed by prompt patch deployment.
