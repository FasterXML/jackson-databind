package com.fasterxml.jackson.databind.struct;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

// Unit test for [JACKSON-890]
public class TestBackRefsWithPolymorphic extends BaseMapTest
{
    private final String CLASS_NAME = getClass().getName();

    // NOTE: order is arbitrary, test is fragile... has to work for now
    private final String JSON =
        "{\"@class\":\""+CLASS_NAME+"$PropertySheetImpl\",\"id\":0,\"properties\":{\"p1name\":{\"@class\":"
            +"\"" +CLASS_NAME+ "$StringPropertyImpl\",\"id\":0,\"name\":\"p1name\",\"value\":\"p1value\"},"
            +"\"p2name\":{\"@class\":\""+CLASS_NAME+"$StringPropertyImpl\",\"id\":0,"
            +"\"name\":\"p2name\",\"value\":\"p2value\"}}}";

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testDeserialize() throws IOException
    {
        PropertySheet input = MAPPER.readValue(JSON, PropertySheet.class);
        assertEquals(JSON, MAPPER.writeValueAsString(input));
    }

    public void testSerialize() throws IOException
    {
        PropertySheet sheet = new PropertySheetImpl();

        sheet.addProperty(new StringPropertyImpl("p1name", "p1value"));
        sheet.addProperty(new StringPropertyImpl("p2name", "p2value"));
        String actual = MAPPER.writeValueAsString(sheet);
        assertEquals(JSON, actual);
    }

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

        public StringPropertyImpl(String name, String value) {
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
}
