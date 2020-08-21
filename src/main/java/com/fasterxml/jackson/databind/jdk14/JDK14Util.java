package com.fasterxml.jackson.databind.jdk14;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.util.ClassUtil;

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

    public static AnnotatedConstructor findRecordConstructor(DeserializationContext ctxt,
            BeanDescription beanDesc, List<String> names) {
        return new CreatorLocator(ctxt, beanDesc)
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

    static class CreatorLocator {
        protected final BeanDescription _beanDesc;
        protected final DeserializationConfig _config;
        protected final AnnotationIntrospector _intr;

        protected final List<AnnotatedConstructor> _constructors;
        protected final AnnotatedConstructor _primaryConstructor;
        protected final RawTypeName[] _recordFields;

        CreatorLocator(DeserializationContext ctxt, BeanDescription beanDesc)
        {
            _beanDesc = beanDesc;

            _intr = ctxt.getAnnotationIntrospector();
            _config = ctxt.getConfig();

            _recordFields = RecordAccessor.instance().getRecordFields(beanDesc.getBeanClass());
            final int argCount = _recordFields.length;

            // And then locate the canonical constructor; must be found, if not, fail
            // altogether (so we can figure out what went wrong)
            AnnotatedConstructor primary = null;

            // One special case: empty Records, empty constructor is separate case
            if (argCount == 0) {
                primary = beanDesc.findDefaultConstructor();
                _constructors = Collections.singletonList(primary);
            } else {
                _constructors = beanDesc.getConstructors();
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
                        +ClassUtil.getTypeDescription(_beanDesc.getType()));
            }
            _primaryConstructor = primary;
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

            // By now we have established that the canonical constructor is the one to use
            // and just need to gather implicit names to return
            for (RawTypeName field : _recordFields) {
                names.add(field.name);
            }
            return _primaryConstructor;
        }
    }
}
