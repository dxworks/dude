package org.ardverk.collection;

import lrg.dude.duplication.MatrixLineList;

import org.ardverk.collection.PatriciaTrie;


public class AdaptedPatriciaTrie extends PatriciaTrie<String, MatrixLineList> {
    private static final long serialVersionUID = 5155253417231339498L;
    
    /**
     * {@inheritDoc}
     */
    //public AdaptedPatriciaTrie(KeyAnalyzer<? super String> keyAnalyzer) {
    public AdaptedPatriciaTrie() {
        super(StringKeyAnalyzer.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatrixLineList put(String key, MatrixLineList value) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        int lengthInBits = lengthInBits(key);

        // The only place to store a key with a length
        // of zero bits is the root node
        if (lengthInBits == 0) {
            if (root.isEmpty()) {
                incrementSize();
            } else {
                incrementModCount();
            }

            MatrixLineList crtVal = root.getValue();
            value.addAll(crtVal);
            
            return root.setKeyValue(key, value);
        }

        TrieEntry<String, MatrixLineList> found = getNearestEntryForKey(key, lengthInBits);
        if (compareKeys(key, found.key)) {
            if (found.isEmpty()) { // <- must be the root
                incrementSize();
            } else {
                incrementModCount();
            }
            
            MatrixLineList crtVal = found.getValue();
            value.addAll(crtVal);
            
            return found.setKeyValue(key, value);
        }

        int bitIndex = bitIndex(key, found.key);
        if (!AbstractKeyAnalyzer.isOutOfBoundsIndex(bitIndex)) {
            if (AbstractKeyAnalyzer.isValidBitIndex(bitIndex)) { // in 99.999...9% the case
                /* NEW KEY+VALUE TUPLE */
                TrieEntry<String, MatrixLineList> t = new TrieEntry<String, MatrixLineList>(key, value, bitIndex);
                addEntry(t, lengthInBits);
                incrementSize();
                return null;
            } else if (AbstractKeyAnalyzer.isNullBitKey(bitIndex)) {
                // A bits of the Key are zero. The only place to
                // store such a Key is the root Node!

                /* NULL BIT KEY */
                if (root.isEmpty()) {
                    incrementSize();
                } else {
                    incrementModCount();
                }

                MatrixLineList crtVal = root.getValue();
                value.addAll(crtVal);
                
                return root.setKeyValue(key, value);

            } else if (AbstractKeyAnalyzer.isEqualBitKey(bitIndex)) {
                // This is a very special and rare case.

                /* REPLACE OLD KEY+VALUE */
                if (found != root) {
                    incrementModCount();
                    
                    MatrixLineList crtVal = found.getValue();
                    value.addAll(crtVal);
                                        
                    return found.setKeyValue(key, value);
                }
            }
        }

        throw new IndexOutOfBoundsException("Failed to put: "
                + key + " -> " + value + ", " + bitIndex);
    }
}
