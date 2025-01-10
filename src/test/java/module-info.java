// Jackson 3.x module-info for Tests
module tools.jackson.databind {
    requires static java.desktop;
    requires static java.sql;
    requires static java.sql.rowset;
    requires java.xml;

    // but we probably do want to expose streaming, annotations
    // as transitive dependencies streaming types at least part of API
    requires transitive com.fasterxml.jackson.annotation;

    requires transitive tools.jackson.core;

    // // Actual Test dependencies

    requires guava.testlib;
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

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

    // Additional test opens (not exported by main)
    opens java.util;

    opens tools.jackson.databind.access;
    opens tools.jackson.databind.deser.inject;
    opens tools.jackson.databind.deser.merge;
    opens tools.jackson.databind.testutil.failure;
    opens tools.jackson.databind.tofix;
    opens tools.jackson.databind.util.internal;
    opens tools.jackson.databind.views;

    // Also needed for some reason
    uses tools.jackson.databind.JacksonModule;
}
