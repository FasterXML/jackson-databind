package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

class Jdk8ConstructorParameterNameAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public String findImplicitPropertyName(AnnotatedMember member) {
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
