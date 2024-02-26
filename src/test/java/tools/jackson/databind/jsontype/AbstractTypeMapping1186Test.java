package tools.jackson.databind.jsontype;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractTypeMapping1186Test extends DatabindTestUtil
{
    public interface IContainer<T> {
        @JsonProperty("ts")
        List<T> getTs();
    }

    static class MyContainer<T> implements IContainer<T> {

        final List<T> ts;

        @JsonCreator
        public MyContainer(@JsonProperty("ts") List<T> ts) {
            this.ts = ts;
        }

        @Override
        public List<T> getTs() {
            return ts;
        }
    }

    public static class MyObject {
        public String msg;
    }

    @Test
    public void testDeserializeMyContainer() throws Exception {
        SimpleModule module = new SimpleModule().addAbstractTypeMapping(IContainer.class, MyContainer.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        String json = "{\"ts\": [ { \"msg\": \"hello\"} ] }";
        final Object o = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(IContainer.class, MyObject.class));
        assertEquals(MyContainer.class, o.getClass());
        MyContainer<?> myc = (MyContainer<?>) o;
        assertEquals(1, myc.ts.size());
        Object value = myc.ts.get(0);
        assertEquals(MyObject.class, value.getClass());
    }
}
