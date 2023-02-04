package org.apache.camel.example.yaml;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

public class SimpleAggregator implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
      if (oldExchange == null) {
          return newExchange;
      }

      String oldBody = oldExchange.getIn().getBody(String.class);
      String newBody = newExchange.getIn().getBody(String.class);
      oldExchange.getIn().setBody(oldBody + "+" + newBody);
      return oldExchange;
    }

}
