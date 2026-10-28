# Contributing Guidelines — BankOps Platform

Thank you for contributing to **BankOps**. To maintain enterprise banking software quality, please adhere to these engineering standards.

## Code Standards
- **Java**: Java 17/21 language features. Follow Google Java Style guidelines.
- **TypeScript**: Strict typing with comprehensive interface models. Avoid raw `any`.
- **Formatting**: Keep code cleanly formatted; remove unused imports and commented-out dead code.

## Git & Pull Request Conventions
- Commit messages should follow conventional commits:
  - `feat(rules): add high-velocity channel anomaly rule`
  - `fix(case): prevent state transition to RESOLVED without summary`
  - `test(audit): add regression suite for actor correlation logging`
- Branch naming: `feature/feature-name` or `fix/issue-description`.

## Testing Rules
- Every business rule must have a corresponding JUnit 5 test.
- Run the full test suite before committing:
  ```bash
  cd backend && mvn test
  cd frontend && npm run build
  ```
