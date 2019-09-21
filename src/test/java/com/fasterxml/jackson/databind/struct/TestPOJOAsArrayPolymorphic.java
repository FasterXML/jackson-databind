package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestPOJOAsArrayPolymorphic extends BaseMapTest
{
    // [databind#2077]
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY)  // Both WRAPPER_OBJECT and WRAPPER_ARRAY cause the same problem
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DirectLayout.class, name = "Direct"),
    })
    public interface Layout {
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    public static class DirectLayout implements Layout {
    }

    private final ObjectMapper MAPPER = sharedMapper();

    // [databind#2077]
    public void testPolymorphicAsArray() throws Exception
    {
        // 20-Sep-2019, taut: this fails to add shape information, due to class annotations
        //   not being checked due to missing `property` for `createContextual()`

        String json = MAPPER.writeValueAsString(new DirectLayout());

        Layout instance = MAPPER.readValue(json, Layout.class);
        assertNotNull(instance);
    }
}
