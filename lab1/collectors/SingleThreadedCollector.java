package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

import java.util.Arrays;

import static io.github.parallel.collectors.CollectorUtils.calculatePercentile;

public class SingleThreadedCollector implements  MetricsCollector {
    private final long[] buckets;
    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private long p50;       // в мс: индекс_корзины * 4
    private long p99;

    public SingleThreadedCollector() {
        buckets = new long[256];
    }

    @Override
    public void record(long value) {
        var bucketIdx = (int) Math.min(value / 4, buckets.length - 1);
        buckets[bucketIdx]++;
        count++;
        sum += value;
        min = Math.min(value, min);
        max = Math.max(value, max);
    }

    @Override
    public Snapshot snapshot() {
        return new Snapshot(
                Arrays.copyOf(buckets, buckets.length),
                count,
                sum,
                min,
                max,
                calculatePercentile(0.5, count, buckets),
                calculatePercentile(0.99, count, buckets)
        );
    }
}