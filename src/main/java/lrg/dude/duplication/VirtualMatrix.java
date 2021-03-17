// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 10.12.2004 16:49:47
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   VirtualMatrix.java

package lrg.dude.duplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class VirtualMatrix {

    public VirtualMatrix(int size) {
        list = new ArrayList();
        for (int i = 0; i < size; i++)
            list.add(new HashMap());

    }

    public Boolean get(int x, int y) {
        HashMap map = (HashMap) list.get(x);
        return (Boolean) map.get(new Integer(y));
    }

    public void set(int x, int y, Boolean element) {
        HashMap map = (HashMap) list.get(x);
        map.put(new Integer(y), element);
    }

    public Iterator iterator(int i) {
        HashMap map = (HashMap) list.get(i);
        TreeSet set = new TreeSet(map.keySet());
        return set.iterator();
    }

    public void freeLines(int start, int end) {
        for (int i = start; i < end; i++)
            ((HashMap) list.get(i)).clear();

    }

    private ArrayList list;
}