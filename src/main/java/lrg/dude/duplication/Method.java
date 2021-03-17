package lrg.dude.duplication;



public class Method implements Entity {

	private static final long serialVersionUID = 5547204581455252291L;
	private String methodName;
    private StringList methodBody;

    private int noOfRelevantLines = 0;

    public Method(String name) {
        methodName = name;
    }

    public Method(String name, StringList code) {
        methodName = name;
        methodBody = code;
    }

    public String getName() {
        return methodName;
    }

    public StringList getCode() {
        return methodBody;
    }

    public int getNoOfRelevantLines() {
        return noOfRelevantLines;
    }

    public void setNoOfRelevantLines(int norl) {
        noOfRelevantLines = norl;
    }
}
