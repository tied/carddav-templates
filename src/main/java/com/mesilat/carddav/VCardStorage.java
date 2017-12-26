package com.mesilat.carddav;

import java.io.Serializable;
import java.util.List;

public interface VCardStorage extends Runnable {
    String getCTag();
    List<String> list();
    List<String> list(String ctag);
    VCard getVCard(String id);
    void putVCard(String id, byte[] vcard);
    
    public class VCard implements Serializable {
        private String id;
        private Long etag;
        private byte[] vcard;

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public Long getEtag() {
            return etag;
        }
        public void setEtag(Long etag) {
            this.etag = etag;
        }
        public byte[] getVcard() {
            return vcard;
        }
        public void setVcard(byte[] vcard) {
            this.vcard = vcard;
        }

        public VCard(){
        }
        public VCard(String id, Long etag, byte[] vcard){
            this.id = id;
            this.etag = etag;
            this.vcard = vcard;
        }
    }
}