package tools.jackson.databind.deser;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reproduction for the bug: disabling {@link MapperFeature#DETECT_PARAMETER_NAMES}
 * (to opt out of bytecode-derived parameter names) also silently disabled
 * {@code @ConstructorProperties}-based implicit name resolution, even though
 * that annotation-based mechanism has nothing to do with {@code -parameters}
 * bytecode metadata and worked fine in Jackson 2 regardless of this setting.
 */
public class ConstructorPropertiesRegressionTest
    extends DatabindTestUtil
{
    static class CtorPropsDto {
        public final int id;
        public final String name;

        @ConstructorProperties({"id", "name"})
        public CtorPropsDto(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // Control case: no `@ConstructorProperties`, relies solely on bytecode
    // (`-parameters`) name detection -- this one SHOULD still be suppressed
    // when the feature is disabled.
    static class BytecodeOnlyDto {
        public final int id;
        public final String name;

        public BytecodeOnlyDto(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // `@ConstructorProperties` must keep working regardless of
    // `DETECT_PARAMETER_NAMES`, since it is an independent, older mechanism.
    @Test
    public void constructorPropertiesWorksWhenDetectParameterNamesEnabled()
    {
        JsonMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.DETECT_PARAMETER_NAMES)
                .build();
        CtorPropsDto dto = mapper.readValue("{\"id\":1,\"name\":\"x\"}", CtorPropsDto.class);
        assertEquals(1, dto.id);
        assertEquals("x", dto.name);
    }

    @Test
    public void constructorPropertiesWorksWhenDetectParameterNamesDisabled()
    {
        JsonMapper mapper = JsonMapper.builder()
                .disable(MapperFeature.DETECT_PARAMETER_NAMES)
                .build();
        CtorPropsDto dto = mapper.readValue("{\"id\":1,\"name\":\"x\"}", CtorPropsDto.class);
        assertEquals(1, dto.id);
        assertEquals("x", dto.name);
    }

    // Guard rail: the feature must still suppress bytecode-only implicit
    // names when there's no `@ConstructorProperties` to fall back on.
    @Test
    public void bytecodeOnlyNamesStillSuppressedWhenDetectParameterNamesDisabled()
    {
        JsonMapper mapper = JsonMapper.builder()
                .disable(MapperFeature.DETECT_PARAMETER_NAMES)
                .build();
        assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{\"id\":1,\"name\":\"x\"}", BytecodeOnlyDto.class));
    }
}
