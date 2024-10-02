package com.fasterxml.jackson.databind.util;

public enum NamingStrategy {
  /**
   * beanName -> beanName
   */
  LOWER_CAMEL_CASE {
    @Override
    public String translate(String beanName) {
      return beanName; // beanName is already in lower camel case
    }
  },

  /**
   * beanName -> BeanName
   */
  UPPER_CAMEL_CASE {
    @Override
    public String translate(String beanName) {
      if (beanName == null || beanName.isEmpty()) {
        return beanName; // garbage in, garbage out
      }
      // Replace first lower-case letter with upper-case equivalent
      char c = beanName.charAt(0);
      char uc = Character.toUpperCase(c);
      if (c == uc) {
        return beanName;
      }
      StringBuilder sb = new StringBuilder(beanName);
      sb.setCharAt(0, uc);
      return sb.toString();
    }
  },

  /**
   * beanName -> bean_name
   */
  SNAKE_CASE {
    @Override
    public String translate(String beanName) {
      if (beanName == null) return beanName; // garbage in, garbage out
      int length = beanName.length();
      StringBuilder result = new StringBuilder(length * 2);
      int resultLength = 0;
      boolean wasPrevTranslated = false;
      for (int i = 0; i < length; i++) {
        char c = beanName.charAt(i);
        if (i > 0 || c != '_') // skip first starting underscore
        {
          if (Character.isUpperCase(c)) {
            if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
              result.append('_');
              resultLength++;
            }
            c = Character.toLowerCase(c);
            wasPrevTranslated = true;
          } else {
            wasPrevTranslated = false;
          }
          result.append(c);
          resultLength++;
        }
      }
      return resultLength > 0 ? result.toString() : beanName;
    }
  },

  /**
   * beanName -> BEAN_NAME
   */
  UPPER_SNAKE_CASE {
    @Override
    public String translate(String beanName) {
      String output = SNAKE_CASE.translate(beanName);
      if (output == null) {
        return null;
      }
      return output.toUpperCase();
    }
  },

  /**
   * beanName -> beanname
   */
  LOWER_CASE {
    @Override
    public String translate(String beanName) {
      if (beanName == null || beanName.isEmpty()) {
        return beanName;
      }
      return beanName.toLowerCase();
    }
  },

  /**
   * beanName -> bean-name
   */
  KEBAB_CASE {
    @Override
    public String translate(String beanName) {
      return translateLowerCaseWithSeparator(beanName, '-');
    }
  },

  /**
   * beanName -> bean.name
   */
  LOWER_DOT_CASE {
    @Override
    public String translate(String beanName) {
      return translateLowerCaseWithSeparator(beanName, '.');
    }
  },
  ;

  public abstract String translate(final String beanName);

  /**
   * Helper method to share implementation between snake and dotted case.
   */
  private static String translateLowerCaseWithSeparator(final String beanName, final char separator) {
    if (beanName == null || beanName.isEmpty()) {
      return beanName;
    }

    final int length = beanName.length();
    final StringBuilder result = new StringBuilder(length + (length >> 1));
    int upperCount = 0;
    for (int i = 0; i < length; ++i) {
      char ch = beanName.charAt(i);
      char lc = Character.toLowerCase(ch);

      if (lc == ch) { // lower-case letter means we can get new word
        // but need to check for multi-letter upper-case (acronym), where assumption
        // is that the last upper-case char is start of a new word
        if (upperCount > 1) {
          // so insert hyphen before the last character now
          result.insert(result.length() - 1, separator);
        }
        upperCount = 0;
      } else {
        // Otherwise starts new word, unless beginning of string
        if ((upperCount == 0) && (i > 0)) {
          result.append(separator);
        }
        ++upperCount;
      }
      result.append(lc);
    }
    return result.toString();
  }
}
