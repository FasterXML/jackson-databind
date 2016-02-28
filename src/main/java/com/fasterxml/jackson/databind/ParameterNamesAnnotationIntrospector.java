package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.introspect.*;

import java.lang.reflect.Parameter;

/**
 * Introspector that uses parameter name information provided by the Java Reflection API additions in Java 8 to
 * determine the parameter name for methods and constructors.
 *
 * @author Lovro Pandzic
 * @see AnnotationIntrospector
 * @see Parameter
 */
public class ParameterNamesAnnotationIntrospector extends NopAnnotationIntrospector
{
    private static final long serialVersionUID = 1L;

    private final JsonCreator.Mode creatorBinding;

    public ParameterNamesAnnotationIntrospector(JsonCreator.Mode creatorBinding) {

        this.creatorBinding = creatorBinding;
    }

    @Override
    public String findImplicitPropertyName(AnnotatedMember m) {
        if (m instanceof AnnotatedParameter) {
            return findParameterName((AnnotatedParameter) m);
        }
        return null;
    }

    @Override
    public JsonCreator.Mode findCreatorBinding(Annotated a) {

        JsonCreator ann = a.getAnnotation(JsonCreator.class);
        if (ann != null) {
            return ann.mode();
        }

        return creatorBinding;
    }

    /**
     * Returns the parameter name, or {@code null} if it could not be determined.
     *
     * @param annotatedParameter containing constructor or method from which {@link Parameter} can be extracted
     *
     * @return name or {@code null} if parameter could not be determined
     */
    private String findParameterName(AnnotatedParameter annotatedParameter) {

        AnnotatedWithParams owner = annotatedParameter.getOwner();
        Parameter[] params;

        if (owner instanceof AnnotatedConstructor) {
            params = ((AnnotatedConstructor) owner).getAnnotated().getParameters();
        } else if (owner instanceof AnnotatedMethod) {
            params = ((AnnotatedMethod) owner).getAnnotated().getParameters();
        } else {
            return null;
        }
        Parameter p = params[annotatedParameter.getIndex()];
        return p.isNamePresent() ? p.getName() : null;
    }
}
