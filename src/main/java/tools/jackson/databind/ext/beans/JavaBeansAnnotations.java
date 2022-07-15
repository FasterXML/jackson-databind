package tools.jackson.databind.ext.beans;

import tools.jackson.databind.PropertyName;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.util.ClassUtil;

/**
 * Since 2 JDK7-added annotations were left out of JDK 9+ core modules,
 * moved into "java.beans", support for them will be left as dynamic
 * for Jackson 3.x, and handled via this class
 */
public abstract class JavaBeansAnnotations
{
    private final static JavaBeansAnnotations IMPL;

    static {
        JavaBeansAnnotations impl = null;
        try {
            Class<?> cls = Class.forName("tools.jackson.databind.ext.beans.JavaBeansAnnotationsImpl");
            impl = (JavaBeansAnnotations) ClassUtil.createInstance(cls, false);
        } catch (Throwable t) {
            // 09-Sep-2019, tatu: Used to log earlier, but with 2.10.0 let's not log
//            java.util.logging.Logger.getLogger(Java7Support.class.getName())
//                .warning("Unable to load JDK7 annotations (@ConstructorProperties, @Transient): no Java7 annotation support added");
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
