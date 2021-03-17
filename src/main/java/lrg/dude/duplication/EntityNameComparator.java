package lrg.dude.duplication;


import java.util.Comparator;

public class EntityNameComparator implements Comparator<Duplication> {
    public int compare(Duplication o1, Duplication o2) {
        String entityName1 = (o1).getReferenceCode().getEntityName();
        String entityName2 = (o2).getReferenceCode().getEntityName();
        return entityName1.compareTo(entityName2);
    }
}
