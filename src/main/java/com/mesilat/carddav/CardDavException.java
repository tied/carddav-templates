package com.mesilat.carddav;

public class CardDavException extends Exception {
    public CardDavException(String msg){
        super(msg);
    }
    public CardDavException(Throwable cause){
        super(cause);
    }
    public CardDavException(String msg, Throwable cause){
        super(msg, cause);
    }
}