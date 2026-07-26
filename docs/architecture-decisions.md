# Architecture decisions

## ADR-001: modular monolith first
The system is a Maven reactor with explicit dependency direction. This keeps the first release deployable as one Spring Boot process while isolating RAG, analysis, tools, trace and infrastructure.

## ADR-002: state transitions are centralized
`AgentStateMachine` owns legal lifecycle transitions. Orchestrators request transitions instead of embedding workflow branches.

## ADR-003: external services are optional at boot
Model and service configuration comes from environment variables. No credentials are hard-coded; the application starts before adapters are enabled.

## ADR-004: trace data is observable, not reasoning data
Trace storage records plans, action summaries and outcomes. It never persists hidden reasoning or secrets.

## ADR-005: syntax-aware chunks, hash-based incremental indexing
JavaParser produces class, interface, enum, method, constructor and field symbols. Code chunks follow those symbols and retain their line ranges instead of using blind character windows. A SHA-256 content hash skips unchanged source files; the storage boundary can later be backed by MySQL and Milvus.

## ADR-006: explainable retrieval precedes vector retrieval
The first hybrid retriever scores exact symbol, file-path and content matches with a recorded match reason. The same `CodeIndexStore` boundary will accept Milvus similarity scores in the infrastructure stage, avoiding a retrieval API rewrite.

## ADR-007: model adapters and offline tests
`CodingModel` is the domain port. Production uses LangChain4j `OpenAiChatModel` with environment-provided base URL, model and key; tests and local startup without credentials use a deterministic model. Model responses are converted to typed plans, edits and errors before execution.

## ADR-008: one writer per repository
Async tasks use a repository-scoped lock. A second writer enters `WAITING_CONFIRMATION`; unrelated repositories can run concurrently. Redis implements the distributed form for multi-instance deployments.

## ADR-009: risk-sensitive changes require confirmation
Build descriptors, migrations, authentication/security files and changes exceeding ten files cannot be applied automatically. File deletion is absent from model-proposed operations and remains an explicit protected tool.

## ADR-010: trace failure isolation
Tools are traced by a decorator and trace persistence errors are swallowed at the execution boundary. Trace records observable plans/actions/results only, never hidden model reasoning.
