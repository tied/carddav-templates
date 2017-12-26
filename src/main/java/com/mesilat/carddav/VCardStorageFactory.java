package com.mesilat.carddav;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.ServletException;

public class VCardStorageFactory {
    private static final VCardStorageFactory instance;
    private VCardStorage storage;
    private Thread thread;

    public VCardStorage getStorage() {
        return storage;
    }
    protected void init(String className, String params) throws ServletException {
        try {
            Class c = Class.forName(className);
            Method m = c.getMethod("create", String.class);
            storage = (VCardStorage)m.invoke(null, params);
            thread = new Thread(storage);
            thread.start();
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException ex) {
            throw new ServletException(ex);
        }
    }
    protected void stop(){
        if (thread != null){
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ex) {
            }
            thread = null;
        }
    }

    public static VCardStorageFactory getFactory() {
        return instance;
    }

    private VCardStorageFactory(){
    }

    static {
        instance = new VCardStorageFactory();
    }
}