package tools.jackson.databind.introspect;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;

// Tests for [databind#4620]: 
//
// @since 2.18
public class DefaultCreatorResolution4620Test extends DatabindTestUtil
{
    static class PrimaryCreatorFindingIntrospector extends ImplicitNameIntrospector
    {
        private static final long serialVersionUID = 1L;

        private final Class<?>[] _argTypes;

        private JsonCreator.Mode _mode;

        private final String _factoryName;
        
        public PrimaryCreatorFindingIntrospector(JsonCreator.Mode mode,
                Class<?>... argTypes) {
            _mode = mode;
            _factoryName = null;
            _argTypes = argTypes;
        }

        public PrimaryCreatorFindingIntrospector(JsonCreator.Mode mode,
                String factoryName) {
            _mode = mode;
            _factoryName = factoryName;
            _argTypes = new Class<?>[0];
        }

        @Override
        public PotentialCreator findDefaultCreator(MapperConfig<?> config,
                AnnotatedClass valueClass,
                List<PotentialCreator> declaredConstructors,
                List<PotentialCreator> declaredFactories)
        {
            // Apply to all test POJOs here but nothing else
            if (!valueClass.getRawType().toString().contains("4584")) {
                return null;
            }

            if (_factoryName != null) {
                for (PotentialCreator ctor : declaredFactories) {
                    if (ctor.creator().getName().equals(_factoryName)) {
                        return ctor;
                    }
                }
                return null;
            }

            List<PotentialCreator> combo = new ArrayList<>(declaredConstructors);
            combo.addAll(declaredFactories);
            final int argCount = _argTypes.length;
            for (PotentialCreator ctor : combo) {
                if (ctor.paramCount() == argCount) {
                    int i = 0;
                    for (; i < argCount; ++i) {
                        if (_argTypes[i] != ctor.param(i).getRawType()) {
                            break;
                        }
                    }
                    if (i == argCount) {
                        ctor.overrideMode(_mode);
                        return ctor;
                    }
                }
            }
            return null;
        }
    }

    /*
    /**********************************************************************
    /* Test methods; simple properties-based Creators
    /**********************************************************************
     */

    @Test
    public void testCanonicalConstructor1ArgPropertiesCreator() throws Exception
    {
        // TODO
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    /*
    private ObjectReader readerWith(AnnotationIntrospector intr) {
        return mapperWith(intr).readerFor(POJO4584.class);
    }
    */

    private ObjectMapper mapperWith(AnnotationIntrospector intr) {
        return JsonMapper.builder()
                .annotationIntrospector(intr)
                .build();
    }
}
