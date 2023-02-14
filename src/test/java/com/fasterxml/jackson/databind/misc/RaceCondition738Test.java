package com.fasterxml.jackson.databind.misc;

import java.util.*;
import java.util.concurrent.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RaceCondition738Test extends BaseMapTest
{
    static abstract class AbstractHasSubTypes implements HasSubTypes { }

    static class TypeOne extends AbstractHasSubTypes {
        private final String id;
        public TypeOne(String id) {
            this.id = id;
        }
        @JsonProperty
        public String getId() {
            return id;
        }
        @Override
        public String getType() {
            return TypeOne.class.getSimpleName();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TypeOne.class, name = "one")
    })
    public interface HasSubTypes {
        String getType();
    }

    static class Wrapper {
        private final HasSubTypes hasSubTypes;

        Wrapper(HasSubTypes hasSubTypes) {
            this.hasSubTypes = hasSubTypes;
        }

        @JsonProperty
        public HasSubTypes getHasSubTypes() {
            return hasSubTypes;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testRepeatedly() throws Exception {
        final int COUNT = 3000;
        for (int i = 0; i < COUNT; i++) {
            runOnce(i, COUNT);
        }
    }

    void runOnce(int round, int max) throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        Callable<String> writeJson = new Callable<String>() {
            @Override
            public String call() throws Exception {
                Wrapper wrapper = new Wrapper(new TypeOne("test"));
                return mapper.writeValueAsString(wrapper);
            }
        };

        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<String>> jsonFutures = new ArrayList<Future<String>>();
        for (int i = 0; i < numThreads; i++) {
            jsonFutures.add(executor.submit(writeJson));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        for (Future<String> jsonFuture : jsonFutures) {
            String json = jsonFuture.get();
            JsonNode tree = mapper.readTree(json);
            JsonNode wrapped = tree.get("hasSubTypes");

            if (!wrapped.has("one")) {
                throw new IllegalStateException("Round #"+round+"/"+max+" ; missing property 'one', source: "+json);
            }
        }
    }
}
