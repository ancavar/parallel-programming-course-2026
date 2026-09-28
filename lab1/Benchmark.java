package io.github.parallel;

import io.github.parallel.collectors.MetricsCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Benchmark {
    private final int[] values;

    private void zipf(double alpha, int n, Random random) {
        var c = 0D;

        for (int i = 1; i <= n; i++) {
            c += 1.0 / Math.pow(i, alpha);
        }
        c = 1.0 / c;



        for (int i = 0; i < values.length; i++) {
            double z;
            do {
                z = random.nextDouble();
            } while (z == 0);

            var sumProb = 0D;
            int j;
            for (j = 1; j <= n; j++) {
                sumProb += c / Math.pow(j, alpha);
                if (sumProb >= z) {
                    break;
                }
            }
            values[i] = j;
        }
    }

    public Benchmark() {
        var random = new Random(5);


        int len = 1 << 20;
        values = new int[len];
        zipf(1.15, 1023, random);
    }

    private double run (MetricsCollector collector, int T, int seconds) throws InterruptedException {
        var start = new CountDownLatch(1);
        var stop = new AtomicBoolean(false);
        var ops = new long[T];
        var threads = new Thread[T];

        for (int k = 0; k < T; k++) {

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

        var t0 = System.nanoTime();
        start.countDown();

        Thread.sleep(seconds * 1000L);
        stop.set(true);

        var t1 = System.nanoTime();

        for (var thread :  threads) {
            thread.join();
        }

        var totalOps = 0L;
        for (var o : ops) {
            totalOps += o;
        }

        var sec = (t1 - t0) / 1_000_000_000D;

        return totalOps / sec;
    }

    public double measurePoint(MetricsCollector collector, int T) throws InterruptedException {
        run(collector, T, 5);

        var results = new ArrayList<Double>();
        for (int i = 0; i < 5; i++) {
            results.add(run(collector, T, 5));
        }
        System.out.println(collector.snapshot().count());
        Collections.sort(results);
        var n = results.size();
        return ((results.get(n / 2) + results.get((n - 1) / 2))  / 2);
    }

}
