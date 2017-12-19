package org.springframework.jacksontest;

// Made-up test class that should trigger checks for [databind#1855]
public class BogusPointcutAdvisor extends AbstractPointcutAdvisor {
    public BogusPointcutAdvisor(String s) {
        super();
        throw new Error("Wrong!");
    }
}
