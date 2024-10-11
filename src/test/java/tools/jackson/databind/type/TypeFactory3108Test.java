package tools.jackson.databind.type;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#3108]: canonical type description for non-generic subtypes
@SuppressWarnings("serial")
public class TypeFactory3108Test extends DatabindTestUtil
{
    static class StringList3108 extends ArrayList<String> {}

    static class StringStringMap3108 extends HashMap<String, String> {}

    static class ParamType3108<T> {}

    static class ConcreteType3108 extends ParamType3108<Integer> {}

    private final TypeFactory TF = defaultTypeFactory();

    // [databind#3108] with custom Collection
    @Test
    public void testCanonicalWithCustomCollection()
    {
        JavaType stringListType = TF.constructType(StringList3108.class);
        String canonical = stringListType.toCanonical();
        JavaType type = TF.constructFromCanonical(canonical);
        assertEquals(StringList3108.class, type.getRawClass());
        assertTrue(type.isCollectionLikeType());
    }

    // [databind#3108] with custom Map
    @Test
    public void testCanonicalWithCustomMap()
    {
        JavaType stringListType = TF.constructType(StringStringMap3108.class);
        String canonical = stringListType.toCanonical();
        JavaType type = TF.constructFromCanonical(canonical);
        assertEquals(StringStringMap3108.class, type.getRawClass());
        assertTrue(type.isMapLikeType());
    }

    // [databind#3108] with custom generic type
    @Test
    public void testCanonicalWithCustomGenericType()
    {
        JavaType stringListType = TF.constructType(ConcreteType3108.class);
        String canonical = stringListType.toCanonical();
        JavaType type = TF.constructFromCanonical(canonical);
        assertEquals(ConcreteType3108.class, type.getRawClass());
    }
}
