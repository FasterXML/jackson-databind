package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.*;

public class UnwrappedCaching2461Test extends BaseMapTest
{
    // [databind#2461]
    static class Base {
        public String id;

        Base(String id) {
            this.id = id;
        }
    }

    static class BaseContainer {
        @JsonUnwrapped(prefix = "base.")
        public Base base;

        BaseContainer(Base base) {
            this.base = base;
        }
    }

    static class BaseContainerContainer {
        @JsonUnwrapped(prefix = "container.")
        public BaseContainer container;

        BaseContainerContainer(BaseContainer container) {
            this.container = container;
        }
    }

    // [databind#2461]
    public void testUnwrappedCaching() throws Exception {
        final BaseContainer inner = new BaseContainer(new Base("12345"));
        final BaseContainerContainer outer = new BaseContainerContainer(inner);

        final ObjectMapper mapperOrder1 = newJsonMapper();
        assertEquals("{\"container.base.id\":\"12345\"}", mapperOrder1.writeValueAsString(outer));
        assertEquals("{\"base.id\":\"12345\"}", mapperOrder1.writeValueAsString(inner));
        assertEquals("{\"container.base.id\":\"12345\"}", mapperOrder1.writeValueAsString(outer));

        final ObjectMapper mapperOrder2 = newJsonMapper();
        assertEquals("{\"base.id\":\"12345\"}", mapperOrder2.writeValueAsString(inner));
        //  Will fail here
        assertEquals("{\"container.base.id\":\"12345\"}", mapperOrder2.writeValueAsString(outer));
    }
}
