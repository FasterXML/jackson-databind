package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.type.CollectionLikeType;

public class NodeContext2049Test extends BaseMapTest
{
    public interface HasParent {
        void setParent(Parent parent);
        Parent getParent();
    }

    static class Child implements HasParent {
        public Parent parent;
        public String property;

        @Override
        public void setParent(Parent p) { parent = p; }
        @Override
        public Parent getParent() { return parent; }
    }

    static class Parent {
        public List<Child> children;
        public Child singleChild;
    }

    static class ListValueInstantiator extends ValueInstantiator {
        @Override
        public String getValueTypeDesc() {
             return List.class.getName();
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
             return new ArrayList<>();
        }
    }

    static class ParentSettingDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
                  BeanDeserializerBuilder builder) {
             for (Iterator<SettableBeanProperty> propertyIt = builder.getProperties(); propertyIt.hasNext(); ) {
                  SettableBeanProperty property = propertyIt.next();
                  builder.addOrReplaceProperty(property.withValueDeserializer(new ParentSettingDeserializerContextual()), false);
             }
             return builder;
        }
    }

    @SuppressWarnings("serial")
    static class ParentSettingDeserializer extends DelegatingDeserializer {
        public ParentSettingDeserializer(JsonDeserializer<?> delegatee) {
             super(delegatee);
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
             Object retValue = super.deserialize(jp, ctxt);
             if (retValue instanceof HasParent) {
                  HasParent obj = (HasParent) retValue;
                  Parent parent = null;
                  JsonStreamContext parsingContext = jp.getParsingContext();
                  while (parent == null && parsingContext != null) {
                       Object currentValue = parsingContext.getCurrentValue();
                       if (currentValue != null && currentValue instanceof Parent) {
                            parent = (Parent) currentValue;
                       }
                       parsingContext = parsingContext.getParent();
                  }
                  if (parent != null) {
                       obj.setParent(parent);
                  }
             }
             return retValue;
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
             return new ParentSettingDeserializer(newDelegatee);
        }

   }

    static class ParentSettingDeserializerContextual extends JsonDeserializer<Object> implements ContextualDeserializer {

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JsonMappingException
        {
             JavaType propertyType = property.getType();
             JavaType contentType = propertyType;
             if (propertyType.isCollectionLikeType()) {
                  contentType = propertyType.getContentType();
             }
             JsonDeserializer<Object> delegatee = ctxt.findNonContextualValueDeserializer(contentType);
             JsonDeserializer<Object> objectDeserializer = new ParentSettingDeserializer(delegatee);
             JsonDeserializer<?> retValue;
             if (propertyType.isCollectionLikeType()) {
                  CollectionLikeType collectionType = ctxt.getTypeFactory().constructCollectionLikeType(propertyType.getRawClass(),
                            contentType);
                  ValueInstantiator instantiator = new ListValueInstantiator();
                  retValue = new CollectionDeserializer(collectionType, objectDeserializer, null, instantiator);
             } else {
                  retValue = objectDeserializer;
             }
             return retValue;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
             // TODO Auto-generated method stub
             return null;
        }

   }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private ObjectMapper objectMapper;
    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.databind.Module() {
              @Override
              public String getModuleName() {
                   return "parentSetting";
              }
              @Override
              public Version version() {
                   return Version.unknownVersion();
              }
              @Override
              public void setupModule(SetupContext context) {
                   context.addBeanDeserializerModifier(new ParentSettingDeserializerModifier());
              }
         });
    }

    final static String JSON = "{\n" +
            "     \"children\": [\n" +
            "          {\n" +
            "               \"property\": \"value1\"\n" +
            "          },\n" +
            "          {\n" +
            "               \"property\": \"value2\"\n" +
            "          }\n" +
            "     ],\n" +
            "     \"singleChild\": {\n" +
            "          \"property\": \"value3\"\n" +
            "     }\n" +
            "}";

    public void testReadNoBuffering() throws IOException {
        Parent obj = objectMapper.readerFor(Parent.class).readValue(JSON);
        assertSame(obj, obj.singleChild.getParent());
        for (Child child : obj.children) {
            assertSame(obj, child.getParent());
        }
    }

    public void testReadFromTree() throws IOException {
        JsonNode tree = objectMapper.readTree(JSON);
        Parent obj = objectMapper.reader().forType(Parent.class).readValue(tree);
        assertSame(obj, obj.singleChild.getParent());
        for (Child child : obj.children) {
            assertSame(obj, child.getParent());
        }
    }
}
