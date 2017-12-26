package com.mesilat.forms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public class NsContext implements NamespaceContext {
    private static final Map<String, String> keyPrefix = new HashMap<>();
    private static final Map<String, String> keyUri = new HashMap<>();

    static {
        addToMap("xhtml",   "http://www.w3.org/1999/xhtml");
        addToMap("ac",      "http://www.atlassian.com/schema/confluence/4/ac/");
        addToMap("at",      "http://www.atlassian.com/schema/confluence/4/at/");
        addToMap("ri",      "http://www.atlassian.com/schema/confluence/4/ri/");
        addToMap("acxhtml", "http://www.atlassian.com/schema/confluence/4/");        
    }

    @Override
    public String getNamespaceURI(final String prefix) {
        String uri = keyPrefix.get(prefix);
        if (uri == null) {
            return XMLConstants.NULL_NS_URI;
        } else {
            return uri;
        }
    }
    @Override
    public Iterator<String> getPrefixes(final String val) {
        String prefix = keyUri.get(val);
        Set<String> pfxs = new TreeSet<>();
        if (prefix != null) {
            pfxs.add(prefix);
        }
        return pfxs.iterator();
    }
    public Set<String> getPrefixes() {
        return keyPrefix.keySet();
    }
    public void clear() {
        keyPrefix.clear();
        keyUri.clear();
    }
    public void add(final String prefix, final String uri) {
        addToMap(prefix, uri);
    }
    @Override
    public String getPrefix(final String uri) {
        return keyUri.get(uri);
    }
    public void appendNsName(final StringBuilder sb,
            final QName nm) {
        String uri = nm.getNamespaceURI();
        String abbr;

        abbr = keyUri.get(uri);
        if (abbr == null) {
            abbr = uri;
        }

        if (abbr != null) {
            sb.append(abbr);
            sb.append(":");
        }

        sb.append(nm.getLocalPart());
    }

    private static void addToMap(final String prefix, final String uri) {
        if (keyPrefix.get(prefix) != null) {
            throw new RuntimeException("Attempt to replace namespace prefix");
        }
        if (keyUri.get(uri) != null) {
            throw new RuntimeException("Attempt to replace namespace");
        }
        keyPrefix.put(prefix, uri);
        keyUri.put(uri, prefix);
    }
}