package tools.jackson.databind.introspect;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.AnySetterTest;
import tools.jackson.databind.testutil.DatabindTestUtil;

import com.fasterxml.jackson.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

public class UnwrapRootCause4603Test
    extends DatabindTestUtil
{
    static class CustomException extends RuntimeException {
        public CustomException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class Feature1347DeserBean {

        int value;

        public void setValue(int x) {
            try {
                // Throws ArithmeticException
                int a = x / 0;
            } catch (Exception e) {
                // should NOT get called, but just to
                throw new CustomException("Should NOT get called", e);
            }
        }
    }

    static class AnySetterBean
    {
        protected Map<String, Integer> props = new HashMap<String, Integer>();

        @JsonAnySetter
        public void prop(String name, Integer value) {
            throw new CustomException("@JsonAnySetter, Should NOT get called", null);
        }
    }

    static class Baz {
        private String qux;

        @JsonCreator
        public Baz(@JsonProperty("qux") String qux) {
            this.qux = qux;
        }

        public String getQux() {
            return qux;
        }
    }

    final String JSON = a2q("{'value':3}");

    final ObjectMapper MAPPER = newJsonMapper();

    // Intentionally enable
    final ObjectMapper ENABLED = jsonMapperBuilder()
            .configure(MapperFeature.UNWRAP_ROOT_CAUSE, true)
            .build();

    final ObjectMapper DISABLED = jsonMapperBuilder()
            .configure(MapperFeature.UNWRAP_ROOT_CAUSE, false)
            .build();

    // Whether disabled or enabled, should get ArithmeticException
    @Test
    public void testExceptionWrappingConfigurationEnabled()
            throws Exception
    {
        DatabindException enabledResult = _tryDeserializeWith(ENABLED);
        assertInstanceOf(ArithmeticException.class, enabledResult.getCause());

        // Default is same
        DatabindException defaultResult = _tryDeserializeWith(MAPPER);
        assertInstanceOf(ArithmeticException.class, defaultResult.getCause());

    }

    // Whether disabled or enabled, should get ArithmeticException
    @Test
    public void testExceptionWrappingConfiguration()
            throws Exception
    {
        DatabindException disabledResult = _tryDeserializeWith(DISABLED);
        // We are throwing exception inside a setter, so....
        assertInstanceOf(InvocationTargetException.class, disabledResult.getCause());
        assertInstanceOf(CustomException.class, disabledResult.getCause().getCause());

    }

    private DatabindException _tryDeserializeWith(ObjectMapper mapper) {
        return assertThrows(DatabindException.class,
                () -> mapper.readValue(JSON, Feature1347DeserBean.class)
        );
    }

    @Test
    public void testWithAnySetter()
            throws Exception
    {
        DatabindException result = assertThrows(DatabindException.class,
                () -> ENABLED.readValue(a2q("{'a':3}"), AnySetterBean.class));
        assertInstanceOf(CustomException.class, result.getCause());

        result = assertThrows(DatabindException.class,
                () -> DISABLED.readValue(a2q("{'a':3}"), AnySetterBean.class));
        assertInstanceOf(CustomException.class, result.getCause());
    }



}
