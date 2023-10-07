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

package org.apache.camel.example.resume.cassandra.main;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExampleDao {
    public static final String KEY_SPACE = "camel_ks";
    public static final String TABLE_NAME = "example";

    private static final Logger LOG = LoggerFactory.getLogger(ExampleDao.class);

    private final CqlSession session;

    public ExampleDao(CqlSession session) {
        this.session = session;
    }

    public void createKeySpace() {
        Map<String, Object> replication = new HashMap<>();

        replication.put("class", "SimpleStrategy");
        replication.put("replication_factor", 3);

        String statement = SchemaBuilder.createKeyspace(KEY_SPACE)
                .ifNotExists()
                .withReplicationOptions(replication)
                .asCql();

        LOG.info("Executing {}", statement);

        ResultSet rs = session.execute(statement);

        if (!rs.wasApplied()) {
            LOG.warn("The create key space statement did not execute");
        }
    }

    public void useKeySpace() {
        // Use String.format because "Bind variables cannot be used for keyspace names"
        String statement = String.format("USE %s", KEY_SPACE);

        session.execute(statement);
    }

    public void createTable() {
        SimpleStatement statement = SchemaBuilder.createTable(TABLE_NAME)
                .ifNotExists()
                .withPartitionKey("id", DataTypes.TIMEUUID)
                .withClusteringColumn("number", DataTypes.BIGINT)
                .withColumn("text", DataTypes.TEXT)
                .builder()
                .setTimeout(Duration.ofSeconds(10)).build();

        LOG.info("Executing create table {}", statement);

        ResultSet rs = session.execute(statement);
        if (!rs.wasApplied()) {
            LOG.warn("The create table statement did not execute");
        }
    }

    public static String getInsertStatement() {
        return "insert into example(id, number, text) values (now(), ?, ?)";
    }

    public static String getSelectStatement(int limitSize) {
        return String.format("select id, dateOf(id) as insertion_date,number,text from example limit %d", limitSize);
    }

    public void delete(UUID id) {
        session.execute("delete from camel_ks.example where id=?", id);
    }

    public void getData(Consumer<String> consumer) {
        ResultSet rs = session.execute("select * from example");

        if (rs != null) {
            Iterator<Row> iterator = rs.iterator();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                String data = row.getString("text");
                LOG.trace("Retrieved data: {}", data);
                consumer.accept(data);
            }
        } else {
            LOG.warn("No records were returned");
        }
    }

    public void insert(long number, String text) {
        session.execute(getInsertStatement(), number, text);
    }
}
