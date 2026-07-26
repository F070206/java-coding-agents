package com.javacodingagent.api.repository;
import com.javacodingagent.common.web.ApiResponse; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/repositories") public class RepositoryController { private final InMemoryRepositoryService repositories; public RepositoryController(InMemoryRepositoryService repositories) { this.repositories = repositories; }
 @PostMapping public ResponseEntity<ApiResponse<RepositoryView>> create(@Valid @RequestBody CreateRepositoryRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(repositories.create(request))); }
 @GetMapping public ApiResponse<?> all() { return ApiResponse.ok(repositories.all()); }
 @GetMapping("/{id}") public ApiResponse<?> get(@PathVariable long id) { return repositories.find(id).<ApiResponse<?>>map(ApiResponse::ok).orElseGet(() -> ApiResponse.failure("REPOSITORY_NOT_FOUND", "Repository not found")); }
 @PostMapping("/{id}/index") public ApiResponse<?> index(@PathVariable long id) { return repositories.indexed(id).<ApiResponse<?>>map(ApiResponse::ok).orElseGet(() -> ApiResponse.failure("REPOSITORY_NOT_FOUND", "Repository not found")); }
 @GetMapping("/{id}/index/status") public ApiResponse<?> status(@PathVariable long id) { return get(id); }
}
