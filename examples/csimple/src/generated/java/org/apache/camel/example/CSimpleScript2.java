package org.apache.camel.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.camel.*;
import org.apache.camel.util.*;
import org.apache.camel.spi.*;
import static org.apache.camel.language.csimple.CSimpleHelper.*;


public class CSimpleScript2 extends org.apache.camel.language.csimple.CSimpleSupport {

    Language bean;

    public CSimpleScript2() {
    }

    @Override
    public boolean isPredicate() {
        return true;
    }

    @Override
    public String getText() {
        return "${body} > 10";
    }

    @Override
    public Object evaluate(CamelContext context, Exchange exchange, Message message, Object body) throws Exception {
        return isGreaterThan(exchange, body, 10);
    }
}

