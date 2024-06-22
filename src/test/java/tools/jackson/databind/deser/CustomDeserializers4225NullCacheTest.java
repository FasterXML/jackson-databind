package tools.jackson.databind.deser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to check that getNullValue for deserializer is not cached, by default.
 */
public class CustomDeserializers4225NullCacheTest extends DatabindTestUtil
{
    static class CustomListDeserializer extends ValueDeserializer<List<String>> {

        private static int getNullValueInvocationCount = 0;

        @Override
        public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            return makeList("regular");
        }

        @Override
        public List<String> getNullValue(DeserializationContext ctxt) throws JacksonException {
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

    static class Bean4225 {
        @JsonDeserialize(using = CustomListDeserializer.class)
        public List<String> myList;
    }

    @Test
    public void testGetNullValueIsCached() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

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
