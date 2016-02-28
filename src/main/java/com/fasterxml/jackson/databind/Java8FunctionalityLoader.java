package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.*;

class Java8FunctionalityLoader {

    List<Module> getJava8Modules() {

        List<Module> defaultModules = new ArrayList<Module>();

        defaultModules.add(createModule("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"));
        defaultModules.add(createModule("com.fasterxml.jackson.datatype.jdk8.Jdk8Module"));
        return defaultModules;
    }

    AnnotationIntrospector getJava8AnnotationIntrospector() {
        return new ParameterNamesAnnotationIntrospector(JsonCreator.Mode.DELEGATING);
    }

    private Module createModule(String moduleClassName) {

        try {
            return (Module) getClass().getClassLoader().loadClass(moduleClassName).getConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
