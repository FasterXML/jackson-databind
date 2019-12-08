package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

public class TestEmptyArrayBlockingQueueDeser {

    @Test
    public void emptyCollectionDeser() throws JsonProcessingException, IOException {
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(new RemoteEntity());
        Entity entity = om.readValue(json, Entity.class);
        Assert.assertEquals(0, entity.values.size());
    }



    private static class RemoteEntity{
        private Collection<Double> values = new ArrayBlockingQueue<>(20);

        public Collection<Double> getValues() {
            return values;
        }
    }

    private static class Entity{
        private ArrayBlockingQueue<Double> values;

        public Collection<Double> getValues() {
            return values;
        }
    }
}
