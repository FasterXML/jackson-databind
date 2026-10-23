package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5962]: Case-insensitive BeanPropertyMap rebuild undoes per-property
 * {@code @JsonIgnoreProperties}.
 *
 * {@code BeanDeserializerBase.createContextual()} calls {@code _handleByNameInclusion()}
 * to filter properties according to per-property {@code @JsonIgnoreProperties}, producing
 * a contextual deserializer with the restricted {@code BeanPropertyMap}. However, the
 * subsequent case-insensitivity block read {@code _beanProperties} (the *original*
 * unfiltered map from {@code this}) rather than {@code contextual._beanProperties} (the
 * filtered map). {@code withCaseInsensitivity()} then rebuilt the map from the unfiltered
 * source, and {@code contextual.withBeanProperties(props)} overwrote the filtered map with
 * the unfiltered one — any properties removed by {@code _handleByNameInclusion} were
 * restored.
 *
 * Patch: source the case-insensitive rebuild from {@code contextual._beanProperties}.
 */
public class IgnorePropertiesCaseInsensitive5962Test extends DatabindTestUtil
{
    static class AdminDto {
        public String adminKey = "DEFAULT";
        public String username;
    }

    // Container that ignores "adminKey" on the AdminDto field AND enables case-insensitive matching
    static class Container {
        @JsonIgnoreProperties("adminKey")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        public AdminDto admin;
    }

    // Baseline container: only @JsonIgnoreProperties, no case-insensitive format override
    static class BaselineContainer {
        @JsonIgnoreProperties("adminKey")
        public AdminDto admin;
    }

    /**
     * NEGATIVE CONTROL: without the @JsonFormat case-insensitive override, @JsonIgnoreProperties
     * correctly suppresses adminKey on the nested AdminDto field.
     */
    @Test
    public void test5962_negativeControl_withoutCaseInsensitivity() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().build();
        String json = "{\"admin\":{\"adminKey\":\"HACKED\",\"username\":\"alice\"}}";
        BaselineContainer result = mapper.readValue(json, BaselineContainer.class);
        // Without case-insensitive format, @JsonIgnoreProperties blocks adminKey
        assertNotEquals("HACKED", result.admin.adminKey,
                "@JsonIgnoreProperties alone (no case-insensitive format) should block adminKey");
        assertEquals("alice", result.admin.username);
    }

    /**
     * EXPLOIT PATH: the case-insensitive BeanPropertyMap rebuild (triggered by
     * @JsonFormat ACCEPT_CASE_INSENSITIVE_PROPERTIES) restores the unfiltered original
     * _beanProperties, undoing the @JsonIgnoreProperties("adminKey") exclusion.
     * Case-insensitive matching then routes "adminKey" (or "ADMINKEY") to the setter.
     *
     * Security assertion: adminKey must NOT be settable via JSON when the enclosing
     * container declares @JsonIgnoreProperties("adminKey") on the field.
     */
    @Test
    public void test5962_caseInsensitiveRebuildRestoresIgnoredProperty() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().build();

        // Exact case — should be blocked by @JsonIgnoreProperties
        String json = "{\"admin\":{\"adminKey\":\"HACKED\",\"username\":\"alice\"}}";
        Container result = mapper.readValue(json, Container.class);
        assertNotEquals("HACKED", result.admin.adminKey,
                "[databind#5962]: case-insensitive BeanPropertyMap rebuild restored 'adminKey' " +
                "after it was removed by @JsonIgnoreProperties. The property was set to 'HACKED'.");
        assertEquals("alice", result.admin.username);

        // Mixed case — exploits the case-insensitive rebuild more directly
        String jsonMixed = "{\"admin\":{\"AdminKey\":\"HACKED2\",\"username\":\"bob\"}}";
        Container result2 = mapper.readValue(jsonMixed, Container.class);
        assertNotEquals("HACKED2", result2.admin.adminKey,
                "[databind#5962]: 'AdminKey' (mixed case) matched 'adminKey' via case-insensitive " +
                "lookup that was rebuilt from the unfiltered property map.");
    }
}
