package org.example.watchdog;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates a worker processing many items and kicking the watchdog.
 */
public class Worker implements Runnable {
    private final List<String> items;
    private final Watchdog watchdog;
    private final int kickEveryN;
    private final long processMillisPerItem;
    private final AtomicBoolean shouldHang;
    private final AtomicInteger processed = new AtomicInteger(0);

    public Worker(List<String> items, Watchdog watchdog, int kickEveryN, long processMillisPerItem, AtomicBoolean shouldHang) {
        this.items = items;
        this.watchdog = watchdog;
        this.kickEveryN = kickEveryN;
        this.processMillisPerItem = processMillisPerItem;
        this.shouldHang = shouldHang;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < items.size(); i++) {
                String it = items.get(i);

                // simulate processing time
                try {
                    Thread.sleep(processMillisPerItem);
                } catch (InterruptedException e) {
                    System.err.println("[Worker] Interrupted while processing item " + i + ". Exiting.");
                    Thread.currentThread().interrupt();
                    break;
                }

                // simulate a hang at some point if requested
                if (shouldHang.get() && i == items.size() / 3) {
                    System.err.println("[Worker] Simulating hang at item " + i);
                    // don't kick the watchdog anymore — simulate blocked parse (infinite loop)
                    while (true) {
                        try {
                            Thread.sleep(10_000);
                        } catch (InterruptedException e) {
                            System.err.println("[Worker] Interrupted while hung; exiting hang loop.");
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                int c = processed.incrementAndGet();
                if ((c % kickEveryN) == 0) {
                    watchdog.kick();
                    System.out.println("[Worker] processed=" + c + " -> kicked watchdog");
                } else if (c % Math.max(1, kickEveryN/4) == 0) {
                    // occasional progress log
                    System.out.println("[Worker] processed=" + c);
                }
            }
            System.out.println("[Worker] Completed processing. Total=" + processed.get());
        } finally {
            // Stop watchdog when done
            try {
                watchdog.stop();
            } catch (Throwable ignored) {}
        }
    }

    public int getProcessedCount() {
        return processed.get();
    }
}