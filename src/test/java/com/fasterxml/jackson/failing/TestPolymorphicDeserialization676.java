package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reproduction of [https://github.com/FasterXML/jackson-databind/issues/676]
 * <p/>
 * Deserialization of class with generic collection inside
 * depends on how is was deserialized first time.
 */
public class TestPolymorphicDeserialization676 extends BaseMapTest {
	private static final int TIMESTAMP = 123456;
	private final MapContainer originMap;

	public TestPolymorphicDeserialization676() {
		Map<String, Object> localMap = new HashMap<String, Object>();
		localMap.put("DateValue", new Date(TIMESTAMP));
		originMap = new MapContainer(localMap);
	}

	/**
	 * If the class was first deserialized as polymorphic field,
	 * deserialization will fail at complex type.
	 */
	public void testDeSerFail() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		MapContainer deserMapBad = createDeSerMapContainer(originMap, mapper);

		// map is deserialized as list
		List<Object> list = Arrays.asList(new Object[] {"java.util.Date", TIMESTAMP});
		assertFalse(list.equals(deserMapBad.getMap().get("DateValue")));
		assertTrue(originMap.equals(deserMapBad));
		assertTrue(originMap.equals(mapper.readValue(mapper.writeValueAsString(originMap),
				MapContainer.class)));
	}

	/**
	 * If the class was first deserialized as is,
	 * deserialization will work correctly.
	 */
	public void testDeSerCorrect() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("1", 1);
		// commenting out the following statement will fail the test
		assertEquals(new MapContainer(map),
				mapper.readValue(
						mapper.writeValueAsString(new MapContainer(map)),
						MapContainer.class));

		MapContainer deserMapGood = createDeSerMapContainer(originMap, mapper);

		assertEquals(originMap, deserMapGood);
		assertEquals(new Date(TIMESTAMP), deserMapGood.getMap().get("DateValue"));

		assertEquals(originMap, mapper.readValue(mapper.writeValueAsString(originMap), MapContainer.class));
	}

	private static MapContainer createDeSerMapContainer(MapContainer originMap, ObjectMapper mapper) throws IOException {
		PolymorphicValueWrapper result = new PolymorphicValueWrapper();
		result.setValue(originMap);
		String json = mapper.writeValueAsString(result);
		assertEquals("{\"value\":{\"@class\":"
						+ "\"com.fasterxml.jackson.failing.TestPolymorphicDeserialization676$MapContainer\","
						+ "\"map\":{\"DateValue\":[\"java.util.Date\",123456]}}}",
				json);
		PolymorphicValueWrapper deserializedResult = mapper.readValue(json, PolymorphicValueWrapper.class);
		return (MapContainer) deserializedResult.getValue();
	}

	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	public static class MapContainer {
		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
				include = JsonTypeInfo.As.PROPERTY,
				property = "@class")
		private Map<String, Object> map;

		@SuppressWarnings("unused")
		public MapContainer() {
		}

		public MapContainer(Map<String, Object> map) {
			this.map = map;
		}

		public Map<String, Object> getMap() {
			return map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			MapContainer that = (MapContainer) o;

			return !(map != null ? !map.equals(that.map) : that.map != null);
		}

		@Override
		public int hashCode() {
			return map != null ? map.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "MapContainer{" +
					"map=" + map +
					'}';
		}
	}

	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	public static class PolymorphicValueWrapper {
		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
				include = JsonTypeInfo.As.PROPERTY,
				property = "@class")
		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
	}
}
