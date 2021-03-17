package lrg.dude.duplication;

import java.util.Comparator;

public class DupLengthComparator implements Comparator<Duplication> {
    public int compare(Duplication d1, Duplication d2) {
        if (d1.copiedLength() == d2.copiedLength())
            return 0;
        else if (d1.copiedLength() > d2.copiedLength())
            return 1;
        else
            return -1;
    }
}
