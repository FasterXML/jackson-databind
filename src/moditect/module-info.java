// Generated 14-Mar-2019 using Moditect maven plugin
module com.fasterxml.jackson.databind {
    requires java.desktop;
    requires java.logging;
    requires java.sql;
    requires java.xml;

    // but we probably do want to expose streaming, annotations
    // as transitive dependencies streaming types at least part of API
    requires transitive com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.core;

    exports com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.databind.annotation;
    exports com.fasterxml.jackson.databind.cfg;
    exports com.fasterxml.jackson.databind.deser;
    exports com.fasterxml.jackson.databind.deser.impl;
    exports com.fasterxml.jackson.databind.deser.std;
    exports com.fasterxml.jackson.databind.exc;
    exports com.fasterxml.jackson.databind.introspect;
    exports com.fasterxml.jackson.databind.json;
    exports com.fasterxml.jackson.databind.jsonFormatVisitors;
    exports com.fasterxml.jackson.databind.jsontype;
    exports com.fasterxml.jackson.databind.jsontype.impl;
    exports com.fasterxml.jackson.databind.module;
    exports com.fasterxml.jackson.databind.node;
    exports com.fasterxml.jackson.databind.ser;
    exports com.fasterxml.jackson.databind.ser.impl;
    exports com.fasterxml.jackson.databind.ser.std;
    exports com.fasterxml.jackson.databind.type;
    exports com.fasterxml.jackson.databind.util;

    // [databind#2485]: prevent warning for "unused" with self-use
    uses com.fasterxml.jackson.databind.Module;

    provides com.fasterxml.jackson.databind.ObjectMapper with
        com.fasterxml.jackson.databind.json.JsonMapper;
}
