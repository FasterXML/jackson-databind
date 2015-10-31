package com.fasterxml.jackson.databind;

enum JavaVersion {

    EIGHT("java.util.function.Function");

    private final boolean available;

    /**
     * @param providedClassName className required to be available in this java version
     */
    JavaVersion(String providedClassName) {

        boolean available;
        try {
            getClass().getClassLoader().loadClass("java.util.function.Function");
            available = true;
        } catch (ClassNotFoundException e) {
            available =  false;
        }

        this.available = available;
    }

    boolean isAvailable() {
        return available;
    }
}
