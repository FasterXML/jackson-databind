package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// Tests for [#81]
public class TestUnwrappedWithTypeInfo extends BaseMapTest
{

	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type")
	@JsonTypeName("OuterType")
	static class Outer {

		private @JsonProperty String p1;
		public String getP1() { return p1; }
		public void setP1(String p1) { this.p1 = p1; }


		private Inner inner;
		public void setInner(Inner inner) { this.inner = inner; }

		@JsonUnwrapped
		public Inner getInner() {
			return inner;
		}
	}

	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type")
	@JsonTypeName("InnerType")
	static class Inner {

		private @JsonProperty String p2;
		public String getP2() { return p2; }
		public void setP2(String p2) { this.p2 = p2; }

	}
    
    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

	// [Issue#81]
	public void testDefaultUnwrappedWithTypeInfo() throws Exception
	{
	    Outer outer = new Outer();
	    outer.setP1("101");

	    Inner inner = new Inner();
	    inner.setP2("202");
	    outer.setInner(inner);

	    ObjectMapper mapper = new ObjectMapper();

	    try {
	        mapper.writeValueAsString(outer);
	         fail("Expected exception to be thrown.");
	    } catch (JsonMappingException ex) {
	        verifyException(ex, "requires use of type information");
	    }
	}

	public void testUnwrappedWithTypeInfoAndFeatureDisabled() throws Exception
	{
		Outer outer = new Outer();
		outer.setP1("101");

		Inner inner = new Inner();
		inner.setP2("202");
		outer.setInner(inner);

		ObjectMapper mapper = new ObjectMapper();
		mapper = mapper.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);

		String json = mapper.writeValueAsString(outer);
		assertEquals("{\"@type\":\"OuterType\",\"p1\":\"101\",\"p2\":\"202\"}", json);
	}
}