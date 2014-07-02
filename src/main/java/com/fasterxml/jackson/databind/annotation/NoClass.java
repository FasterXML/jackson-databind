package com.fasterxml.jackson.databind.annotation;

/**
 * Marker class used with annotations to indicate "no class". This is
 * a silly but necessary work-around -- annotations can not take nulls
 * as either default or explicit values. Hence for class values we must
 * explicitly use a bogus placeholder to denote equivalent of
 * "no class" (for which 'null' is usually the natural choice).
 * 
 * @deprecated Since 2.4 use {@link java.lang.Void} instead as the general
 *   "no class specified" marker.
 */
@Deprecated
public final class NoClass
{
    private NoClass() { }
}
