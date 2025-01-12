package tools.jackson.databind.ext.sql;

import tools.jackson.databind.*;
import tools.jackson.databind.ser.jdk.JavaUtilDateSerializer;

/**
 * Helper class used for isolating details of handling optional+external types
 * (java.sql classes) from standard factories that offer them.
 */
public class JavaSqlTypeHandlerFactory
{
    public final static JavaSqlTypeHandlerFactory instance = new JavaSqlTypeHandlerFactory();

    private final static String CLS_NAME_JAVA_SQL_TIMESTAMP = "java.sql.Timestamp";
    private final static String CLS_NAME_JAVA_SQL_DATE = "java.sql.Date";
    private final static String CLS_NAME_JAVA_SQL_TIME = "java.sql.Time";
    private final static String CLS_NAME_JAVA_SQL_BLOB = "java.sql.Blob";
    private final static String CLS_NAME_JAVA_SQL_SERIALBLOB = "javax.sql.rowset.serial.SerialBlob";

    protected JavaSqlTypeHandlerFactory() { }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type)
    {
        switch (type.getRawClass().getName()) {
        case CLS_NAME_JAVA_SQL_TIMESTAMP:
            return JavaUtilDateSerializer.instance;
        case CLS_NAME_JAVA_SQL_DATE:
            return JavaSqlDateSerializer.instance;
        case CLS_NAME_JAVA_SQL_TIME:
            return JavaSqlTimeSerializer.instance;

        // 09-Nov-2020, tatu: Not really optimal way to deal with these, problem  being that
        //   Blob is interface and actual instance we get is usually different. So may
        //   need to improve if we reported bugs. But for now, do this

        case CLS_NAME_JAVA_SQL_BLOB:
            return JavaSqlBlobSerializer.instance;
        case CLS_NAME_JAVA_SQL_SERIALBLOB:
            return JavaSqlBlobSerializer.instance;
        }

        return null;
    }

    public ValueDeserializer<?> findDeserializer(DeserializationConfig config, JavaType type)
    {
        switch (type.getRawClass().getName()) {
        case CLS_NAME_JAVA_SQL_DATE:
            return JavaSqlDateDeserializer.instance;
        case CLS_NAME_JAVA_SQL_TIMESTAMP:
            return JavaSqlTimestampDeserializer.instance;
        }
        return null;
    }

    public boolean hasDeserializerFor(Class<?> valueType) {
        switch (valueType.getName()) {
        case CLS_NAME_JAVA_SQL_DATE:
        case CLS_NAME_JAVA_SQL_TIMESTAMP:
            return true;
        }
        return false;
    }
}
