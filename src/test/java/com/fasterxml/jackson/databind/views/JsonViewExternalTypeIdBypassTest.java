package com.fasterxml.jackson.databind.views;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

// Tests to verify that {@code @JsonView} is honored for polymorphic properties
// that use an EXTERNAL_PROPERTY type id AND are bound via a property-based
// (@JsonCreator) constructor. A {@code @JsonView} never changes the type id, so
// these tests check the two distinct things a view CAN do:
//
//  (1) hide the whole polymorphic property -> property left null
//  (2) hide a field of the resolved subtype -> subtype built, field left null
public class JsonViewExternalTypeIdBypassTest extends DatabindTestUtil
{
    static class PublicView { }
    static class AdminView extends PublicView { }

    static abstract class Asset {
        public String name;
    }

    static class PublicAsset extends Asset { }

    static class AdminAsset extends Asset {
        public String secret;
    }

    // Subtype used by case (2): here the admin-only marker is on the field, not
    // on the owning property.
    static class FieldGatedAdminAsset extends Asset {
        @JsonView(AdminView.class)
        public String secret;
    }

    // Case (1): the entire polymorphic property is admin-only.
    static class PropertyGatedContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = PublicAsset.class, name = "pub"),
                @JsonSubTypes.Type(value = AdminAsset.class, name = "admin")
        })
        @JsonView(AdminView.class)
        public Asset asset;

        public String label;

        @JsonCreator
        public PropertyGatedContainer(
                @JsonProperty("label") String label,
                @JsonProperty("asset") @JsonView(AdminView.class) Asset asset) {
            this.label = label;
            this.asset = asset;
        }
    }

    // Case (2): the property is always visible, but the resolved subtype has an
    // admin-only field.
    static class FieldGatedContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = PublicAsset.class, name = "pub"),
                @JsonSubTypes.Type(value = FieldGatedAdminAsset.class, name = "admin")
        })
        public Asset asset;

        public String label;

        @JsonCreator
        public FieldGatedContainer(
                @JsonProperty("label") String label,
                @JsonProperty("asset") Asset asset) {
            this.label = label;
            this.asset = asset;
        }
    }

    // Case (3): like case (1), but the external type id "kind" is ITSELF a creator
    // parameter ([databind#999]). The value "asset" is admin-only, but "kind" carries
    // no view, so hiding the value must not also drop the (visible) type id property.
    static class TypeIdCreatorContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind",
                visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = PublicAsset.class, name = "pub"),
                @JsonSubTypes.Type(value = AdminAsset.class, name = "admin")
        })
        @JsonView(AdminView.class)
        public Asset asset;

        public String label;
        public String kind;

        @JsonCreator
        public TypeIdCreatorContainer(
                @JsonProperty("label") String label,
                @JsonProperty("kind") String kind,
                @JsonProperty("asset") @JsonView(AdminView.class) Asset asset) {
            this.label = label;
            this.kind = kind;
            this.asset = asset;
        }
    }

    // Case (4): admin-only external-type property on a default-constructor (no
    // @JsonCreator) bean -- exercises the other ExternalTypeHandler.complete() path.
    static class DefaultCtorContainer {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = PublicAsset.class, name = "pub"),
                @JsonSubTypes.Type(value = AdminAsset.class, name = "admin")
        })
        @JsonView(AdminView.class)
        public Asset asset;

        public String label;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Case (1): admin-only polymorphic property must NOT be bound when reading
    // with the less-privileged PublicView. A view never rewrites the type id, so
    // the only correct outcome is that "asset" is skipped entirely (left null).
    @Test
    void testViewGatedExternalTypeProperty() throws Exception
    {
        String json = a2q("{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}");

        PropertyGatedContainer result = MAPPER.readerWithView(PublicView.class)
                .forType(PropertyGatedContainer.class)
                .readValue(json);

        assertEquals("hello", result.label);
        // Admin-only property hidden from PublicView -> not bound at all
        assertNull(result.asset, "Admin-only 'asset' must not be bound under PublicView");
    }

    // Sanity check: with the AdminView active, the same property IS bound, and
    // resolves to AdminAsset (the type id is "admin").
    @Test
    void testViewGatedExternalTypePropertyVisibleForAdmin() throws Exception
    {
        String json = a2q("{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'shh'}}");

        PropertyGatedContainer result = MAPPER.readerWithView(AdminView.class)
                .forType(PropertyGatedContainer.class)
                .readValue(json);

        AdminAsset asset = assertInstanceOf(AdminAsset.class, result.asset);
        assertEquals("foo", asset.name);
        assertEquals("shh", asset.secret);
    }

    // Case (2): the property itself is always visible, so the type id "admin"
    // correctly produces the admin subtype -- but its admin-only "secret" field
    // must be left out when reading with PublicView.
    @Test
    void testViewGatedFieldOfExternalTypeSubtype() throws Exception
    {
        String json = a2q("{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}");

        FieldGatedContainer result = MAPPER.readerWithView(PublicView.class)
                .forType(FieldGatedContainer.class)
                .readValue(json);

        assertEquals("hello", result.label);
        // View does not change the type id: still the admin subtype...
        FieldGatedAdminAsset asset = assertInstanceOf(FieldGatedAdminAsset.class, result.asset);
        assertEquals("foo", asset.name);
        // ...but the admin-only field must not leak under PublicView
        assertNull(asset.secret, "Admin-only 'secret' must not leak under PublicView");
    }

    // Case (3): hiding the admin-only value must NOT drop the (non-view) type id
    // property when the type id is itself a creator parameter ([databind#999]).
    @Test
    void testViewGatedValueKeepsVisibleTypeIdCreatorProp() throws Exception
    {
        String json = a2q("{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}");

        TypeIdCreatorContainer result = MAPPER.readerWithView(PublicView.class)
                .forType(TypeIdCreatorContainer.class)
                .readValue(json);

        assertEquals("hello", result.label);
        // Admin-only value is hidden...
        assertNull(result.asset, "Admin-only 'asset' must not be bound under PublicView");
        // ...but the visible type id creator property must still be bound
        assertEquals("admin", result.kind, "Visible type id 'kind' must still be bound");
    }

    // Case (4): admin-only external-type property on a default-constructor bean must
    // also be skipped under PublicView (covers the other complete() overload).
    @Test
    void testViewGatedExternalTypePropertyDefaultCtor() throws Exception
    {
        String json = a2q("{'label':'hello','kind':'admin',"
                + "'asset':{'name':'foo','secret':'LEAKED'}}");

        DefaultCtorContainer result = MAPPER.readerWithView(PublicView.class)
                .forType(DefaultCtorContainer.class)
                .readValue(json);

        assertEquals("hello", result.label);
        assertNull(result.asset, "Admin-only 'asset' must not be bound under PublicView");
    }
}
