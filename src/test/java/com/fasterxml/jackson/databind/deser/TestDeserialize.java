package com.fasterxml.jackson.databind.deser;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.beans.ConstructorProperties;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDeserialize {
	private ObjectMapper objectMapper;

	@Before
	public void setUp(){
		this.objectMapper = new ObjectMapper();
	}

	@Test
	public void deserialize() throws IOException {
		String json = "{\"testEnum\":\"\",\"name\":\"changyong\"}";

		Person person = this.objectMapper.readValue(json, Person.class);

		assertThat(person.getTestEnum(), is(nullValue()));
		assertThat(person.getName(), is("changyong"));
	}

	public static class Person{
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		private TestEnum testEnum;
		private String name;

		public Person() {
		}

		@ConstructorProperties({"testEnum", "name"})
		public Person(TestEnum testEnum, String name) {
			this.testEnum = testEnum;
			this.name = name;
		}

		public TestEnum getTestEnum() {
			return testEnum;
		}

		public void setTestEnum(TestEnum testEnum) {
			this.testEnum = testEnum;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	enum TestEnum{
		TEST
	}
}
