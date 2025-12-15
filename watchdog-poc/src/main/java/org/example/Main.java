package org.example;

import org.example.watchdog.Watchdog;
import org.example.watchdog.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        boolean simulateHang = args.length > 0 && args[0].equalsIgnoreCase("hang");

        // Build sample items
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            items.add("item-" + i);
        }

        // Worker options
        final long timePerItemMs = 8;
        int kickEveryN = 10;

        AtomicBoolean shouldHang = new AtomicBoolean(simulateHang);

        // We will create worker AFTER watchdog, but reference must be final
        final Thread[] workerHolder = new Thread[1];

        // Create watchdog
        long timeoutMs = 2_000;
        Watchdog watchdog = new Watchdog("demo", timeoutMs, () -> {
            System.err.println("[Watchdog] Timeout detected. Interrupting worker...");
            Thread w = workerHolder[0];
            if (w != null) {
                w.interrupt();
            }
        });

        // Create worker with watchdog
        Worker workerRunnable =
                new Worker(items, watchdog, kickEveryN, timePerItemMs, shouldHang);

        final Thread worker = new Thread(workerRunnable, "Worker-Thread");
        workerHolder[0] = worker;

        System.out.println("[App] Starting worker. simulateHang=" + simulateHang);
        worker.start();

        // Wait for worker to finish
        worker.join(10 * 60_000);

        System.out.println("[App] Worker thread state: " + worker.getState());
        System.out.println("[App] Exiting main.");
    }

}
