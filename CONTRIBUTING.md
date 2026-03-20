# Contributing to Foto Gallery QR Code Generator

Thank you for your interest in contributing! Contributions of any kind — bug reports, feature suggestions, documentation improvements, and code — are warmly welcome.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Report a Bug](#how-to-report-a-bug)
- [How to Suggest a Feature](#how-to-suggest-a-feature)
- [Development Setup](#development-setup)
- [Making a Pull Request](#making-a-pull-request)
- [Coding Standards](#coding-standards)

## Code of Conduct

This project follows a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold it.

## How to Report a Bug

1. **Search existing issues** to avoid duplicates.
2. Open a new issue with the **Bug report** template.
3. Include:
   - A clear title and description
   - Steps to reproduce the problem
   - Expected vs. actual behaviour
   - Java version, OS, and whether you use the JAR or native binary

## How to Suggest a Feature

1. **Search existing issues** to see if the idea has already been discussed.
2. Open a new issue with the **Feature request** template.
3. Describe the use case and why it would be valuable.

## Development Setup

### Prerequisites

- **Java 25** (Temurin distribution recommended)
- **Maven** (or use the included `./mvnw` wrapper)

### Build

```bash
# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test
```

### Code Style

The project uses [Spring Java Format](https://github.com/spring-io/spring-javaformat) to enforce consistent formatting. Run the formatter before committing:

```bash
./mvnw spring-javaformat:apply
```

The CI pipeline will fail if the code is not formatted correctly.

## Commit Messages

This project uses **[Conventional Commits](https://www.conventionalcommits.org/)**.

Format: `<type>(<scope>): <short description>`

| Type | When to use |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `docs` | Documentation only |
| `test` | Adding or correcting tests |
| `chore` | Build tooling, dependency updates, CI |
| `perf` | Performance improvement |

**Rules:**
- Use the **imperative mood** in the short description ("add" not "added")
- Keep the subject line ≤ 72 characters
- Reference issues in the footer: `Closes #42`
- **Make small, atomic commits** — one logical change per commit

**Examples:**
```
feat(scripts): add PicPeak gallery event prompt in interactive mode
fix(pdf): correct QR code padding calculation
refactor(service): extract URL validation into helper
docs(contributing): add conventional commit guidelines
```

## Making a Pull Request

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feat/my-new-feature
   ```
2. Make your changes in **small, atomic commits** following the commit conventions above.
3. **Add tests** where appropriate.
4. Run the full test suite:
   ```bash
   ./mvnw verify
   ```
5. Push your branch and open a pull request against `main`.
6. Fill in the pull request template and describe *what* and *why*.

PRs are reviewed promptly. Please be patient if feedback takes a day or two.

## Coding Standards

- Follow standard Java conventions and the existing code style.
- Keep services stateless and use constructor injection.
- Do not add REST endpoints — this is a CLI tool only.
- Architecture rules are enforced by [Taikai](https://github.com/enofex/taikai) tests; make sure they pass.
