package com.fasterxml.jackson.databind.exc;


/**
 * Exception used when flagging a Matcher that will allow all subtypes of Object.class or Serializable.class
 * As allowing such a wide array of classes to be deserialized will open the application up to security vulnerabilities
 * and so should be avoided.
 */
public class InsecureTypeMatchException extends IllegalArgumentException {

    public InsecureTypeMatchException(String msg) {
        super(msg);
    }
}
