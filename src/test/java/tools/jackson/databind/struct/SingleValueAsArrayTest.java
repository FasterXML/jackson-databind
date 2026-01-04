package tools.jackson.databind.struct;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SingleValueAsArrayTest extends DatabindTestUtil
{
    static class Bean1421A
    {
        List<Messages> bs = Collections.emptyList();

        @JsonCreator
        Bean1421A(final List<Messages> bs)
        {
            this.bs = bs;
        }
    }

    static class Messages
    {
        List<MessageWrapper> cs = Collections.emptyList();

        @JsonCreator
        Messages(final List<MessageWrapper> cs)
        {
            this.cs = cs;
        }
    }

    static class MessageWrapper
    {
        String message;

        @JsonCreator
        MessageWrapper(@JsonProperty("message") String message)
        {
            this.message = message;
        }
    }

    static class Bean1421B<T> {
        T value;

        @JsonCreator
        public Bean1421B(T value) {
            this.value = value;
        }
    }

    // for [databind#5537]
    static class Bean5537 {
        Collection<IdentifiedType5537> collection;
        IdentifiedType5537 value;

        public void setValue(IdentifiedType5537 value) {
            this.value = value;
        }

        public void setCollection(Collection<IdentifiedType5537> collection) {
            this.collection = collection;
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
    static class IdentifiedType5537 {
        String entry;

        @JsonCreator
        IdentifiedType5537(@JsonProperty("entry") String entry)
        {
            this.entry = entry;
        }
    }

    static class Bean5541 {
        IdentifiedType5537[] array;
        IdentifiedType5537 value;

        public void setValue(IdentifiedType5537 value) {
            this.value = value;
        }

        public void setArray(IdentifiedType5537[] array) {
            this.array = array;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    @Test
    public void testSuccessfulDeserializationOfObjectWithChainedArrayCreators() throws Exception
    {
        Bean1421A result = MAPPER.readValue("[{\"message\":\"messageHere\"}]", Bean1421A.class);
        assertNotNull(result);
        assertNotNull(result.bs);
        assertEquals(1, result.bs.size());
    }

    @Test
    public void testWithSingleString() throws Exception {
        Bean1421B<List<String>> a = MAPPER.readValue(q("test2"),
                new TypeReference<Bean1421B<List<String>>>() {});
        List<String> expected = new ArrayList<>();
        expected.add("test2");
        assertEquals(expected, a.value);
    }

    @Test
    public void testPrimitives() throws Exception {
        int[] i = MAPPER.readValue("16", int[].class);
        assertEquals(1, i.length);
        assertEquals(16, i[0]);

        long[] l = MAPPER.readValue("1234", long[].class);
        assertEquals(1, l.length);
        assertEquals(1234L, l[0]);

        double[] d = MAPPER.readValue("12.5", double[].class);
        assertEquals(1, d.length);
        assertEquals(12.5, d[0]);

        boolean[] b = MAPPER.readValue("true", boolean[].class);
        assertEquals(1, d.length);
        assertTrue(b[0]);
    }

    // for [databind#5537]
    @Test
    public void testCollectionWithObjectId() throws Exception
    {
        Bean5537 result = MAPPER.readValue("{\"collection\":1,\"value\":{\"@id\":1,\"entry\":\"s\"}}",
                Bean5537.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(1, result.collection.size());
        assertNotNull(result.collection.iterator().next());
        assertEquals("s", result.collection.iterator().next().entry);
    }

    // for [databind#5541]
    @Test
    public void testArrayWithObjectId() throws Exception
    {
        Bean5541 result = MAPPER.readValue("{\"array\":1,\"value\":{\"@id\":1,\"entry\":\"s\"}}",
                Bean5541.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(1, result.array.length);
        assertNotNull(result.array[0]);
        assertEquals("s", result.array[0].entry);
    }
}
