package tools.jackson.databind.records;

import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedConstructor;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class Jdk8ConstructorParameterNameAnnotationIntrospector extends JacksonAnnotationIntrospector
{
    private static final long serialVersionUID = 1L;

    @Override
    public String findImplicitPropertyName(MapperConfig<?> cfg, AnnotatedMember member) {
        if (!(member instanceof AnnotatedParameter)) {
            return null;
        }
        AnnotatedParameter parameter = (AnnotatedParameter) member;
        if (!(parameter.getOwner() instanceof AnnotatedConstructor)) {
            return null;
        }
        AnnotatedConstructor constructor = (AnnotatedConstructor) parameter.getOwner();
        String parameterName = constructor.getAnnotated().getParameters()[parameter.getIndex()].getName();

        if (parameterName == null || parameterName.isBlank()) {
            throw new IllegalArgumentException("Unable to extract constructor parameter name for: " + member);
        }

        return parameterName;
    }
}
