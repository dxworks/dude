package lrg.dude.duplication;

import java.io.Serializable;

public class CodeFragment implements Serializable{

	private static final long serialVersionUID = 2577184349530702246L;
	
	private Entity entity;
    private long beginLine;
    private long endLine;

    public CodeFragment(Entity entity, long beginLine, long endLine) {
        this.entity = entity;
        this.beginLine = beginLine;
        this.endLine = endLine;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getEntityName() {
        return entity.getName();
    }

    public long getBeginLine() {
        return beginLine;
    }

    public long getEndLine() {
        return endLine;
    }

    public int getLength() {
        return (int)(endLine - beginLine + 1);
    }
}
