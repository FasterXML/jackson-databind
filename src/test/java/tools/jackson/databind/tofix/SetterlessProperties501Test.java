package tools.jackson.databind.tofix;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

class SetterlessProperties501Test extends DatabindTestUtil {
    static class Poly {
        public int id;

        public Poly(int id) {
            this.id = id;
        }

        protected Poly() {
            this(0);
        }
    }

    static class Issue501Bean {
        protected Map<String, Poly> m = new HashMap<String, Poly>();
        protected List<Poly> l = new ArrayList<Poly>();

        protected Issue501Bean() {
        }

        public Issue501Bean(String key, Poly value) {
            m.put(key, value);
            l.add(value);
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public List<Poly> getList() {
            return l;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public Map<String, Poly> getMap() {
            return m;
        }

//        public void setMap(Map<String,Poly> m) { this.m = m; }
//        public void setList(List<Poly> l) { this.l = l; }
    }

    // For [databind#501]
    @JacksonTestFailureExpected
    @Test
    void setterlessWithPolymorphic() throws Exception {
        Issue501Bean input = new Issue501Bean("a", new Poly(13));
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL)
                .build();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        Issue501Bean output = mapper.readValue(json, Issue501Bean.class);
        assertNotNull(output);

        assertEquals(1, output.l.size());
        assertEquals(1, output.m.size());

        assertEquals(13, output.l.get(0).id);
        Poly p = output.m.get("a");
        assertNotNull(p);
        assertEquals(13, p.id);
    }
}
