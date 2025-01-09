// Test version for modules
module tools.jackson.databind {
    requires static java.desktop;
    requires static java.sql;
    requires java.xml;

    // but we probably do want to expose streaming, annotations
    // as transitive dependencies streaming types at least part of API
    requires transitive com.fasterxml.jackson.annotation;

    requires transitive tools.jackson.core;

    // // Test dependencies

    requires guava.testlib;
    // CHLM tests require, alas, JUnit 4:
    requires junit.framework;
    requires org.junit.jupiter.api;
    
    exports tools.jackson.databind;
    exports tools.jackson.databind.annotation;
    exports tools.jackson.databind.cfg;
    exports tools.jackson.databind.deser;
    exports tools.jackson.databind.deser.bean;
//    exports tools.jackson.databind.deser.impl;
    exports tools.jackson.databind.deser.jackson;
    exports tools.jackson.databind.deser.jdk;
    exports tools.jackson.databind.deser.std;
    exports tools.jackson.databind.exc;
    // No need to expose these handlers?
//    exports tools.jackson.databind.ext;
    exports tools.jackson.databind.introspect;
    exports tools.jackson.databind.json;
    exports tools.jackson.databind.jsonFormatVisitors;
    exports tools.jackson.databind.jsontype;
    exports tools.jackson.databind.jsontype.impl;
    exports tools.jackson.databind.module;
    exports tools.jackson.databind.node;
    exports tools.jackson.databind.ser;
    exports tools.jackson.databind.ser.bean;
//    exports tools.jackson.databind.ser.impl;
    exports tools.jackson.databind.ser.jackson;
    exports tools.jackson.databind.ser.jdk;
    exports tools.jackson.databind.ser.std;
    exports tools.jackson.databind.type;
    exports tools.jackson.databind.util;

    // [databind#2485]: prevent warning for "unused" with self-use
    uses tools.jackson.databind.JacksonModule;

    provides tools.jackson.databind.ObjectMapper with
        tools.jackson.databind.json.JsonMapper;
}
