package org.apache.camel.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.camel.*;
import org.apache.camel.util.*;
import org.apache.camel.spi.*;
import static org.apache.camel.language.csimple.CSimpleHelper.*;


public class CSimpleScript1 extends org.apache.camel.language.csimple.CSimpleSupport {

    Language bean;
    UuidGenerator uuid;

    public CSimpleScript1() {
    }

    @Override
    public boolean isPredicate() {
        return false;
    }

    @Override
    public String getText() {
        return "${random(20)}";
    }

    @Override
    public Object evaluate(CamelContext context, Exchange exchange, Message message, Object body) throws Exception {
        return random(exchange, 0, 20);
    }
}

