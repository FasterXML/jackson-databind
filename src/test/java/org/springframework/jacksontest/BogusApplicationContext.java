package org.springframework.jacksontest;

//Made-up test class that should trigger checks for [databind#1855]
public class BogusApplicationContext extends AbstractApplicationContext {
    public BogusApplicationContext(String s) {
        super();
        throw new Error("Wrong!");
    }
}
