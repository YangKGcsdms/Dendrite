# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Batch evaluation processing with 5-minute queue scan
- Pipeline architecture (Evaluation → Summary → Vector Storage)
- Concurrent batch search endpoints (`/search/batch`, `/ask/batch`)
- Token usage tracking and reporting
- Economy mode to reduce AI costs
- Query expansion caching

### Changed
- Queue scan interval from 100ms to 5 minutes
- Default AI model from `gemini-2.5-pro` to `gemini-2.0-flash` (25x cost reduction)
- Batch processing limit set to 10 evaluations per cycle

### Fixed
- N/A

## [0.0.1-SNAPSHOT] - 2025-12-16

### Added
- Initial project setup with Spring Boot 3.4.0
- PostgreSQL + pgvector integration for vector storage
- Redis queue for async task processing
- Google Gemini AI integration via Spring AI
- Basic evaluation submission and processing
- Talent profile generation with AI
- Semantic search with vector similarity
- AI-powered search recommendations
- Gamification system (rewards, levels)
- Docker Compose configuration

