package com.fasterxml.jackson.databind.struct;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PojoAsArrayRoundtripTest extends BaseMapTest
{
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"content", "images"})
    static class MediaItemAsArray
    {
        public enum Player { JAVA, FLASH;  }
        public enum Size { SMALL, LARGE; }

        private List<Photo> _photos;
        private Content _content;

        public MediaItemAsArray() { }

        public MediaItemAsArray(Content c)
        {
            _content = c;
        }

        public void addPhoto(Photo p) {
            if (_photos == null) {
                _photos = new ArrayList<Photo>();
            }
            _photos.add(p);
        }
        
        public List<Photo> getImages() { return _photos; }
        public void setImages(List<Photo> p) { _photos = p; }

        public Content getContent() { return _content; }
        public void setContent(Content c) { _content = c; }

        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({"uri","title","width","height","size"})
        static class Photo
        {
            private String _uri;
            private String _title;
            private int _width;
            private int _height;
            private Size _size;
        
            public Photo() {}
            public Photo(String uri, String title, int w, int h, Size s)
            {
              _uri = uri;
              _title = title;
              _width = w;
              _height = h;
              _size = s;
            }
        
          public String getUri() { return _uri; }
          public String getTitle() { return _title; }
          public int getWidth() { return _width; }
          public int getHeight() { return _height; }
          public Size getSize() { return _size; }
        
          public void setUri(String u) { _uri = u; }
          public void setTitle(String t) { _title = t; }
          public void setWidth(int w) { _width = w; }
          public void setHeight(int h) { _height = h; }
          public void setSize(Size s) { _size = s; }
        }
          
        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({"uri","title","width","height","format","duration","size","bitrate","persons","player","copyright"})
        public static class Content
        {
            private Player _player;
            private String _uri;
            private String _title;
            private int _width;
            private int _height;
            private String _format;
            private long _duration;
            private long _size;
            private int _bitrate;
            private List<String> _persons;
            private String _copyright;
        
            public Content() { }

            public void addPerson(String p) {
                if (_persons == null) {
                    _persons = new ArrayList<String>();
                }
                _persons.add(p);
            }
            
            public Player getPlayer() { return _player; }
            public String getUri() { return _uri; }
            public String getTitle() { return _title; }
            public int getWidth() { return _width; }
            public int getHeight() { return _height; }
            public String getFormat() { return _format; }
            public long getDuration() { return _duration; }
            public long getSize() { return _size; }
            public int getBitrate() { return _bitrate; }
            public List<String> getPersons() { return _persons; }
            public String getCopyright() { return _copyright; }
        
            public void setPlayer(Player p) { _player = p; }
            public void setUri(String u) {  _uri = u; }
            public void setTitle(String t) {  _title = t; }
            public void setWidth(int w) {  _width = w; }
            public void setHeight(int h) {  _height = h; }
            public void setFormat(String f) {  _format = f;  }
            public void setDuration(long d) {  _duration = d; }
            public void setSize(long s) {  _size = s; }
            public void setBitrate(int b) {  _bitrate = b; }
            public void setPersons(List<String> p) {  _persons = p; }
            public void setCopyright(String c) {  _copyright = c; }
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testMedaItemRoundtrip() throws Exception
    {
        MediaItemAsArray.Content c = new MediaItemAsArray.Content();
        c.setBitrate(9600);
        c.setCopyright("none");
        c.setDuration(360000L);
        c.setFormat("lzf");
        c.setHeight(640);
        c.setSize(128000L);
        c.setTitle("Amazing Stuff For Something Or Oth\u00CBr!");
        c.setUri("http://multi.fario.us/index.html");
        c.setWidth(1400);

        c.addPerson("Joe Sixp\u00e2ck");
        c.addPerson("Ezekiel");
        c.addPerson("Sponge-Bob Squarepant\u00DF");
        
        MediaItemAsArray input = new MediaItemAsArray(c);
        input.addPhoto(new MediaItemAsArray.Photo());
        input.addPhoto(new MediaItemAsArray.Photo());
        input.addPhoto(new MediaItemAsArray.Photo());

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        MediaItemAsArray output = MAPPER.readValue(new java.io.StringReader(json), MediaItemAsArray.class);
        assertNotNull(output);

        assertNotNull(output.getImages());
        assertEquals(input.getImages().size(), output.getImages().size());
        assertNotNull(output.getContent());
        assertEquals(input.getContent().getTitle(), output.getContent().getTitle());
        assertEquals(input.getContent().getUri(), output.getContent().getUri());

        // compare re-serialization as a simple check as well
        assertEquals(json, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(output));
    }
}
