package tools.jackson.databind.ext.sql;

import java.util.HashMap;
import java.util.Map;

import tools.jackson.databind.*;
import tools.jackson.databind.ser.jdk.JavaUtilDateSerializer;
import tools.jackson.databind.util.ClassUtil;

/**
 * Helper class used for isolating details of handling optional+external types
 * (java.sql classes) from standard factories that offer them.
 */
public class JavaSqlTypeHandlerFactory
{
    public final static JavaSqlTypeHandlerFactory instance = new JavaSqlTypeHandlerFactory();

    // classes from java.sql module, this module may or may not be present at runtime
    // (is included on Java 8, but not part of JDK core for Java 9 and beyond)
    private final Map<String, String> _sqlDeserializers;
    private final Map<String, Object> _sqlSerializers;

    private final static String CLS_NAME_JAVA_SQL_TIMESTAMP = "java.sql.Timestamp";
    private final static String CLS_NAME_JAVA_SQL_DATE = "java.sql.Date";
    private final static String CLS_NAME_JAVA_SQL_TIME = "java.sql.Time";
    private final static String CLS_NAME_JAVA_SQL_BLOB = "java.sql.Blob";
    private final static String CLS_NAME_JAVA_SQL_SERIALBLOB = "javax.sql.rowset.serial.SerialBlob";

    protected JavaSqlTypeHandlerFactory() {
        _sqlDeserializers = new HashMap<>();
        _sqlDeserializers.put(CLS_NAME_JAVA_SQL_DATE,
                "tools.jackson.databind.ext.sql.JavaSqlDateDeserializer");
        _sqlDeserializers.put(CLS_NAME_JAVA_SQL_TIMESTAMP,
                "tools.jackson.databind.ext.sql.JavaSqlTimestampDeserializer");
        // 09-Nov-2020, tatu: No deserializer for `java.sql.Blob` yet; would require additional
        //    dependency and not yet requested by anyone. Add if requested

        _sqlSerializers = new HashMap<>();
        // 09-Jan-2015, tatu: As per [databind#1073], let's try to guard against possibility
        //   of some environments missing `java.sql.` types

        // note: timestamps are very similar to java.util.Date, thus serialized as such
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_TIMESTAMP, JavaUtilDateSerializer.instance);
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_DATE, "tools.jackson.databind.ext.sql.JavaSqlDateSerializer");
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_TIME, "tools.jackson.databind.ext.sql.JavaSqlTimeSerializer");

        // 09-Nov-2020, tatu: Not really optimal way to deal with these, problem  being that
        //   Blob is interface and actual instance we get is usually different. So may
        //   need to improve if we reported bugs. But for now, do this

        _sqlSerializers.put(CLS_NAME_JAVA_SQL_BLOB, "tools.jackson.databind.ext.sql.JavaSqlBlobSerializer");
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_SERIALBLOB, "tools.jackson.databind.ext.sql.JavaSqlBlobSerializer");
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type)
    {
        final Class<?> rawType = type.getRawClass();

        String className = rawType.getName();
        Object sqlHandler = _sqlSerializers.get(className);

        if (sqlHandler != null) {
            if (sqlHandler instanceof ValueSerializer<?>) {
                return (ValueSerializer<?>) sqlHandler;
            }
            // must be class name otherwise
            return (ValueSerializer<?>) instantiate((String) sqlHandler, type);
        }
        return null;
    }

    public ValueDeserializer<?> findDeserializer(DeserializationConfig config, JavaType type)
    {
        final Class<?> rawType = type.getRawClass();
        String className = rawType.getName();
        final String deserName = _sqlDeserializers.get(className);
        if (deserName != null) {
            return (ValueDeserializer<?>) instantiate(deserName, type);
        }
        return null;
    }

    public boolean hasDeserializerFor(Class<?> valueType) {
        String className = valueType.getName();

        // 06-Nov-2020, tatu: One of "java.sql" types?
        return _sqlDeserializers.containsKey(className);
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    private Object instantiate(String className, JavaType valueType)
    {
        try {
            return instantiate(Class.forName(className), valueType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to find class `"
+className+"` for handling values of type "+ClassUtil.getTypeDescription(valueType)
+", problem: ("+e.getClass().getName()+") "+e.getMessage());
        }
    }

    private Object instantiate(Class<?> handlerClass, JavaType valueType)
    {
        try {
            return ClassUtil.createInstance(handlerClass, false);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create instance of `"
+handlerClass.getName()+"` for handling values of type "+ClassUtil.getTypeDescription(valueType)
+", problem: ("+e.getClass().getName()+") "+e.getMessage());
        }
    }
}
