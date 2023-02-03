package com.fasterxml.jackson.databind.jsontype.deftyping;

import org.junit.Assert;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestDefaultWithCreators
    extends BaseMapTest
{
    static abstract class Job
    {
        public long id;
    }

    static class UrlJob extends Job
    {
        private final String url;
        private final int count;

        @JsonCreator
        public UrlJob(@JsonProperty("id") long id, @JsonProperty("url") String url,
                @JsonProperty("count") int count)
        {
            this.id = id;
            this.url = url;
            this.count = count;
        }

        public String getUrl() { return url; }
        public int getCount() { return count; }
    }

    // [databind#1385]
    static class Bean1385Wrapper
    {
        public Object value;

        protected Bean1385Wrapper() { }
        public Bean1385Wrapper(Object v) { value = v; }
    }

    static class Bean1385
    {
        byte[] raw;

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public Bean1385(byte[] raw) {
            this.raw = raw.clone();
        }

        @JsonValue
        public byte[] getBytes() {
            return raw;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testWithCreators() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL)
                .build();
        UrlJob input = new UrlJob(123L, "http://foo", 3);
        String json = mapper.writeValueAsString(input);
        assertNotNull(json);
        Job output = mapper.readValue(json, Job.class);
        assertNotNull(output);
        assertSame(UrlJob.class, output.getClass());
        UrlJob o2 = (UrlJob) output;
        assertEquals(123L, o2.id);
        assertEquals("http://foo", o2.getUrl());
        assertEquals(3, o2.getCount());
    }

    // [databind#1385]
    public void testWithCreatorAndJsonValue() throws Exception
    {
        final byte[] BYTES = new byte[] { 1, 2, 3, 4, 5 };
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                .build();
        String json = mapper.writeValueAsString(new Bean1385Wrapper(
                new Bean1385(BYTES)
        ));
        Bean1385Wrapper result = mapper.readValue(json, Bean1385Wrapper.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(Bean1385.class, result.value.getClass());
        Bean1385 b = (Bean1385) result.value;
        Assert.assertArrayEquals(BYTES, b.raw);
    }
 }
