package tools.jackson.databind.ser.std;

import tools.jackson.databind.*;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.Converter;

/**
 * Serializer implementation where given Java type is first converted
 * to an intermediate "delegate type" (using a configured
 * {@link Converter}, and then this delegate value is serialized by Jackson.
 *<p>
 * Note that although types may be related, they must not be same; trying
 * to do this will result in an exception.
 *
 * @deprecated Since 3.1 should use correctly named {@link StdConvertingSerializer}
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
