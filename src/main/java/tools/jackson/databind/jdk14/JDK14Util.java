package tools.jackson.databind.jdk14;

import java.lang.reflect.Method;
import java.util.List;

import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedConstructor;
import tools.jackson.databind.introspect.PotentialCreator;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.NativeImageUtil;

/**
 * Helper class to support some of JDK 14 (and later) features
 * without Jackson itself being run on (or even built with) Java 14.
 * In particular allows better support of {@code java.lang.Record}
 * types (see <a href="https://openjdk.java.net/jeps/359">JEP 359</a>).
 */
public class JDK14Util
{
    public static String[] getRecordFieldNames(Class<?> recordType) {
        return RecordAccessor.instance().getRecordFieldNames(recordType);
    }

    /**
     * @since 2.18
     */
    public static PotentialCreator findCanonicalRecordConstructor(MapperConfig<?> config,
            AnnotatedClass recordClass,
            List<PotentialCreator> constructors)
    {
        final RawTypeName[] recordFields = RecordAccessor.instance().getRecordFields(recordClass.getRawType());

        if (recordFields == null) {
            // not a record, or no reflective access on native image
            return null;
        }

        // And then locate the canonical constructor
        final int argCount = recordFields.length;
        // One special case: zero-arg constructor not included in candidate List
        if (argCount == 0) {
            // Bit hacky but has to do: create new PotentialCreator let caller deal
            AnnotatedConstructor defCtor = recordClass.getDefaultConstructor();
            if (defCtor != null) {
                return new PotentialCreator(defCtor, null);
            }
        }

        main_loop:
        for (PotentialCreator ctor : constructors) {
            if (ctor.paramCount() != argCount) {
                continue;
            }
            for (int i = 0; i < argCount; ++i) {
                if (!ctor.creator().getRawParameterType(i).equals(recordFields[i].rawType)) {
                    continue main_loop;
                }
            }
            // Found it! One more thing; get implicit Record field names:
            final PropertyName[] implicits = new PropertyName[argCount];
            for (int i = 0; i < argCount; ++i) {
                implicits[i] = PropertyName.construct(recordFields[i].name);
            }
            return ctor.introspectParamNames(config, implicits);
        }

        throw new IllegalArgumentException("Failed to find the canonical Record constructor of type "
                        +ClassUtil.getTypeDescription(recordClass.getType()));
    }

    static class RecordAccessor {
        private final Method RECORD_GET_RECORD_COMPONENTS;
        private final Method RECORD_COMPONENT_GET_NAME;
        private final Method RECORD_COMPONENT_GET_TYPE;

        private final static RecordAccessor INSTANCE;
        private final static RuntimeException PROBLEM;

        static {
            RuntimeException prob = null;
            RecordAccessor inst = null;
            try {
                inst = new RecordAccessor();
            } catch (RuntimeException e) {
                prob = e;
            }
            INSTANCE = inst;
            PROBLEM = prob;
        }

        private RecordAccessor() throws RuntimeException {
            try {
                RECORD_GET_RECORD_COMPONENTS = Class.class.getMethod("getRecordComponents");
                Class<?> c = Class.forName("java.lang.reflect.RecordComponent");
                RECORD_COMPONENT_GET_NAME = c.getMethod("getName");
                RECORD_COMPONENT_GET_TYPE = c.getMethod("getType");
            } catch (Exception e) {
                throw new RuntimeException(String.format(
"Failed to access Methods needed to support `java.lang.Record`: (%s) %s",
e.getClass().getName(), e.getMessage()), e);
            }
        }

        public static RecordAccessor instance() {
            if (PROBLEM != null) {
                throw PROBLEM;
            }
            return INSTANCE;
        }

        public String[] getRecordFieldNames(Class<?> recordType) throws IllegalArgumentException
        {
            final Object[] components = recordComponents(recordType);
            if (components == null) {
                // not a record, or no reflective access on native image
                return null;
            }
            final String[] names = new String[components.length];
            for (int i = 0; i < components.length; i++) {
                try {
                    names[i] = (String) RECORD_COMPONENT_GET_NAME.invoke(components[i]);
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format(
"Failed to access name of field #%d (of %d) of Record type %s",
i, components.length, ClassUtil.nameOf(recordType)), e);
                }
            }
            return names;
        }

        public RawTypeName[] getRecordFields(Class<?> recordType) throws IllegalArgumentException
        {
            final Object[] components = recordComponents(recordType);
            if (components == null) {
                // not a record, or no reflective access on native image
                return null;
            }
            final RawTypeName[] results = new RawTypeName[components.length];
            for (int i = 0; i < components.length; i++) {
                String name;
                try {
                    name = (String) RECORD_COMPONENT_GET_NAME.invoke(components[i]);
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format(
"Failed to access name of field #%d (of %d) of Record type %s",
i, components.length, ClassUtil.nameOf(recordType)), e);
                }
                Class<?> type;
                try {
                    type = (Class<?>) RECORD_COMPONENT_GET_TYPE.invoke(components[i]);
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format(
"Failed to access type of field #%d (of %d) of Record type %s",
i, components.length, ClassUtil.nameOf(recordType)), e);
                }
                results[i] = new RawTypeName(type, name);
            }
            return results;
        }

        protected Object[] recordComponents(Class<?> recordType) throws IllegalArgumentException
        {
            try {
                return (Object[]) RECORD_GET_RECORD_COMPONENTS.invoke(recordType);
            } catch (Exception e) {
                if (NativeImageUtil.isUnsupportedFeatureError(e)) {
                    return null;
                }
                throw new IllegalArgumentException("Failed to access RecordComponents of type "
                        +ClassUtil.nameOf(recordType));
            }
        }

    }

    static class RawTypeName {
        public final Class<?> rawType;
        public final String name;

        public RawTypeName(Class<?> rt, String n) {
            rawType = rt;
            name = n;
        }
    }
}
