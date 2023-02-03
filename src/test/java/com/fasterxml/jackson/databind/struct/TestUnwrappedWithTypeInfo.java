package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

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

	// [databind#81]
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
	    } catch (DatabindException ex) {
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

		ObjectMapper mapper = jsonMapperBuilder()
		        .disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
		        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
		        .build();

		String json = mapper.writeValueAsString(outer);

		assertEquals("{\"@type\":\"OuterType\",\"p2\":\"202\",\"p1\":\"101\"}", json);
	}
}