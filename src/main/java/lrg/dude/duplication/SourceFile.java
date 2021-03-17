  
  package lrg.dude.duplication;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


  public class SourceFile implements Entity {

		private static final long serialVersionUID = -1935777691950363375L;
		private String fileName;
	    private StringList codelines;

	    private int noOfRelevantLines = 0;

	    List<String> ignoredLines;

	    /**
	     * Constructor
	     *
	     * @param file File
	     */
	    public SourceFile(File file, String shortName) {
	        fileName = shortName;
	        codelines = new StringList();
	        ignoredLines = new ArrayList<>();
	        
//	        System.out.println(file);  
	        try {
	            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf8"));
	            String aLine = null;
	            int counter = 0;
	            while ((aLine = in.readLine()) != null) {
	            	counter++;
					if(aLine.length() <= DuDe.maxLineSize) codelines.add(aLine);
					else ignoredLines.add(fileName + "," + counter + "," + aLine.length() + "," + DuDe.maxLineSize);
	            }
	            in.close();
	        } catch (FileNotFoundException fe) {
	            System.out.println("Files does not exist: " + file + ": " + fe);
	        } catch (IOException ioe) {
	            System.out.println("Error readin file : " + ioe);
	        }
	    }

	    public String getName() {
	        return fileName;
	    }

	    public StringList getCode() {
	        return codelines;
	    }

	    public int getNoOfRelevantLines() {
	        return noOfRelevantLines;
	    }

	    public void setNoOfRelevantLines(int norl) {
	        noOfRelevantLines = norl;
	    }
	}
