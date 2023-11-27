package com.fasterxml.jackson.failing;

import java.util.EnumSet;
import java.util.Set;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.google.common.base.Objects;

// For [databind#4214]
public class EnumSetSerialization4214Test extends BaseMapTest
{
	static enum MyEnum {
		ITEM_A, ITEM_B;
	}

	static class EnumSetHolder {
		public Set<MyEnum> enumSet; // use Set instead of EnumSet for type of this

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof EnumSetHolder)) {
				return false;
			}
			EnumSetHolder eh = (EnumSetHolder) o;
			return Objects.equal(enumSet, eh.enumSet);
		}
	}

	public void testSerialization() throws Exception
	{
		ObjectMapper mapper = jsonMapperBuilder().build()
				// this type information is needed in json string, for this test
				.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
						DefaultTyping.NON_FINAL_AND_ENUMS);

		EnumSetHolder enumSetHolder = new EnumSetHolder();
		enumSetHolder.enumSet = EnumSet.allOf(MyEnum.class);
		String json = mapper.writeValueAsString(enumSetHolder);

		//System.err.println("JSON = \n"+json);

		EnumSetHolder result = mapper.readValue(json, EnumSetHolder.class);
		assertEquals(result, enumSetHolder);
	}
}
