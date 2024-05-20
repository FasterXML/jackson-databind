package com.fasterxml.jackson.databind.jdk14;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.PotentialCreator;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.NativeImageUtil;

/**
 * Helper class to support some of JDK 14 (and later) features
 * without Jackson itself being run on (or even built with) Java 14.
 * In particular allows better support of {@code java.lang.Record}
 * types (see <a href="https://openjdk.java.net/jeps/359">JEP 359</a>).
 *
 * @since 2.12
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

    @Deprecated // since 2.18
    public static AnnotatedConstructor findRecordConstructor(DeserializationContext ctxt,
            BeanDescription beanDesc, List<String> names) {
        return findRecordConstructor(beanDesc.getClassInfo(), ctxt.getAnnotationIntrospector(), ctxt.getConfig(), names);
    }

    @Deprecated // since 2.18
    public static AnnotatedConstructor findRecordConstructor(AnnotatedClass recordClass,
            AnnotationIntrospector intr, MapperConfig<?> config, List<String> names) {
        return new CreatorLocator(config, recordClass)
            .locate(names);
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

    @Deprecated // since 2.18
    static class RawTypeName {
        public final Class<?> rawType;
        public final String name;

        public RawTypeName(Class<?> rt, String n) {
            rawType = rt;
            name = n;
        }
    }

    @Deprecated // since 2.18
    static class CreatorLocator {
        protected final AnnotatedClass _recordClass;
        protected final MapperConfig<?> _config;
        protected final AnnotationIntrospector _intr;

        protected final List<AnnotatedConstructor> _constructors;
        protected final AnnotatedConstructor _primaryConstructor;
        protected final RawTypeName[] _recordFields;

        CreatorLocator(MapperConfig<?> config, AnnotatedClass recordClass)
        {
            _recordClass = recordClass;

            _intr = config.getAnnotationIntrospector();
            _config = config;

            _recordFields = RecordAccessor.instance().getRecordFields(recordClass.getRawType());
            if (_recordFields == null) {
                // not a record, or no reflective access on native image
                _constructors = recordClass.getConstructors();
                _primaryConstructor = null;
            } else {
                final int argCount = _recordFields.length;

                // And then locate the canonical constructor; must be found, if not, fail
                // altogether (so we can figure out what went wrong)
                AnnotatedConstructor primary = null;

                // One special case: empty Records, empty constructor is separate case
                if (argCount == 0) {
                    primary = recordClass.getDefaultConstructor();
                    _constructors = Collections.singletonList(primary);
                } else {
                    _constructors = recordClass.getConstructors();
                    main_loop:
                    for (AnnotatedConstructor ctor : _constructors) {
                        if (ctor.getParameterCount() != argCount) {
                            continue;
                        }
                        for (int i = 0; i < argCount; ++i) {
                            if (!ctor.getRawParameterType(i).equals(_recordFields[i].rawType)) {
                                continue main_loop;
                            }
                        }
                        primary = ctor;
                        break;
                    }
                }
                if (primary == null) {
                    throw new IllegalArgumentException("Failed to find the canonical Record constructor of type "
                            +ClassUtil.getTypeDescription(_recordClass.getType()));
                }
                _primaryConstructor = primary;
            }
        }

        public AnnotatedConstructor locate(List<String> names)
        {
            // First things first: ensure that either there are no explicit marked constructors
            // or that there is just one and it is the canonical one and it is not
            // declared as "delegating" constructor
            for (AnnotatedConstructor ctor : _constructors) {
                JsonCreator.Mode creatorMode = _intr.findCreatorAnnotation(_config, ctor);
                if ((null == creatorMode) || (Mode.DISABLED == creatorMode)) {
                    continue;
                }
                // If there's a delegating Creator let caller figure out
                if (Mode.DELEGATING == creatorMode) {
                    return null;
                }
                if (ctor != _primaryConstructor) {
                    return null;
                }
            }

            if (_recordFields == null) {
                // not a record, or no reflective access on native image
                return null;
            }

            // By now we have established that the canonical constructor is the one to use
            // and just need to gather implicit names to return
            for (RawTypeName field : _recordFields) {
                names.add(field.name);
            }
            return _primaryConstructor;
        }
    }
}
