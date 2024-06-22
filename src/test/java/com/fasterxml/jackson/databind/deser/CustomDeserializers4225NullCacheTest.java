package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to check that getNullValue for deserializer is not cached.
 */
@SuppressWarnings("serial")
public class CustomDeserializers4225NullCacheTest extends DatabindTestUtil {

    static class CustomListDeserializer extends JsonDeserializer<List<String>> {

        private static int getNullValueInvocationCount = 0;

        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return makeList("regular");
        }

        @Override
        public List<String> getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            // Increment invocation count
            getNullValueInvocationCount++;
            return makeList("nullVal_" + getNullValueInvocationCount);
        }

        public List<String> makeList(String content) {
            List<String> randomList = new ArrayList<>();
            randomList.add(content);
            return randomList;
        }
    }

    // [databind#2467]: Allow missing "content" for as-array deserialization
    static class Bean4225 {
        @JsonDeserialize(using = CustomListDeserializer.class)
        public List<String> myList;
    }

    @Test
    public void testGetNullValueIsCached() throws Exception
    {
        ObjectMapper mapper = objectMapper();

        // First time deserializing null
        verifyGetNullValueInvokedTimes(mapper, 1);
        // Second time deserializing null, should be invoked twice
        verifyGetNullValueInvokedTimes(mapper, 2);
    }

    private void verifyGetNullValueInvokedTimes(ObjectMapper mapper, int times)
            throws Exception
    {
        Bean4225 someBean = mapper.readValue(a2q("{'myList': null}"), Bean4225.class);

        assertThat(someBean.myList).hasSize(1);
        assertThat(someBean.myList.get(0)).isEqualTo("nullVal_" + times);
        assertThat(CustomListDeserializer.getNullValueInvocationCount).isEqualTo(times);
    }
}
