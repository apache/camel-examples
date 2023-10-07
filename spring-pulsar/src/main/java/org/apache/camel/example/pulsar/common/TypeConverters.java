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
package org.apache.camel.example.pulsar.common;

import java.nio.ByteBuffer;

import org.apache.camel.Converter;

public class TypeConverters implements org.apache.camel.TypeConverters {

    // In Camel 3.17 and later, this will not be called because overridden by
    // built-in Bulk Converter which handles byte array to Integer conversion
    // by first converting the byte array to a String
    // @Converter
    public int intFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    @Converter
    public byte[] intToByteArray(Integer value) {
        // Write it as a string since that is how Bulk Converter will handle it
        return value.toString().getBytes();
    }

}
