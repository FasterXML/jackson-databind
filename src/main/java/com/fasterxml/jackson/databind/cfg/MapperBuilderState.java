package com.fasterxml.jackson.databind.cfg;

/**
 * Interface for State object used for preserving initial state of a
 * {@link MapperBuilder} before modules are configured and resulting
 * {@link com.fasterxml.jackson.databind.ObjectMapper} isn't constructed.
 * It is passed to mapper to allow "re-building" via newly created builder.
 */
public abstract class MapperBuilderState {

}
