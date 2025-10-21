package tools.jackson.databind.ext.cglib;

import java.util.Collections;
import java.util.Map;

import org.hibernate.repackage.cglib.MockedHibernateCglibProxy;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.proxy.MockedSpringCglibProxy;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sf.cglib.MockedNetCglibProxy;

// [databind#5354] Test for filtering out CGLIB-generated properties
public class CglibFiltering5354Test
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // https://github.com/FasterXML/jackson-databind/issues/5354
    @Test
    public void testWriteWithSpringCglibProxyDoesNotIncludeCallbacksProperty() throws Exception
    {
        // 20-Oct-2025, tatu: Temporarily disable until #5357 merged
        if (true) { return; }

        MockedSpringCglibProxy mockedProxy = new MockedSpringCglibProxy("hello");
        String json = MAPPER.writeValueAsString(mockedProxy);
        Map<?, ?> properties = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singleton("propertyName"), properties.keySet());
    }

    // https://github.com/FasterXML/jackson-databind/issues/5354
    //@Test
    public void testWriteWithHibernateCglibProxyDoesNotIncludeCallbacksProperty() throws Exception
    {
        MockedHibernateCglibProxy mockedProxy = new MockedHibernateCglibProxy("hello");
        String json = MAPPER.writeValueAsString(mockedProxy);
        Map<?, ?> properties = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singleton("propertyName"), properties.keySet());
    }

    // https://github.com/FasterXML/jackson-databind/issues/5354
    //@Test
    public void testWriteWithNetCglibProxyDoesNotIncludeCallbacksProperty() throws Exception
    {
        MockedNetCglibProxy mockedProxy = new MockedNetCglibProxy("hello");
        String json = MAPPER.writeValueAsString(mockedProxy);
        Map<?, ?> properties = MAPPER.readValue(json, Map.class);
        assertEquals(Collections.singleton("propertyName"), properties.keySet());
    }
}
