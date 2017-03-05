module jackson.databind {
    requires jackson.annotations;
    requires jackson.core;
    requires java.desktop;
    requires java.sql;
    requires java.xml;

    exports com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.databind.annotation;
    exports com.fasterxml.jackson.databind.cfg;
    exports com.fasterxml.jackson.databind.deser;
    exports com.fasterxml.jackson.databind.deser.std;
    exports com.fasterxml.jackson.databind.exc;
    exports com.fasterxml.jackson.databind.ext;
    exports com.fasterxml.jackson.databind.introspect;
    exports com.fasterxml.jackson.databind.jsonFormatVisitors;
    exports com.fasterxml.jackson.databind.jsonschema;
    exports com.fasterxml.jackson.databind.jsontype;
    exports com.fasterxml.jackson.databind.module;
    exports com.fasterxml.jackson.databind.node;
    exports com.fasterxml.jackson.databind.ser;
    exports com.fasterxml.jackson.databind.ser.std;
    exports com.fasterxml.jackson.databind.type;
    exports com.fasterxml.jackson.databind.util;
}