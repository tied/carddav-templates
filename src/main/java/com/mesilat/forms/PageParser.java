package com.mesilat.forms;

import com.atlassian.confluence.pages.AbstractPage;
import org.codehaus.jackson.node.ArrayNode;

public interface PageParser {
    ArrayNode parse(AbstractPage page) throws ParseException;
}