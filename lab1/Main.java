package io.github.parallel;

import io.github.parallel.collectors.SingleThreadedCollector;
import io.github.parallel.collectors.StubCollector;
import io.github.parallel.collectors.SynchronizedCollector;

class Main {
    public static void main(String[] args) throws InterruptedException {
        var bench = new Benchmark();
        var singleThreadedCollector =  new SingleThreadedCollector();
        var synchronizedCollector = new SynchronizedCollector(singleThreadedCollector);
        var stubCollector = new StubCollector();


//        System.out.println(String.format("%.9f", bench.measurePoint(singleThreadedCollector, 1)));

        for (int i = 0; i < 5; i++) {
            var threads = 1 << i;
            System.out.println(threads);
            System.out.println(String.format("%.9f", bench.measurePoint(stubCollector, threads)));
        }
    }
}