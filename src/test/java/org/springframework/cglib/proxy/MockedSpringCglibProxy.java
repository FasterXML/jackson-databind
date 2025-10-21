package org.springframework.cglib.proxy;

/**
 * A mock Proxy object used to test writing a Springframework cglib proxy object.
 * @author Rob Winch
 */
public class MockedSpringCglibProxy {

    private final String propertyName;

    public MockedSpringCglibProxy(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public Callback[] getCallbacks() {
        return new Callback[] { new Callback(), new Callback() };
    }
}
