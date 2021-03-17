package lrg.dude.duplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class VirtualColumnMatrix {

    public VirtualColumnMatrix(int size, int startColumnIndex) {
    	this.startColumnIndex = startColumnIndex;
        list = new ArrayList<HashMap<Integer, Boolean>>();
        for (int i = 0; i < size; i++)
            list.add(new HashMap<Integer,Boolean>());

    }
    
    public void removeAll() {
    	freeColumns(startColumnIndex, startColumnIndex + list.size());
    	list.removeAll(list);
    }

    public Boolean get(int x, int y) {
        HashMap<Integer, Boolean> map =  list.get(y - startColumnIndex);
        return (Boolean) map.get(new Integer(x));
    }

    public void set(int x, int y, Boolean element) {
        HashMap<Integer, Boolean> map =  list.get(y - startColumnIndex);
        map.put(Integer.valueOf(x), element);
    }

    public Iterator<Integer> iterator(int i) {
        HashMap<Integer, Boolean> map = list.get(i - startColumnIndex);
        TreeSet<Integer> set = new TreeSet<Integer>(map.keySet());
        return set.iterator();
    }

    public void freeColumns(int start, int end) {
        for (int i = start - startColumnIndex; i < end - startColumnIndex; i++)
            list.get(i).clear();

    }

    private ArrayList<HashMap<Integer, Boolean>> list;
    private int startColumnIndex;
}