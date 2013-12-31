package com.fasterxml.jackson.databind.module;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import com.fasterxml.jackson.test.BaseTest;

import java.lang.reflect.Type;

public class TestTypeModifierNameResolution extends BaseTest {

	interface MyType {
		String getData();
		void setData(String data);
	}

	static class MyTypeImpl implements MyType {
		private String data;

		@Override
		public String getData() {
		    return data;
		}

		@Override
		public void setData(String data) {
		    this.data = data;
		}
	}

	static class CustomTypeModifier extends TypeModifier {
		@Override
		public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory) {
			if (type.getRawClass().equals(MyTypeImpl.class)) {
				return typeFactory.constructType(MyType.class);
			}
			return type;
		}
	}

	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
	public interface Mixin { }

	// Expect that the TypeModifier kicks in when the type id is written.
	public void testTypeModiferNameResolution() throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.setTypeFactory(mapper.getTypeFactory().withModifier(new CustomTypeModifier()));
		mapper.addMixInAnnotations(MyType.class, Mixin.class);

		MyType obj = new MyTypeImpl();
		obj.setData("something");

		String s = mapper.writer().writeValueAsString(obj);
		assertTrue(s.startsWith("{\"TestTypeModifierNameResolution$MyType\":"));
	}
}