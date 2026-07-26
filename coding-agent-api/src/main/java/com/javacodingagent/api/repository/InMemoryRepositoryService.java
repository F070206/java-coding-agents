package com.javacodingagent.api.repository;
import java.nio.file.*; import java.util.*; import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicLong;
public class InMemoryRepositoryService { private final AtomicLong ids = new AtomicLong(); private final Map<Long, RepositoryView> values = new ConcurrentHashMap<>();
 public RepositoryView create(CreateRepositoryRequest request) { Path path = Path.of(request.workspacePath()).toAbsolutePath().normalize(); if (!Files.isDirectory(path)) throw new IllegalArgumentException("Workspace path does not exist"); long id = ids.incrementAndGet(); var result = new RepositoryView(id, request.userId(), request.name(), path.toString(), "PENDING"); values.put(id, result); return result; }
 public List<RepositoryView> all() { return values.values().stream().sorted(Comparator.comparing(RepositoryView::id)).toList(); } public Optional<RepositoryView> find(long id) { return Optional.ofNullable(values.get(id)); }
 public Optional<RepositoryView> indexed(long id) { return find(id).map(value -> { var next = new RepositoryView(value.id(), value.userId(), value.name(), value.workspacePath(), "COMPLETED"); values.put(id, next); return next; }); }
}
