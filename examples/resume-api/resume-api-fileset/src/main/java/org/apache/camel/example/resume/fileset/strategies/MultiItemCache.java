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

package org.apache.camel.example.resume.fileset.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cache that can store multiple key-based resumables
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public class MultiItemCache<K, V> implements ListBasedCache<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(MultiItemCache.class);
    public static final int CACHE_SIZE = 10000;

    private final Cache<K, List<V>> cache = Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .build();

    @Override
    public synchronized void add(K key, V offsetValue) {
        LOG.trace("Adding entry to the cache (k/v): {}/{}", key, offsetValue);
        LOG.trace("Adding entry to the cache (k/v) with types: {}/{}", key.getClass(), offsetValue.getClass());
        List<V> entries = cache.get(key, k -> new ArrayList<>());

        entries.add(offsetValue);
    }

    @Override
    public Optional<V> get(K key) {
        throw new UnsupportedOperationException("Unsupported");
    }


    @Override
    public synchronized boolean contains(K key, V entry) {
        final List<V> entries = cache.getIfPresent(key);

        if (entries == null) {
            return false;
        }


        boolean ret = entries.contains(entry);
        LOG.trace("Checking if cache contains key {} with value {} ({})", key, entry, ret);

        return ret;
    }


    @Override
    public boolean isFull() {
        if (cache.estimatedSize() >= CACHE_SIZE) {
            return true;
        }

        return false;
    }
}
