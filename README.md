# REST Assured API Test Automation

This repository contains a deterministic 451-test API automation suite built with Java 17, JUnit 5, REST Assured, data-driven CSV tests, and Allure reporting. The tests exercise the repository's local mock API, so the suite does not depend on an external service.

## Prerequisites

- Java 17
- Maven 3.9 or newer

## Run the test suite

```bash
mvn clean test
```

All 451 tests run during the Maven `test` phase. Surefire XML reports are written under `target/surefire-reports`, and the Allure JUnit 5 adapter writes raw results to `target/allure-results`.

## Generate an Allure report locally

Run the tests and generate the static HTML report:

```bash
mvn clean test allure:report
```

The report entry point is `target/site/allure-maven-plugin/index.html`.

To generate the report, start a local report server, and open it in the default browser:

```bash
mvn allure:serve
```

The report commands use the Allure Maven plugin, so a separate system-wide Allure CLI installation is not required.

## GitHub Pages

The `Allure Report` GitHub Actions workflow runs on pushes to `main` and on manual dispatch. It uses Java 17, verifies the exact 451-test total, generates the Allure HTML report, uploads it as a workflow artifact, and deploys the same report to GitHub Pages.

Before the first deployment, configure the repository's Pages source as **GitHub Actions** under **Settings → Pages**.

Generated output such as `target/`, `allure-results/`, `allure-report/`, and the Allure runtime cache is ignored and must not be committed.
