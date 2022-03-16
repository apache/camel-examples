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

package org.apache.camel.example.resume.strategies.kafka.file;

import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.camel.ResumeCache;

/**
 * This is a simple cache implementation that uses Caffeine to store
 * the 100 offsets.
 *
 * @param <K> The type of the data to cache
 */
public class SingleItemCache<K> implements ResumeCache<K, Long> {
    public static final int CACHE_SIZE = 100;
    private final Cache<K, Long> cache = Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .build();

    @Override
    public void add(K key, Long offsetValue) {
        cache.put(key, offsetValue);
    }

    @Override
    public Optional<Long> get(K key) {
        Long entry = cache.getIfPresent(key);

        if (entry == null) {
            return Optional.empty();
        }

        return Optional.of(entry.longValue());
    }

    @Override
    public boolean isFull() {
        if (cache.estimatedSize() >= CACHE_SIZE) {
            return true;
        }

        return false;
    }
}
