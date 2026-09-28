package io.github.parallel;

import io.github.parallel.collectors.MetricsCollector;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class InconsistencyTest {
    private static final int NUM_THREADS = 4;

    public static void run (MetricsCollector collector, int[] values) throws InterruptedException {
        var start = new CountDownLatch(1);
        var stop = new AtomicBoolean(false);
        var ops = new long[NUM_THREADS];
        var threads = new Thread[NUM_THREADS];

        for (int k = 0; k < NUM_THREADS; k++) {

            final var threadIdx = k;
            threads[k] = new Thread(() -> {
                var localCount = 0L;
                var i = threadIdx * 1000;

                try {
                    start.await();

                    while (!stop.get()) {
                        collector.record(values[i]);
                        localCount++;

                        i++;
                        if (i == values.length) i = 0;
                    }

                    ops[threadIdx] = localCount;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            threads[k].start();
        }

        start.countDown();
        var le = 0;
        var ge = 0;

        for (int i = 0; i < 10_000; i++) {
            var snapshot = collector.snapshot();
            var sum = Arrays.stream(snapshot.buckets()).sum();
            var count = snapshot.count();
            if (sum < count) {
                le++;
            } else if (sum > count) {
                ge++;
            }
        }

        stop.set(true);


        for (var thread :  threads) {
            thread.join();
        }

        var totalOps = Arrays.stream(ops).sum();
        System.out.printf("le %d\nge %d\ntotalOps %d\ncount %d%n\n", le, ge, totalOps, collector.snapshot().count());

    }
}
