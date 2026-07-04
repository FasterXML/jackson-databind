package tools.jackson.databind.views;

import com.fasterxml.jackson.annotation.*;

import org.junit.jupiter.api.Test;

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

    // Admin-only "asset" is annotated @JsonView(AdminView.class); AdminView extends
    // PublicView, so the property is NOT visible under PublicView and must be
    // skipped (left null) -- including its external type id -- rather than read.
    @Test
    void externalTypeIdFilteredByPublicView() throws Exception {
        String json = a2q("{'data':{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}}");

        Wrapper r = MAPPER.readerWithView(PublicView.class)
                .forType(Wrapper.class)
                .readValue(json);

        assertEquals("hello", r.data.label);
        // Admin-only "asset" must not be populated when reading with PublicView
        assertNull(r.data.asset);
    }

    // Sanity check: with AdminView the property IS visible and read normally.
    @Test
    void externalTypeIdVisibleForAdminView() throws Exception {
        String json = a2q("{'data':{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}}");

        Wrapper r = MAPPER.readerWithView(AdminView.class)
                .forType(Wrapper.class)
                .readValue(json);

        assertEquals("hello", r.data.label);
        AdminAsset asset = (AdminAsset) r.data.asset;
        assertEquals("foo", asset.name);
        assertEquals("LEAKED", asset.secret);
    }

    // Same as above but for the bean-based (default-constructor) path.
    @Test
    void externalTypeIdFilteredByPublicView_beanBased() throws Exception {
        String json = a2q("{'data':{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}}");

        BeanWrapper r = MAPPER.readerWithView(PublicView.class)
                .forType(BeanWrapper.class)
                .readValue(json);

        assertEquals("hello", r.data.label);
        assertNull(r.data.asset);
    }

    @Test
    void externalTypeIdVisibleForAdminView_beanBased() throws Exception {
        String json = a2q("{'data':{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}}");

        BeanWrapper r = MAPPER.readerWithView(AdminView.class)
                .forType(BeanWrapper.class)
                .readValue(json);

        assertEquals("hello", r.data.label);
        AdminAsset asset = (AdminAsset) r.data.asset;
        assertEquals("foo", asset.name);
        assertEquals("LEAKED", asset.secret);
    }
}
