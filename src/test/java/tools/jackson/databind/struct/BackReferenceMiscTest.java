package tools.jackson.databind.struct;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Unit test for [JACKSON-890] and other back-reference edge cases
public class BackReferenceMiscTest extends DatabindTestUtil
{
    @JsonPropertyOrder(alphabetic=true)
    interface Entity
    {
        @JsonIgnore String getEntityType();
        Long getId();
        void setId(Long id);
        @JsonIgnore void setPersistable();
    }

    @JsonDeserialize(as = NestedPropertySheetImpl.class)
    interface NestedPropertySheet
        extends Property<PropertySheet>
    {
        @Override PropertySheet getValue();
        void setValue(PropertySheet propertySheet);
    }

    @JsonDeserialize(as = AbstractProperty.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include  = JsonTypeInfo.As.PROPERTY,
        property = "@class")
    interface Property<T> extends Entity
    {
        String getName();
        PropertySheet getParentSheet();
        T getValue();
        void setName(String name);
        void setParentSheet(PropertySheet parentSheet);
    }

    @JsonDeserialize(as = PropertySheetImpl.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include  = JsonTypeInfo.As.PROPERTY,
        property = "@class")
    @SuppressWarnings("rawtypes")
    interface PropertySheet extends Entity
    {
        void addProperty(Property property);
        Map<String, Property> getProperties();
        void setProperties(Map<String, Property> properties);
    }

    @JsonDeserialize(as = StringPropertyImpl.class)
    interface StringProperty
        extends Property<String>
    {
        @Override String getValue();
        void setValue(String value);
    }

    static class AbstractEntity implements Entity
    {
        private long id;

        @Override public String getEntityType() {
            return "";
        }

        @Override public Long getId() {
            return id;
        }

        @Override public void setId(Long id)
        {
            this.id = id;
        }

        @Override public void setPersistable() { }
    }

    abstract static class AbstractProperty<T>
        extends AbstractEntity
        implements Property<T>
    {
        private String        m_name;
        private PropertySheet m_parentSheet;

        protected AbstractProperty() { }

        protected AbstractProperty(String name) {
            m_name = name;
        }

        @Override public String getName() {
            return m_name;
        }

        @JsonBackReference("propertySheet-properties")
        @Override public PropertySheet getParentSheet() {
            return m_parentSheet;
        }

        @Override public void setName(String name) {
            m_name = name;
        }

        @Override public void setParentSheet(PropertySheet parentSheet) {
            m_parentSheet = parentSheet;
        }
    }

    @JsonPropertyOrder(alphabetic=true)
    static class NestedPropertySheetImpl
        extends AbstractProperty<PropertySheet>
        implements NestedPropertySheet
    {
        private PropertySheet m_propertySheet;

        protected NestedPropertySheetImpl(String name,
                PropertySheet propertySheet)
        {
            super(name);
            m_propertySheet = propertySheet;
        }

        NestedPropertySheetImpl() { }

        @Override public PropertySheet getValue() {
            return m_propertySheet;
        }

        @Override public void setValue(PropertySheet propertySheet) {
            m_propertySheet = propertySheet;
        }
    }

    @SuppressWarnings("rawtypes")
    static class PropertySheetImpl
        extends AbstractEntity
        implements PropertySheet
    {
        private Map<String, Property> m_properties;

        @Override public void addProperty(Property property)
        {
            if (m_properties == null) {
                m_properties = new TreeMap<String, Property>();
            }
            property.setParentSheet(this);
            m_properties.put(property.getName(), property);
        }

        @JsonDeserialize(as = TreeMap.class,
            keyAs     = String.class,
            contentAs = Property.class)
        @JsonManagedReference("propertySheet-properties")
        @Override public Map<String, Property> getProperties() {
            return m_properties;
        }

        @Override public void setProperties(Map<String, Property> properties) {
            m_properties = properties;
        }
    }

    static class StringPropertyImpl
        extends AbstractProperty<String>
        implements StringProperty
    {
        private String m_value;

        protected StringPropertyImpl(String name, String value) {
            super(name);
            m_value = value;
        }

        StringPropertyImpl() { }

        @Override public String getValue() {
            return m_value;
        }

        @Override public void setValue(String value) {
            m_value = value;
        }
    }

    static class YetAnotherClass extends StringPropertyImpl { }

    // [databind#3304]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleChild3304.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = RefChild3304.class, name = "REF")
    })
    interface IChild3304 { }

    static class SimpleChild3304 implements IChild3304 {
        public String value;

        public SimpleChild3304() { }
        public SimpleChild3304(String v) { value = v; }
    }

    static class RefChild3304 implements IChild3304 {
        public String data;

        @JsonBackReference
        public Container3304 parent;

        public RefChild3304() { }
        public RefChild3304(String d) { data = d; }
    }

    static class Container3304 {
        @JsonManagedReference
        public IChild3304[] children;
    }

    static class ContainerWithList3304 {
        @JsonManagedReference
        public java.util.List<IChild3304> children;
    }

    // [databind#1516]
    static class ParentWithCreator
    {
        String id, name;

        @JsonManagedReference
        ChildObject1 child;

        @ConstructorProperties({"id", "name", "child"})
        public ParentWithCreator(String id, String name, ChildObject1 child) {
            this.id = id;
            this.name = name;
            this.child = child;
        }
    }

    static class ChildObject1
    {
        public String id, name;

        @JsonBackReference
        public ParentWithCreator parent;

        @ConstructorProperties({"id", "name", "parent"})
        public ChildObject1(String id, String name, ParentWithCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    static class ParentWithoutCreator {
        public String id, name;

        @JsonManagedReference
        public ChildObject2 child;
    }

    static class ChildObject2 {
        public String id, name;

        @JsonBackReference
        public ParentWithoutCreator parent;

        @ConstructorProperties({"id", "name", "parent"})
        public ChildObject2(String id, String name,
                            ParentWithoutCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    // [Kotlin#129]
    static class Car {
        private final long id;

        @JsonManagedReference
        private final List<Color> colors;

        @JsonCreator
        public Car(@JsonProperty("id") long id,
                   @JsonProperty("colors") List<Color> colors) {
            this.id = id;
            this.colors = colors != null ? colors : new ArrayList<>();
        }

        public long getId() { return id; }
        public List<Color> getColors() { return colors; }
    }

    static class Color {
        private final long id;
        private final String code;

        @JsonBackReference
        private Car car;

        @JsonCreator
        public Color(@JsonProperty("id") long id,
                     @JsonProperty("code") String code) {
            this.id = id;
            this.code = code;
        }

        public long getId() { return id; }
        public String getCode() { return code; }
        public Car getCar() { return car; }
        public void setCar(Car car) { this.car = car; }
    }

    // [databind#2686]
    public static class Container2686 {
        Content2686 forward;

        String containerValue;

        @JsonManagedReference
        public Content2686 getForward() {
            return forward;
        }

        @JsonManagedReference
        public void setForward(Content2686 forward) {
            this.forward = forward;
        }

        public String getContainerValue() {
            return containerValue;
        }

        public void setContainerValue(String containerValue) {
            this.containerValue = containerValue;
        }
    }

    @JsonDeserialize(builder = Content2686.Builder.class)
    public static class Content2686 {
        private Container2686 back;

        private String contentValue;

        public Content2686(Container2686 back, String contentValue) {
            this.back = back;
            this.contentValue = contentValue;
        }

        public String getContentValue() {
            return contentValue;
        }

        @JsonBackReference
        public Container2686 getBack() {
            return back;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private Container2686 back;
            private String contentValue;

            @JsonBackReference
            Builder back(Container2686 b) {
                this.back = b;
                return this;
            }

            Builder contentValue(String cv) {
                this.contentValue = cv;
                return this;
            }

            Content2686 build() {
                return new Content2686(back, contentValue);
            }
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final String CLASS_NAME = getClass().getName();
    // NOTE: order is arbitrary, test is fragile... has to work for now
    private final String JSON =
        "{\"@class\":\""+CLASS_NAME+"$PropertySheetImpl\",\"id\":0,\"properties\":{\"p1name\":{\"@class\":"
            +"\"" +CLASS_NAME+ "$StringPropertyImpl\",\"id\":0,\"name\":\"p1name\",\"value\":\"p1value\"},"
            +"\"p2name\":{\"@class\":\""+CLASS_NAME+"$StringPropertyImpl\",\"id\":0,"
            +"\"name\":\"p2name\",\"value\":\"p2value\"}}}";

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeserialize() throws IOException
    {
        PropertySheet input = MAPPER.readValue(JSON, PropertySheet.class);
        assertEquals(JSON, MAPPER.writeValueAsString(input));
    }

    @Test
    public void testSerialize() throws IOException
    {
        PropertySheet sheet = new PropertySheetImpl();

        sheet.addProperty(new StringPropertyImpl("p1name", "p1value"));
        sheet.addProperty(new StringPropertyImpl("p2name", "p2value"));
        String actual = MAPPER.writeValueAsString(sheet);
        assertEquals(JSON, actual);
    }

    // [databind#3304]: Verify error message mentions the abstract type issue
    @Test
    public void testBackRefOnSubtypeOnly_array() {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.readValue(
                        "{\"children\":[{\"type\":\"SIMPLE\",\"value\":\"x\"}]}",
                        Container3304.class));
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("abstract"),
                "Error message should mention abstract type issue, got: " + msg);
        assertTrue(msg.contains("@JsonBackReference"),
                "Error message should mention @JsonBackReference, got: " + msg);
    }

    // [databind#3304]
    @Test
    public void testBackRefOnSubtypeOnly_list() {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.readValue(
                        "{\"children\":[{\"type\":\"SIMPLE\",\"value\":\"x\"}]}",
                        ContainerWithList3304.class));
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("abstract"),
                "Error message should mention abstract type issue, got: " + msg);
        assertTrue(msg.contains("@JsonBackReference"),
                "Error message should mention @JsonBackReference, got: " + msg);
    }

    // [databind#1516]
    @Test
    public void testWithParentCreator() throws Exception {
        String json = a2q(
                "{ 'id': 'abc',\n" +
                "  'name': 'Bob',\n" +
                "  'child': { 'id': 'def', 'name':'Bert' }\n" +
                "}");
        ParentWithCreator result = MAPPER.readValue(json, ParentWithCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }

    // [databind#1516]
    @Test
    public void testWithParentNoCreator() throws Exception {
        String json = a2q(
                "{ 'id': 'abc',\n" +
                "  'name': 'Bob',\n" +
                "  'child': { 'id': 'def', 'name':'Bert' }\n" +
                "}");
        ParentWithoutCreator result = MAPPER.readValue(json, ParentWithoutCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }

    // [Kotlin#129]
    @Test
    public void testManagedReferenceOnCreator() throws Exception
    {
        Car car = new Car(100, new ArrayList<>());
        Color color = new Color(100, "#FFFFF");
        color.setCar(car);
        car.getColors().add(color);

        String json = MAPPER.writeValueAsString(car);
        Car result = MAPPER.readValue(json, Car.class);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertNotNull(result.getColors());
        assertEquals(1, result.getColors().size());

        Color resultColor = result.getColors().get(0);
        assertEquals(100, resultColor.getId());
        assertEquals("#FFFFF", resultColor.getCode());

        assertNotNull(resultColor.getCar());
        assertSame(result, resultColor.getCar());
    }

    // [databind#2686]
    @Test
    public void testBuildWithBackRefs2686() throws Exception {
        Container2686 container = new Container2686();
        container.containerValue = "containerValue";
        Content2686 content = new Content2686(container, "contentValue");
        container.forward = content;

        String json = MAPPER.writeValueAsString(container);
        Container2686 result = MAPPER.readValue(json, Container2686.class);

        assertNotNull(result);
        assertNotNull(result.getForward());
        assertEquals("contentValue", result.getForward().getContentValue());
    }
}
