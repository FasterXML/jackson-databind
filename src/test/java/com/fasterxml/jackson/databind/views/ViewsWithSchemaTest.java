package com.fasterxml.jackson.databind.views;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;

public class ViewsWithSchemaTest extends BaseMapTest
{
    interface ViewBC { }
    interface ViewAB { }

    @JsonPropertyOrder({ "a", "b", "c" })
    static class POJO {
        @JsonView({ ViewAB.class })
        public int a;

        @JsonView({ ViewAB.class, ViewBC.class })
        public int b;

        @JsonView({ ViewBC.class })
        public int c;
    }

    static class ListingVisitor extends JsonFormatVisitorWrapper.Base
    {
        public final List<String> names = new ArrayList<String>();

        @Override
        public JsonObjectFormatVisitor expectObjectFormat(JavaType type) {
            return new JsonObjectFormatVisitor.Base() {
                @Override
                public void optionalProperty(BeanProperty writer) {
                    names.add(writer.getName());
                }

                @Override
                public void optionalProperty(String name,
                        JsonFormatVisitable handler, JavaType propertyTypeHint) {
                    names.add(name);
                }
            };
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSchemaWithViews() throws Exception
    {
        ListingVisitor v = new ListingVisitor();
        MAPPER.writerWithView(ViewBC.class)
            .acceptJsonFormatVisitor(POJO.class, v);
        assertEquals(Arrays.asList("b", "c"), v.names);

        v = new ListingVisitor();
        MAPPER.writerWithView(ViewAB.class)
            .acceptJsonFormatVisitor(POJO.class, v);
        assertEquals(Arrays.asList("a", "b"), v.names);
    }

    public void testSchemaWithoutViews() throws Exception
    {
        ListingVisitor v = new ListingVisitor();
        MAPPER.acceptJsonFormatVisitor(POJO.class, v);
        assertEquals(Arrays.asList("a", "b", "c"), v.names);
    }
}
