package lrg.dude.duplication;

import java.util.ArrayList;
import java.util.List;

public class CoordinateList {
    private List<Coordinate> list = new ArrayList<Coordinate>();

    public void add(Coordinate c) {
        list.add(c);
    }

    public Coordinate get(int index) {
        return (Coordinate) list.get(index);
    }

    public int size() {
        return list.size();
    }

    public void remove(int index) {
        list.remove(index);
    }
}
