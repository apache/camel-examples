package org.apache.camel.example.artemis;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.BindToRegistry;
import org.apache.camel.Configuration;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;

@Configuration
public class ArtemisConfiguration {

    @BindToRegistry("jms")
    public JmsComponent createArtemisComponent() {
        // Sets up the Artemis core protocol connection factory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        JmsConfiguration configuration = new JmsConfiguration();
        configuration.setConnectionFactory(connectionFactory);

        return new JmsComponent(configuration);
    }
}
