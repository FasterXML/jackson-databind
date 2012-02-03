package com.fasterxml.jackson.databind.util;

/**
 * Mix-in interface exposed by things that expose a single
 * property whose value can be read.
 * 
 * @since 2.0
 */
public interface Getter
{
    public Object getValue(Object pojo);
}
