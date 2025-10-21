package net.sf.cglib;

/**
 * A mock Proxy object used to test writing a cglib proxy object.
 * @author Rob Winch
 */
public class MockedNetCglibProxy {

    private final String propertyName;

    public MockedNetCglibProxy(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public Callback[] getCallbacks() {
        return new Callback[] { new Callback(), new Callback() };
    }
}
