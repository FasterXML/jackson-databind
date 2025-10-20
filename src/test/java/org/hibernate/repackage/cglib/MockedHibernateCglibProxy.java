package org.hibernate.repackage.cglib;

/**
 * A mock Proxy object used to test writing a Hibernate cglib proxy object.
 * @author Rob Winch
 */
public class MockedHibernateCglibProxy {

    private final String propertyName;

    public MockedHibernateCglibProxy(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public Callback[] getCallbacks() {
        return new Callback[] { new Callback(), new Callback() };
    }
}
