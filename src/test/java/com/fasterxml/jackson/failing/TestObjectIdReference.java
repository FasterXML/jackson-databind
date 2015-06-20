package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestObjectIdReference extends BaseMapTest {

    static class Player {

        private int id;
        private String name;
        private List<Translation> translations = new ArrayList<Translation>();

        public Player(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public Player(){}

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void addTranslation(Translation translation) {
            this.translations.add(translation);
        }


    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class Translation {

        private Lang languageId;
        private String translation;

        public Translation(String translation, Lang languageId){
            this.translation = translation;
            this.languageId = languageId;
        }

        public Translation(){}

        public Lang getLanguageId() {
            return languageId;
        }

        public String getTranslation() {
            return translation;
        }

    }

    static interface Lang{}

    static class LanguageOk implements Lang{

        private int value;

        private String kod;

        public LanguageOk(int value, String kod){
            this.value = value;
            this.kod = kod;
        }

        public LanguageOk(){}

        public int getValue(){
            return this.value;
        }

        public String getKod(){
            return this.kod;
        }

    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="value")
    static class LanguageId implements Lang {

        private Key value;

        private String kod;

        public LanguageId(Key value, String kod){
            this.value = value;
            this.kod = kod;
        }

        public LanguageId(){}

        public Key getValue(){
            return this.value;
        }

        public String getKod(){
            return this.kod;
        }

    }

    static class Key {

        public int val;

        public Key(int val){
            this.val = val;
        }

        public Key(){}

        public int getVal(){
            return this.val;
        }

    }


    private final ObjectMapper mapper = new ObjectMapper();

    public void testDeserializationOfReferenceOK() throws IOException {

        Player player = new Player(1, "Dino");

        LanguageOk languageId = new LanguageOk(55, "kod");

        Translation commonTranslation = new Translation("DinoT1", languageId);
        Translation commonTranslation2 = new Translation("DinoT2", languageId);

        player.addTranslation(commonTranslation);
        player.addTranslation(commonTranslation);
        player.addTranslation(commonTranslation2);

        Player player2 = new Player(2, "Bojan");
        player2.addTranslation(commonTranslation);
        player2.addTranslation(commonTranslation2);

        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String serialized = mapper.writeValueAsString(player);
        String serialized2 = mapper.writeValueAsString(player2);

        //System.out.println(serialized);
        //System.out.println(serialized2);


        Player p1 = mapper.readValue(serialized, Player.class);
        Player p2 = mapper.readValue(serialized2, Player.class);
    }


    public void testDeserializationOfReference() throws IOException {

        Player player = new Player(1, "Dino");

        LanguageId languageId = new LanguageId(new Key(44), "kod");

        Translation commonTranslation = new Translation("DinoT1", languageId);
        Translation commonTranslation2 = new Translation("DinoT2", languageId);

        player.addTranslation(commonTranslation);
        player.addTranslation(commonTranslation);
        player.addTranslation(commonTranslation2);

        Player player2 = new Player(2, "Bojan");
        player2.addTranslation(commonTranslation);
        player2.addTranslation(commonTranslation2);

        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        String serialized = mapper.writeValueAsString(player);
        String serialized2 = mapper.writeValueAsString(player2);

        System.out.println(serialized);
        System.out.println(serialized2);

        Player p1 = mapper.readValue(serialized, Player.class);
        Player p2 = mapper.readValue(serialized2, Player.class);

    }

}
