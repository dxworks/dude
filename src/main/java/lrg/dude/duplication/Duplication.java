package lrg.dude.duplication;

import java.io.Serializable;

public class Duplication implements Serializable{
	private static final long serialVersionUID = -7290202303094562834L;
	private CodeFragment referenceCode;
    private CodeFragment duplicateCode;
    private DuplicationType type;
    private String signature;   /*E12M1E3I1E4*/
    private int realLength;        /*real length (number of duplicated lines
                                   including the "noise" lines) in file*/
    private int copiedLength;

    /**
     * Constructor
     *
     * @param refCode CodeFragment of the duplication belonging to this entity
     * @param dupCode Code Fragment belonging to another entity, duplicate of refCode
     */
    public Duplication(CodeFragment refCode, CodeFragment dupCode,
                       DuplicationType type, String signature, int copiedLength) {
        this.signature = signature;
        referenceCode = refCode;
        duplicateCode = dupCode;
        this.type = type;
        realLength = refCode.getLength() <= dupCode.getLength() ? refCode.getLength() : dupCode.getLength();
        this.copiedLength = copiedLength;
    }


    public CodeFragment getReferenceCode() {
        return referenceCode;
    }


    public CodeFragment getDuplicateCode() {
        return duplicateCode;
    }


    public DuplicationType getType() {
        return type;
    }

    public long copiedLength(){
        return copiedLength;
    }

    public long realLength() {
        return realLength;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("[");
        sb.append(referenceCode.getEntityName() +
                "," + referenceCode.getBeginLine() +
                "," + referenceCode.getEndLine());
        sb.append("] - [");
        sb.append(duplicateCode.getEntityName() +
                "," + duplicateCode.getBeginLine() +
                "," + duplicateCode.getEndLine());
        sb.append("]");
        sb.append(" - copiedLength = " + copiedLength +
                " (realLength = " + realLength +
                ") - type = " + type +
                " - signature = " + signature);
        return new String(sb);
    }


    public String getSignature() {
        return signature;
    }


    /**
     * Makes the invert Duplication object starting with a Duplication
     *
     * @return
     */
    public Duplication makeInvert() {
        StringBuffer invertSB = new StringBuffer(signature);
        DuplicationType newType;
        if (type == DuplicationType.DELETE)
            newType = DuplicationType.INSERT;
        else if (type == DuplicationType.INSERT)
            newType = DuplicationType.DELETE;
        else
            newType = type;
        for (int i = 0; i < invertSB.length(); i++) {
            if (invertSB.charAt(i) == 'D')
                invertSB.setCharAt(i, 'I');
            else if (invertSB.charAt(i) == 'I')
                invertSB.setCharAt(i, 'D');
        }
        String invertSignature = invertSB.toString();
        return new Duplication(duplicateCode, referenceCode, newType, invertSignature, copiedLength);
    }


    /**
     * Checks if the duplications is between an entity and itself
     *
     * @return true if it is, else otherwise
     */
    public boolean isSelfDuplication() {
        return getReferenceCode().getEntity().equals(getDuplicateCode().getEntity());
    }
}
