package com.mesilat.carddav;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileStorageImpl implements VCardStorage, Serializable {
    private final HashMap<String, VCardStorage.VCard> map = new HashMap<>();
    private long ctag;
    private File file;

    @Override
    public String getCTag(){
        return Long.toString(ctag);
    }
    @Override
    public List<String> list() {
        List<String> result = new ArrayList<>();
        for (VCardStorage.VCard vcard : map.values()){
            result.add(vcard.getId());
        }
        return result;
    }
    @Override
    public List<String> list(String ctag){
        long tag = Long.parseLong(ctag);
        List<String> result = new ArrayList<>();
        for (VCardStorage.VCard vcard : map.values()){
            if (vcard.getEtag() > tag){
                result.add(vcard.getId());
            }
        }
        return result;
    }
    @Override
    public VCard getVCard(String id){
        return map.get(id);
    }
    @Override
    public void putVCard(String id, byte[] vcard) {
        synchronized(this){
            ctag++;
            map.put(id, new VCardStorage.VCard(id, ctag, vcard));
        }
    }

    @Override
    public void run(){
        long tag = 0;
        while(true){
            try {
                Thread.sleep(10000);
                synchronized(this){
                    if (tag < ctag){
                        save();
                        tag = ctag;
                    }
                }
            } catch (InterruptedException ex) {
                break;
            }
        }
        save();
        
    }
    private void save(){
        try (FileOutputStream out = new FileOutputStream(file)){
            try (ObjectOutputStream oout = new ObjectOutputStream(out)){
                oout.writeUnshared(this);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileStorageImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private FileStorageImpl(){        
        this.ctag = System.currentTimeMillis();
    }
    private FileStorageImpl(File file){        
        this.ctag = System.currentTimeMillis();
        this.file = file;
    }

    public static FileStorageImpl create(String fileStoragePath) throws IOException, ClassNotFoundException{
        File file = new File(fileStoragePath);
        if (!file.exists()) {
            return new FileStorageImpl(file);
        } else {
            try (FileInputStream in = new FileInputStream(file)){
                try (ObjectInputStream oin = new ObjectInputStream(in)){
                    FileStorageImpl storage = (FileStorageImpl)oin.readObject();
                    storage.file = file;
                    return storage;
                }
            }
        }
    }
}