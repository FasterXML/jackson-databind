package com.fasterxml.jackson.databind.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.*;

public class ThreadSafety1759Test extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testCalendarForDeser() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();

        final int numThreads = 4;
        final int COUNT = 3000;
        final AtomicInteger counter = new AtomicInteger();

        // IMPORTANT! Use different timestamp for every thread
        List<Callable<Throwable>> calls = new ArrayList<Callable<Throwable>>();
        for (int thread = 1; thread <= numThreads; ++thread) {
            final String json = q(String.format("2017-01-%02dT16:30:49Z", thread));
            final long timestamp = mapper.readValue(json, Date.class).getTime();

            calls.add(createCallable(thread, mapper, json, timestamp, COUNT, counter));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Throwable>> results = new ArrayList<>();
        for (Callable<Throwable> c : calls) {
            results.add(executor.submit(c));
        }
        executor.shutdown();
        for (Future<Throwable> f : results) {
            Throwable t = f.get(5, TimeUnit.SECONDS);
            if (t != null) {
                fail("Exception during processing: "+t.getMessage());
            }
        }
        assertEquals(numThreads * COUNT, counter.get());
    }

    private Callable<Throwable> createCallable(final int threadId,
            final ObjectMapper mapper,
            final String json, final long timestamp,
            final int count, final AtomicInteger counter)
    {
        return new Callable<Throwable>() {
            @Override
            public Throwable call() throws IOException {
                for (int i = 0; i < count; ++i) {
                    Date dt = mapper.readValue(json, Date.class);
                    if (dt.getTime() != timestamp) {
                        return new IllegalArgumentException("Wrong timestamp (thread id "+threadId+", input: "+json+": should get "+timestamp+", got "+dt.getTime());
                    }
                    counter.addAndGet(1);
                }
                return null;
            }
        };
    }
}
