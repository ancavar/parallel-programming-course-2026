package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

import java.util.concurrent.atomic.AtomicLong;

import static io.github.parallel.collectors.CollectorUtils.calculatePercentile;

public class LockStripingCollector implements  MetricsCollector {
    private final long[] buckets = new long[256];
    private final Object[] locks = new Object[16];
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong  max = new AtomicLong(Long.MIN_VALUE);
    private long p50;       // в мс: индекс_корзины * 4
    private long p99;

    public LockStripingCollector() {
        for (int i = 0; i < 16; i++) {
            locks[i] = new Object();
        }

    }
    @Override
    public void record(long value) {
        var bucketIdx = (int) Math.min(value / 4, buckets.length - 1);

        synchronized (locks[bucketIdx % locks.length]) {
            buckets[bucketIdx]++;
        }

        count.getAndIncrement();
        sum.getAndAdd(value);

        long current = 0;
        do {
            current = min.get();
            if (value >= current) {
                break;
            }
        } while (!min.compareAndSet(current, value));
        do {
            current = max.get();
            if (value <= current) {
                break;
            }
        } while (!max.compareAndSet(current, value));
    }

    @Override
    public Snapshot snapshot() {
        var arr = new long[buckets.length];
        for (int i = 0; i < locks.length; i++) {
            synchronized (locks[i]) {
                for (int j = i; j < buckets.length; j += locks.length) {
                    arr[j] = buckets[j];
                }
            }
        }


        var c = count.get();
        return new Snapshot(
                arr,
                // TODO: getAcquire?
                c,
                sum.get(),
                min.get(),
                max.get(),
                calculatePercentile(0.5, c, arr),
                calculatePercentile(0.99, c, arr)
        );
    }
}
