// Generated 14-Mar-2019 using Moditect maven plugin
module tools.jackson.databind {
    // required for
    // java.beans.ConstructorProperties
    // java.beans.Transient
    // support
    requires static java.desktop;

    // these types were suggested as transitive, but aren't actually
    // exposed externally (only within internal APIs)
    requires static java.sql;

    // 05-Nov-2020, tatu: made optional in 2.x ("static") but for simplicity
    //    is (for now?) hard dep on 3.0. May want to change
    requires java.xml;

    // but we probably do want to expose streaming, annotations
    // as transitive dependencies streaming types at least part of API
    requires transitive com.fasterxml.jackson.annotation;

    requires transitive tools.jackson.core;

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
