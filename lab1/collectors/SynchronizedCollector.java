package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

public class SynchronizedCollector implements  MetricsCollector {
    private final MetricsCollector collector;

    public SynchronizedCollector(MetricsCollector collector) {
        this.collector = collector;
    }


    @Override
    public synchronized void record(long value) {
        collector.record(value);
    }

    @Override
    public synchronized Snapshot snapshot() {
        return collector.snapshot();
    }
}
