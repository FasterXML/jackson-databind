package com.fasterxml.jackson.databind.deser.creators.jdk8;

class Person {

    // mandatory fields
    private final String name;
    private final String surname;

    // optional fields
    private String nickname;

    // no annotations are required if preconditions are met (details below)
    public Person(String name, String surname) {

        this.name = name;
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getNickname() {

        return nickname;
    }

    public void setNickname(String nickname) {

        this.nickname = nickname;
    }
}