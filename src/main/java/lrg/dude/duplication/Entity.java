package lrg.dude.duplication;

import java.io.Serializable;

public interface Entity extends Serializable{
    public String getName();

    public StringList getCode();

    public int getNoOfRelevantLines(); //for clustering reasons

    public void setNoOfRelevantLines(int norl); 
}
