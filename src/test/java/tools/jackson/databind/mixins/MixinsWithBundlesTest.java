package tools.jackson.databind.mixins;

import java.lang.annotation.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// for [databind#771]
public class MixinsWithBundlesTest extends DatabindTestUtil
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

    @Test
    public void testMixinWithBundles() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
               .addMixIn(Foo.class, FooMixin.class)
               .build();
        String result = mapper.writeValueAsString(new Foo("result"));
        assertEquals("{\"bar\":\"result\"}", result);
    }
}
