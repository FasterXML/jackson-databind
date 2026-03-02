package tools.jackson.databind.type;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecursiveTypeTest extends DatabindTestUtil
{
    // for [databind#1301]
    @SuppressWarnings("serial")
    static class HashTree<K, V> extends HashMap<K, HashTree<K, V>> { }

 // for [databind#938]
    public static interface Ability<T> { }

    // for [databind#1647]
    static interface IFace<T> {}

    // for [databind#1647]
    static class Base implements IFace<Sub> { }

    // for [databind#1647]
    static class Sub extends Base { }

    public static final class ImmutablePair<L, R> implements Map.Entry<L, R>, Ability<ImmutablePair<L, R>> {
        public final L key;
        public final R value;

        public ImmutablePair(final L key, final R value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public L getKey() {
            return key;
        }

        @Override
        public R getValue() {
            return value;
        }

        @Override
        public R setValue(final R value) {
            throw new UnsupportedOperationException();
        }

        static <L, R> ImmutablePair<L, R> of(final L left, final R right) {
            return new ImmutablePair<L, R>(left, right);
        }
    }

    // for [databind#1301]
    @Test
    public void testRecursiveType()
    {
        TypeFactory tf = defaultTypeFactory();
        JavaType type = tf.constructType(HashTree.class);
        assertNotNull(type);
    }

    // for [databind#1301]
    @SuppressWarnings("serial")
    static class DataDefinition extends HashMap<String, DataDefinition> {
        public DataDefinition definition;
        public DataDefinition elements;
        public String regex;
        public boolean required;
        public String type;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#938]
    @Test
    public void testRecursivePair() throws Exception
    {
        JavaType t = MAPPER.constructType(ImmutablePair.class);

        assertNotNull(t);
        assertEquals(ImmutablePair.class, t.getRawClass());

        List<ImmutablePair<String, Double>> list = new ArrayList<ImmutablePair<String, Double>>();
        list.add(ImmutablePair.of("Hello World!", 123d));
        String json = MAPPER.writeValueAsString(list);

        assertNotNull(json);

        // cannot deserialize with current definition, however
    }

    // for [databind#1301]
    @Test
    public void testJavaTypeToString() throws Exception
    {
        TypeFactory tf = MAPPER.getTypeFactory();
        String desc = tf.constructType(DataDefinition.class).toString();
        assertNotNull(desc);
        // could try comparing exact message, but since it's informational try looser:
        if (!desc.contains("map type")) {
            fail("Description should contain 'map type', did not: "+desc);
        }
        if (!desc.contains("recursive type")) {
            fail("Description should contain 'recursive type', did not: "+desc);
        }
    }

    // for [databind#1647]
    @Test
    public void testSuperClassWithReferencedJavaType() {
        TypeFactory tf = MAPPER.getTypeFactory();
        tf.constructType(Base.class); // must be constructed before sub to set the cache correctly
        JavaType subType = tf.constructType(Sub.class);
        // baseTypeFromSub should be a ResolvedRecursiveType in this test
        JavaType baseTypeFromSub = subType.getSuperClass();
        assertNotNull(baseTypeFromSub.getSuperClass());
    }

    /*
    /**********************************************************
    /* Unit tests: recursive type with default typing [databind#1658]
    /**********************************************************
     */

    @SuppressWarnings("serial")
    static class Tree1658<T> extends HashMap<T, Tree1658<T>>
    {
        public Tree1658() { }

        public Tree1658(List<T> children) {
            this();
            for (final T t : children) {
                this.put(t, new Tree1658<T>());
            }
        }

        public List<Tree1658<T>> getLeafTrees() {
            return null;
        }
    }

    // [databind#1658]
    @Test
    public void testRecursive1658() throws Exception
    {
        Tree1658<String> t = new Tree1658<String>(Arrays.asList("hello", "world"));
        final TypeResolverBuilder<?> typer = new StdTypeResolverBuilder(JsonTypeInfo.Id.CLASS,
                JsonTypeInfo.As.PROPERTY, null);
        ObjectMapper mapper = jsonMapperBuilder()
                .setDefaultTyping(typer)
                .build();
        String res = mapper.writeValueAsString(t);
        Tree1658<?> tRead = mapper.readValue(res, Tree1658.class);
        assertNotNull(tRead);

        // 30-Oct-2019, tatu: Let's actually verify that description will be safe to use, too
        JavaType resolved = mapper.getTypeFactory()
                .constructType(new TypeReference<Tree1658<String>> () { });
        final String namePath = Tree1658.class.getName().replace('.', '/');
        assertEquals("L"+namePath+";", resolved.getErasedSignature());
        assertEquals("L"+namePath+"<Ljava/lang/String;L"+namePath+";>;",
                resolved.getGenericSignature());
    }
}
