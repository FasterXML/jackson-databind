package com.fasterxml.jackson.databind.deser.bean;

import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdValueProperty;
import com.fasterxml.jackson.databind.type.TypeFactory;

// for [databind#884]
public class BeanPropertyMapTest extends BaseMapTest
{
    protected final static JavaType BOGUS_TYPE = TypeFactory.unknownType();
    
    @SuppressWarnings("serial")
    static class MyObjectIdReader extends ObjectIdReader
    {
        public MyObjectIdReader(String name) {
            super(BOGUS_TYPE, new PropertyName(name), null,
                    null, null, null);
        }
    }

    // Highly specialized test in which we get couple of hash collisions for
    // small (16) hash map
    public void testArrayOutOfBounds884() throws Exception
    {
        List<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>();
        PropertyMetadata md = PropertyMetadata.STD_REQUIRED;
        props.add(new ObjectIdValueProperty(new MyObjectIdReader("pk"), md));
        props.add(new ObjectIdValueProperty(new MyObjectIdReader("firstName"), md));
        BeanPropertyMap propMap = new BeanPropertyMap(props,
                null, Locale.getDefault(), false, true);
        propMap = propMap.withProperty(new ObjectIdValueProperty(new MyObjectIdReader("@id"), md));
        assertNotNull(propMap);
    }
}
