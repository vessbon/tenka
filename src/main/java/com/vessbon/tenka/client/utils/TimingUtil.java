package com.vessbon.tenka.client.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimingUtil {

    private static final Random random = new Random();

    public static void randomSleep(long minMillis, long maxMillis, AtomicBoolean paused) {

        long sleepTime = minMillis + (long) (random.nextDouble() * (maxMillis - minMillis));
        long startTime = System.currentTimeMillis();

        long elapsedActiveTime = 0;

        while (elapsedActiveTime < sleepTime && !Thread.currentThread().isInterrupted()) {
            if (paused.get()) {
                long pauseStartTime = System.currentTimeMillis();

                while (paused.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Macro sleep interrupted: " + e.getMessage());
                        return;
                    }
                }

                startTime += System.currentTimeMillis() - pauseStartTime;
            }

            long remainingSleep = sleepTime - elapsedActiveTime;

            if (remainingSleep > 0) {
                try {
                    Thread.sleep(Math.min(remainingSleep, 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Macro sleep interrupted: " + e.getMessage());
                    return;
                }
            }

            elapsedActiveTime = System.currentTimeMillis() - startTime;
        }
    }

    public static float uniform(float min, float max) {
        return min + (random.nextFloat() * (max - min));
    }

    public static double uniform(double min, double max) {
        return min + (random.nextDouble() * (max - min));
    }
}
