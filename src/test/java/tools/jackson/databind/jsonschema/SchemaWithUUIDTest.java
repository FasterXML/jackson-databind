package tools.jackson.databind.jsonschema;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaWithUUIDTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUUIDSchema() throws Exception
    {
        final AtomicReference<JsonValueFormat> format = new AtomicReference<>();

        MAPPER.acceptJsonFormatVisitor(UUID.class, new JsonFormatVisitorWrapper.Base() {
            @Override
            public JsonStringFormatVisitor expectStringFormat(JavaType type) {
                return new JsonStringFormatVisitor() {
                    @Override
                    public void enumTypes(Set<String> enums) { }

                    @Override
                    public void format(JsonValueFormat f) {
                        format.set(f);
                    }
                };
            }
        });
        assertEquals(JsonValueFormat.UUID, format.get());
    }
}
