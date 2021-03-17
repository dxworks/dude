package lrg.dude.duplication;

import java.util.ArrayList;

//this cleaner should be put to work after a whitespace cleaner did its job
public class NoiseCleaner extends CleaningDecorator {
//	private final String noiseFile = "noise.dat";

	public NoiseCleaner(CleaningDecorator next) {
		super(next);
	}

	protected StringList specificClean(StringList text) {
		String[] noise = getNoise();
		for (int i = 0; i < text.size(); i++)
			for (int n = 0; n < noise.length; n++)
				if (text.get(i).equals(noise[n]))
					text.set(i, "");
		return text;
	}

	private String[] getNoise() {
		ArrayList<String> lines = new ArrayList<String>();
			lines.add("break");
			lines.add("else");
			lines.add("{");
			lines.add("}");
		String[] sintacticElements = new String[lines.size()];
		for (int i = 0; i < sintacticElements.length; i++)
			sintacticElements[i] = (String) lines.get(i);
		return sintacticElements;
	}
}
