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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CqlPopulateBean {

    private static final Logger log = LoggerFactory.getLogger(CqlPopulateBean.class);

    public void populate() {
        try (Cluster cluster = Cluster.builder().addContactPoint("cassandra").build();
            Session session = cluster.connect()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1};");
            session.execute("CREATE TABLE IF NOT EXISTS test.users ( id int primary key, name text );");
            session.execute("INSERT INTO test.users (id,name) VALUES (1, 'oscerd') IF NOT EXISTS;");
            session.execute("INSERT INTO test.users (id,name) VALUES (2, 'not-a-bot') IF NOT EXISTS;");
        }
        log.info("Cassandra was populated with sample values for test.users table");
    }

}
