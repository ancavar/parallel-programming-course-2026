package io.github.parallel;

import java.util.Random;

public class ZipfDistribution {
    public static void zipf(int[] values, double alpha, int n, Random random) {
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
}
