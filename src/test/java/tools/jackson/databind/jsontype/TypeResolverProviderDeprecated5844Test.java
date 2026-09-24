package tools.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify that the deprecated 3-argument
 * {@code _constructStdTypeResolverBuilder()} overload (retained for subclasses
 * like the one in `jackson-dataformat-xml`) delegates to the 4-argument variant
 * instead of recursing into itself.
 */
public class TypeResolverProviderDeprecated5844Test extends DatabindTestUtil
{
    @SuppressWarnings("deprecation")
    static class DelegatingProvider extends TypeResolverProvider {
        public TypeResolverBuilder<?> callDeprecated(MapperConfig<?> config,
                JsonTypeInfo.Value typeInfo, JavaType baseType) {
            return _constructStdTypeResolverBuilder(config, typeInfo, baseType);
        }
    }

    @Test
    void deprecatedOverloadDoesNotRecurse() throws Exception
    {
        MapperConfig<?> config = JsonMapper.shared().serializationConfig();
        JavaType baseType = defaultTypeFactory().constructType(Object.class);
        JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME,
                JsonTypeInfo.As.PROPERTY, null, null, false, null);

        // Before fix: StackOverflowError due to self-recursion
        assertNotNull(new DelegatingProvider().callDeprecated(config, typeInfo, baseType));
    }
}
