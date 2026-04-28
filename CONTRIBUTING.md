# Contributing to chat-app

Thank you for taking the time to contribute! Here's how to get started.

## How to contribute

1. **Fork** the repository
2. **Create a branch** from `develop`:
   ```bash
   git checkout develop
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** — one feature or fix per branch
4. **Test** your changes locally (see [Local Setup](#local-setup))
5. **Open a Pull Request** against the `develop` branch (not `main`)

## Branch naming

| Type | Example |
|------|---------|
| New feature | `feature/read-receipts` |
| Bug fix | `fix/stomp-reconnect` |
| Documentation | `docs/websocket-guide` |
| Refactor | `refactor/message-service` |

## What we welcome

- Bug fixes
- Performance improvements
- New features that fit the project scope (real-time chat backend)
- Documentation improvements
- Better test coverage

## What to discuss first

For anything large — new architectural patterns, breaking API changes, new dependencies — please open an **issue** first so we can align before you invest the time.

## Code style

- Follow existing Spring Boot conventions in the project
- No unused imports or commented-out code
- Keep controllers thin — logic belongs in services
- New endpoints should follow the existing REST conventions

## Local Setup

### Requirements

- Java 17+
- Docker (for MongoDB)

### Run

```bash
cd backend
cp .env.example .env
# Fill in your values in .env
docker-compose up -d mongo
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

## Reporting bugs

Use the **Bug Report** issue template. Include:
- Steps to reproduce
- Expected vs actual behaviour
- Relevant logs or error messages

## Suggesting features

Use the **Feature Request** issue template. Describe the use case, not just the solution.

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
