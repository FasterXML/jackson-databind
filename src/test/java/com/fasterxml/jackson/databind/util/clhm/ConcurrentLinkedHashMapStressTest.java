package com.fasterxml.jackson.databind.util.clhm;

import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ConcurrentLinkedHashMapStressTest {

    //increase these to increase the stress
    private static int iterations = 100000;
    private static int threads = 20;
    private static int waitSeconds = 60;

    @Test
    public void testManyEntries() throws Exception {
        final int maxEntries = 30;
        final int maxKey = 100;
        final Random rnd = new Random();
        final ConcurrentLinkedHashMap<Integer, UUID> clhm =
                new ConcurrentLinkedHashMap.Builder<Integer, UUID>().maximumWeightedCapacity(maxEntries).build();
        final Map<Integer, UUID> map = new ConcurrentHashMap<>();
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < maxKey; i++) {
                final int key = i;
                executor.submit(() -> {
                    UUID uuid = UUID.randomUUID();
                    clhm.put(key, uuid);
                    map.put(key, uuid);
                });
            }
            for (int i = 0; i < iterations; i++) {
                executor.submit(() -> {
                    int key = rnd.nextInt(maxKey);
                    UUID uuid = UUID.randomUUID();
                    clhm.put(key, uuid);
                    map.put(key, uuid);
                });
            }
        } finally {
            executor.shutdown();
        }
        executor.awaitTermination(waitSeconds, TimeUnit.SECONDS);

        //ConcurrentLinkedHashMap runs evictions in the background using its own ExecutorService
        //the following code retries the assertions to allow for the evictions to finish
        final long endTime = System.nanoTime() + Duration.of(waitSeconds, ChronoUnit.SECONDS).toNanos();
        boolean assertsFailing = true;
        while(assertsFailing) {
            try {
                assertEquals(maxEntries, clhm.size());
                int matched = 0;
                for (int i = 0; i < maxKey; i++) {
                    UUID uuid = clhm.get(i);
                    if (uuid != null) {
                        matched++;
                        assertEquals(map.get(i), clhm.get(i));
                    }
                }
                assertEquals(maxEntries, matched);
                assertsFailing = false;
            } catch (Throwable t) {
                if (System.nanoTime() > endTime) {
                    throw t;
                }
                Thread.sleep(100);
            }
        }
    }
}
