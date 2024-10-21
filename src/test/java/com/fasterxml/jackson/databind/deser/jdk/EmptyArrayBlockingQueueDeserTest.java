package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

class EmptyArrayBlockingQueueDeserTest
{
    static class RemoteEntity{
        private Collection<Double> values = new ArrayBlockingQueue<>(20);

        public Collection<Double> getValues() {
            return values;
        }
    }

    static class Entity{
        ArrayBlockingQueue<Double> values;

        public Collection<Double> getValues() {
            return values;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void emptyBlockingQueue() throws Exception
    {
        String json = MAPPER.writeValueAsString(new RemoteEntity());
        Entity entity = MAPPER.readValue(json, Entity.class);
        assertEquals(0, entity.getValues().size());
    }
}
