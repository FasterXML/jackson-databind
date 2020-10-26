package com.fasterxml.jackson.databind.jsontype;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MapSerializingTest {
	class Inner {
		@JsonKey
		String key;

		@JsonValue
		String value;

		Inner(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String toString() {
			return "Inner(" + this.key + "," + this.value + ")";
		}

	}

	class Outer {
		@JsonKey
		@JsonValue
		Inner inner;

		Outer(Inner inner) {
			this.inner = inner;
		}

	}

	class NoKeyOuter {
		@JsonValue
		Inner inner;

		NoKeyOuter(Inner inner) {
			this.inner = inner;
		}
	}
	
	@Test
	public void testClassAsKey() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Outer outer = new Outer(new Inner("innerKey", "innerValue"));
		Map<Outer, String> map = Collections.singletonMap(outer, "value");
		String actual = mapper.writeValueAsString(map);
		Assert.assertEquals("{\"innerKey\":\"value\"}", actual);
	}

	@Test
	public void testClassAsValue() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Outer> mapA = Collections.singletonMap("key", new Outer(new Inner("innerKey", "innerValue")));
		String actual = mapper.writeValueAsString(mapA);
		Assert.assertEquals("{\"key\":\"innerValue\"}", actual);
	}

	@Test
	public void testNoKeyOuter() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, NoKeyOuter> mapA = Collections.singletonMap("key", new NoKeyOuter(new Inner("innerKey", "innerValue")));
		String actual = mapper.writeValueAsString(mapA);
		Assert.assertEquals("{\"key\":\"innerValue\"}", actual);
	}
}
