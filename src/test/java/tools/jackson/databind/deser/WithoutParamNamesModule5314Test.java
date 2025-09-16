package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class WithoutParamNamesModule5314Test
    extends DatabindTestUtil
{
    // Constructor can (and will) be auto-detected if (and only if!)
    // Implicit Parameter Names are detected (see
    // {@link MapperFeature#DETECT_PARAMETER_NAMES})
    static class Bean178
    {
        final String hiddenName;
        final int hiddenAge;

        public Bean178(String openName, int openAge) {
            hiddenName = openName;
            hiddenAge = openAge;
        }
    }

    private final String JSON = a2q("{'openName':'stu','openAge':22}");

    @Test
    public void testWorksByDefault()
    {
        // Passes... by default
        _runTestSuccess(JsonMapper.builder()
                .build());
        // Passes... when enabled
        _runTestSuccess(JsonMapper.builder()
                .enable(MapperFeature.DETECT_PARAMETER_NAMES).build());
        // Fails when...disabled
        _runTestFailure(JsonMapper.builder()
                .disable(MapperFeature.DETECT_PARAMETER_NAMES).build());
        // Fails when...used with Jackson2Defaults
        _runTestFailure(JsonMapper
                .builderWithJackson2Defaults().build());
    }

    private void _runTestSuccess(JsonMapper mapper)
    {
        Bean178 bean = mapper.readValue(JSON, Bean178.class);

        assertEquals("stu", bean.hiddenName);
        assertEquals(22, bean.hiddenAge);
    }


    private void _runTestFailure(JsonMapper mapper) {
        try {
            mapper.readValue(JSON, Bean178.class);
            fail("Should have thrown an exception");
        } catch (InvalidDefinitionException e) {
            assertThat(e.getMessage())
                .contains("Cannot construct instance of")
                .contains("no Creators, like default constructor, exist");
        }
    }
}
