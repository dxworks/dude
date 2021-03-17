package lrg.dude.duplication;

public class CommentsCleaner extends CleaningDecorator {
    private final String sStartComment = "/*";
    private final String sEndComment = "*/";
    private final String sLineComments = "//";

    public CommentsCleaner(CleaningDecorator next) {
        super(next);
    }

    protected StringList specificClean(StringList text) {
        boolean openComment = false;
        for (int i = 0; i < text.size(); i++) {
            //check if I have openBraces form previous lines
            if (openComment) {
                int iEndComment = getLastIndexOf(sEndComment, text.get(i));
                if (iEndComment >= 0) {
                    //previous opened braces are being closed on this line
                    text.set(i, text.get(i).substring(iEndComment + sEndComment.length()));
                    openComment = false;
                } else  //previous opened braces are not being closed on this line
                    text.set(i,"");
            }
            while (containsComments(text.get(i), openComment)) {
                int iStartComment = getIndexOf(sStartComment, text.get(i));
                int iLineComment = getIndexOf(sLineComments, text.get(i));
                //varianta1
                if (iStartComment >= 0) {       // I have /*
                    if (iLineComment >= 0 && iLineComment < iStartComment) {
                        // situation:  ...//.../*
                        text.set(i, text.get(i).substring(0, iLineComment));
                    } else {
                        // either I have no //, or it is after /*
                        int iEndComment = getIndexOf(sEndComment, text.get(i),iStartComment);//search for iEndCommend only after /* - else out of memory
                        if (iEndComment >= 0)
                            text.set(i, text.get(i).substring(0, iStartComment) + text.get(i).substring(iEndComment + sEndComment.length()));
                        else {
                            // opened Braces but won't close them on this line
                            openComment = true;
                            text.set(i, text.get(i).substring(0, iStartComment));
                        }
                    }
                } else if (iLineComment >= 0) //...got //
                    text.set(i, text.get(i).substring(0, iLineComment));
            }
        }
        return text;
    }

    private boolean containsComments(String s, boolean openComment) {
        String temp = new String(s);
        temp = temp.replaceAll("\".*[^\\\\]\"", "");
        if ((temp.indexOf(sLineComments) >= 0) && (!insideString(s, temp.indexOf(sLineComments), 2)))
            return true;
        if ((temp.indexOf(sStartComment) >= 0) && (!insideString(s, temp.indexOf(sStartComment), 2)))
            return true;
        if (temp.indexOf(sEndComment) >= 0 && openComment && (!insideString(s, temp.indexOf(sStartComment), 2)))
            return true;
        return false;
    }
    
    private int getIndexOf(String substr, String line, int last){
        boolean finished = false;
        int lastIndex = last;
        return returnIndexOf(substr, line, finished, lastIndex);
    }

	
    private int getIndexOf(String substr, String line){
        boolean finished = false;
        int lastIndex = -1;
        return returnIndexOf(substr, line, finished, lastIndex);
    }
    
    private int returnIndexOf(String substr, String line, boolean finished,
			int lastIndex) {
		int index;
        while(!finished){
            index = line.indexOf(substr, lastIndex + 1);
            if(index == -1)
                finished = true;
            else{
                if(!insideString(line, index, substr.length()))
                    return index;
                else
                    lastIndex = index;
            }
        }
        return -1;
	}

    private int getLastIndexOf(String substr, String line ) {
		int index;
		boolean finished = false;
		int lastIndex = -1;
        while(!finished){
            index = line.indexOf(substr, lastIndex + 1);
            if(index == -1)
                finished = true;
            else{
                if(!insideString(line, index, substr.length()) && line.indexOf(substr, index + 1) < 0 )
                    return index;
                else
                    lastIndex = index;
            }
        }
        return -1;
	}

    private boolean insideString(String line, int index, int length) {
        String front = line.substring(0, index);
        String back = line.substring(index + length);
        front = front.replaceAll("\".*[^\\\\]\"", "");
        back = back.replaceAll("\".*[^\\\\]\"", "");
        if(front.indexOf("\"") > -1 && back.indexOf("\"") > -1)
            return true;
        return false;
    }

}