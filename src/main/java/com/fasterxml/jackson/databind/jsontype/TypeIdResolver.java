package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;

/**
 * Interface that defines standard API for converting types
 * to type identifiers and vice versa. Used by type resolvers
 * ({@link com.fasterxml.jackson.databind.jsontype.TypeSerializer},
 * {@link com.fasterxml.jackson.databind.jsontype.TypeDeserializer}) for converting
 * between type and matching id; id is stored in JSON and needed for
 * creating instances of proper subtypes when deserializing values.
 *<p>
 * NOTE: it is <b>strongly</b> recommended that developers always extend
 * abstract base class {@link com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase}
 * instead of directly implementing this interface; this helps prevent
 * breakage in case new methds need to be added in this interface (something
 * we try to avoid doing; but which may be necessary in some cases).
 */
public interface TypeIdResolver
{
    /*
    /**********************************************************
    /* Initialization/configuration methods
    /**********************************************************
     */

    /**
     * Method that will be called once before any type resolution calls;
     * used to initialize instance with configuration. This is necessary
     * since instances may be created via reflection, without ability to
     * call specific constructor to pass in configuration settings.
     *
     * @param baseType Base type for which this id resolver instance is
     *   used
     */
    public void init(JavaType baseType);

    /*
    /**********************************************************
    /* Conversions between types and type ids
    /**********************************************************
     */

    /**
     * Method called to serialize type of the type of given value
     * as a String to include in serialized JSON content.
     */
    public String idFromValue(Object value);

    /**
     * Alternative method used for determining type from combination of
     * value and type, using suggested type (that serializer provides)
     * and possibly value of that type. Most common implementation will
     * use suggested type as is.
     */
    public String idFromValueAndType(Object value, Class<?> suggestedType);

    /**
     * Method that can be called to figure out type id to use for instances
     * of base type (declared type of property). This is usually only used
     * for fallback handling, for cases where real type information is not
     * available for some reason.
     */
    public String idFromBaseType();

    /**
     * Method called to resolve type from given type identifier.
     *
     * @since 2.5 (throws clause added in 2.8)
     */
    public JavaType typeFromId(DatabindContext context, String id) throws IOException;

    /**
     * Method called for error-reporting and diagnostics purposes.
     *
     * @since 2.7 -- but since 2.5 has existed in {@link com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase}
     */
    public String getDescForKnownTypeIds();

    /*
    /**********************************************************
    /* Accessors for metadata
    /**********************************************************
     */

     /**
      * Accessor for mechanism that this resolver uses for determining
      * type id from type. Mostly informational; not required to be called
      * or used.
      */
     public JsonTypeInfo.Id getMechanism();
}
