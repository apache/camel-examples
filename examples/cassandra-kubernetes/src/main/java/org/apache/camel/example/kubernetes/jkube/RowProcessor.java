/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.kubernetes.jkube;

import com.datastax.oss.driver.api.core.cql.Row;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;
import java.util.stream.Collectors;

public class RowProcessor implements Processor {
  @SuppressWarnings("unchecked")
  @Override
  public void process(Exchange exchange) {
    final List<Row> rows = exchange.getIn().getBody(List.class);
    exchange.getIn().setBody(rows.stream()
      .map(row -> String.format("%s-%s", row.getInt("id"), row.getString("name")))
      .collect(Collectors.joining(","))
    );
  }
}
