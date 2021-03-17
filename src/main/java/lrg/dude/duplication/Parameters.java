package lrg.dude.duplication;

public class Parameters {
    private int minLength;
    private int maxLineBias;
    private int minExactChunk;
    private boolean considerComments;

    public Parameters(int minLength, int maxLineBias, int minExactChunk, boolean considerComments) {
        this.minLength = minLength;
        this.maxLineBias = maxLineBias;
        this.minExactChunk = minExactChunk;
        this.considerComments = considerComments;
    }
    
    public int getMinLength() {
        return minLength;
    }

    public int getMaxLineBias() {
        return maxLineBias;
    }

    public int getMinExactChunk() {
        return minExactChunk;
    }

    public boolean isConsiderComments() {
        return considerComments;
    }

}
