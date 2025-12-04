package org.example.watchdog;

import java.time.Instant;
import java.util.Map;

public class Watchdog {
    private final long timeoutMillis;
    private final Thread monitor;
    private volatile long lastKick;
    private final Runnable onTimeout;
    private volatile boolean running = true;
    private final String name;

    public Watchdog(String name, long timeoutMillis, Runnable onTimeout) {
        this.name = name;
        this.timeoutMillis = timeoutMillis;
        this.onTimeout = onTimeout;
        this.lastKick = System.currentTimeMillis();
        this.monitor = new Thread(this::monitorLoop, "Watchdog-" + name);
        this.monitor.setDaemon(true);
        this.monitor.start();
    }

    /** Mark the worker as alive right now. */
    public void kick() {
        lastKick = System.currentTimeMillis();
    }

    private void monitorLoop() {
        while (running) {
            try {
                Thread.sleep(Math.max(200, timeoutMillis / 6));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            long age = System.currentTimeMillis() - lastKick;
            if (age > timeoutMillis) {
                try {
                    log("Timeout detected. age=" + age + "ms, timeout=" + timeoutMillis + "ms at " + Instant.now());
                    dumpAllThreads();
                    onTimeout.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                // stop monitoring after timeout fired once; change behavior if you want repeat handling
                running = false;
            }
        }
    }

    public void stop() {
        running = false;
        monitor.interrupt();
    }

    private void dumpAllThreads() {
        StringBuilder sb = new StringBuilder();
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        sb.append("=== Thread dump by Watchdog[").append(name).append("] ===\n");
        for (Map.Entry<Thread, StackTraceElement[]> e : stacks.entrySet()) {
            Thread t = e.getKey();
            sb.append("\"").append(t.getName()).append("\" id=").append(t.getId()).append(" state=").append(t.getState()).append("\n");
            for (StackTraceElement st : e.getValue()) {
                sb.append("\t at ").append(st.toString()).append("\n");
            }
            sb.append("\n");
        }
        log(sb.toString());
    }

    private void log(String msg) {
        // try SLF4J if available, otherwise fallback to stdout
        try {
            Class.forName("org.slf4j.LoggerFactory");
            org.slf4j.LoggerFactory.getLogger(Watchdog.class).error(msg);
        } catch (Exception e) {
            System.err.println(msg);
        }
    }
}
