package com.vessbon.tenka.client.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimingUtil {

    private static final Random random = new Random();

    public static void randomSleep(long minMillis, long maxMillis, AtomicBoolean paused) throws InterruptedException {
        long sleepTime = minMillis + (long) (Math.random() * (maxMillis - minMillis));
        long elapsed = 0;
        long chunk = 50;

        while (elapsed < sleepTime) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();

            if (paused.get()) {
                // While paused, just sleep in small increments and keep checking
                Thread.sleep(chunk);
                continue;
            }

            long remaining = sleepTime - elapsed;
            long sleepDuration = Math.min(chunk, remaining);
            Thread.sleep(sleepDuration);
            elapsed += sleepDuration;
        }
    }
}
