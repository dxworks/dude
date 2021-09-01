package lrg.dude.duplication;

import org.ardverk.collection.AdaptedPatriciaTrie;
import org.dxworks.ignorerLibrary.Ignorer;
import org.dxworks.ignorerLibrary.IgnorerBuilder;
import org.dxworks.linguist.Linguist;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class SuffixTreeProcessor extends Processor {
	public static final String SPLIT_STRING = "\r\n|\r|\n";

	private ArrayList<Observer> observers = new ArrayList<Observer>();

	private Linguist linguist;
	private Ignorer ignorer;

	private Entity[] entities;
	private IMethodEntity referenceEntity;
	private int noOfRefLines;
	private MatrixLineList matrixLines;
	private VirtualColumnMatrix coolMatrix;
	private DuplicationList duplicates;

	private StringCompareStrategy compareStrategy;

	private Parameters params = new Parameters(0, 1, 2, false);

	// statistical data
	private long numberOfRawLines = 0;
	private long numberOfDots = 0; // in half of the matrix (non-redundant)
	private long numberOfDuplicatedLines = 0;

	/**
	 * Constructor for dead mode (starting with a path)
	 *
	 * @param rawValue
	 * @param linguistFile
	 * @param path         The path where to start searching for files
	 */
	public SuffixTreeProcessor(String linguistFile, String ignoreFile, String path, StringCompareStrategy compareStrategy) throws IOException {
		this.linguist = new Linguist(Path.of(linguistFile).toFile());
		this.ignorer = new IgnorerBuilder(Path.of(ignoreFile)).compile();
		this.compareStrategy = compareStrategy;
		long start = System.currentTimeMillis();
		DirectoryReader cititorDirector = new DirectoryReader(path);

		if (path.endsWith("/") || path.endsWith("\\")) path = path.substring(0, path.length() - 1);

		PrintWriter ignoredFiles = new PrintWriter("ignored_files.csv");
		PrintWriter linesTooLong = new PrintWriter("ignored_lines.csv");

		int counterSmall = 0;
		int counterLarge = 0;

		int countFilesWithIgnoredLines = 0;
		int countIgnoredLines = 0;

		ignoredFiles.println("filename,size,category,categoryThreshold");
		linesTooLong.println("filename,lineNumber,size,threshold");

		final ArrayList<File> filesRecursive = cititorDirector.getFilesRecursive();
		final int allFilesSize = filesRecursive.size();
		List<File> files = filesRecursive
				.stream().filter(file -> ignorer.accepts(file.getAbsolutePath()) && isSourceFile(file))
				.collect(Collectors.toList());

		System.out.println("Ignored " + (allFilesSize - files.size()) + " files from ignore file (out of " + allFilesSize + " files)");

		ArrayList<Entity> allFiles = new ArrayList<>();
		for (File currentFile : files) {
			// this check is needed to filter only source files, else it will throw an
			// exception
				// TODO: aici am problema daca e cale relativa
				String shortName = currentFile.getAbsolutePath().substring(path.length() + 1);

				Entity aFile = new SourceFile(currentFile, shortName);

				List<String> ignoredLines = ((SourceFile) aFile).ignoredLines;
				if (ignoredLines.size() > 0) {
					countIgnoredLines += ignoredLines.size();
					countFilesWithIgnoredLines++;
					ignoredLines.forEach(line -> linesTooLong.println(line));
				}

				int lines = aFile.getCode().size();
				if (lines < DuDe.minDuplicationLength) {
					ignoredFiles.println(aFile.getName() + "," + lines + ",SMALL," + DuDe.minDuplicationLength);
					counterSmall++;
				} else if (lines >= DuDe.maxFileSize) {
					ignoredFiles.println(aFile.getName() + "," + lines + ",LARGE," + DuDe.maxFileSize);
					counterLarge++;
				} else allFiles.add(aFile);
		}
		entities = allFiles.toArray(new Entity[allFiles.size()]);
		long stop = System.currentTimeMillis();
		System.out.print("\nDUDE: Got " + entities.length + " files in: ");
		System.out.println(TimeMeasurer.convertTimeToString(stop - start) + "\n");

		if (counterSmall > 0)
			System.out.println("Ignored " + counterSmall + " too small files (<" + DuDe.minDuplicationLength + ")");
		if (counterLarge > 0)
			System.out.println("Ignored " + counterLarge + " too large files (>=" + DuDe.maxFileSize + ")");
		if (countFilesWithIgnoredLines > 0)
			System.out.println("Ignored " + countIgnoredLines + " too large lines (> " + DuDe.maxLineSize + " characters) in " + countFilesWithIgnoredLines + " files");

		ignoredFiles.close();
		linesTooLong.close();
	}


	/**
	 * Constructor for the alive mode
	 *
	 * @param methods Entities (methods from MeMoJ / MeMoRIA)
	 */
	public SuffixTreeProcessor(Entity[] methods, StringCompareStrategy compareStrategy) {
		this.compareStrategy = compareStrategy;
		entities = methods;
		referenceEntity = null;
	}

	public SuffixTreeProcessor(Entity[] methods) {
		this(methods, new IdenticalCompareStrategy());
	}

	public SuffixTreeProcessor(Entity[] methods, IMethodEntity reference) {
		this(methods, new IdenticalCompareStrategy());
		referenceEntity = reference;
	}

	private boolean isSourceFile(File currentFile) {
		String absolutePath = currentFile.getAbsolutePath();

		for (String extension : DuDe.extensions) {
			if (!extension.isEmpty() && absolutePath.endsWith(extension) && isAcceptable(absolutePath))
				return true;
		}

		for (String filename : DuDe.fileNames) {
			String fname = absolutePath.substring(absolutePath.lastIndexOf(File.separator) + 1);

			if (fname.equals(filename) && isAcceptable(absolutePath)) {
				System.out.println(absolutePath + " included because it matches filename: " + filename);
				return true;
			}
		}

		return linguist.isOf(absolutePath, DuDe.languages.toArray(new String[0]));
	}

	private boolean isAcceptable(String absolutePath) {
		return true;
	}

	public static double time;

	public void run() {
		time = 0.0;
		long startTime = System.currentTimeMillis();
		if (referenceEntity != null)
			rearrangeEntities();
		createNewMatrixLines(); /* cleans code */
		/* if(referenceEntity == null) */
		clusteredSearchWithSuffixTries();
		// else clusteredSearchWithReferenceEntity();

		numberOfDuplicatedLines = matrixLines.countDuplicatedLines();
		notifyObservers();
		long currentTimeMillis = System.currentTimeMillis();
		time = ((double) (currentTimeMillis - startTime)) / 1000.0;
		System.out.println("Computed duplications in: " + time + " seconds");
	}

	/**
	 * Cleans the code of a single entity (method body, file)
	 *
	 * @param entity The entity to work on
	 * @return an array of "clean" code (no whitespaces etc.)
	 */
	private MatrixLineList entityToNewMatrixLines(Entity entity, int startPos) {
		StringList code = entity.getCode();
		numberOfRawLines += code.size();
		try {
			code = cleanCode(code);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		/* create matrix lines */
		int matrixPos = startPos;
		MatrixLineList matrixLines = new MatrixLineList();
		for (int i = 0; i < code.size(); i++) {
			if (code.get(i).length() > 0) {
				matrixLines.add(new MatrixLine(code.get(i), entity, i + 1, matrixPos++));
			}
		}
		return matrixLines;
	}

	/**
	 * Having the entities (method bodies, files etc.) the method will create an
	 * array of Strings with "clean" code (without the code that should be ignored).
	 *
	 * @return array of "clean" code
	 */
	public MatrixLineList createNewMatrixLines() {
		long start = System.currentTimeMillis();
		matrixLines = new MatrixLineList();
		int noOfMatrixLinesBefore, noOfMatrixLinesAfter;
		for (int i = 0; i < entities.length; i++) {
			noOfMatrixLinesBefore = matrixLines.size();

			if (entities[i] == null) {
				System.out.println("entities[" + i + "] is null");
			}

			matrixLines.addAll(entityToNewMatrixLines(entities[i], noOfMatrixLinesBefore));
			noOfMatrixLinesAfter = matrixLines.size();
			entities[i].setNoOfRelevantLines(noOfMatrixLinesAfter - noOfMatrixLinesBefore);
			setRelevantLinesForReferenceEntity(entities[i]);
		}
		long stop = System.currentTimeMillis();
		System.out.print("DUDE: Got " + matrixLines.size() + " lines of clean code in: ");
		System.out.println(TimeMeasurer.convertTimeToString(stop - start) + "\n");
		return matrixLines;
	}

	private void clusteredSearchWithSuffixTries() {
		int startingMatrixColumn;
		int noOfEntities = entities.length;
		noOfRefLines = 0;

		duplicates = new DuplicationList();
		AdaptedPatriciaTrie trie = new AdaptedPatriciaTrie();
		startingMatrixColumn = 0;
		int totalNoOfRows = 0;

		long start = 0;
		long first = System.currentTimeMillis();
		long stop = 0;
		double duration = 0.0;

		int crtPercent = 0;

		for (int j = 0; j < noOfEntities; j++) {

			// add lines to trie and create dot-matrix
			int noOfColumns = entities[j].getNoOfRelevantLines();

			if (referenceEntity != null) {
				if (j == 0) {
					noOfRefLines = noOfColumns;
				}
			}

			int perc = j * 100 / noOfEntities;
			if (perc >= crtPercent + 5) {
				crtPercent = perc;
				System.err.println(crtPercent + "% files processed (" + j + "/" + noOfEntities + ")");
			}

			try {
				coolMatrix = new VirtualColumnMatrix(noOfColumns, startingMatrixColumn);

				start = System.currentTimeMillis();
				createMatrixCells(totalNoOfRows, noOfColumns, trie);
				totalNoOfRows += noOfColumns;
				// search dot-matrix for duplicates
				if (referenceEntity != null) {
					// if reference entity, search dot-matrix against the reference only
					searchRefDuplicates(startingMatrixColumn, noOfColumns, noOfRefLines);
				} else {
					// if no reference entity, search dot-matrix against all previous entities
					searchDuplicates(startingMatrixColumn, noOfColumns);
					duration = (System.currentTimeMillis() - start) / 1000;
					if (duration >= 1.0) {
						System.err.println("File " + j + " of " + noOfEntities + ": " + entities[j].getName() + ": " + noOfColumns + " lines");
						System.err.println("Search duplicates in matrix (" + totalNoOfRows + " x " + noOfColumns + ") in " + duration + " sec.");
					}
				}
				// removeAll does also the free
				coolMatrix.removeAll();
				coolMatrix = null;
				startingMatrixColumn += noOfColumns;

			} catch (OutOfMemoryError e) {
				stop = System.currentTimeMillis();
				long durationInSeconds = (stop - first) / 1000;
				System.err.println("Out of memory at file: " + entities[j].getName() + ": " + noOfColumns + " lines analysed in " + durationInSeconds + " seconds");
				System.exit(-1);
			}
		}
	}

	/**
	 * Starting from the matrix lines ("clean" code), it compares the lines to
	 * establish the matrix.
	 *
	 * @param startingMatrixRow
	 * @param rows
	 */
	private void createMatrixCells(int startingMatrixColumn, int columns, AdaptedPatriciaTrie trie) {
		int endMatrixColumn = startingMatrixColumn + columns;
		for (int j = startingMatrixColumn; j < endMatrixColumn; j++) { // aici am corectat in loc de j = 0!! RADU
			MatrixLineList ml = new MatrixLineList();
			ml.add(matrixLines.get(j));
			MatrixLineList newList = trie.put(matrixLines.get(j).getCode(), ml);
			if (null != newList) {
				for (int i = 0; i < newList.size(); i++) {
					if ((referenceEntity != null)
							&& (j >= noOfRefLines && newList.get(i).getMatrixIndex() >= noOfRefLines)) {
						continue;
					}
					coolMatrix.set(newList.get(i).getMatrixIndex(), j, Boolean.valueOf(false));
					numberOfDots++;
				}
			}
		}
	}

	private void rearrangeEntities() {
		int referenceIndex = 0;
		if (referenceEntity == null)
			return;
		IMethodEntity firstEntity = (IMethodEntity) entities[0];
		if (firstEntity.getMethod() == referenceEntity.getMethod())
			return;

		for (referenceIndex = 1; ((IMethodEntity) entities[referenceIndex]).getMethod() != referenceEntity
				.getMethod(); referenceIndex++)
			;

		if (referenceIndex >= entities.length) {
			System.out.println("ERROR");
			return;
		}

		entities[referenceIndex] = firstEntity;
		entities[0] = referenceEntity;
	}

	/**
	 * Once established the duplicate lines, this method will try to group the lines
	 * in Duplication entities (fragments of duplicated code)
	 *
	 * @param startingMatrixRow
	 * @param rows
	 */
	private void searchDuplicates(int startingMatrixColumn, int columns) {
		Duplication newDup;
		Iterator iterator;
		// for every cell above the diagonal
		int endMatrixColumn = startingMatrixColumn + columns;
		for (int i = startingMatrixColumn; i < endMatrixColumn; i++) {
			iterator = coolMatrix.iterator(i); // returneaza urmatorul index (Integer)
			while (iterator.hasNext()) {
				int j = ((Integer) iterator.next()).intValue();
				// if there is a duplicate [i,j] and it hasn't been used in a previous
				// duplication
				if (coolMatrix.get(j, i) != null && !((Boolean) coolMatrix.get(j, i)).booleanValue()
						&& (newDup = traceDuplication(j, i)) != null) {
					duplicates.add(newDup);
				}
			}
		}
	}

	private void searchRefDuplicates(int startingMatrixColumn, int columns, int refRows) {
		Duplication newDup;
		Iterator iterator;
		// for every cell above the diagonal
		int endMatrixColumn = startingMatrixColumn + columns;
		for (int i = startingMatrixColumn; i < endMatrixColumn; i++) {
			iterator = coolMatrix.iterator(i); // returneaza urmatorul index (Integer)
			while (iterator.hasNext()) {
				int j = ((Integer) iterator.next()).intValue();
				// if there is a duplicate [i,j] and it hasn't been used in a previous
				// duplication
				if (j < refRows && coolMatrix.get(j, i) != null && !((Boolean) coolMatrix.get(j, i)).booleanValue()
						&& (newDup = traceDuplication(j, i)) != null) {
					duplicates.add(newDup);
				}
			}
		}
	}

	/**
	 * Checks if the cell can be taken as part of the current duplication. Checks
	 * out if the coordinate (reference.X+dx,reference.Y+dy) is within the matrix
	 * boundaries, if it is a part of the same 2 entities as the current coordinate,
	 * and if it is a duplication
	 *
	 * @param reference Current coordinate
	 * @param dx        the x bias
	 * @param dy        the y bias
	 * @return true if it is a valid coordinate, and false if not
	 */
	private boolean validCoordinate(Coordinate reference, int dx, int dy) {
		int oldX = reference.getX();
		int oldY = reference.getY();
		int newX = oldX + dx;
		int newY = oldY + dy;
		if (newX < matrixLines.size() && /* still within the matrix */
				newY < matrixLines.size() &&
				/* still within the same entity */
				matrixLines.get(newX).getEntity() == matrixLines.get(oldX).getEntity()
				&& matrixLines.get(newY).getEntity() == matrixLines.get(oldY).getEntity()
				&& (coolMatrix.get(newX, newY)) != null && /* is duplicate */
				((Boolean) coolMatrix.get(newX, newY)).booleanValue() == false /* not used */
		) {
			return true;
		} else
			return false;
	}

	/**
	 * Tries to find a valid coordinate to be added to the current Duplication
	 *
	 * @param start The last coordinate of the current Duplication
	 * @return The next valid coordinate or null if none found
	 */
	private Coordinate getNextCoordinate(Coordinate start, int currentExactSize) {
		if (validCoordinate(start, 1, 1)) {
			return new Coordinate(start.getX() + 1, start.getY() + 1);
		}
		if (currentExactSize < params.getMinExactChunk())
			return null;
		for (int i = 1; i <= params.getMaxLineBias(); i++) {
			if (validCoordinate(start, 1, 1 + i)) {
				return new Coordinate(start.getX() + 1, start.getY() + 1 + i);
			}
		}
		for (int i = 1; i <= params.getMaxLineBias(); i++) {
			if (validCoordinate(start, 1 + i, 1)) {
				return new Coordinate(start.getX() + 1 + i, start.getY() + 1);
			}
		}
		for (int i = 1; i <= params.getMaxLineBias(); i++) {
			if (validCoordinate(start, 1 + i, 1 + i)) {
				return new Coordinate(start.getX() + 1 + i, start.getY() + 1 + i);
			}
		}
		return null;
	}

	/**
	 * Starting from a duplicate cell in the matrix, this method will try to follow
	 * some pattern (diagonal) to group duplicate code lines into duplicate code
	 * fragments
	 *
	 * @param rowNo Row number of the cell where the pattern tracing starts
	 * @param colNo Column number of the cell where the pattern tracing starts
	 * @return Duplication entity or null if the Duplication is too short to be
	 * accepted.
	 */
	private Duplication traceDuplication(int rowNo, int colNo) {
		CoordinateList coordinates = new CoordinateList();
		Coordinate start = new Coordinate(rowNo, colNo);
		Coordinate end = start;
		Coordinate current = start;
		coordinates.add(current);
		int currentExactChunkSize = 1; // first duplication line is always an Exact
		while ((current = getNextCoordinate(current, currentExactChunkSize)) != null) {
			Coordinate previous = coordinates.get(coordinates.size() - 1);
			/*
			 * check that the duplication is not within the same entity, and the end of the
			 * referenceCode has not reached the start of the dupCode
			 */
			if (current.getX() < start.getY())
				coordinates.add(current);
			else
				continue;
			int dx = current.getX() - previous.getX();
			int dy = current.getY() - previous.getY();
			if (dx == 1 && dy == 1)
				currentExactChunkSize++;
			else
				currentExactChunkSize = 1;
		}
		if (currentExactChunkSize < params.getMinExactChunk()) {
			// remove coordinates representing the last exact chunk
			for (int i = 0; i < currentExactChunkSize; i++) {
				int index = coordinates.size() - 1;
				coordinates.remove(index);
			}
		}
		if (coordinates.size() > 0) {
			end = coordinates.get(coordinates.size() - 1);
			// length considered the number of lines that form the duplication chain
			int lengthX = end.getX() - start.getX() + 1;
			int lengthY = end.getY() - start.getY() + 1;
			int length = lengthX <= lengthY ? lengthX : lengthY;
			if (length >= params.getMinLength())
				return makeDuplication(coordinates, length);
		}
		return null;
	}

	/**
	 * Makes a duplication entity starting from a list of coordinates
	 *
	 * @param coordinates
	 * @return new Duplication entity
	 */
	private Duplication makeDuplication(CoordinateList coordinates, int length) {
		Duplication newDuplication;
		String signature = extractSignature(coordinates);
		DuplicationType type = extractType(signature);
		markCoordinates(coordinates);
		/* create duplication */
		Coordinate start = coordinates.get(0);
		Coordinate end = coordinates.get(coordinates.size() - 1);

		MatrixLine referenceStart = matrixLines.get(start.getX());
		MatrixLine referenceEnd = matrixLines.get(end.getX());
		MatrixLine duplicateStart = matrixLines.get(start.getY());
		MatrixLine duplicateEnd = matrixLines.get(end.getY());
		CodeFragment referenceCode = new CodeFragment(referenceStart.getEntity(), referenceStart.getRealIndex(),
				referenceEnd.getRealIndex());
		CodeFragment duplicateCode = new CodeFragment(duplicateStart.getEntity(), duplicateStart.getRealIndex(),
				duplicateEnd.getRealIndex());
		newDuplication = new Duplication(referenceCode, duplicateCode, type, signature, length);
		return newDuplication;
	}

	/**
	 * Marks the used coordinates in the matrix
	 *
	 * @param coordinates The coordinates list
	 */
	private void markCoordinates(CoordinateList coordinates) {
		int size = coordinates.size();
		for (int i = 0; i < size; i++) {
			Coordinate current = coordinates.get(i);
			coolMatrix.set(current.getX(), current.getY(), new Boolean(true));
			// set the matrixLines involved as duplicated (for the statistics)
			matrixLines.get(current.getX()).setDuplicated();
			matrixLines.get(current.getY()).setDuplicated();
		}
	}

	/**
	 * Extracts the duplication type, from a given signature
	 *
	 * @param signature Duplication's signature
	 * @return Duplication type
	 */
	private DuplicationType extractType(String signature) {
		StringBuffer buffer = new StringBuffer(signature);
		int iModified = buffer.indexOf("M");
		int iDelete = buffer.indexOf("D");
		int iInsert = buffer.indexOf("I");
		if (iModified < 0 && iDelete < 0 && iInsert < 0)
			return DuplicationType.EXACT;
		if (iModified > -1 && iDelete < 0 && iInsert < 0)
			return DuplicationType.MODIFIED;
		if (iModified < 0 && iDelete > -1 && iInsert < 0)
			return DuplicationType.DELETE;
		if (iModified < 0 && iDelete < 0 && iInsert > -1)
			return DuplicationType.INSERT;
		return DuplicationType.COMPOSED;
	}

	/**
	 * Extracts a duplicate signature starting from a list of Coordinates
	 *
	 * @param coordinates The list
	 * @return The duplication signature
	 */
	private String extractSignature(CoordinateList coordinates) {
		StringBuffer signature = new StringBuffer();
		int exactChunkSize = 1; /* duplicates always start from an exact line duplication */
		char separator = '.';
		for (int i = 1; i < coordinates.size(); i++) {
			Coordinate current = coordinates.get(i);
			Coordinate previous = coordinates.get(i - 1);

			int xBias = current.getX() - previous.getX() - 1;
			int yBias = current.getY() - previous.getY() - 1;

			if (xBias == yBias && xBias == 0) { /* exact */
				exactChunkSize++;
			} else { /* delete, insert or modified */
				signature.append("E" + exactChunkSize);
				exactChunkSize = 1;

				if (xBias == yBias && xBias > 0) { /* modified */
					signature.append(separator + "M" + xBias + separator);
				} else if (xBias > 0) { /* delete */
					signature.append(separator + "D" + xBias + separator);
				} else if (yBias > 0) {
					signature.append(separator + "I" + yBias + separator);
				}
			}
		}
		signature.append("E" + exactChunkSize);
		return signature.toString();
	}

	/**
	 * Having the entities (method bodies, files etc.) the method will create an
	 * array of Strings with "clean" code (without the code that should be ignored).
	 *
	 * @return array of "clean" code
	 */
	public MatrixLineList createMatrixLines() {
		long start = System.currentTimeMillis();
		matrixLines = new MatrixLineList();
		int noOfMatrixLinesBefore, noOfMatrixLinesAfter;
		for (int i = 0; i < entities.length; i++) {
			noOfMatrixLinesBefore = matrixLines.size();
			if (entities[i] == null) {
				System.out.println("null");
			}
			matrixLines.addAll(entityToMatrixLines(entities[i]));
			noOfMatrixLinesAfter = matrixLines.size();
			entities[i].setNoOfRelevantLines(noOfMatrixLinesAfter - noOfMatrixLinesBefore);
			setRelevantLinesForReferenceEntity(entities[i]);
		}
		long stop = System.currentTimeMillis();
		System.out.print("\nDUDE: Got " + matrixLines.size() + " lines of clean code in: ");
		System.out.println(TimeMeasurer.convertTimeToString(stop - start) + "\n");
		return matrixLines;
	}

	private void setRelevantLinesForReferenceEntity(Entity entity) {
		if (referenceEntity == null)
			return;
		IMethodEntity reference = referenceEntity;
		IMethodEntity crtEntity = (IMethodEntity) entity;

		if (reference.getMethod() == crtEntity.getMethod()) {
			reference.setNoOfRelevantLines(entity.getNoOfRelevantLines());
		}

	}

	/**
	 * Filters the source code fragment
	 *
	 * @param bruteText Code unfiltered
	 * @return clean code
	 */
	private StringList cleanCode(StringList bruteText) {
		return DuplicationUtil.cleanCode(bruteText, params.isConsiderComments());
	}

	public int computeLinesOfCleanCode(String codeFragment) {
		StringList stringList = new StringList(codeFragment.split(SPLIT_STRING));
		StringList cleanCode = DuplicationUtil.cleanCode(stringList, false);
		int length = 0;
		for (int i = 0; i < cleanCode.size(); i++) {
			if (cleanCode.get(i).isEmpty())
				continue;
			length++;
		}
		return length;
	}

	/**
	 * Cleans the code of a single entity (method body, file)
	 *
	 * @param entity The entity to work on
	 * @return an array of "clean" code (no whitespaces etc.)
	 */
	private MatrixLineList entityToMatrixLines(Entity entity) {
		StringList code = entity.getCode();
		numberOfRawLines += code.size();

		try {
			code = cleanCode(code);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		/* create matrix lines */
		MatrixLineList matrixLines = new MatrixLineList();
		for (int i = 0; i < code.size(); i++) {
			if (code.get(i).length() > 0) {
				matrixLines.add(new MatrixLine(code.get(i), entity, i + 1));
			}
		}
		return matrixLines;
	}

	/**
	 * *********************************** Statistical data retrievers
	 * *************************************
	 */

	public long getNumberOfRawLines() {
		return numberOfRawLines;
	}

	public long getNumberOfCleanLines() {
		if (matrixLines != null)
			return matrixLines.size();
		else
			return -1;
	}

	public int getNumberOfEntities() {
		if (entities != null)
			return entities.length;
		else
			return -1;
	}

	public long getNumberOfDots() {
		return numberOfDots;
	}

	public Duplication[] getSearchResults() {
		return duplicates.toArray();
	}

	public long getNumberOfDuplicatedLines() {
		return numberOfDuplicatedLines;
	}

	/**
	 * Pentru eventuale date statistice
	 *
	 * @return
	 */
	public int getMatrixLinesLength() {
		return matrixLines.size();
	}

	public Entity[] testGetEntities() {
		return entities;
	}

	// Observer interface
	public void attach(Observer observer) {
		observers.add(observer);
	}

	public void detach(Observer observer) {
		observers.remove(observer);
	}

	public void notifyObservers() {
		Iterator<Observer> iterator = observers.iterator();
		while (iterator.hasNext())
			iterator.next().getDuplication(this);
	}

	public void setParams(Parameters params) {
		this.params = params;
	}
}
