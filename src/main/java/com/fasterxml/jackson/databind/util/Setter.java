package com.fasterxml.jackson.databind.util;

/**
 * Mix-in interface exposed by things that expose a single
 * property whose value can be set.
 * 
 * @since 2.0
 */
public interface Setter
{
    public void setValue(Object pojo, Object value);
}
