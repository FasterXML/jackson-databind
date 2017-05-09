package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

// Tests for [#1308]
public class TestCollectionsWithTypeInfo extends BaseMapTest
{

	@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "@type")
	static class Inner {

		private String p2;
		public String getP2() { return p2; }
		public void setP2(String p2) { this.p2 = p2; }

	}

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

	// [Issue#1308]
	public void testUnwrappedWithTypeInfoAndFeatureDisabled() throws Exception
	{
		List<Inner> list = new ArrayList<>();
		Inner inner = new Inner();
		inner.setP2("asdf");
		list.add(inner);

		ObjectMapper mapper = new ObjectMapper();

		String json = mapper.writeValueAsString(list);
		assertEquals("[{\"@type\":\"TestCollectionsWithTypeInfo$Inner\",\"p2\":\"asdf\"}]", json);
	}
}