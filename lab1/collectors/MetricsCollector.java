package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

public interface MetricsCollector {
    void record(long value);
    Snapshot snapshot();
}