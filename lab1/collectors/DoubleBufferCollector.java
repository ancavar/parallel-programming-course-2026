package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.parallel.collectors.CollectorUtils.calculatePercentile;

public class DoubleBufferCollector implements MetricsCollector {
    private static final int NOWHERE = -1;
    private volatile int active = 0;

    static final class ThreadBuffers {
        // Два буфера: [0] и [1]. Обычные массивы и переменные!
        final long[][] buckets = new long[2][256];
        final long[] count = new long[2];
        final long[] sum = new long[2];
        final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
        final long[] max = {0, 0};

        // Флаг входа: -1 = вне буферов, 0 = запись в буфер 0, 1 = запись в буфер 1
        final AtomicInteger inside = new AtomicInteger(NOWHERE);
    }

    private final List<ThreadBuffers> allBuffers = new ArrayList<>();
    private final Object lock = new Object();


    private final ThreadLocal<ThreadBuffers> myBuffers = ThreadLocal.withInitial(() -> {
        ThreadBuffers b = new ThreadBuffers();
        synchronized (lock) {
            allBuffers.add(b);
        }
        return b;
    });

    private final long[] globalBuckets = new long[256];
    private long globalCount;
    private long globalSum;
    private long globalMin = Long.MAX_VALUE;
    private long globalMax;


    @Override
    public void record(long value) {
        ThreadBuffers my = myBuffers.get();
        int buffer;
        while (true) {
            buffer = active;
            my.inside.set(buffer);

            if (active == buffer) break;
            my.inside.setRelease(NOWHERE);

        }


        int bucket = (int) Math.min(value / 4, 255);


        my.buckets[buffer][bucket]++;
        my.count[buffer]++;
        my.sum[buffer] += value;
        my.min[buffer] = Math.min(my.min[buffer], value);
        my.max[buffer] = Math.max(my.max[buffer], value);
        my.inside.setRelease(NOWHERE);
    }

    @Override
    public Snapshot snapshot() {
        synchronized (lock) {
            var old = active;
            active = 1 - old;

            for (var buffer : allBuffers) {
                while (buffer.inside.get() == old) {
                    Thread.onSpinWait();
                }

                for (int i = 0; i < globalBuckets.length; i++) {
                    globalBuckets[i] += buffer.buckets[old][i];
                }
                globalCount += buffer.count[old];
                globalSum += buffer.sum[old];
                globalMin = Math.min(globalMin, buffer.min[old]);
                globalMax = Math.max(globalMax, buffer.max[old]);

                Arrays.fill(buffer.buckets[old], 0);
                buffer.count[old] = 0;
                buffer.sum[old] = 0;
                buffer.min[old] = Long.MAX_VALUE;
                buffer.max[old] = 0;
            }

            long[] buckets = Arrays.copyOf(globalBuckets, globalBuckets.length);
            return new Snapshot(buckets, globalCount, globalSum, globalMin, globalMax,
                    calculatePercentile(0.5, globalCount, buckets),
                    calculatePercentile(0.99, globalCount, buckets));

        }
    }

}
