package com.pizzacheeseashdod.types;

public class Pic {

    String uri;
    String id;

    public Pic(){

    }

    public Pic(String uri, String id){
        this.uri = uri;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
