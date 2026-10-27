# Security Policy

## Supported Versions

Security updates and patches are actively maintained on the `main` branch and active release tags.

| Version | Supported          | Security Fix Delivery |
| ------- | ------------------ | --------------------- |
| 1.0.x   | :white_check_mark: | Automated Dependabot & CI |
| < 1.0   | :x:                | Deprecated |

---

## Automated Security & Quality Enforcement

This repository implements multi-layered continuous security and dependency enforcement:

1. **Dependabot Daily Tracking**: Dependabot scans both Gradle dependencies (`build.gradle.kts`, `gradle/libs.versions.toml`) and GitHub Actions workflows on a daily cadence.
2. **Automated Dependabot Merge & Enforcement**: Minor, patch, and security pull requests opened by Dependabot are automatically analyzed, reviewed, and queued for auto-merge upon passing all CI and CodeQL status checks.
3. **CodeQL Advanced Static Analysis**: Continuous CodeQL scanning monitors both `java-kotlin` (Android application logic) and `actions` (CI/CD workflows) using the extended `security-and-quality` rulesets.
4. **Pull Request Dependency Review**: PRs introducing third-party dependencies are blocked if any moderate or higher severity CVEs are present.
5. **Resolution Strategy & Constraints**: Transitive vulnerabilities (such as Netty, BouncyCastle, JDOM2, Jose4j, Commons Lang3, and HttpClient) are explicitly constrained in `app/build.gradle.kts` and forced in the root `build.gradle.kts`.

---

## Reporting a Vulnerability

If you discover a security vulnerability or potential threat within this application:

1. **Do not create a public GitHub issue.**
2. Report the vulnerability privately via [GitHub Private Vulnerability Reporting](https://github.com/FABRICADO08/Budget-Tracker/security/advisories/new) or contact the maintainers directly.
3. Include detailed steps to reproduce the issue, attack vectors, and any relevant logs or sample inputs.
4. Vulnerability disclosures receive initial triage and response within 48 hours, followed by prompt patch deployment.
