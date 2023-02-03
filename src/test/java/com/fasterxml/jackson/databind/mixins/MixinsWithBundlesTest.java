package com.fasterxml.jackson.databind.mixins;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// for [databind#771]
public class MixinsWithBundlesTest extends BaseMapTest
{
    @Target(value={ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD })
    @Retention(value=RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonProperty("bar")
    public @interface ExposeStuff {

    }

    public abstract class FooMixin {
        @ExposeStuff
        public abstract String getStuff();
    }

    public static class Foo {

        private String stuff;

        Foo(String stuff) {
            this.stuff = stuff;
        }

        public String getStuff() {
            return stuff;
        }
    }
    public void testMixinWithBundles() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper().addMixIn(Foo.class, FooMixin.class);
        String result = mapper.writeValueAsString(new Foo("result"));
        assertEquals("{\"bar\":\"result\"}", result);
    }
}
