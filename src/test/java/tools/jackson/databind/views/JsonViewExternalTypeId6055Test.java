package tools.jackson.databind.views;

import com.fasterxml.jackson.annotation.*;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#6055] A property with an EXTERNAL_PROPERTY type id that is filtered
// out by an active JsonView should be skipped gracefully (left null), the same
// way ordinary view-filtered properties are -- the dangling external type id
// ("kind") must not trigger a "Missing property" MismatchedInputException.
public class JsonViewExternalTypeId6055Test extends DatabindTestUtil {
    public static class PublicView {}
    public static class AdminView extends PublicView {}

    public static abstract class Asset {
        @JsonView(AdminView.class)
        public String name;
    }
    public static class PublicAsset extends Asset {}
    public static class AdminAsset extends Asset {
        @JsonView(AdminView.class)
        public String secret;
    }

    public static class Container {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = PublicAsset.class, name = "pub"),
                @JsonSubTypes.Type(value = AdminAsset.class,  name = "admin")
        })
        @JsonView(AdminView.class)
        public Asset asset;

        @JsonView(PublicView.class)
        public String label;

        @JsonCreator
        public Container(
                @JsonProperty("label") @JsonView(PublicView.class) String label,
                @JsonProperty("asset") @JsonView(AdminView.class) Asset asset) {
            this.label = label;
            this.asset = asset;
        }
    }

    public static class Wrapper {
        @JsonView(PublicView.class)
        public Container data;
    }

    // Default-constructor variant, to exercise the bean-based (non-creator)
    // external-type-id path in addition to the property-based-creator one above.
    public static class BeanContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = PublicAsset.class, name = "pub"),
                @JsonSubTypes.Type(value = AdminAsset.class,  name = "admin")
        })
        @JsonView(AdminView.class)
        public Asset asset;

        @JsonView(PublicView.class)
        public String label;
    }

    public static class BeanWrapper {
        @JsonView(PublicView.class)
        public BeanContainer data;
    }

    private final ObjectMapper MAPPER = sharedMapper();

    // Type id ("kind") appears BEFORE the value ("asset"): exercises the eager-bind
    // branch (both type id and value available when value token arrives).
    private static final String JSON_TYPEID_FIRST = a2q(
            "{'data':{'label':'hello','kind':'admin',"
            + "'asset':{'name':'foo','secret':'LEAKED'}}}");

    // Type id ("kind") appears AFTER the value ("asset"): value is buffered first,
    // then the type id arrives -- exercises the other ordering of the same paths.
    private static final String JSON_VALUE_FIRST = a2q(
            "{'data':{'label':'hello',"
            + "'asset':{'name':'foo','secret':'LEAKED'},'kind':'admin'}}");

    // Admin-only "asset" is annotated @JsonView(AdminView.class); AdminView extends
    // PublicView, so the property is NOT visible under PublicView and must be
    // skipped (left null) -- including its external type id -- rather than read.
    @Test
    void externalTypeIdFilteredByPublicView() throws Exception {
        Wrapper r = MAPPER.readerWithView(PublicView.class)
                .forType(Wrapper.class)
                .readValue(JSON_TYPEID_FIRST);

        assertEquals("hello", r.data.label);
        // Admin-only "asset" must not be populated when reading with PublicView
        assertNull(r.data.asset);
    }

    // As above, but with the type id appearing after the value in the input.
    @Test
    void externalTypeIdFilteredByPublicView_valueBeforeTypeId() throws Exception {
        Wrapper r = MAPPER.readerWithView(PublicView.class)
                .forType(Wrapper.class)
                .readValue(JSON_VALUE_FIRST);

        assertEquals("hello", r.data.label);
        assertNull(r.data.asset);
    }

    // Sanity check: with AdminView the property IS visible and read normally.
    @Test
    void externalTypeIdVisibleForAdminView() throws Exception {
        Wrapper r = MAPPER.readerWithView(AdminView.class)
                .forType(Wrapper.class)
                .readValue(JSON_TYPEID_FIRST);

        assertEquals("hello", r.data.label);
        AdminAsset asset = (AdminAsset) r.data.asset;
        assertEquals("foo", asset.name);
        assertEquals("LEAKED", asset.secret);
    }

    // Same as above but for the bean-based (default-constructor) path.
    @Test
    void externalTypeIdFilteredByPublicView_beanBased() throws Exception {
        BeanWrapper r = MAPPER.readerWithView(PublicView.class)
                .forType(BeanWrapper.class)
                .readValue(JSON_TYPEID_FIRST);

        assertEquals("hello", r.data.label);
        assertNull(r.data.asset);
    }

    // Bean-based path with the type id appearing after the value.
    @Test
    void externalTypeIdFilteredByPublicView_beanBased_valueBeforeTypeId() throws Exception {
        BeanWrapper r = MAPPER.readerWithView(PublicView.class)
                .forType(BeanWrapper.class)
                .readValue(JSON_VALUE_FIRST);

        assertEquals("hello", r.data.label);
        assertNull(r.data.asset);
    }

    @Test
    void externalTypeIdVisibleForAdminView_beanBased() throws Exception {
        BeanWrapper r = MAPPER.readerWithView(AdminView.class)
                .forType(BeanWrapper.class)
                .readValue(JSON_TYPEID_FIRST);

        assertEquals("hello", r.data.label);
        AdminAsset asset = (AdminAsset) r.data.asset;
        assertEquals("foo", asset.name);
        assertEquals("LEAKED", asset.secret);
    }

    // Bean-based, AdminView, value-before-type-id: confirms the eager-bind path
    // still binds the value correctly when the property IS visible.
    @Test
    void externalTypeIdVisibleForAdminView_beanBased_valueBeforeTypeId() throws Exception {
        BeanWrapper r = MAPPER.readerWithView(AdminView.class)
                .forType(BeanWrapper.class)
                .readValue(JSON_VALUE_FIRST);

        assertEquals("hello", r.data.label);
        AdminAsset asset = (AdminAsset) r.data.asset;
        assertEquals("foo", asset.name);
        assertEquals("LEAKED", asset.secret);
    }

    // When a view-filtered external property is skipped, its value and its type id
    // must be fully consumed -- not left dangling as "unknown properties". Verify by
    // reading with FAIL_ON_UNKNOWN_PROPERTIES enabled: a leak would throw here.
    @Test
    void filteredExternalTypeIdIsFullyConsumed() throws Exception {
        ObjectMapper strict = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        for (String json : new String[] { JSON_TYPEID_FIRST, JSON_VALUE_FIRST }) {
            // creator-based
            Wrapper w = strict.readerWithView(PublicView.class)
                    .forType(Wrapper.class)
                    .readValue(json);
            assertEquals("hello", w.data.label);
            assertNull(w.data.asset);

            // bean-based
            BeanWrapper bw = strict.readerWithView(PublicView.class)
                    .forType(BeanWrapper.class)
                    .readValue(json);
            assertEquals("hello", bw.data.label);
            assertNull(bw.data.asset);
        }
    }
}
