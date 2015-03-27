package com.fasterxml.jackson.failing;

import java.util.*;
import java.util.concurrent.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class RaceCondition738Test extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TypeOne.class, name = "one"),
        @JsonSubTypes.Type(value = TypeTwo.class, name = "two"),
        @JsonSubTypes.Type(value = TypeThree.class, name = "three")
    })
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

    static class TypeTwo extends AbstractHasSubTypes {
        private final String id;
        public TypeTwo(String id) {
            this.id = id;
        }
        @JsonProperty
        public String getId() {
            return id;
        }
        @Override
        public String getType() {
            return TypeTwo.class.getSimpleName();
        }
    }    

    static class TypeThree extends AbstractHasSubTypes {
        private final String id;
        public TypeThree(String id) {
            this.id = id;
        }
        @JsonProperty
        public String getId() {
            return id;
        }
        @Override
        public String getType() {
            return TypeThree.class.getSimpleName();
        }
    }

    public interface HasSubTypes {
        String getType();
    }

    static class Wrapper {
        private final HasSubTypes hasSubTypes;

        private Wrapper(HasSubTypes hasSubTypes) {
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
        for (int i = 0; i < 1000; i++) {
            runOnce();
        }
    }
    
    void runOnce() throws Exception {
        final ObjectMapper mapper = getObjectMapper();
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
                throw new IllegalStateException("Missing 'one', source: "+json);
            }
        }
    }

    private static ObjectMapper getObjectMapper() {
        SimpleModule module = new SimpleModule("subTypeRace");
        module.setMixInAnnotation(HasSubTypes.class, AbstractHasSubTypes.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper;
    }
}
