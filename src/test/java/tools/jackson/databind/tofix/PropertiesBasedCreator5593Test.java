package tools.jackson.databind.tofix;

import java.io.*;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// https://github.com/FasterXML/jackson-databind/issues/5593
public class PropertiesBasedCreator5593Test
        extends DatabindTestUtil
{

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class AuditEvent implements Serializable
    {

        private final Instant timestamp;

        private final String principal;

        private final String type;

        private final Map<String, Object> data;

        public AuditEvent(String principal, String type, Map<String, Object> data) {
            this(Instant.now(), principal, type, data);
        }

        public AuditEvent(String principal, String type, String... data) {
            this(Instant.now(), principal, type, convert(data));
        }

        public AuditEvent(Instant timestamp, String principal,
                          String type, Map<String, Object> data) {
            this.timestamp = timestamp;
            this.principal = (principal != null) ? principal : "";
            this.type = type;
            this.data = Collections.unmodifiableMap(data);
        }


        private static Map<String, Object> convert(String[] data) {
            Map<String, Object> result = new HashMap<>();
            for (String entry : data) {
                int index = entry.indexOf('=');
                if (index != -1) {
                    result.put(entry.substring(0, index), entry.substring(index + 1));
                }
                else {
                    result.put(entry, null);
                }
            }
            return result;
        }
        public Instant getTimestamp() { return timestamp; }
        public String getPrincipal() { return principal; }
        public String getType() { return type; }
        public Map<String, Object> getData() { return data; }
    }


    /*
    /**********************************************************************
    /* Test
    /**********************************************************************
     */


    @JacksonTestFailureExpected
    @Test
    public void testMultipleCtorsWithUsePropertiesBasedDetector() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .annotationIntrospector(new ImplicitNameIntrospector())
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build();

        String json = a2q("{"
                + "'timestamp':'2026-01-17T21:00:44.532975Z',"
                + "'principal':'user',"
                + "'type':'type',"
                + "'data':{'keyA':'ValueA','keyB':'ValueB'}"
                + "}");

        AuditEvent result = mapper.readValue(json, AuditEvent.class);

        assertNotNull(result);
        assertEquals("user", result.getPrincipal());
        assertEquals("type", result.getType());
        assertNotNull(result.getTimestamp());
        assertEquals(2, result.getData().size());
        assertEquals("ValueA", result.getData().get("keyA"));
    }
}
