package com.fasterxml.jackson.databind.ser;

import java.io.InputStream;
import java.net.URL;

public class JsonTestResource {

    private String resourceName;
    private ClassLoader classLoader;

    public JsonTestResource(String resourceName) {
        this(resourceName, JsonTestResource.class.getClassLoader());
    }

    public JsonTestResource(String resourceName, ClassLoader classLoader) {
        this.resourceName = fix(resourceName);
        this.classLoader = classLoader;
    }
    
    private static String fix(String resourceName) {
        if (resourceName.startsWith("/")) {
            return resourceName.substring(1);
        }
        return resourceName;
    }

    public URL getURL() {
        return classLoader.getResource(resourceName);
    }
    
    public InputStream getInputStream() {
        return classLoader.getResourceAsStream(resourceName);
    }
    
    @Override
    public String toString() {
        URL url = getURL();
        String content;
        if (url == null) {
            content = "resourceName=" + resourceName + " (not on classpath)";
        } else {
            content = "url=" + url;
        }
        
        return getClass().getSimpleName() + "(" + content + ")";
    }
}
