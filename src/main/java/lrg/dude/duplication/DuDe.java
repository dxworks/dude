package lrg.dude.duplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dxworks.argumenthor.Argumenthor;
import org.dxworks.argumenthor.config.ArgumenthorConfiguration;
import org.dxworks.argumenthor.config.fields.impl.StringField;
import org.dxworks.argumenthor.config.fields.impl.StringListField;
import org.dxworks.argumenthor.config.sources.impl.ArgsSource;
import org.dxworks.argumenthor.config.sources.impl.EnvSource;
import org.dxworks.argumenthor.config.sources.impl.PropertiesSource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;

public class DuDe {
	public static final String PROJECT_NAME = "project.name";
	public static final String PROJECT_FOLDER = "project.folder";
	public static final String DEFAULT_PROJECT_FOLDER = ".";

	public static final String RESULTS_FOLDER = "results.folder";
	public static final String DEFAULT_RESULTS_FOLDER = "results";

	public static final String MIN_EXACT_CHUNK = "min.chunk";
	public static final int DEFAULT_MIN_EXACT_CHUNK = 10;

	public static final String MAX_LINEBIAS = "max.linebias";
	public static final int DEFAULT_MAX_LINEBIAS = 2;

	public static final String MIN_LENGTH = "min.length";
	public static final int DEFAULT_MIN_LENGTH = 30;

	public static final String MAX_LINESIZE = "max.linesize";
	public static final int DEFAULT_MAX_LINESIZE = 500;

	public static final String MAX_FILESIZE = "max.filessize";
	public static final int DEFAULT_MAX_FILESIZE = 10000;


	public static final String EXTENSIONS = "extensions";
	private static final String LANGUAGES = "languages";

	private static final String LINGUIST_FILE = "linguist.file";
	private static final String DEFAULT_LINGUIST_FILE = "languages.yml";

	private static final String IGNORE_FILE = "ignore.file";
	private static final String DEFAULT_IGNORE_FILE = ".ignore";

	public static String projectFolder = null;
	public static String resultsFolder = "results";
	public static String projectName = "";
	public static int minDuplicationLength = 20;
	public static int minExactChunk = 5;
	public static int maxLineBias = 3;
	public static int maxLineSize = 500;
	public static int maxFileSize = 10000;


	public static List<String> extensions = new ArrayList<>();
	public static List<String> languages = new ArrayList<>();
	public static ArrayList<String> fileNames = new ArrayList<>();

	private static Argumenthor init(List<String> args, String filename) {
		final Argumenthor argumenthor = configureArgumenthor(args, filename);
		projectFolder = (String) argumenthor.getRawValue(PROJECT_FOLDER);
		projectName = (String) argumenthor.getRawValue(PROJECT_NAME);
		resultsFolder = (String) argumenthor.getRawValue(RESULTS_FOLDER);
		minExactChunk = (int) argumenthor.getRawValue(MIN_EXACT_CHUNK);
		maxLineBias = (int) argumenthor.getRawValue(MAX_LINEBIAS);
		minDuplicationLength = (int) argumenthor.getRawValue(MIN_LENGTH);
		maxLineSize = (int) argumenthor.getRawValue(MAX_LINESIZE);
		if (maxLineSize < 0) maxLineSize = Integer.MAX_VALUE;
		maxFileSize = (int) argumenthor.getRawValue(MAX_FILESIZE);
		extensions = (List<String>) argumenthor.getRawValue(EXTENSIONS);
		languages = (List<String>) argumenthor.getRawValue(LANGUAGES);

		if (projectFolder == null) {
			System.err.println("Project folder was not initialized");
			System.exit(-1);
		}
		if (extensions.size() == 0) {
			extensions = Arrays.asList(".java", ".js", ".php", ".c", ".cc", ".cpp", ".h", ".hh", ".hpp", ".cs", ".sql", "lua", "groovy");
		}

		System.err.println("project.name=" + projectName);
		System.err.println("project.folder=" + projectFolder);
		System.err.println("results.folder=" + resultsFolder);
		System.err.println("extensions=" + String.join(",", extensions));
		System.err.println("languages=" + String.join(",", languages));
		System.err.println("min.length=" + minDuplicationLength);
		System.err.println("max.linebias= " + maxLineBias);
		System.err.println("min.chunk=" + minExactChunk);

		return argumenthor;
	}

	public static HashMap<String, List<Duplication>> resultsMap = new HashMap<>();

	public static void main(String[] args) throws IOException {
		final Argumenthor argumenthor = init(Arrays.asList(args), "config.txt");

		Processor processor = new SuffixTreeProcessor((String) argumenthor.getRawValue(LINGUIST_FILE), (String) argumenthor.getRawValue(IGNORE_FILE), projectFolder, new IdenticalCompareStrategy());

		Parameters params = new Parameters(minDuplicationLength, maxLineBias, minExactChunk, true);
		processor.setParams(params);

		processor.run();

		Duplication[] results = processor.getSearchResults();

		if (results.length == 0) {
			System.out.println("No duplication results");
			System.exit(-1);
		}

		for (int index = 0; index < results.length; index++) {
			String referenceFile = results[index].getReferenceCode().getEntityName();
			String duplicatedFile = results[index].getDuplicateCode().getEntityName();

			List<Duplication> duplicationForReferenceFile = resultsMap.get(referenceFile);
			if (duplicationForReferenceFile == null)
				duplicationForReferenceFile = new ArrayList<>();
			duplicationForReferenceFile.add(results[index]);
			resultsMap.put(referenceFile, duplicationForReferenceFile);

			if (results[index].isSelfDuplication())
				continue;

			List<Duplication> duplicationForSecondaryFile = resultsMap.get(duplicatedFile);
			if (duplicationForSecondaryFile == null)
				duplicationForSecondaryFile = new ArrayList<>();
			duplicationForSecondaryFile.add(results[index]);
			resultsMap.put(duplicatedFile, duplicationForSecondaryFile);
		}

		PrintWriter external_duplication = new PrintWriter(projectName + "-duplication.csv");

		List<PropertyDTO> listOfInternalDuplicationObjects = new ArrayList<>();

		int externalDuplicationCases = 0;
		for (String filename : resultsMap.keySet()) {
			List<Duplication> duplicationObjects = resultsMap.get(filename);

			int internalDuplicationLength = 0;
			for (Duplication crtDuplication : duplicationObjects) {
				if (crtDuplication.isSelfDuplication())
					internalDuplicationLength += crtDuplication.copiedLength();
				else {
					externalDuplicationCases++;
					external_duplication.println(crtDuplication.getReferenceCode().getEntityName() + ","
							+ crtDuplication.getDuplicateCode().getEntityName() + "," + crtDuplication.copiedLength());
				}
			}

			if (internalDuplicationLength > 0)
				listOfInternalDuplicationObjects.add(new PropertyDTO(filename, "Internal File Duplication",
						"Duplication", internalDuplicationLength));
		}

		File internalDuplicationFile = Paths.get(resultsFolder, projectName + "-internal_duplication.json").toFile();
		internalDuplicationFile.getAbsoluteFile().getParentFile().mkdirs();
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(internalDuplicationFile, listOfInternalDuplicationObjects);

		external_duplication.close();

		System.err.println(listOfInternalDuplicationObjects.size() + " file with internal duplication wrote to: "
				+ projectName + "-internal_duplication.json");
		System.err.println(externalDuplicationCases + " pairs of with files with inter-file duplication wrote to: "
				+ projectName + "-duplication.csv");
	}

	private static Argumenthor configureArgumenthor(List<String> args, String filename) {
		final ArgumenthorConfiguration configuration = new ArgumenthorConfiguration(
				new StringField(PROJECT_NAME, null),
				new StringField(PROJECT_FOLDER, DEFAULT_PROJECT_FOLDER),
				new StringField(RESULTS_FOLDER, DEFAULT_RESULTS_FOLDER),
				new IntField(MIN_EXACT_CHUNK, DEFAULT_MIN_EXACT_CHUNK),
				new IntField(MAX_LINEBIAS, DEFAULT_MAX_LINEBIAS),
				new IntField(MIN_LENGTH, DEFAULT_MIN_LENGTH),
				new IntField(MAX_LINESIZE, DEFAULT_MAX_LINESIZE),
				new IntField(MAX_FILESIZE, DEFAULT_MAX_FILESIZE),
				new StringListField(LANGUAGES, Collections.emptyList(), ","),
				new StringListField(EXTENSIONS, Collections.emptyList(), ","),
				new StringField(LINGUIST_FILE, DEFAULT_LINGUIST_FILE),
				new StringField(IGNORE_FILE, DEFAULT_IGNORE_FILE)
		);
		final ArgsSource source = new ArgsSource();
		source.setArgsList(args);
		configuration.addSource(source);
		configuration.addSource(new EnvSource("dude"));
		final PropertiesSource propertiesSource = new PropertiesSource();
		propertiesSource.setPath(filename);
		configuration.addSource(propertiesSource);
		return new Argumenthor(configuration);
	}
}
