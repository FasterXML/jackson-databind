package tools.jackson.databind.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class InnerClassType3288Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes(@JsonSubTypes.Type(value = ConcreteValue.class, name = "concrete"))
    interface Value { }

    static class ConcreteValue implements Value { }

    interface Wrapper<T> {
        T getValue();
    }

    static class Outer<T> {
        class Inner implements Wrapper<T> {
            private final T value;

            Inner(T value) {
                this.value = value;
            }

            @Override
            public T getValue() {
                return value;
            }
        }

        class Generic<U> {
            public T outer;
            public U inner;
        }

        @SuppressWarnings("serial")
        class NestedMap<U> extends HashMap<U, List<T>> { }

        class Shadow<T> {
            public T value;

            public <T extends Number> T getNumber() {
                return null;
            }
        }

        class Bounded<U extends T> { }

        class Middle {
            class Dollar$Name<U> { }
        }
    }

    static abstract class RecursiveOuter<T> implements Supplier<RecursiveOuter<T>.Inner> {
        class Inner {
            public T value;
        }
    }

    static class Root<T> {
        static class Mid<U> {
            class Leaf {
                public U value;
            }
        }
    }

    @SuppressWarnings("serial")
    static class Same<T> extends HashMap<T, T> { }

    static class Container {
        public Wrapper<Value> wrapper;

        Container(Wrapper<Value> wrapper) {
            this.wrapper = wrapper;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    private final TypeFactory TF = MAPPER.getTypeFactory();

    @Test
    void serializeRuntimeInnerClass() {
        Container input = new Container(new Outer<Value>().new Inner(new ConcreteValue()));
        assertEquals("{\"wrapper\":{\"value\":{\"@type\":\"concrete\"}}}",
                MAPPER.writeValueAsString(input));
    }

    @Test
    void distinguishOwnerBindings() throws Exception {
        JavaType strings = TF.constructType(new TypeReference<Outer<String>.Generic<Integer>>() { });
        JavaType longs = TF.constructType(new TypeReference<Outer<Long>.Generic<Integer>>() { });

        assertNotEquals(strings, longs);
        assertEquals(1, strings.containedTypeCount());
        assertEquals(Integer.class, strings.containedType(0).getRawClass());
        assertEquals(String.class, TF.resolveMemberType(
                Outer.Generic.class.getField("outer").getGenericType(), strings.getBindings()).getRawClass());
        assertEquals(Long.class, TF.resolveMemberType(
                Outer.Generic.class.getField("outer").getGenericType(), longs.getBindings()).getRawClass());
    }

    @Test
    void roundTripOwnerBindings() {
        assertThrows(IllegalArgumentException.class,
                () -> TF.constructFromCanonical("java.util.List<>"));
        JavaType type = TF.constructType(new TypeReference<Outer<String>.Generic<Integer>>() { });
        assertEquals(type, TF.constructFromCanonical(type.toCanonical()));
        assertEquals("L" + Outer.class.getName().replace('.', '/')
                + "<Ljava/lang/String;>.Generic<Ljava/lang/Integer;>;", type.getGenericSignature());
        assertEquals("L" + Outer.Generic.class.getName().replace('.', '/') + ";", type.getErasedSignature());

        JavaType nested = TF.constructType(new TypeReference<Outer<String>.Middle.Dollar$Name<Integer>>() { });
        assertEquals(nested, TF.constructFromCanonical(nested.toCanonical()));
        assertEquals("L" + Outer.class.getName().replace('.', '/')
                + "<Ljava/lang/String;>.Middle.Dollar$Name<Ljava/lang/Integer;>;", nested.getGenericSignature());
    }

    @Test
    void specializeOwnAndOwnerParameters() {
        JavaType base = TF.constructType(new TypeReference<Map<Integer, List<String>>>() { });
        JavaType specialized = TF.constructSpecializedType(base, Outer.NestedMap.class);

        assertEquals(Integer.class, specialized.containedType(0).getRawClass());
        assertEquals(String.class, specialized.getBindings().getOwnerType().containedType(0).getRawClass());
        assertEquals(Integer.class, specialized.getKeyType().getRawClass());
        assertEquals(String.class, specialized.getContentType().getContentType().getRawClass());
        assertEquals(specialized, TF.constructFromCanonical(specialized.toCanonical()));

        JavaType wildcardBase = TF.constructType(new TypeReference<Map<?, String>>() { });
        JavaType same = TF.constructSpecializedType(wildcardBase, Same.class);
        assertEquals(String.class, same.containedType(0).getRawClass());
    }

    @Test
    void distinguishVariableDeclarations() throws Exception {
        JavaType type = TF.constructType(new TypeReference<Outer<String>.Shadow<Integer>>() { });
        TypeBindings bindings = type.getBindings();

        assertEquals(Integer.class, TF.resolveMemberType(
                Outer.Shadow.class.getField("value").getGenericType(), bindings).getRawClass());
        assertEquals(Number.class, TF.resolveMemberType(
                Outer.Shadow.class.getMethod("getNumber").getGenericReturnType(), bindings).getRawClass());
        assertEquals(String.class, TF.resolveMemberType(Outer.class.getTypeParameters()[0], bindings).getRawClass());
    }

    @Test
    void resolveOwnerDependentWildcardBound() {
        JavaType type = TF.constructType(new TypeReference<Outer<Number>.Bounded<?>>() { });
        assertEquals(Number.class, type.containedType(0).getRawClass());
    }

    @Test
    void resolveRecursiveOwner() throws Exception {
        JavaType type = TF.constructType(new TypeReference<RecursiveOuter<String>.Inner>() { });
        JavaType supplied = type.getBindings().getOwnerType().findSuperType(Supplier.class).containedType(0);
        assertEquals(String.class, TF.resolveMemberType(
                RecursiveOuter.Inner.class.getField("value").getGenericType(), supplied.getBindings()).getRawClass());
        assertEquals(String.class, TF.resolveMemberType(
                RecursiveOuter.Inner.class.getField("value").getGenericType(), type.getBindings()).getRawClass());
    }

    @Test
    void stopOwnerScopeAtStaticClass() throws Exception {
        JavaType type = TF.constructType(new TypeReference<Root.Mid<Long>.Leaf>() { });
        assertEquals(0, type.containedTypeCount());
        assertTrue(type.hasGenericTypes());
        assertNull(type.getBindings().getOwnerType().getBindings().getOwnerType());
        assertEquals(Long.class, TF.resolveMemberType(
                Root.Mid.Leaf.class.getField("value").getGenericType(), type.getBindings()).getRawClass());
        assertEquals(type, TF.constructFromCanonical(type.toCanonical()));
    }
}
