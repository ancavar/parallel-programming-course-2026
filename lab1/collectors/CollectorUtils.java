package io.github.parallel.collectors;

public class CollectorUtils {
    public static int calculatePercentile(double p, long count, long[] buckets)  {
        var boundary = count * p;
        var sum = 0L;
        int i;
        for (i = 0; i < buckets.length; i++) {
            sum += buckets[i];
            if (sum >= boundary) break;
        }

        return i * 4;
    }
}
