package tools.jackson.failing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.deser.jdk.CollectionDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.CollectionLikeType;

import static org.junit.jupiter.api.Assertions.assertSame;

// 30-Mar-2024, tatu: For some reason fails for 3.0 after conversion
//   to JUnit5. Should fix.
public class NodeContext2049Test extends DatabindTestUtil
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
        public Object createUsingDefault(DeserializationContext ctxt) throws JacksonException {
             return new ArrayList<>();
        }

        @Override
        public ValueInstantiator createContextual(DeserializationContext ctxt, BeanDescription beanDesc) {
            return this;
        }

        @Override
        public Class<?> getValueClass() {
            return List.class;
        }
    }

    static class ParentSettingDeserializerModifier extends ValueDeserializerModifier {
        private static final long serialVersionUID = 1L;

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

    static class ParentSettingDeserializer extends DelegatingDeserializer {
        public ParentSettingDeserializer(ValueDeserializer<?> delegatee) {
             super(delegatee);
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
             Object retValue = super.deserialize(jp, ctxt);
             if (retValue instanceof HasParent) {
                  HasParent obj = (HasParent) retValue;
                  Parent parent = null;
                  TokenStreamContext parsingContext = jp.streamReadContext();
                  while (parent == null && parsingContext != null) {
                       Object currentValue = parsingContext.currentValue();
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
        protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
             return new ParentSettingDeserializer(newDelegatee);
        }

   }

    static class ParentSettingDeserializerContextual extends ValueDeserializer<Object> {

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JacksonException
        {
             JavaType propertyType = property.getType();
             JavaType contentType = propertyType;
             if (propertyType.isCollectionLikeType()) {
                  contentType = propertyType.getContentType();
             }
             ValueDeserializer<Object> delegatee = ctxt.findNonContextualValueDeserializer(contentType);
             ValueDeserializer<Object> objectDeserializer = new ParentSettingDeserializer(delegatee);
             ValueDeserializer<?> retValue;
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
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
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
        objectMapper = JsonMapper.builder()
                .addModule(new tools.jackson.databind.JacksonModule() {
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
                   context.addDeserializerModifier(new ParentSettingDeserializerModifier());
              }
         })
        .build();
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

    @Test
    public void testReadNoBuffering() throws Exception {
        Parent obj = objectMapper.readerFor(Parent.class).readValue(JSON);
        assertSame(obj, obj.singleChild.getParent());
        for (Child child : obj.children) {
            assertSame(obj, child.getParent());
        }
    }

    @Test
    public void testReadFromTree() throws Exception {
        JsonNode tree = objectMapper.readTree(JSON);
        Parent obj = objectMapper.reader().forType(Parent.class).readValue(tree);
        assertSame(obj, obj.singleChild.getParent());
        for (Child child : obj.children) {
            assertSame(obj, child.getParent());
        }
    }
}
