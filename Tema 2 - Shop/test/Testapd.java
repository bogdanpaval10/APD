package test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Testapd {
    public static AtomicInteger resultAdd = new AtomicInteger(0);
    public static AtomicInteger resultInm = new AtomicInteger(1);
    public static AtomicInteger resultMin;
    public static AtomicInteger resultMax;
    public static AtomicInteger resultAvg = new AtomicInteger(0);
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Insufficient arguments");
            System.exit(-1);
        }

        int n = Integer.parseInt(args[0]);
        int p = Integer.parseInt(args[1]);
        int[] v = new int[n];
        for (int i = 0; i < n; i++) {
            v[i] = Integer.parseInt(args[i + 2]);
        }

        Testapd.resultMin = new AtomicInteger(v[0]);
        Testapd.resultMax = new AtomicInteger(v[0]);


        Thread[] threads = new Thread[p];
        CyclicBarrier barrier = new CyclicBarrier(p);
        for (int i = 0; i < p; i++) {
            threads[i] = new MyThread(i, v, n, p, barrier);
        }

        for (int i = 0; i < p; i++) {
            threads[i].start();
        }

        for (int i = 0; i < p; i++) {
            threads[i].join();
        }


    }
}
