package io.github.parallel.collectors;

import io.github.parallel.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import static io.github.parallel.collectors.CollectorUtils.calculatePercentile;

public class ThreadLocalCollector implements MetricsCollector {
    private static final class ThreadState {
        final AtomicLongArray buckets = new AtomicLongArray(256);
        final AtomicLong count = new AtomicLong();
        final AtomicLong sum = new AtomicLong();
        final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong max = new AtomicLong(0);
    }

    private final List<ThreadState> allStates = new ArrayList<>();
    private final Object listLock = new Object();

    private final ThreadLocal<ThreadState> myState = ThreadLocal.withInitial(() -> {
        ThreadState s = new ThreadState();
        synchronized (listLock) {
            allStates.add(s);
        }
        return s;
    });

    @Override
    public void record(long value) {
        ThreadState s = myState.get();
        int b = (int) Math.min(value / 4, 255);

        s.buckets.setRelease(b, s.buckets.getPlain(b) + 1);
        s.count.setRelease(s.count.getPlain() + 1);
        s.sum.setRelease(s.sum.getPlain() + value);

        if (value < s.min.getPlain()) s.min.setRelease(value);
        if (value > s.max.getPlain()) s.max.setRelease(value);
    }

    @Override
    public Snapshot snapshot() {
        List<ThreadState> copyOfStates;
        synchronized (listLock) {
            copyOfStates = new ArrayList<>(allStates);
        }


        long[] buckets = new long[256];
        long count = 0, sum = 0, min = Long.MAX_VALUE, max = 0;

        for (ThreadState state : copyOfStates) {
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] += state.buckets.get(i);
            }
            count += state.count.get();
            sum += state.sum.get();
            min = Math.min(min, state.min.get());
            max = Math.max(max, state.max.get());
        }

        return new Snapshot(buckets, count, sum, min, max,
                calculatePercentile(0.5, count, buckets),
                calculatePercentile(0.99, count, buckets));
    }
}
