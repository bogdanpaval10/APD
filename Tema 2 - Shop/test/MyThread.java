package test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MyThread extends Thread {
    private final int id;
    private final int[] v;
    private final int n;
    private final int p;
    private final CyclicBarrier barrier;

    public MyThread(int id, int[] v, int n, int p, CyclicBarrier barrier) {
        this.id = id;
        this.v = v;
        this.n = n;
        this.p = p;
        this.barrier = barrier;
    }

    @Override
    public void run() {

        if (id == 0) { // facem +
            for (int i = 0; i < n; i++) {
                Testapd.resultAdd.addAndGet(v[i]);
            }
        }
        if (id == 1) { // facem + *
            for (int i = 0; i < n; i++) {
                Testapd.resultAdd.addAndGet(v[i]);
                Testapd.resultInm.addAndGet(v[i] * Testapd.resultInm.get());
            }
        }
        if (id == 2) { // facem + * min
            for (int i = 0; i < n; i++) {
                Testapd.resultAdd.addAndGet(v[i]);
                Testapd.resultInm.addAndGet(v[i] * Testapd.resultInm.get());
                if (Testapd.resultMin.get() > v[i]) {
                    Testapd.resultMin.set(v[i]);
                }
            }
        }
        if (id == 3) { // facem + * min max
            for (int i = 0; i < n; i++) {
                Testapd.resultAdd.addAndGet(v[i]);
                Testapd.resultInm.addAndGet(v[i] * Testapd.resultInm.get());
                if (Testapd.resultMin.get() > v[i]) {
                    Testapd.resultMin.set(v[i]);
                }
                if (Testapd.resultMax.get() < v[i]) {
                    Testapd.resultMax.set(v[i]);
                }
            }
        }
        if (id == 4) { // facem + * min max media
            for (int i = 0; i < n; i++) {
                Testapd.resultAdd.addAndGet(v[i]);
                Testapd.resultInm.addAndGet(v[i] * Testapd.resultInm.get());
                if (Testapd.resultMin.get() > v[i]) {
                    Testapd.resultMin.set(v[i]);
                }
                if (Testapd.resultMax.get() < v[i]) {
                    Testapd.resultMax.set(v[i]);
                }
            }
            Testapd.resultAvg.set(Testapd.resultAdd.get() / n);
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }

        if (id == 0) { // afisare
            if (p == 1) {
                System.out.println("sum: " + Testapd.resultAdd.get());
            }
            if (p == 2) {
                System.out.println("sum: " + Testapd.resultAdd.get());
                System.out.println("prod: " + Testapd.resultInm.get());
            }
            if (p == 3) {
                System.out.println("sum: " + Testapd.resultAdd.get());
                System.out.println("prod: " + Testapd.resultInm.get());
                System.out.println("min: " + Testapd.resultMin.get());
            }
            if (p == 4) {
                System.out.println("sum: " + Testapd.resultAdd.get());
                System.out.println("prod: " + Testapd.resultInm.get());
                System.out.println("min: " + Testapd.resultMin.get());
                System.out.println("max: " + Testapd.resultMax.get());
            }
            if (p == 5) {
                System.out.println("sum: " + Testapd.resultAdd.get());
                System.out.println("prod: " + Testapd.resultInm.get());
                System.out.println("min: " + Testapd.resultMin.get());
                System.out.println("max: " + Testapd.resultMax.get());
                System.out.println("avg: " + Testapd.resultAvg.get());
            }
        }

    }

}
