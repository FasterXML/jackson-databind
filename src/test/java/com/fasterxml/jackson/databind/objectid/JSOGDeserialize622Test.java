package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

/**
 * Unit test(s) for [databind#622], supporting non-scalar-Object-ids,
 * to support things like JSOG.
 */
public class JSOGDeserialize622Test extends BaseMapTest
{
    /** the key of the property that holds the ref */
    public static final String REF_KEY = "@ref";

    /**
     * JSON input
     */
    private static final String EXP_EXAMPLE_JSOG =  a2q(
            "{'@id':'1','foo':66,'next':{'"+REF_KEY+"':'1'}}");

    /**
     * Customer IdGenerator
     */
    static class JSOGGenerator extends ObjectIdGenerator<JSOGRef>  {

    private static final long serialVersionUID = 1L;
    protected transient int _nextValue;
    protected final Class<?> _scope;

    protected JSOGGenerator() { this(null, -1); }

    protected JSOGGenerator(Class<?> scope, int nextValue) {
        _scope = scope;
        _nextValue = nextValue;
    }

    @Override
    public Class<?> getScope() {
        return _scope;
    }

    @Override
    public boolean canUseFor(ObjectIdGenerator<?> gen) {
        return (gen.getClass() == getClass()) && (gen.getScope() == _scope);
    }

    @Override
    public ObjectIdGenerator<JSOGRef> forScope(Class<?> scope) {
          return (_scope == scope) ? this : new JSOGGenerator(scope, _nextValue);
    }

    @Override
    public ObjectIdGenerator<JSOGRef> newForSerialization(Object context) {
          return new JSOGGenerator(_scope, 1);
    }

    @Override
    public com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey key(Object key) {
          return new IdKey(getClass(), _scope, key);
    }

    // important: otherwise won't get proper handling
    @Override
    public boolean maySerializeAsObject() { return true; }

    // ditto: needed for handling Object-valued Object references
    @Override
    public boolean isValidReferencePropertyName(String name, Object parser) {
        return REF_KEY.equals(name);
    }

    @Override
    public JSOGRef generateId(Object forPojo) {
          int id = _nextValue;
          ++_nextValue;
          return new JSOGRef(id);
    }
    }

    /**
     * The reference deserializer
     */
    static class JSOGRefDeserializer extends JsonDeserializer<JSOGRef>
    {
      @Override
      public JSOGRef deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
          JsonNode node = p.readValueAsTree();
          if (node.isTextual()) {
              return new JSOGRef(node.asInt());
          }
          JsonNode n = node.get(REF_KEY);
          if (n == null) {
              ctx.reportInputMismatch(JSOGRef.class, "Could not find key '"+REF_KEY
                      +"' from ("+node.getClass().getName()+"): "+node);
          }
          return new JSOGRef(n.asInt());
      }
    }

    /**
     * The reference object
     */
    @JsonDeserialize(using=JSOGRefDeserializer.class)
    static class JSOGRef
    {
        @JsonProperty(REF_KEY)
        public int ref;

        public JSOGRef() { }

        public JSOGRef(int val) {
            ref = val;
        }

        @Override
        public String toString() { return "[JSOGRef#"+ref+"]"; }

        @Override
        public int hashCode() {
            return ref;
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof JSOGRef)
                    && ((JSOGRef) other).ref == this.ref;
        }
    }

    /**
     * Example class using JSOGGenerator
     */
    @JsonIdentityInfo(generator=JSOGGenerator.class, property="@id")
    public static class IdentifiableExampleJSOG {
        public int foo;
        public IdentifiableExampleJSOG next;

        protected IdentifiableExampleJSOG() { }
        public IdentifiableExampleJSOG(int v) {
            foo = v;
        }
    }

    public static class JSOGWrapper {
        public int value;

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object jsog;

        JSOGWrapper() { }
        public JSOGWrapper(int v) { value = v; }
    }

    // For [databind#669]

    @JsonIdentityInfo(generator=JSOGGenerator.class)
    @JsonTypeInfo(use=Id.CLASS, include= As.PROPERTY, property="@class")
    public static class Inner {
        public String bar;

        protected Inner() {}
        public Inner(String bar) { this.bar = bar; }
    }

    public static class SubInner extends Inner {
        public String extra;

        protected SubInner() {}
        public SubInner(String bar, String extra) {
            super(bar);
            this.extra = extra;
        }
    }

    @JsonIdentityInfo(generator=JSOGGenerator.class)
    public static class Outer {
        public String foo;
        public Inner inner1;
        public Inner inner2;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // Basic for [databind#622]
    public void testStructJSOGRef() throws Exception
    {
        IdentifiableExampleJSOG result = MAPPER.readValue(EXP_EXAMPLE_JSOG,
                IdentifiableExampleJSOG.class);
        assertEquals(66, result.foo);
        assertSame(result, result.next);
    }

    // polymorphic alternative for [databind#622]
    public void testPolymorphicRoundTrip() throws Exception
    {
        JSOGWrapper w = new JSOGWrapper(15);
        // create a nice little loop
        IdentifiableExampleJSOG ex = new IdentifiableExampleJSOG(123);
        ex.next = ex;
        w.jsog = ex;

        String json = MAPPER.writeValueAsString(w);

        JSOGWrapper out = MAPPER.readValue(json, JSOGWrapper.class);
        assertNotNull(out);
        assertEquals(15, out.value);
        assertTrue(out.jsog instanceof IdentifiableExampleJSOG);
        IdentifiableExampleJSOG jsog = (IdentifiableExampleJSOG) out.jsog;
        assertEquals(123, jsog.foo);
        assertSame(jsog, jsog.next);
    }

    // polymorphic alternative for [databind#669]
    public void testAlterativePolymorphicRoundTrip669() throws Exception
    {
        Outer outer = new Outer();
        outer.foo = "foo";
        outer.inner1 = outer.inner2 = new SubInner("bar", "extra");

        String jsog = MAPPER.writeValueAsString(outer);

        Outer back = MAPPER.readValue(jsog, Outer.class);

        assertSame(back.inner1, back.inner2);
    }
}
