package lrg.dude.duplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DuplicationList implements Serializable{
	private static final long serialVersionUID = 5958532463861498892L;
	private List<Duplication> list = new ArrayList<Duplication>();

    public void add(Duplication d) {
        list.add(d);
    }

    public void add(DuplicationList anotherList) {
        for (int i = 0; i < anotherList.size(); i++)
            add(anotherList.get(i));
    }

    public boolean contains(Duplication d) {
        return list.contains(d);
    }

    public Duplication get(int index) {
        return (Duplication) list.get(index);
    }

    public int size() {
        return list.size();
    }

    public Duplication[] toArray() {
        Duplication[] dups = new Duplication[list.size()];
        System.arraycopy(list.toArray(), 0, dups, 0, list.size());
        return dups;
    }

    /**
     * Computes all duplication (inclusive redundant) starting from a
     * this list, obtained probably by searching only half a matrix
     *
     * @return
     */
    public DuplicationList getRedundantList() {
        DuplicationList redundantList = new DuplicationList();
        for (int i = 0; i < size(); i++) {
            redundantList.add(this.get(i));
            redundantList.add(this.get(i).makeInvert());
        }
        return redundantList;
    }

    public static DuplicationList sortByNameAsc(DuplicationList unsorted) {
        DuplicationList sorted = new DuplicationList();
        for (int i = 0; i < unsorted.size(); i++)
            sorted.add(unsorted.get(i));
        Comparator<Duplication> c = new EntityNameComparator();
        Collections.sort(sorted.list, c);
        return sorted;
    }

    public static DuplicationList sortByLengthDesc(DuplicationList unsorted) {
        DuplicationList sorted = new DuplicationList();
        for (int i = 0; i < unsorted.size(); i++)
            sorted.add(unsorted.get(i));
        Comparator<Duplication> c = new DupLengthComparator();
        Collections.sort(sorted.list, c);
        Collections.reverse(sorted.list);
        return sorted;
    }

    /**
     * Sorts list: by entity, and within the same entity by duplication length
     *
     * @param unsorted
     * @return
     */
    public static DuplicationList sort(DuplicationList unsorted) {
        if (unsorted == null || unsorted.size() == 0)
            return unsorted;
        DuplicationList sorted = new DuplicationList();
        DuplicationList dupsForAnEntity = null;
        String lastEntityName = "";
        for (int i = 0; i < unsorted.size(); i++) {
            Duplication current = unsorted.get(i);
            String entityName = current.getReferenceCode().getEntityName();
            if (entityName.compareTo(lastEntityName) != 0) {
                lastEntityName = entityName;
                /*new forAnEntity list*/
                if (dupsForAnEntity != null) {
                    Comparator<Duplication> c = new DupLengthComparator();
                    Collections.sort(dupsForAnEntity.list, c);
                    Collections.reverse(dupsForAnEntity.list);
                    sorted.add(dupsForAnEntity);
                }
                dupsForAnEntity = new DuplicationList();
            }
            dupsForAnEntity.add(current);
        }
        Comparator<Duplication> c = new DupLengthComparator();
        Collections.sort(dupsForAnEntity.list, c);
        Collections.reverse(dupsForAnEntity.list);
        sorted.add(dupsForAnEntity);
        return sorted;
    }

    public String toString(){
        String str = "";
        for(int i = 0; i < size(); i++)
            str += get(i).toString() + "\n";
        return str;
    }
}
