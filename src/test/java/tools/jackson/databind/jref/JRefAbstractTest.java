package tools.jackson.databind.jref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import tools.jackson.databind.JRefModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JRefAbstractTest {

	public static boolean TRACE = false;

	public static JsonMapper.Builder jsonMapperBuilder() {
		return JsonMapper.builder();
	}

	protected ObjectMapper buildObjectMapperJRef() {
		return jsonMapperBuilder().addModule(new JRefModule()).build();
	}

	protected ObjectMapper buildObjectMapperNoJRef() {
		return jsonMapperBuilder().build();
	}

	static long countMatches(String text, String target) {
		if (text == null || target == null || target.isEmpty())
			return 0;
		String quotedTarget = Pattern.quote(target);

		return Pattern.compile(quotedTarget).matcher(text).results().count();
	}

	protected void assertJRefCount(String input, long expectedJRefs) {
		assertEquals(expectedJRefs, countMatches(input, "$ref"));
	}

	void trace(String method, String s) {
		if (TRACE) {
			System.out.println(method + "." + s);
		}
	}
}
