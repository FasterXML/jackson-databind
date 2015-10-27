package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DefaultModuleLoader {

    private final List<DefaultModuleDefinition> defaultModuleDefinitions;

    DefaultModuleLoader() {
        defaultModuleDefinitions = new ArrayList<DefaultModuleDefinition>();
        defaultModuleDefinitions.add(new DefaultModuleDefinition("com.fasterxml.jackson.module.paramnames.ParameterNamesModule", JsonCreator.Mode.DEFAULT));
        defaultModuleDefinitions.add(new DefaultModuleDefinition("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"));
        defaultModuleDefinitions.add(new DefaultModuleDefinition("com.fasterxml.jackson.datatype.jdk8.Jdk8Module"));
    }

    List<Module> getAvailableDefaultModules() {

        if (isRuntimeNotJava8Compatible()) {
            return Collections.emptyList();
        }

        List<Module> defaultModules = new ArrayList<Module>();

        for (DefaultModuleDefinition defaultModuleDefinition : defaultModuleDefinitions) {

            Module module = defaultModuleDefinition.createModule();

            if (module != null) {
                defaultModules.add(module);
            }
        }

        return defaultModules;
    }

    private boolean isRuntimeNotJava8Compatible() {

        try {
            getClass().getClassLoader().loadClass("java.util.function.Function");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    private static class DefaultModuleDefinition {

        private final String moduleClassName;
        private final Object[] constructorArguments;

        private DefaultModuleDefinition(String moduleClassName, Object... constructorArguments) {
            this.moduleClassName = moduleClassName;
            this.constructorArguments = constructorArguments;
        }

        Module createModule() {

            List<Class<?>> constructorParameters = new ArrayList<Class<?>>();
            for (Object constructorArgument : constructorArguments) {
                constructorParameters.add(constructorArgument.getClass());
            }

            try {
                return (Module) getClass().getClassLoader().loadClass(moduleClassName).getConstructor(constructorParameters.toArray(new Class[constructorParameters.size()])).newInstance(constructorArguments);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
