package com.fasterxml.jackson.databind.node;

/**
 * @author tgullotta
 *FieldReference to store fieldName and ObjectNode r
 */
public class FieldReference {

    ObjectNode o;
    String fieldName;

    public FieldReference(ObjectNode o, String fieldName) {
        this.o = o;
        this.fieldName = fieldName;
    }

    public void setObject(ObjectNode o) {
        this.o = o;
    }

    public ObjectNode getObject() {
        return this.o;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

}
