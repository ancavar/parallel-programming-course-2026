package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

public class StubCollector implements  MetricsCollector {
    private final Snapshot snapshot = new Snapshot(
            new long[256],
            0,
            0,
            0,
            0,
            0,
            0

    );
    @Override
    public synchronized void record(long value) {
    }

    @Override
    public synchronized Snapshot snapshot() {
        return snapshot;
    }
}
