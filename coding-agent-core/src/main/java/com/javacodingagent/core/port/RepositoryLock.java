package com.javacodingagent.core.port;
import java.time.Duration;
public interface RepositoryLock { boolean tryAcquire(long repositoryId, String owner, Duration ttl); void release(long repositoryId, String owner); }
