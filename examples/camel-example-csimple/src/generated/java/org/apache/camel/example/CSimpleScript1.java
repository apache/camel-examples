package org.apache.camel.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.camel.*;
import org.apache.camel.util.*;
import static org.apache.camel.language.csimple.CSimpleHelper.*;


public class CSimpleScript1 extends org.apache.camel.language.csimple.CSimpleSupport {

    public CSimpleScript1(CamelContext context) {
        init(context);
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

