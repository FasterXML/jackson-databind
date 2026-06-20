package tools.jackson.databind.jref;

import static org.junit.Assert.assertEquals;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

import java.util.regex.Pattern;

import tools.jackson.databind.JRefModule;
import tools.jackson.databind.ObjectMapper;

public class JRefAbstractTest {

	protected ObjectMapper buildObjectMapperWithJRefSupport() {
		return jsonMapperBuilder().addModule(new JRefModule()).build();
	}

	protected ObjectMapper buildObjectMapperWithoutJRefSupport() {
		return jsonMapperBuilder().build();
	}

    static long countMatches(String text, String target) {
        if (text == null || target == null || target.isEmpty()) return 0;
        String quotedTarget = Pattern.quote(target); 
        
        return Pattern.compile(quotedTarget)
                      .matcher(text)
                      .results() 
                      .count();  
    }
    
	protected void assertJRefCount(String input, long expectedJRefs) {
		assertEquals(expectedJRefs, countMatches(input,"$ref"));
	}
	
	void trace(String method, String s) {
		System.out.println(method+"."+s);
	}
}
