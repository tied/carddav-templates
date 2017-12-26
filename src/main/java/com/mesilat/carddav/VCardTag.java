package com.mesilat.carddav;

import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;

public interface VCardTag extends RawEntity<String> {
    @NotNull
    @PrimaryKey("ID")
    String getId();
    void setId(String id);
    long getPageId();
    void setPageId(long pageId);
    long getETag();
    void setETag(long etag);
    @StringLength(1)
    String getStatus();
    void setStatus(String status);

    public static class VCardTagWrapper implements com.mesilat.carddav.server.VCardTag {
        private final VCardTag card;

        @Override
        public String getId() {
            return card.getId();
        }
        @Override
        public void setId(String id) {
            card.setId(id);
        }
        @Override
        public long getPageId() {
            return card.getPageId();
        }
        @Override
        public void setPageId(long pageId) {
            card.setPageId(pageId);
        }
        @Override
        public long getETag() {
            return card.getETag();
        }
        @Override
        public void setETag(long etag) {
            card.setETag(etag);
        }
        @Override
        public String getStatus() {
            return card.getStatus();
        }
        @Override
        public void setStatus(String status) {
            card.setStatus(status);
        }

        public VCardTagWrapper(VCardTag card){
            this.card = card;
        }
    }
    public static com.mesilat.carddav.server.VCardTag toTag(VCardTag card){
        return new VCardTagWrapper(card);
    }
}