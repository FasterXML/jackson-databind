package com.fasterxml.jackson.databind.deser;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.beans.ConstructorProperties;
import java.io.IOException;

import org.hamcrest.MatcherAssert;
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
	public void deserialize_annotations_one_field() throws IOException {
		String json = "{\"testEnum\":\"\"}";

		PersonAnnotations person = this.objectMapper.readValue(json, PersonAnnotations.class);

		MatcherAssert.assertThat(person.getTestEnum(), is(nullValue()));
	}

	@Test
	public void deserialize_annotations_two_fields() throws IOException {
		String json = "{\"testEnum\":\"\",\"name\":\"changyong\"}";

		PersonAnnotations person = this.objectMapper.readValue(json, PersonAnnotations.class);

		MatcherAssert.assertThat(person.getTestEnum(), is(nullValue()));
		MatcherAssert.assertThat(person.getName(), is("changyong"));
	}

	@Test
	public void deserialize_one_field() throws IOException {
		String json = "{\"testEnum\":\"\"}";

		Person person = this.objectMapper.readValue(json, Person.class);

		MatcherAssert.assertThat(person.getTestEnum(), is(nullValue()));
	}

	@Test
	public void deserialize_two_fields() throws IOException {
		String json = "{\"testEnum\":\"\",\"name\":\"changyong\"}";

		Person person = this.objectMapper.readValue(json, Person.class);

		MatcherAssert.assertThat(person.getTestEnum(), is(nullValue()));
		MatcherAssert.assertThat(person.getName(), is("changyong"));
	}

	public static class PersonAnnotations {
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		private TestEnum testEnum;
		private String name;

		public PersonAnnotations() {
		}

		@ConstructorProperties({"testEnum", "name"})
		public PersonAnnotations(TestEnum testEnum, String name) {
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

	public static class Person {
		@JsonProperty(access = JsonProperty.Access.READ_ONLY)
		private TestEnum testEnum;
		private String name;

		public Person() {
		}

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
