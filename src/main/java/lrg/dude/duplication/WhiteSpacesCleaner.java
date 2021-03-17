package lrg.dude.duplication;

public class WhiteSpacesCleaner extends CleaningDecorator {

    public WhiteSpacesCleaner(CleaningDecorator next) {
        super(next);
    }

    public StringList specificClean(StringList text) {
        String[] whitespace = getWhiteSpace();
        for (int i = 0; i < text.size(); i++) {
            for (int w = 0; w < whitespace.length; w++) {
                text.set(i, text.get(i).replaceAll(whitespace[w], ""));
            }
        }
        return text;
    }

    /**
     * Reads the Strings considered as whitespace
     *
     * @return array of whitespace Strings
     */
    private String[] getWhiteSpace() {
        String[] whitespace;

        whitespace = new String[3];
        whitespace[0] = " ";
        whitespace[1] = "\t";
        whitespace[2] = "\n";

        return whitespace;
    }

}
