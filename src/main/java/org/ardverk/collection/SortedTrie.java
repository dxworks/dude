/*
 * Copyright 2005-2009 Roger Kapsi, Sam Berlin
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ardverk.collection;

import java.util.SortedMap;

/**
 * Defines the interface for a prefix tree, an ordered tree data structure. For 
 * more information, see <a href="http://en.wikipedia.org/wiki/Trie">Tries</a>.
 * 
 * @author Roger Kapsi
 * @author Sam Berlin
 */
public interface SortedTrie<K, V> extends Trie<K, V>, SortedMap<K, V> {
    
    /**
     * Returns a view of this {@link SortedTrie} of all elements that are prefixed 
     * by the given key.
     * 
     * <p>In a {@link SortedTrie} with fixed size keys, this is essentially a 
     * {@link #get(Object)} operation.
     * 
     * <p>For example, if the {@link SortedTrie} contains 'Anna', 'Anael', 
     * 'Analu', 'Andreas', 'Andrea', 'Andres', and 'Anatole', then
     * a lookup of 'And' would return 'Andreas', 'Andrea', and 'Andres'.
     */
    public SortedMap<K, V> getPrefixedBy(K key);
    
    /**
     * Returns a view of this {@link SortedTrie} of all elements that are prefixed 
     * by the length of the key.
     * 
     * <p>{@link SortedTrie}s with fixed size keys will not support this operation 
     * (because all keys are the same length).
     * 
     * <p>For example, if the {@link SortedTrie} contains 'Anna', 'Anael', 'Analu', 
     * 'Andreas', 'Andrea', 'Andres', and 'Anatole', then a lookup for 'Andrey' 
     * and a length of 4 would return 'Andreas', 'Andrea', and 'Andres'.
     */
    public SortedMap<K, V> getPrefixedBy(K key, int length);
    
    /**
     * Returns a view of this {@link SortedTrie} of all elements that are prefixed
     * by the key, starting at the given offset and for the given length.
     * 
     * <p>{@link SortedTrie}s with fixed size keys will not support this operation 
     * (because all keys are the same length).
     * 
     * <p>For example, if the {@link SortedTrie} contains 'Anna', 'Anael', 'Analu', 
     * 'Andreas', 'Andrea', 'Andres', and 'Anatole', then a lookup for 
     * 'Hello Andrey Smith', an offset of 6 and a length of 4 would return 
     * 'Andreas', 'Andrea', and 'Andres'.
     */
    public SortedMap<K, V> getPrefixedBy(K key, int offset, int length);
    
    /**
     * Returns a view of this {@link SortedTrie} of all elements that are prefixed
     * by the number of bits in the given Key.
     * 
     * <p>In {@link SortedTrie}s with fixed size keys like IP addresses this method
     * can be used to lookup partial keys. That is you can lookup all addresses
     * that begin with '192.168' by providing the key '192.168.X.X' and a 
     * length of 16.
     */
    public SortedMap<K, V> getPrefixedByBits(K key, int lengthInBits);
    
    /**
     * Returns a view of this {@link SortedTrie} of all elements that are prefixed
     * by the number of bits in the given Key.
     */
    public SortedMap<K, V> getPrefixedByBits(K key, 
            int offsetInBits, int lengthInBits);
}
