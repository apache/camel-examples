package org.apache.camel.example.widget;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Configuration;
import org.apache.camel.component.activemq.ActiveMQComponent;

@Configuration
public class WidgetConfiguration {

    @BindToRegistry("activemq")
    public ActiveMQComponent activeMQComponent() {
        ActiveMQComponent amq = new ActiveMQComponent();

        // The ActiveMQ Broker allows anonymous connection by default
        // amq.setUserName("admin");
        // amq.setPassword("admin");

        // the url to the remote ActiveMQ broker
        amq.setBrokerURL("tcp://localhost:61616");

        return amq;
    }
}
