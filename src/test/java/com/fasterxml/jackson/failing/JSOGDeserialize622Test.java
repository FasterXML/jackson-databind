package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.databind.*;

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
  private static final String EXP_EXAMPLE_JSOG =  aposToQuotes(
          "{'@id':'1','foo':66,'next':{'"+REF_KEY+"':'1'}}");

  private final ObjectMapper mapper = new ObjectMapper();

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
    public JSOGRef deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JsonProcessingException {
      JsonNode node = jp.readValueAsTree();
      return node.isTextual()
              ? new JSOGRef(node.asInt()) : new JSOGRef(node.get(REF_KEY).asInt());
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
  }


  /**
   * Example class using JSOGGenerator
   */
  @JsonIdentityInfo(generator=JSOGGenerator.class)
  public static class IdentifiableExampleJSOG {
    public int foo;
    public IdentifiableExampleJSOG next;
  }

  /*
  /**********************************************************************
  /* Test methods
  /**********************************************************************
   */

  // for [databind#622]
  public void testStructJSOGRef() throws Exception {

    // Because the value ({@ref:1}) is not scalar, parser thinks it is not an id 
    // and tries to deserialize as normal a new IdentifiableExampleJSOG 
    // then  complains about unrecognized field "@ref"
    IdentifiableExampleJSOG result = mapper.readValue(EXP_EXAMPLE_JSOG,
            IdentifiableExampleJSOG.class);

    assertEquals(66, result.foo);
    assertSame(result, result.next);
  }
}
