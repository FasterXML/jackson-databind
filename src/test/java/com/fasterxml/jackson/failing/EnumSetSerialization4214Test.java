package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.EnumSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

public class EnumSetSerialization4214Test extends BaseMapTest {

	static enum MyEnum {
		ITEM_A, ITEM_B;
	}

	static class EnumSetHolder {
		private Set<MyEnum> enumSet; // use Set instead of EnumSet for type of this

		public Set<MyEnum> getEnumSet() {
			return enumSet;
		}

		public void setEnumSet(Set<MyEnum> enumSet) {
			this.enumSet = enumSet;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof EnumSetHolder)) {
				return false;
			}
			EnumSetHolder eh = (EnumSetHolder) o;
			if (eh == this) {
				return true;
			}
			if (enumSet == null) {
				if (eh.getEnumSet() == null) {
					return true;
				} else {
					return false;
				}
			} else {
				if (eh.getEnumSet() == null) {
					return false;
				}
				return enumSet.containsAll(eh.getEnumSet());
			}
		}
	}

	public void testSerialization() throws Exception {
		ObjectMapper mapper = jsonMapperBuilder().build()
				// this type information is needed in json string, for this test
				.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
						DefaultTyping.EVERYTHING);

		EnumSetHolder enumSetHolder = new EnumSetHolder();
		enumSetHolder.setEnumSet(EnumSet.allOf(MyEnum.class));
		String jsonStr = mapper.writeValueAsString(enumSetHolder);
		EnumSetHolder result = mapper.readValue(jsonStr, EnumSetHolder.class);
		assertEquals(result, enumSetHolder);
	}
}
