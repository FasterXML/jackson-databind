package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class TestBuilderSimple extends BaseMapTest
{
    // // Simple 2-property value class, builder with standard naming
	
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    @SuppressWarnings("hiding")
    static class SimpleBuilderXY
    {
        public int x, y;
    	
        public SimpleBuilderXY withX(int x) {
    		    this.x = x;
    		    return this;
        }

        public SimpleBuilderXY withY(int y) {
    		    this.y = y;
    		    return this;
        }

        public ValueClassXY build() {
    		    return new ValueClassXY(x, y);
        }
    }

    // // 3-property value, with more varied builder
	
    @JsonDeserialize(builder=BuildABC.class)
    static class ValueClassABC
    {
        final int a, b, c;

        protected ValueClassABC(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @SuppressWarnings("hiding")
    static class BuildABC
    {
        public int a; // to be used as is
        private int b, c;
    	
        @JsonProperty("b")
        public BuildABC assignB(int b) {
            this.b = b;
            return this;
        }

        // Also ok NOT to return 'this'
        @JsonSetter("c")
        public void c(int c) {
            this.c = c;
        }

        public ValueClassABC build() {
            return new ValueClassABC(a, b, c);
        }
    }

    // // Then Builder that is itself immutable
    
    @JsonDeserialize(builder=BuildImmutable.class)
    static class ValueImmutable
    {
        final int value;
        protected ValueImmutable(int v) { value = v; }
    }
    
    static class BuildImmutable {
        private final int value;
        
        private BuildImmutable() { this(0); }
        private BuildImmutable(int v) {
            value = v;
        }
        public BuildImmutable withValue(int v) {
            return new BuildImmutable(v);
        }
        public ValueImmutable build() {
            return new ValueImmutable(value);
        }
    }
    
    // And then with custom naming:

    @JsonDeserialize(builder=BuildFoo.class)
    static class ValueFoo
    {
        final int value;
        protected ValueFoo(int v) { value = v; }
    }

    @JsonPOJOBuilder(withPrefix="foo", buildMethodName="construct")
    static class BuildFoo {
        private int value;
        
        public BuildFoo fooValue(int v) {
            value = v;
            return this;
        }
        public ValueFoo construct() {
            return new ValueFoo(value);
        }
    }

    // And with creator(s)
	
    @JsonDeserialize(builder=CreatorBuilder.class)
    static class CreatorValue
    {
        final int a, b, c;

        protected CreatorValue(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    static class CreatorBuilder {
        private final int a, b;
        private int c;

        @JsonCreator
        public CreatorBuilder(@JsonProperty("a") int a,
                @JsonProperty("b") int b)
        {
            this.a = a;
            this.b = b;
        }
        
        public CreatorBuilder withC(int v) {
            c = v;
            return this;
        }
        public CreatorValue build() {
            return new CreatorValue(a, b, c);
        }
    }
        
	/*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testSimple() throws Exception
    {
    	String json = "{\"x\":1,\"y\":2}";
    	Object o = mapper.readValue(json, ValueClassXY.class);
    	assertNotNull(o);
    	assertSame(ValueClassXY.class, o.getClass());
    	ValueClassXY value = (ValueClassXY) o;
    	// note: ctor adds one to both values
    	assertEquals(value._x, 2);
    	assertEquals(value._y, 3);
    }

    public void testMultiAccess() throws Exception
    {
    	String json = "{\"c\":3,\"a\":2,\"b\":-9}";
    	ValueClassABC value = mapper.readValue(json, ValueClassABC.class);
    	assertNotNull(value);
    	// note: ctor adds one to both values
    	assertEquals(value.a, 2);
    	assertEquals(value.b, -9);
    	assertEquals(value.c, 3);
    }

    // test for Immutable builder, to ensure return value is used
    public void testImmutable() throws Exception
    {
        final String json = "{\"value\":13}";
        ValueImmutable value = mapper.readValue(json, ValueImmutable.class);        
        assertEquals(13, value.value);
    }

    // test with custom 'with-prefix'
    public void testCustomWith() throws Exception
    {
        final String json = "{\"value\":1}";
        ValueFoo value = mapper.readValue(json, ValueFoo.class);        
        assertEquals(1, value.value);
    }

    // test to ensure @JsonCreator also work
    public void testWithCreator() throws Exception
    {
        final String json = "{\"a\":1,\"c\":3,\"b\":2}";
        CreatorValue value = mapper.readValue(json, CreatorValue.class);        
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);
    }
}
