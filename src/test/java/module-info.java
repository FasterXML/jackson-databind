// Jackson 3.x module-info for jackson-databind Tests
module tools.jackson.databind
{
    requires java.desktop;
    requires java.sql;
    requires java.sql.rowset;
    requires java.xml;

    // but we probably do want to expose streaming, annotations
    // as transitive dependencies streaming types at least part of API
    requires com.fasterxml.jackson.annotation;

    requires tools.jackson.core;

    // // Actual Test dependencies

    // Test frameworks, libraries:

    // Guava testlib needed by CLMH tests, alas; brings in junit4
    requires guava.testlib;
    // JUnit4 should NOT be needed but is transitively required
    requires junit;
    requires org.assertj.core;
    requires org.mockito;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires org.junit.platform.commons;

    // Main exports need to switch to "opens" for testing
    opens tools.jackson.databind;
    opens tools.jackson.databind.annotation;
    opens tools.jackson.databind.cfg;
    opens tools.jackson.databind.deser;
    opens tools.jackson.databind.deser.bean;
    opens tools.jackson.databind.deser.jackson;
    opens tools.jackson.databind.deser.jdk;
    opens tools.jackson.databind.deser.std;
    opens tools.jackson.databind.exc;
    opens tools.jackson.databind.ext.javatime;
    opens tools.jackson.databind.ext.javatime.deser;
    opens tools.jackson.databind.ext.javatime.deser.key;
    opens tools.jackson.databind.ext.javatime.key;
    opens tools.jackson.databind.ext.javatime.misc;
    opens tools.jackson.databind.ext.javatime.ser;
    opens tools.jackson.databind.ext.javatime.util;
    opens tools.jackson.databind.introspect;
    opens tools.jackson.databind.json;
    opens tools.jackson.databind.jsonFormatVisitors;
    opens tools.jackson.databind.jsontype;
    opens tools.jackson.databind.jsontype.impl;
    opens tools.jackson.databind.module;
    opens tools.jackson.databind.node;
    opens tools.jackson.databind.ser;
    opens tools.jackson.databind.ser.bean;
    opens tools.jackson.databind.ser.jackson;
    opens tools.jackson.databind.ser.jdk;
    opens tools.jackson.databind.ser.std;
    opens tools.jackson.databind.type;
    opens tools.jackson.databind.util;

    // Additional test opens (not exported by main, or needed from src/test/java)
    // needed by JUnit and other test libs
    opens tools.jackson.databind.access;
    opens tools.jackson.databind.contextual;
    opens tools.jackson.databind.convert;
    opens tools.jackson.databind.deser.builder;
    opens tools.jackson.databind.deser.creators;
    opens tools.jackson.databind.deser.dos;
    opens tools.jackson.databind.deser.enums;
    opens tools.jackson.databind.deser.filter;
    opens tools.jackson.databind.deser.inject;
    opens tools.jackson.databind.deser.lazy;
    opens tools.jackson.databind.deser.merge;
    opens tools.jackson.databind.deser.validate;
    opens tools.jackson.databind.ext;
    opens tools.jackson.databind.ext.cglib;
    opens tools.jackson.databind.ext.desktop;
    opens tools.jackson.databind.ext.jdk8;
    opens tools.jackson.databind.ext.jdk9;
    opens tools.jackson.databind.ext.jdk17;
    opens tools.jackson.databind.ext.sql;
    opens tools.jackson.databind.ext.xml;
    opens tools.jackson.databind.format;
    opens tools.jackson.databind.interop;
    opens tools.jackson.databind.jsonschema;
    opens tools.jackson.databind.jsontype.deftyping;
    opens tools.jackson.databind.jsontype.ext;
    opens tools.jackson.databind.jsontype.jdk;
    opens tools.jackson.databind.jsontype.vld;
    opens tools.jackson.databind.misc;
    opens tools.jackson.databind.mixins;
    opens tools.jackson.databind.objectid;
    opens tools.jackson.databind.records;
    opens tools.jackson.databind.records.tofix;
    opens tools.jackson.databind.ser.dos;
    opens tools.jackson.databind.ser.enums;
    opens tools.jackson.databind.ser.filter;
    opens tools.jackson.databind.seq;
    opens tools.jackson.databind.struct;
    opens tools.jackson.databind.testutil.failure;
    exports tools.jackson.databind.testutil.failure;
    opens tools.jackson.databind.tofix;
    opens tools.jackson.databind.util.internal;
    opens tools.jackson.databind.views;

    // Also needed for some reason
    uses tools.jackson.databind.JacksonModule;
}
