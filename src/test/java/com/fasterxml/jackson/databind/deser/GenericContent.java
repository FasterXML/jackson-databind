package com.fasterxml.jackson.databind.deser;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonInclude(Include.NON_EMPTY)
public class GenericContent {

    private Collection innerObjects;

    public Collection getInnerObjects() {
        return innerObjects;
    }

    public void setInnerObjects(Collection innerObjects) {
        this.innerObjects = innerObjects;
    }

}