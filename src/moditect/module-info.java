// Generated 08-Mar-2019 using Moditect maven plugin
module com.fasterxml.jackson.databind {
    // required for
    // java.beans.ConstructorProperties
    // java.beans.Transient
    // support
    requires static java.desktop;

    requires transitive com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.core;
    // these types were suggested as transitive, but aren't actually
    // exposed externally (only within internal APIs)
    requires static java.sql;
    requires static java.xml;

    exports com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.databind.annotation;
    exports com.fasterxml.jackson.databind.cfg;
    exports com.fasterxml.jackson.databind.deser;
    exports com.fasterxml.jackson.databind.deser.impl;
    exports com.fasterxml.jackson.databind.deser.std;
    exports com.fasterxml.jackson.databind.exc;
    exports com.fasterxml.jackson.databind.ext;
    exports com.fasterxml.jackson.databind.introspect;
    exports com.fasterxml.jackson.databind.json;
    exports com.fasterxml.jackson.databind.jsonFormatVisitors;
    exports com.fasterxml.jackson.databind.jsonschema;
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

    provides com.fasterxml.jackson.core.ObjectCodec with
        com.fasterxml.jackson.databind.ObjectMapper;
}
