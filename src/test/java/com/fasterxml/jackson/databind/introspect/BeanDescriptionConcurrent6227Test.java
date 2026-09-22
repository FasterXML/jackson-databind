package com.fasterxml.jackson.databind.introspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#6227]
public class BeanDescriptionConcurrent6227Test extends DatabindTestUtil
{
    static class Probe {
        public final String id;
        public final int count;

        @JsonCreator
        public Probe(@JsonProperty("id") String id, @JsonProperty("count") int count) {
            this.id = id;
            this.count = count;
        }
    }

    @Test
    public void testConcurrentFindProperties() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        JavaType type = mapper.constructType(Probe.class);

        // Repeat a number of times to increase odds of hitting race
        for (int round = 0; round < 50; ++round) {
            final BeanDescription shared = mapper.getSerializationConfig().introspect(type);
            _runRound(shared, 16);
        }
    }

    private void _runRound(final BeanDescription shared, int parallelism) throws Exception
    {
        final CyclicBarrier barrier = new CyclicBarrier(parallelism);
        final Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
        ExecutorService pool = Executors.newFixedThreadPool(parallelism);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < parallelism; i++) {
            futures.add(pool.submit(() -> {
                try {
                    barrier.await();
                    shared.findProperties();
                } catch (Throwable e) {
                    errors.add(e);
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        if (!errors.isEmpty()) {
            throw new AssertionError("Failed with "+errors.size()+" errors, first: "+errors.peek(), errors.peek());
        }
        assertTrue(errors.isEmpty());
    }
}
