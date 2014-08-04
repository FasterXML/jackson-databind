package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.util.List;

/**
 * Test class of {@link JsonMappingException} of collection and array deserializer.
 */
public class TestJsonMappingException extends BaseMapTest {
	/*
	 * /**********************************************************************
	 * /* Helper classes, beans
	 * /**********************************************************************
	 */

	@SuppressWarnings("unused")
	private static class ItemBean {
		private int value;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

	@SuppressWarnings("unused")
	private static class IterableBean {
		private List<ItemBean> collection;
		private ItemBean[] array;

		public List<ItemBean> getCollection() {
			return collection;
		}

		public void setCollection(List<ItemBean> collection) {
			this.collection = collection;
		}

		public ItemBean[] getArray() {
			return array;
		}

		public void setArray(ItemBean[] array) {
			this.array = array;
		}
	}

	/*
	 * /**********************************************************************
	 * /* Unit tests
	 * /**********************************************************************
	 */
	public void testJsonMappingExceptionOnCollectionProperty()
			throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.readValue("{\"collection\":[{\"value\":\"A\"}]}",
					IterableBean.class);
			fail("Expecting a " + JsonMappingException.class);
		} catch (final JsonMappingException e) {
			assertEquals(3, e.getPath().size());
			
			// Base
			assertEquals("collection", e.getPath().get(0).getFieldName());
			assertEquals(-1, e.getPath().get(0).getIndex());
			
			// Index
			assertNull(e.getPath().get(1).getFieldName());
			assertEquals(0, e.getPath().get(1).getIndex());
			
			// Sub property
			assertEquals("value", e.getPath().get(2).getFieldName());
			assertEquals(-1, e.getPath().get(2).getIndex());
		}
	}

	public void testJsonMappingExceptionOnArrayProperty() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.readValue("{\"array\":[{\"value\":\"A\"}]}",
					IterableBean.class);
			fail("Expecting a " + JsonMappingException.class);
		} catch (final JsonMappingException e) {
			assertEquals(3, e.getPath().size());
			
			// Base
			assertEquals("array", e.getPath().get(0).getFieldName());
			assertEquals(-1, e.getPath().get(0).getIndex());
			
			// Index
			assertNull(e.getPath().get(1).getFieldName());
			assertEquals(0, e.getPath().get(1).getIndex());
			
			// Sub property
			assertEquals("value", e.getPath().get(2).getFieldName());
			assertEquals(-1, e.getPath().get(2).getIndex());
		}
	}

}
