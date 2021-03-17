package lrg.dude.duplication;




public class MatrixLine {
    private String code;
    private Entity entity;
    private int realIndex;
    private int matrixIndex;
    private boolean unique = true;

    public MatrixLine(String code, Entity entity, int realIndex) {
        this.code = code;
        this.entity = entity;
        this.realIndex = realIndex;
    }
    
    public MatrixLine(String code, Entity entity, int realIndex, int matrixIndex) {
        this.code = code;
        this.entity = entity;
        this.realIndex = realIndex;
        this.matrixIndex = matrixIndex;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    public Entity getEntity() {
        return entity;
    }

    public int getRealIndex() {
        return realIndex;
    }
    
    public int getMatrixIndex() {
        return matrixIndex;
    }

    public String toString() {
        return "<" + entity.getName() + ",line:" + realIndex + ">\t" + code;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setDuplicated() {
        unique = false;
    }
}
