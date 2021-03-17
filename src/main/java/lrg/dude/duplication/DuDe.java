package lrg.dude.duplication;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DuDe {
    public static final String PROJECT_NAME = "project.name=";
    public static final String PROJECT_FOLDER = "project.folder=";
    public static final String RESULTS_FOLDER = "results.folder=";
    public static final String MIN_EXACT_CHUNK = "min.chunk=";
    public static final String MAX_LINEBIAS = "max.linebias=";
    public static final String MIN_LENGTH = "min.length=";
    public static final String FILE_EXTENSIONS = "file.extensions=";
    public static final String MAX_LINESIZE = "max.linesize=";
    public static final String MAX_FILESIZE = "max.filessize=";

    public static String projectFolder = null;
    public static String resultsFolder = "results";
    public static String projectName = "";
    public static int minDuplicationLength = 20;
    public static int minExactChunk = 5;
    public static int maxLineBias = 3;
    public static int maxLineSize = 500;
    public static int maxFileSize = 10000;


    public static List<String> fileExtensions = new ArrayList<>();
    public static ArrayList<String> fileNames = new ArrayList<>();

    private static List<String> initFileExtensions(String listOfFileExtensions) {
        return Arrays.asList(Pattern.compile(",").split(listOfFileExtensions, 0));
    }

    private static void init(String filename) {
        try {
            System.err.println("Read config files from: " + Paths.get(filename).toAbsolutePath());
            Files.readAllLines(Paths.get(filename).toAbsolutePath()).stream()
                    .map(String::trim)
                    .forEach(line -> {
                        if (line.startsWith(PROJECT_FOLDER)) {
                            projectFolder = line.substring(PROJECT_FOLDER.length());
                        }
                        if (line.startsWith(PROJECT_NAME)) {
                            projectName = line.substring(PROJECT_NAME.length());
                        }
                        if (line.startsWith(RESULTS_FOLDER)) {
                            resultsFolder = line.substring(RESULTS_FOLDER.length());
                        }
                        if (line.startsWith(MIN_EXACT_CHUNK)) {
                            minExactChunk = Integer.parseInt(line.substring(MIN_EXACT_CHUNK.length()));
                        }
                        if (line.startsWith(MAX_LINEBIAS)) {
                            maxLineBias = Integer.parseInt(line.substring(MAX_LINEBIAS.length()));
                        }
                        if (line.startsWith(MIN_LENGTH)) {
                            minDuplicationLength = Integer.parseInt(line.substring(MIN_LENGTH.length()));
                        }
                        if (line.startsWith(FILE_EXTENSIONS)) {
                            fileExtensions = initFileExtensions(line.substring(FILE_EXTENSIONS.length()));
                        }
                        if (line.startsWith(MAX_LINESIZE)) {
                            maxLineSize = Integer.parseInt(line.substring(MAX_LINESIZE.length()));
                            if (maxLineSize < 0) maxLineSize = Integer.MAX_VALUE;
                        }
                        if (line.startsWith(MAX_FILESIZE)) {
                            maxFileSize = Integer.parseInt(line.substring(MAX_FILESIZE.length()));
                        }
                    });
        } catch (IOException e) {
            System.err.println("Cannot read file from: " + Paths.get(filename).toAbsolutePath());
            System.err.println("IOException message: " + e.getMessage());
        }

        if (projectFolder == null) {
            System.err.println("Project folder was not initialized");
            System.exit(-1);
        }
        if (fileExtensions.size() == 0) {
            fileExtensions.addAll(Arrays.asList(".java", ".js", ".php", ".c", ".cc", ".cpp", ".h", ".hh", ".hpp", ".cs", ".sql", "lua", "groovy"));
        }

        System.err.println("project.name=" + projectName);
        System.err.println("project.folder=" + projectFolder);
        System.err.println("results.folder=" + resultsFolder);
        System.err.println("file.extensions=" + String.join(",", fileExtensions));
        System.err.println("min.length=" + minDuplicationLength);
        System.err.println("max.linebias= " + maxLineBias);
        System.err.println("min.chunk=" + minExactChunk);
    }

    public static HashMap<String, List<Duplication>> resultsMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length == 1)
            init(args[0]);
        else
            init("config.txt");

        Processor processor = new SuffixTreeProcessor(projectFolder, new IdenticalCompareStrategy());

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
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(internalDuplicationFile, listOfInternalDuplicationObjects);

        external_duplication.close();

        System.err.println(listOfInternalDuplicationObjects.size() + " file with internal duplication wrote to: "
                + projectName + "-internal_duplication.json");
        System.err.println(externalDuplicationCases + " pairs of with files with inter-file duplication wrote to: "
                + projectName + "-duplication.csv");
    }
}
