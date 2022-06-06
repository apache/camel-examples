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


import java.time.Instant;
import java.util.UUID;

public class ExampleEntry {
    private UUID id;
    private Instant insertionDate;
    private long number;
    private String text;

    public ExampleEntry() {
    }

    public ExampleEntry(UUID id, Instant insertionDate, long number, String text) {
        this.id = id;
        this.insertionDate = insertionDate;
        this.number = number;
        this.text = text;
    }

    public Instant getInsertionDate() {
        return insertionDate;
    }

    public void setInsertionDate(Instant insertionDate) {
        this.insertionDate = insertionDate;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ExampleEntry{" +
                "id=" + id +
                ", insertionDate=" + insertionDate +
                ", number=" + number +
                ", text='" + text + '\'' +
                '}';
    }
}
