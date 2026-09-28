package io.github.parallel;

import io.github.parallel.collectors.LockStripingCollector;
import io.github.parallel.collectors.SingleThreadedCollector;
import io.github.parallel.collectors.StubCollector;
import io.github.parallel.collectors.SynchronizedCollector;

import java.util.Random;

import static io.github.parallel.ZipfDistribution.zipf;

class Main {
    public static void main(String[] args) throws InterruptedException {
        var bench = new Benchmark();
        var singleThreadedCollector =  new SingleThreadedCollector();
        var synchronizedCollector = new SynchronizedCollector(singleThreadedCollector);
        var stubCollector = new StubCollector();
        var lockStripingCollector = new LockStripingCollector();

        var random = new Random(5);

        int len = 1 << 20;
        var values = new int[len];
        zipf(values, 1.15, 1023, random);


//        System.out.println(String.format("%.9f", bench.measurePoint(singleThreadedCollector, 1)));

        for (int i = 0; i < 5; i++) {
            var threads = 1 << i;
            System.out.println(threads);
            System.out.println(String.format("%.9f", bench.measurePoint(lockStripingCollector, threads, values)));
        }

        InconsistencyTest.run(lockStripingCollector, values);
    }
}