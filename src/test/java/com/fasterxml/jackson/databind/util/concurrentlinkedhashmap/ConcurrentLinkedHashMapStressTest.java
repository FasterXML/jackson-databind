package com.fasterxml.jackson.databind.util.concurrentlinkedhashmap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.*;
import static org.junit.Assert.assertEquals;

public class ConcurrentLinkedHashMapStressTest {

    //increase these to increase the stress
    private static int iterations = 100000;
    private static int threads = 20;
    private static int waitSeconds = 60;

    @Test
    public void test() throws Exception {
        for(int i = 0; i < 10; i++) {
            testManyEntries();
        }
    }

    private void testManyEntries() throws Exception {
        final int maxEntries = 30;
        final int maxKey = 100;
        final Random rnd = new Random();
        final ConcurrentLinkedHashMap<Integer, UUID> clhm =
                new ConcurrentLinkedHashMap.Builder<Integer, UUID>().maximumWeightedCapacity(maxEntries).build();
        final Map<Integer, UUID> map = new ConcurrentHashMap<>();
        final List<Future<?>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < maxKey; i++) {
                final int key = i;
                futures.add(executor.submit(() -> {
                    UUID uuid = UUID.randomUUID();
                    clhm.put(key, uuid);
                    map.put(key, uuid);
                }));
            }
            for (int i = 0; i < iterations; i++) {
                futures.add(executor.submit(() -> {
                    int key = rnd.nextInt(maxKey);
                    UUID uuid = UUID.randomUUID();
                    clhm.put(key, uuid);
                    map.put(key, uuid);
                }));
            }
        } finally {
            executor.shutdown();
        }
        executor.awaitTermination(waitSeconds, TimeUnit.SECONDS);
        for(Future<?> future : futures) {
            future.get(waitSeconds, TimeUnit.SECONDS);
        }
        await().atMost(waitSeconds, TimeUnit.SECONDS).until(
            () -> clhm.size() == maxEntries
        );
        assertEquals(maxEntries, clhm.size());
        int matched = 0;
        for (int i = 0; i < maxKey; i++) {
            UUID uuid = clhm.get(i);
            if (uuid != null) {
                matched++;
                final int key = i;
                await().atMost(waitSeconds, TimeUnit.SECONDS).until(
                        () -> uuid == map.get(key)
                );
                assertEquals(map.get(i), uuid);
            }
        }
        assertEquals(maxEntries, matched);
    }
}
