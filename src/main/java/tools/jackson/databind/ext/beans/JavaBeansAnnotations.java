package tools.jackson.databind.ext.beans;

import tools.jackson.databind.PropertyName;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedParameter;

/**
 * Since 2 JDK7-added annotations were left out of JDK 9+ core modules,
 * moved into "java.beans" (module {@code java.desktop}), support for them
 * will be left as dynamic for Jackson 3.x, and handled via this class
 */
public abstract class JavaBeansAnnotations
{
    private final static JavaBeansAnnotations IMPL;

    static {
        JavaBeansAnnotations impl = null;
        try {
            impl = JavaBeansAnnotationsImpl.instance;
        } catch (IllegalAccessError e) {
            // [databind#4078]: make some jdk modules (such as java.desktop) optional, again.
            // no-op
        } catch (Throwable t) {
            // 09-Sep-2019, tatu: Used to log earlier, but with 2.10 let's not log
//            java.util.logging.Logger.getLogger(JavaBeansAnnotations.class.getName())
//                .warning("Unable to load JDK7 annotations (@ConstructorProperties, @Transient): no Java7 annotation support added");
//            ExceptionUtil.rethrowIfFatal(t);
        }
        IMPL = impl;
    }

    public static JavaBeansAnnotations instance() {
        return IMPL;
    }

    public abstract Boolean findTransient(Annotated a);

    public abstract Boolean hasCreatorAnnotation(Annotated a);

    public abstract PropertyName findConstructorName(AnnotatedParameter p);
}
