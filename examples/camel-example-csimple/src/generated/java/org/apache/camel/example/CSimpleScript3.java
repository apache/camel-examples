package org.apache.camel.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.camel.*;
import org.apache.camel.util.*;
import static org.apache.camel.language.csimple.CSimpleHelper.*;


public class CSimpleScript3 extends org.apache.camel.language.csimple.CSimpleSupport {

    public CSimpleScript3(CamelContext context) {
        init(context);
    }

    @Override
    public String getText() {
        return "${body} > 5";
    }

    @Override
    public Object evaluate(CamelContext context, Exchange exchange, Message message, Object body) throws Exception {
        return isGreaterThan(exchange, body, 5);
    }
}

