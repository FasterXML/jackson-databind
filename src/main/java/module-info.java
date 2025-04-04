// Jackson 3.x module-info for Main artifact
module tools.jackson.databind
{
    // required for
    // - `java.beans.ConstructorProperties`
    // - `java.beans.Transient`
    // support
    requires static java.desktop;

    // these types were suggested as transitive, but aren't actually
    // exposed externally (only within internal APIs)
    requires static java.sql;
    requires static java.sql.rowset;

    // 05-Nov-2020, tatu: made optional in 2.x ("static") but for simplicity
    //    is (for now?) hard dep on 3.0.
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
    // Alas multiple types from this package are exported. Would
    // ideally move, but for now expose
    exports tools.jackson.databind.deser.impl;
    exports tools.jackson.databind.deser.jackson;
    exports tools.jackson.databind.deser.jdk;
    exports tools.jackson.databind.deser.std;
    exports tools.jackson.databind.exc;
    exports tools.jackson.databind.ext;
    exports tools.jackson.databind.ext.javatime;
    exports tools.jackson.databind.ext.javatime.deser;
    exports tools.jackson.databind.ext.javatime.deser.key;
    exports tools.jackson.databind.ext.javatime.ser;
    exports tools.jackson.databind.ext.javatime.ser.key;
    exports tools.jackson.databind.ext.jdk8;
    // Needed by Ion module for SqlDate deserializer:
    exports tools.jackson.databind.ext.sql;
    exports tools.jackson.databind.introspect;
    exports tools.jackson.databind.json;
    exports tools.jackson.databind.jsonFormatVisitors;
    exports tools.jackson.databind.jsontype;
    exports tools.jackson.databind.jsontype.impl;
    exports tools.jackson.databind.module;
    exports tools.jackson.databind.node;
    exports tools.jackson.databind.ser;
    exports tools.jackson.databind.ser.bean;
    // 11-Jan-2025, tatu: Needed by XML module, alas:
    exports tools.jackson.databind.ser.impl;
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
