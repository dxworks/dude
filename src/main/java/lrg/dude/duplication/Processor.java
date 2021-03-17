 // add some comment
 package lrg.dude.duplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class Processor extends Thread implements Subject {
    private ArrayList<Observer> observers = new ArrayList<Observer>();

    private Entity[] entities;
    private IMethodEntity referenceEntity;
    private MatrixLineList matrixLines;
    private VirtualMatrix coolMatrix;
    private DuplicationList duplicates;

    private StringCompareStrategy compareStrategy;

    private Parameters params = new Parameters(0, 1, 2, false);

    //statistical data
    private long numberOfRawLines = 0;
    private long numberOfDots = 0;  //in half of the matrix (non-redundant)
    private long numberOfDuplicatedLines = 0;


    public Processor() {
    	
    }
    
    /**
     * Constructor for dead mode (starting with a path)
     *
     * @param path The path where to start searching for files
     */
    public Processor(String path, StringCompareStrategy compareStrategy) {
        this.compareStrategy = compareStrategy;
        long start = System.currentTimeMillis();

        if(path.endsWith("/") || path.endsWith("\\")) path = path.substring(0, path.length() - 1);
        DirectoryReader cititorDirector = new DirectoryReader(path);
        ArrayList<File> files = cititorDirector.getFilesRecursive();
        
        if (files != null) {
            entities = new Entity[files.size()];
            for (int i = 0; i < files.size(); i++) {
                File currentFile = (File) files.get(i);
                //TODO: aici am problema daca e cale realativa
                String shortName = currentFile.getAbsolutePath().substring(path.length() + 1);
                entities[i] = new SourceFile(currentFile, shortName);
            }
        } else
            entities = new SourceFile[0];
        long stop = System.currentTimeMillis();
        System.out.print("\nDUDE: Got " + entities.length + " files in: ");
        System.out.println(TimeMeasurer.convertTimeToString(stop - start) + "\n");
    }


    /**
     * Constructor for the alive mode
     *
     * @param methods Entities (methods from MeMoJ / MeMoRIA)
     */
    public Processor(Entity[] methods, StringCompareStrategy compareStrategy) {
        this.compareStrategy = compareStrategy;
        entities = methods;
        referenceEntity = null;
    }


    public Processor(Entity[] methods) {
        this(methods, new IdenticalCompareStrategy());
    }

    public Processor(Entity[] methods, IMethodEntity reference) {
        this(methods, new IdenticalCompareStrategy());
        referenceEntity = reference;
    }

    public void run() {
    	long startTime = System.currentTimeMillis();
    	if(referenceEntity !=null) rearrangeEntities();
    	createMatrixLines();    /*cleans code*/
        if(referenceEntity == null) clusteredSearch();
        else clusteredSearchWithReferenceEntity();
       
        numberOfDuplicatedLines = matrixLines.countDuplicatedLines();
        notifyObservers();
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println("Computed duplications in: " + (currentTimeMillis - startTime) + "ms");
    }
    

    private void rearrangeEntities() {
    	int referenceIndex = 0;
    	if(referenceEntity == null) return;
    	IMethodEntity firstEntity = (IMethodEntity) entities[0];
    	if(firstEntity.getMethod() == referenceEntity.getMethod()) return;
    	
    	for(referenceIndex = 1; ((IMethodEntity)entities[referenceIndex]).getMethod() != referenceEntity.getMethod(); referenceIndex++);
    	
    	if(referenceIndex >= entities.length) {System.out.println("ERROR"); return; }
    	
    	entities[referenceIndex] = firstEntity;
    	entities[0] = referenceEntity;
    }

    
    private void clusteredSearch() {
        int startingMatrixRow = 0;
        int startingMatrixColumn = 0;
        //long start, end;
        int noOfEntities = entities.length;
        //progressMessage = "" + matrixPieces;
        coolMatrix = new VirtualMatrix(matrixLines.size());
        duplicates = new DuplicationList();
        System.out.println("NO OF ENTITIES (Processor.clusteredSearch): " + noOfEntities);
        for (int i = 0; i < noOfEntities; i++) {  //for every entity
            int noOfRows = entities[i].getNoOfRelevantLines();
            startingMatrixColumn = startingMatrixRow;
            for (int j = i; j < noOfEntities; j++) {  //for every other entity not yet processed
                int noOfColumns = entities[j].getNoOfRelevantLines();
                createMatrixCells(startingMatrixRow, noOfRows, startingMatrixColumn, noOfColumns);
                //when searching I don't have to limit width, the iterator works fine
                searchDuplicates(startingMatrixRow, noOfRows);
                coolMatrix.freeLines(startingMatrixRow, startingMatrixRow + noOfRows);
                startingMatrixColumn += noOfColumns;
                //progressValue++;
            }
            //end = System.currentTimeMillis();

            // System.out.println(TimeMeasurer.convertTimeToString(end - start));
            startingMatrixRow += noOfRows;
        }
        System.out.println("Duplicates size: " + duplicates.size() + ", Duplicate dots: " + numberOfDots);
    }

    private void clusteredSearchWithReferenceEntity() {
        int startingMatrixRow;
        int startingMatrixColumn;
        //long start, end;
        int noOfEntities = entities.length;

        coolMatrix = new VirtualMatrix(matrixLines.size());
        duplicates = new DuplicationList();
        System.out.println("NO OF ENTITIES: " + noOfEntities);
        int noOfRows = referenceEntity.getNoOfRelevantLines();
        startingMatrixColumn = startingMatrixRow = 0;
        for (int j = 0; j < noOfEntities; j++) {  
            int noOfColumns = entities[j].getNoOfRelevantLines();
            createMatrixCells(startingMatrixRow, noOfRows, startingMatrixColumn, noOfColumns);
            //when searching I don't have to limit width, the iterator works fine
            searchDuplicates(startingMatrixRow, noOfRows);
            coolMatrix.freeLines(startingMatrixRow, startingMatrixRow + noOfRows);
            startingMatrixColumn += noOfColumns;
        }
        System.out.println("NO OF Duplicates: " + duplicates.size());
        System.out.println("NO OF Duplicate Dots: " + numberOfDots);
    }    
    /**
     * Starting from the matrix lines ("clean" code), it compares the lines
     * to establish the matrix.
     *
     * @param startingMatrixRow
     * @param rows
     */
    private void createMatrixCells(int startingMatrixRow, int rows, int startingMatrixColumn, int columns) {
        String refCode;
        int endMatrixRow = startingMatrixRow + rows;
        int endMatrixColumn = startingMatrixColumn + columns;
        for (int i = startingMatrixRow; i < endMatrixRow; i++) {
            refCode = matrixLines.get(i).getCode();
            int start = startingMatrixRow == startingMatrixColumn ? i + 1 : startingMatrixColumn;
            for (int j = start; j < endMatrixColumn; j++) {  //aici am corectat in loc de j = 0!! RADU
                //TODO: aici tre sa modific cu strategia de comparare
                //if (refCode.equals(matrixLines.get(j).getCode())) {
                if (compareStrategy.similar(refCode, matrixLines.get(j).getCode())) {
                    coolMatrix.set(i, j, new Boolean(false));
                    numberOfDots++;
                }
            }
        }
    }


    /**
     * Once established the duplicate lines, this method will try to group
     * the lines in Duplication entities (fragments of duplicated code)
     *
     * @param startingMatrixRow
     * @param rows
     */
    private void searchDuplicates(int startingMatrixRow, int rows) {
        Duplication newDup;
        Iterator iterator;
        //for every cell above the diagonal
        int endMatrixRow = startingMatrixRow + rows;
        for (int i = startingMatrixRow; i < endMatrixRow; i++) {
            iterator = coolMatrix.iterator(i);  //returneaza urmatorul index (Integer)            
            while (iterator.hasNext()) {
                int j = ((Integer) iterator.next()).intValue();
                //if there is a duplicate [i,j] and it hasn't been used in a previous duplication
                if (coolMatrix.get(i, j) != null &&
                        !((Boolean) coolMatrix.get(i, j)).booleanValue() 
                        &&
                        (newDup = traceDuplication(i, j)) != null
                  ) {
                  duplicates.add(newDup);
                }
            }
        }
    }


    /**
     * Checks if the cell can be taken as part of the current duplication.
     * Checks out if the coordinate (reference.X+dx,reference.Y+dy) is within the matrix boundaries,
     * if it is a part of the same 2 entities as the current coordinate,
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
        if (newX < matrixLines.size() && /*still within the matrix*/
                newY < matrixLines.size() &&
                /*still within the same entity*/
                matrixLines.get(newX).getEntity() == matrixLines.get(oldX).getEntity() &&
                matrixLines.get(newY).getEntity() == matrixLines.get(oldY).getEntity() &&
                (coolMatrix.get(newX, newY)) != null && /*is duplicate*/
                ((Boolean) coolMatrix.get(newX, newY)).booleanValue() == false  /*not used*/
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
     * Starting from a duplicate cell in the matrix, this method will
     * try to follow some pattern (diagonal) to group duplicate code lines
     * into duplicate code fragments
     *
     * @param rowNo Row number of the cell where the pattern tracing starts
     * @param colNo Column number of the cell where the pattern tracing starts
     * @return Duplication entity or null if the Duplication is too short to be accepted.
     */
    private Duplication traceDuplication(int rowNo, int colNo) {
        CoordinateList coordinates = new CoordinateList();
        Coordinate start = new Coordinate(rowNo, colNo);
        Coordinate end = start;
        Coordinate current = start;
        coordinates.add(current);
        int currentExactChunkSize = 1;     //first duplication line is always an Exact
        while ((current = getNextCoordinate(current, currentExactChunkSize)) != null) {
            Coordinate previous = coordinates.get(coordinates.size() - 1);
            /*check that the duplication is not within the same entity,
            and the end of the referenceCode has not reached the start of the dupCode*/
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
            //remove coordinates representing the last exact chunk
            for (int i = 0; i < currentExactChunkSize; i++) {
                int index = coordinates.size() - 1;
                coordinates.remove(index);
            }
        }
        if (coordinates.size() > 0) {
            end = coordinates.get(coordinates.size() - 1);
            //length considered the number of lines that form the duplication chain
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
        /*create duplication*/
        Coordinate start = coordinates.get(0);
        Coordinate end = coordinates.get(coordinates.size() - 1);

        MatrixLine referenceStart = matrixLines.get(start.getX());
        MatrixLine referenceEnd = matrixLines.get(end.getX());
        MatrixLine duplicateStart = matrixLines.get(start.getY());
        MatrixLine duplicateEnd = matrixLines.get(end.getY());
        CodeFragment referenceCode = new CodeFragment(referenceStart.getEntity(),
                referenceStart.getRealIndex(), referenceEnd.getRealIndex());
        CodeFragment duplicateCode = new CodeFragment(duplicateStart.getEntity(),
                duplicateStart.getRealIndex(), duplicateEnd.getRealIndex());
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
            //set the matrixLines involved as duplicated (for the statistics)
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
        int exactChunkSize = 1; /*duplicates always start from an exact line duplication*/
        char separator = '.';
        for (int i = 1; i < coordinates.size(); i++) {
            Coordinate current = coordinates.get(i);
            Coordinate previous = coordinates.get(i - 1);

            int xBias = current.getX() - previous.getX() - 1;
            int yBias = current.getY() - previous.getY() - 1;

            if (xBias == yBias && xBias == 0) {     /*exact*/
                exactChunkSize++;
            } else {       /*delete, insert or modified*/
                signature.append("E" + exactChunkSize);
                exactChunkSize = 1;

                if (xBias == yBias && xBias > 0) {   /*modified*/
                    signature.append(separator + "M" + xBias + separator);
                } else if (xBias > 0) {     /*delete*/
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
     * Having the entities (method bodies, files etc.) the method will create
     * an array of Strings with "clean" code (without the code that should be ignored).
     *
     * @return array of "clean" code
     */
    public MatrixLineList createMatrixLines() {
        long start = System.currentTimeMillis();
        matrixLines = new MatrixLineList();
        int noOfMatrixLinesBefore, noOfMatrixLinesAfter;
        for (int i = 0; i < entities.length; i++) {
            noOfMatrixLinesBefore = matrixLines.size();
            if (entities[i] == null) { System.out.println("null"); }
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
    	if(referenceEntity == null) return;
    	IMethodEntity reference = referenceEntity;
    	IMethodEntity crtEntity = (IMethodEntity) entity;
    	
    	if(reference.getMethod() == crtEntity.getMethod())  {
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
        CleaningDecorator commonCleaner = new WhiteSpacesCleaner(new NoiseCleaner(null));
        CleaningDecorator cleaner;
        if (!params.isConsiderComments())
            cleaner = new CommentsCleaner(commonCleaner);
        else
            cleaner = commonCleaner;
        return cleaner.clean(bruteText);
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
        code = cleanCode(code);

        /*create matrix lines*/
        MatrixLineList matrixLines = new MatrixLineList();
        for (int i = 0; i < code.size(); i++) {
            if (code.get(i).length() > 0) {
                matrixLines.add(new MatrixLine(code.get(i), entity, i + 1));
            }
        }
        return matrixLines;
    }

    /**
     * ***********************************
     * Statistical data retrievers
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

    //Observer interface
    public void attach(Observer observer) {
        observers.add(observer);
    }

    public void detach(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        Iterator<Observer> iterator = observers.iterator();
        while (iterator.hasNext())
            (iterator.next()).getDuplication(this);
    }

    public void setParams(Parameters params) {
        this.params = params;
    }
}