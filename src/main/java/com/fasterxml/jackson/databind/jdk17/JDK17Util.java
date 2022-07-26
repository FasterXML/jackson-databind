package com.fasterxml.jackson.databind.jdk17;

import java.lang.reflect.Method;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.NativeImageUtil;

/**
 * Helper class to support some of JDK 17 (and later) features without Jackson itself being run on
 * (or even built with) Java 17. In particular allows better support of sealed class types (see
 * <a href="https://openjdk.java.net/jeps/409">JEP 409</a>).
 *
 * @since 2.14
 */
public class JDK17Util {
  public static Boolean isSealed(Class<?> type) {
    return SealedClassAccessor.instance().isSealed(type);
  }

  public static Class<?>[] getPermittedSubclasses(Class<?> sealedType) {
    return SealedClassAccessor.instance().getPermittedSubclasses(sealedType);
  }

  static class SealedClassAccessor {
    private final Method SEALED_IS_SEALED;
    private final Method SEALED_GET_PERMITTED_SUBCLASSES;

    private final static SealedClassAccessor INSTANCE;
    private final static RuntimeException PROBLEM;

    static {
      RuntimeException prob = null;
      SealedClassAccessor inst = null;
      try {
        inst = new SealedClassAccessor();
      } catch (RuntimeException e) {
        prob = e;
      }
      INSTANCE = inst;
      PROBLEM = prob;
    }

    private SealedClassAccessor() throws RuntimeException {
      try {
        SEALED_IS_SEALED = Class.class.getMethod("isSealed");
        SEALED_GET_PERMITTED_SUBCLASSES = Class.class.getMethod("getPermittedSubclasses");
      } catch (Exception e) {
        throw new RuntimeException(
            String.format("Failed to access Methods needed to support sealed classes: (%s) %s",
                e.getClass().getName(), e.getMessage()),
            e);
      }
    }

    public static SealedClassAccessor instance() {
      if (PROBLEM != null) {
        throw PROBLEM;
      }
      return INSTANCE;
    }

    public Boolean isSealed(Class<?> type) throws IllegalArgumentException {
      try {
        return (Boolean) SEALED_IS_SEALED.invoke(type);
      } catch (Exception e) {
        if (NativeImageUtil.isUnsupportedFeatureError(e)) {
          return null;
        }
        throw new IllegalArgumentException(
            "Failed to access sealedness of type " + ClassUtil.nameOf(type));
      }
    }

    public Class<?>[] getPermittedSubclasses(Class<?> sealedType) throws IllegalArgumentException {
      try {
        return (Class<?>[]) SEALED_GET_PERMITTED_SUBCLASSES.invoke(sealedType);
      } catch (Exception e) {
        if (NativeImageUtil.isUnsupportedFeatureError(e)) {
          return null;
        }
        throw new IllegalArgumentException(
            "Failed to access permitted subclasses of type " + ClassUtil.nameOf(sealedType));
      }
    }
  }
}
