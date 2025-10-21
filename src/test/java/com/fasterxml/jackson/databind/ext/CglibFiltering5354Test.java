package com.fasterxml.jackson.databind.ext;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.hibernate.repackage.cglib.MockedHibernateCglibProxy;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.proxy.MockedSpringCglibProxy;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.cglib.MockedNetCglibProxy;

// [databind#5354] Test for filtering out CGLIB-generated properties
public class CglibFiltering5354Test
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // https://github.com/FasterXML/jackson-databind/issues/5354
    @Test
    public void testWriteWithSpringCglibProxyDoesNotIncludeCallbacksProperty() throws Exception
    {
        MockedSpringCglibProxy mockedProxy = new MockedSpringCglibProxy("hello");
        String json = MAPPER.writeValueAsString(mockedProxy);
        Map<?, ?> properties = MAPPER.readValue(json, Map.class);
        assertEquals(properties.keySet(), Collections.singleton("propertyName"));
    }

    // https://github.com/FasterXML/jackson-databind/issues/5354
    @Test
    public void testWriteWithHibernateCglibProxyDoesNotIncludeCallbacksProperty() throws Exception
    {
        MockedHibernateCglibProxy mockedProxy = new MockedHibernateCglibProxy("hello");
        String json = MAPPER.writeValueAsString(mockedProxy);
        Map<?, ?> properties = MAPPER.readValue(json, Map.class);
        assertEquals(properties.keySet(), Collections.singleton("propertyName"));
    }

    // https://github.com/FasterXML/jackson-databind/issues/5354
    @Test
    public void testWriteWithNetCglibProxyDoesNotIncludeCallbacksProperty() throws Exception
    {
        MockedNetCglibProxy mockedProxy = new MockedNetCglibProxy("hello");
        String json = MAPPER.writeValueAsString(mockedProxy);
        Map<?, ?> properties = MAPPER.readValue(json, Map.class);
        assertEquals(properties.keySet(), Collections.singleton("propertyName"));
    }

}
