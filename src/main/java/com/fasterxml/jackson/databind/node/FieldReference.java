/**
 *  Rogue Wave Software, Inc. Copyright (C) 2000-2013, All rights reserved
 *
 *  This  software is the confidential and proprietary information of SOA Software, Inc.
 *  and is subject to copyright protection under laws of the United States of America and
 *  other countries. The  use of this software should be in accordance with the license
 *  agreement terms you entered into with SOA Software, Inc.
 */
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
