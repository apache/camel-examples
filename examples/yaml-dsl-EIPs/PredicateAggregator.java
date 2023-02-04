package org.apache.camel.example.yaml;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

public class PredicateAggregator<E> implements AggregationStrategy,Predicate<Exchange> {

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

    @Override
    public boolean test(Exchange exchange) {

      if (exchange == null) {
          return false;
      }

      String body = exchange.getIn().getBody(String.class);
      return (body.length() > 15);
    }
}
