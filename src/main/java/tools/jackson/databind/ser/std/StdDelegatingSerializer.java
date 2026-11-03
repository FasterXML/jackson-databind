package tools.jackson.databind.ser.std;

import tools.jackson.databind.*;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.Converter;

/**
 * Older incorrectly named (in 3.0) variant of {@link StdConvertingSerializer}.
 *
 * @deprecated Since 3.1 should use correctly named {@link StdConvertingSerializer} instead.
 */
@Deprecated
public class StdDelegatingSerializer
    extends StdConvertingSerializer
{
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    @Deprecated
    public StdDelegatingSerializer(Converter<?,?> converter) {
        super(converter);
    }

    @Deprecated
    public <T> StdDelegatingSerializer(Class<T> cls, Converter<T,?> converter) {
        super(cls, converter);
    }

    @Deprecated
    public StdDelegatingSerializer(Converter<Object,?> converter,
            JavaType delegateType, ValueSerializer<?> delegateSerializer,
            BeanProperty prop)
    {
        super(converter, delegateType, delegateSerializer, prop);
    }

    @Deprecated
    @Override
    protected StdDelegatingSerializer withDelegate(Converter<Object,?> converter,
            JavaType delegateType, ValueSerializer<?> delegateSerializer,
            BeanProperty prop)
    {
        ClassUtil.verifyMustOverride(StdDelegatingSerializer.class, this, "withDelegate");
        return new StdDelegatingSerializer(converter, delegateType, delegateSerializer, prop);
    }
}
