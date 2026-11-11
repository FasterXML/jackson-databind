package tools.jackson.databind.util;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import tools.jackson.databind.*;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class IgnorePropertiesUtilTest extends DatabindTestUtil
{
    static class AllowGettersBean {
        @JsonIgnoreProperties(value = "x", allowGetters = true)
        public Map<String, Object> props;
    }

    static class AllowSettersBean {
        @JsonIgnoreProperties(value = "x", allowSetters = true)
        public Map<String, Object> props;
    }

    static class IncludeBean {
        @JsonIncludeProperties("ok")
        public Map<String, Object> props;
    }

    static class NoAnnotationBean {
        public Map<String, Object> props;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private AnnotatedField _fieldOf(Class<?> cls) {
        DeserializationConfig config = MAPPER.deserializationConfig();
        JavaType type = MAPPER.constructType(cls);
        return AnnotatedClassResolver.resolve(config, type, config).fields().iterator().next();
    }

    private IgnorePropertiesUtil.Checker _checker(Class<?> cls, boolean forSer) {
        return IgnorePropertiesUtil.buildCheckerFor(MAPPER.deserializationConfig(),
                _fieldOf(cls), forSer);
    }

    // `allowGetters`: names still ignored for deserialization, but not serialization
    @Test
    public void buildCheckerForAllowGetters() {
        assertNull(_checker(AllowGettersBean.class, true));
        IgnorePropertiesUtil.Checker checker = _checker(AllowGettersBean.class, false);
        assertNotNull(checker);
        assertTrue(checker.shouldIgnore("x"));
        assertFalse(checker.shouldIgnore("y"));
    }

    // `allowSetters`: names still ignored for serialization, but not deserialization
    @Test
    public void buildCheckerForAllowSetters() {
        assertNull(_checker(AllowSettersBean.class, false));
        IgnorePropertiesUtil.Checker checker = _checker(AllowSettersBean.class, true);
        assertNotNull(checker);
        assertTrue(checker.shouldIgnore("x"));
        assertFalse(checker.shouldIgnore("y"));
    }

    @Test
    public void buildCheckerForInclusion() {
        for (boolean forSer : new boolean[] { true, false }) {
            IgnorePropertiesUtil.Checker checker = _checker(IncludeBean.class, forSer);
            assertNotNull(checker);
            assertFalse(checker.shouldIgnore("ok"));
            assertTrue(checker.shouldIgnore("other"));
        }
    }

    @Test
    public void buildCheckerForNoRules() {
        assertNull(_checker(NoAnnotationBean.class, true));
        assertNull(_checker(NoAnnotationBean.class, false));
        assertNull(IgnorePropertiesUtil.buildCheckerFor(MAPPER.deserializationConfig(), null, true));
    }
}
