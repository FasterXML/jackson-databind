package tools.jackson.databind.jref;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

import tools.jackson.databind.JRefModule;
import tools.jackson.databind.ObjectMapper;

public class JRefAbstractTest {

	protected ObjectMapper buildObjectMapperWithJRefSupport() {
		return jsonMapperBuilder().addModule(new JRefModule()).build();
	}

	protected ObjectMapper buildObjectMapperWithoutJRefSupport() {
		return jsonMapperBuilder().build();
	}

	void trace(String method, String s) {
		System.out.println(method+"."+s);
	}
}
