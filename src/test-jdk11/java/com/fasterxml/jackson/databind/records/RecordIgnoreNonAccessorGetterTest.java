package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RecordIgnoreNonAccessorGetterTest extends BaseMapTest {

    // [databind#3628]
    interface InterfaceWithGetter {

        String getId();

        String getName();
    }

    @JsonPropertyOrder({"id", "name", "count"}) // easier to assert when JSON field ordering is always the same
    record RecordWithInterfaceWithGetter(String name) implements InterfaceWithGetter {

        @Override
        public String getId() {
            return "ID:" + name;
        }

        @Override
        public String getName() {
            return name;
        }

        // [databind#3895]
        public int getCount() {
            return 999;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testSerializeIgnoreInterfaceGetter_WithoutUsingVisibilityConfig() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));

        assertEquals("{\"id\":\"ID:Bob\",\"name\":\"Bob\",\"count\":999}", json);
    }

    public void testSerializeIgnoreInterfaceGetter_UsingVisibilityConfig() throws Exception {
        MAPPER.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        String json = MAPPER.writeValueAsString(new RecordWithInterfaceWithGetter("Bob"));

        assertEquals("{\"name\":\"Bob\"}", json);
    }
}
