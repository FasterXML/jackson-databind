package tools.jackson.databind.jref;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

public class JRefArrayTest extends JRefAbstractTest {

	@Test
	void testObjectArrayRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		Object o1 = new Object();
		Object o2 = o1;
		Object[] arr = new Object[] { o1, o2 };
		String out = mapper.writeValueAsString(arr);
		assertJRefCount(out, 1);
		trace("testObjectArrayRef jrefserialized=",out);
		Object[] oa = mapper.readValue(out, Object[].class);
		// The first and second instance should be both be Maps
		// as Jackson creates Maps from non-types Objects
		assertTrue(oa[0] instanceof Map);
		assertTrue(oa[1] instanceof Map);
		assertEquals(oa[0],oa[1]);
	}
	
	@Test
	void testStringArrayRef() throws Exception {
		ObjectMapper mapper = buildObjectMapperWithJRefSupport();
		String o1 = new String("one");
		String o2 = o1;
		String[] arr = new String[] { o1, o2 };
		String out = mapper.writeValueAsString(arr);
		assertJRefCount(out, 1);
		trace("testStringArrayRef jrefserialized=",out);
		String[] oa = mapper.readValue(out, String[].class);
		// The first and second instance should be both be Maps
		assertTrue(oa[0] instanceof String);
		assertTrue(oa[1] instanceof String);
		assertEquals(oa[0],oa[1]);
	}

}
