package lrg.dude.duplication;

import java.util.ArrayList;
import java.util.List;

public class MatrixLineList {
    private List<MatrixLine> list = new ArrayList<MatrixLine>();

    public void add(MatrixLine ml) {
        list.add(ml);
    }

    public void addAll(MatrixLineList aList){
    	if (aList == null)
    		return;
        int size = aList.size();
        for(int i = 0; i < size; i++)
            list.add(aList.get(i));
    }

    public MatrixLine get(int index) {
        return (MatrixLine) list.get(index);
    }

    public int size() {
        return list.size();
    }

    public long countDuplicatedLines() {
        long counter = 0;
        for(int i = 0; i < list.size(); i++)
            if(!get(i).isUnique())
                counter++;
        return counter;
    }
}
