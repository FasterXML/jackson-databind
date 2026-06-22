package tools.jackson.databind.jref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class JRefMapTest extends JRefAbstractTest {

	@Test
	public void testMapKeyNoRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		String v1 = new String("val1");
		String k2 = new String("second");
		String v2 = new String("first");
		Map<String, String> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 0);
		trace("testMapKeyNoRef jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapKeyRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		String v1 = new String("val1");
		String k2 = new String("second");
		// second value refs first key, but serialization treats
		// it like separate string, since map keys cannot be jrefs
		String v2 = k1;
		Map<String, String> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		// Because the reference is a key, it should not be serialized to jref
		// so count is expected to be 0
		assertJRefCount(out, 0);
		trace("testMapKeyRef jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapValueNoRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		String v1 = new String("val1");
		String k2 = new String("second");
		String v2 = new String("val1");
		Map<String, String> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 1);
		trace("testMapValueNoRef jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapValueRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		String v1 = new String("val1");
		String k2 = new String("second");
		// second value refs first
		String v2 = v1;
		Map<String, String> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 1);
		trace("testMapValueRef jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapValueMultRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		String v1 = new String("val1");
		String k2 = new String("second");
		// second value refs first
		String v2 = v1;
		String k3 = "third";
		// third value refs first
		String v3 = v1;
		String k4 = "fourth";
		String v4 = v1;
		Map<String, String> mi = Map.of(k1, v1, k2, v2, k3, v3, k4, v4);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 3);
		trace("testMapValueMultRef jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapKeyNoRefNoValueType() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		Object v1 = new String("val1");
		String k2 = new String("second");
		Object v2 = new String("first");
		Map<String, Object> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 0);
		trace("testMapKeyNoRefNoValueType jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapKeyRefNoValueType() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		Object v1 = new String("val1");
		String k2 = new String("second");
		// second value refs first key, but serialization treats
		// it like separate string, since map keys cannot be jrefs
		Object v2 = k1;
		Map<String, Object> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		// Because the reference is a key, it should not be serialized to jref
		// so count is expected to be 0
		assertJRefCount(out, 0);
		trace("testMapKeyRefNoValueType jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapValueNoRefNoValueType() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		Object v1 = new String("val1");
		String k2 = new String("second");
		Object v2 = new String("val1");
		Map<String, Object> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 1);
		trace("testMapValueNoRefNoValueType jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapValueRefNoValueType() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		Object v1 = new String("val1");
		String k2 = new String("second");
		// second value refs first
		Object v2 = v1;
		Map<String, Object> mi = Map.of(k1, v1, k2, v2);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 1);
		trace("testMapValueRefNoValueType jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

	@Test
	public void testMapValueMultRefNoValueType() throws Exception {
		ObjectMapper mapper = buildObjectMapperJRef();
		String k1 = new String("first");
		Object v1 = new String("val1");
		String k2 = new String("second");
		// second value refs first
		Object v2 = v1;
		String k3 = "third";
		// third value refs first
		Object v3 = v1;
		String k4 = "fourth";
		Object v4 = v1;
		Map<String, Object> mi = Map.of(k1, v1, k2, v2, k3, v3, k4, v4);
		String out = mapper.writeValueAsString(mi);
		assertJRefCount(out, 3);
		trace("testMapValueMultRefNoValueType jrefserialized=", out);
		Map<?, ?> mo = mapper.readValue(out, Map.class);
		assertEquals(mi, mo);
	}

}
