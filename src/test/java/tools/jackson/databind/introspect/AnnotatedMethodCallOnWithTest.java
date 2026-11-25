package tools.jackson.databind.introspect;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link AnnotatedMethod#callOnWith}, used for method-based
 * {@code @JsonAnySetter} and for static factory methods of Enums.
 */
public class AnnotatedMethodCallOnWithTest extends DatabindTestUtil
{
    public static class Target {
        public String key;
        public int value;

        public void put(String k, int v) {
            key = k;
            value = v;
        }

        public int sum(int a, int b) {
            return a + b;
        }

        public void fail(String msg) {
            throw new IllegalStateException(msg);
        }

        public static String twice(String s) {
            return s + s;
        }

        public static String join(String... parts) {
            return String.join(",", parts);
        }
    }

    private static AnnotatedMethod annotated(String name, Class<?>... paramTypes) throws Exception {
        Method m = Target.class.getMethod(name, paramTypes);
        return new AnnotatedMethod(null, m, null, null);
    }

    @Test
    public void instanceMethodWithTwoArgs() throws Exception {
        AnnotatedMethod put = annotated("put", String.class, Integer.TYPE);
        Target t1 = new Target();
        assertNull(put.callOnWith(t1, "a", 1));
        assertEquals("a", t1.key);
        assertEquals(1, t1.value);

        // same (cached) invoker must be used for other instances
        Object invoker = put._invokerOnWith.get();
        assertNotNull(invoker);
        Target t2 = new Target();
        assertNull(put.callOnWith(t2, "b", 2));
        assertEquals("b", t2.key);
        assertEquals(2, t2.value);
        // and must not have affected first instance
        assertEquals("a", t1.key);
        assertSame(invoker, put._invokerOnWith.get());
    }

    @Test
    public void instanceMethodWithPrimitiveReturn() throws Exception {
        AnnotatedMethod sum = annotated("sum", Integer.TYPE, Integer.TYPE);
        assertEquals(Integer.valueOf(5), sum.callOnWith(new Target(), 2, 3));
    }

    @Test
    public void staticMethodIgnoresPojo() throws Exception {
        AnnotatedMethod twice = annotated("twice", String.class);
        // as with Enum factories, where the "pojo" passed is the Enum class
        assertEquals("abab", twice.callOnWith(Target.class, "ab"));
        assertEquals("cdcd", twice.callOnWith(null, "cd"));
    }

    // [databind#5231], [databind#6426]: varargs passed with fixed arity
    @Test
    public void staticVarargsMethodTakesArray() throws Exception {
        AnnotatedMethod join = annotated("join", String[].class);
        assertEquals("a,b", join.callOnWith(null, (Object) new String[] { "a", "b" }));
    }

    @Test
    public void exceptionIsNotWrapped() throws Exception {
        AnnotatedMethod fail = annotated("fail", String.class);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> fail.callOnWith(new Target(), "boom"));
        assertEquals("boom", e.getMessage());
    }
}
