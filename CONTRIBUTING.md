# Contributing to Dendrite

First off, thank you for considering contributing to Dendrite! 🌿

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues. When creating a bug report, include:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs actual behavior
- **Environment details** (Java version, OS, etc.)
- **Logs or error messages** if applicable

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion:

- Use a **clear and descriptive title**
- Provide a **detailed description** of the suggested enhancement
- Explain **why this enhancement would be useful**
- Include **examples** if possible

### Pull Requests

1. Fork the repo and create your branch from `main`
2. If you've added code that should be tested, add tests
3. Ensure the test suite passes
4. Make sure your code follows the existing code style
5. Write a clear commit message

## Development Setup

```bash
# Clone your fork
git clone https://github.com/your-username/dendrite.git
cd dendrite

# Start infrastructure
docker compose up -d

# Set your Gemini API key
export GEMINI_API_KEY=your_key_here

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test
```

## Code Style

- Use Java 21 features (Records, Pattern Matching, etc.)
- Follow Spring Boot conventions
- Add JavaDoc comments for public methods
- Keep methods small and focused
- Use meaningful variable names

## Commit Messages

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Reference issues and pull requests when relevant

Example:
```
Add batch evaluation endpoint

- Support JSON array input for multiple evaluations
- Return total queued count in response
- Fixes #123
```

## Questions?

Feel free to open an issue with the label `question`.

Thank you for contributing! 🎉

