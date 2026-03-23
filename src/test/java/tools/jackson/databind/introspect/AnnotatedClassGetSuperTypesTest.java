package tools.jackson.databind.introspect;

import java.io.Serializable;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnnotatedClass#getSuperTypes()} added in 3.2.
 */
@SuppressWarnings("serial")
public class AnnotatedClassGetSuperTypesTest extends DatabindTestUtil
{
    static class SimpleBean {
        public int value;
    }

    static class ChildBean extends SimpleBean implements Serializable {
        public String name;
    }

    interface MarkerA { }
    interface MarkerB extends MarkerA { }

    static class MultiLevel extends ChildBean implements MarkerB {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Simple class whose only super type is Object (which is pruned) should
    // have an empty super types list
    @Test
    public void testGetSuperTypesForSimpleClass() {
        AnnotatedClass ac = _resolve(SimpleBean.class);
        List<JavaType> superTypes = ac.getSuperTypes();

        assertNotNull(superTypes);
        // SimpleBean extends Object directly; Object is excluded from super types
        assertTrue(superTypes.isEmpty(),
                "SimpleBean should have no super types (Object is excluded)");
    }

    // Interfaces listed before super-class chain; Object excluded
    @Test
    public void testGetSuperTypesForChildClass() {
        AnnotatedClass ac = _resolve(ChildBean.class);
        List<JavaType> superTypes = ac.getSuperTypes();

        // Exact order: interfaces of ChildBean first, then super-class chain
        _assertExactOrder(superTypes,
                Serializable.class, SimpleBean.class);
    }

    // Multi-level: at each level, interfaces before super-class, recursing up
    @Test
    public void testGetSuperTypesForMultiLevel() {
        AnnotatedClass ac = _resolve(MultiLevel.class);
        List<JavaType> superTypes = ac.getSuperTypes();

        // Order: MultiLevel's own interfaces (MarkerB, MarkerA),
        //   then ChildBean (super-class), ChildBean's interfaces (Serializable),
        //   then SimpleBean (ChildBean's super-class)
        _assertExactOrder(superTypes,
                MarkerB.class, MarkerA.class,
                ChildBean.class, Serializable.class, SimpleBean.class);
    }

    @Test
    public void testGetSuperTypesReturnsUnmodifiableList() {
        AnnotatedClass ac = _resolve(ChildBean.class);
        List<JavaType> superTypes = ac.getSuperTypes();

        assertThrows(UnsupportedOperationException.class, () -> {
            superTypes.add(null);
        });
    }

    // Object class itself should have no super types
    @Test
    public void testGetSuperTypesForObjectClass() {
        AnnotatedClass ac = _resolve(Object.class);
        List<JavaType> superTypes = ac.getSuperTypes();

        assertNotNull(superTypes);
        assertTrue(superTypes.isEmpty(),
                "Object class itself should have no super types");
    }

    // Interface type should list super-interfaces in order
    @Test
    public void testGetSuperTypesForInterface() {
        AnnotatedClass ac = _resolve(MarkerB.class);
        List<JavaType> superTypes = ac.getSuperTypes();

        _assertExactOrder(superTypes, MarkerA.class);
    }

    private AnnotatedClass _resolve(Class<?> cls) {
        DeserializationConfig config = MAPPER.deserializationConfig();
        JavaType type = MAPPER.constructType(cls);
        return AnnotatedClassResolver.resolve(config, type, config);
    }

    private static void _assertExactOrder(List<JavaType> actual, Class<?>... expected) {
        List<Class<?>> actualClasses = new ArrayList<>();
        for (JavaType t : actual) {
            actualClasses.add(t.getRawClass());
        }
        List<Class<?>> expectedList = Arrays.asList(expected);
        assertEquals(expectedList, actualClasses,
                "Super types should match in exact order");
    }
}
