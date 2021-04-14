package com.fasterxml.jackson.databind.introspect;

import java.io.IOException;
import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("serial")
public class CustomAnnotationIntrospector1756Test extends BaseMapTest
{
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Field1756 {

      String value() default "";
    }

    public interface Foobar
    {
      @JsonIgnore // Comment out this annotation or ...
      String foo();

      @JsonDeserialize(using = CustomStringDeserializer.class) // ... this annotation to unbreak deserialization.
      String bar();
    }

    /**
     * Custom String deserializer.
     */
    private static class CustomStringDeserializer extends JsonDeserializer<String> {

      @Override
      public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.getText();
      }
    }

    /**
     * A concrete implementation.
     */
    public static class FoobarImpl implements Foobar {

      private final String foo;
      private final String bar;

      public FoobarImpl(@Field1756("foo") String foo,
              @Field1756("bar") String bar) {
        this.foo = foo;
        this.bar = bar;
      }

      @Override
      public String foo() {
        return foo;
      }

      @Override
      public String bar() {
        return bar;
      }
    }

    /**
     * Instructs jackson that {@link Foobar#foo()}, {@link Foobar#bar()} and the {@code foo} and {@code bar} constructor
     * arguments map to the {@code foo} and {@code bar} properties.
     */
    public static class FoobarAnnotationIntrospector extends NopAnnotationIntrospector {

      @Override
      public String findImplicitPropertyName(final AnnotatedMember member) {
        // Constructor parameter
        if (member instanceof AnnotatedParameter) {
          final Field1756 field = member.getAnnotation(Field1756.class);
          if (field == null) {
            return null;
          }
          return field.value();
        }
        // Getter
        if (member instanceof AnnotatedMethod) {
          return member.getName();
        }
        return null;
      }

      @Override
      public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated ann) {
          final AnnotatedConstructor ctor = (AnnotatedConstructor) ann;
          if (ctor.getParameterCount() > 0) {
              if (ctor.getParameter(0).getAnnotation(Field1756.class) != null) {
                  return JsonCreator.Mode.PROPERTIES;
              }
          }
          return null;
      }
    }

    public static class Issue1756Module extends SimpleModule {
        @Override
        public void setupModule(final SetupContext context) {
            super.setupModule(context);
            context.appendAnnotationIntrospector(new FoobarAnnotationIntrospector());
        }
    }

    public void testIssue1756() throws Exception
    {
        Issue1756Module m = new Issue1756Module();
        m.addAbstractTypeMapping(Foobar.class, FoobarImpl.class);
        final ObjectMapper mapper = JsonMapper.builder()
                .addModule(m)
                .build();

        final Foobar foobar = mapper.readValue(a2q("{'bar':'bar', 'foo':'foo'}"),
                Foobar.class);
        assertNotNull(foobar);
    }
}
