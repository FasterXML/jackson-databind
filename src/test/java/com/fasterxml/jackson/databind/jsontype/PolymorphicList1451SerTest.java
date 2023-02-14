package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class PolymorphicList1451SerTest extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static class A {
        public String a;
    }
    public static class B extends A {
        public String b;
    }

    private final String CLASS_NAME = getClass().getSimpleName();

    public void testCollectionWithTypeInfo() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .disable(SerializationFeature.EAGER_SERIALIZER_FETCH)
//                .disable(DeserializationFeature.EAGER_DESERIALIZER_FETCH)
                ;

        List<A> input = new ArrayList<A>();
        A a = new A();
        a.a = "a1";
        input.add(a);

        B b = new B();
        b.b = "b";
        b.a = "a2";
        input.add(b);

        final TypeReference<?> typeRef =
                new TypeReference<Collection<A>>(){};
        ObjectWriter writer = mapper.writerFor(typeRef);

        String result = writer.writeValueAsString(input);

        assertEquals(a2q(
"[{'@class':'."+CLASS_NAME+"$A','a':'a1'},{'@class':'."+CLASS_NAME+"$B','a':'a2','b':'b'}]"
), result);

        List<A> output = mapper.readerFor(typeRef)
                .readValue(result);
        assertEquals(2, output.size());
        assertEquals(A.class, output.get(0).getClass());
        assertEquals(B.class, output.get(1).getClass());
    }
}
