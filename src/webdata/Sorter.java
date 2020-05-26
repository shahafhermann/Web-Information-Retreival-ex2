package webdata;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sorter {
    /* Data */
    private ArrayList<String> tokensArray;
    private ArrayList<String> productIdsArray;
    private HashMap<String, Integer> tokensMap;
    private HashMap<String, Integer> productIdMap;
    private HashMap<webdata.utils.Line, Integer> tokenLinesFrequency = new HashMap<>();
    private int numOfReviews = 0;

    private ArrayList<webdata.utils.Line> tokenLines = new ArrayList<>();
    private ArrayList<webdata.utils.Line> productIdLines = new ArrayList<>();

    /* String constants */
    private static final String SPLIT_TOKENS_REGEX = "[^A-Za-z0-9]+";

    /* File paths to save the terms lists */
    private String tmpDir;
    private static final int NUM_OF_REVIEWS_PER_FILE = 1000;
    private static final String SORT_TEMP_TOKEN_FILE_NAME = "sort_temp_token_%d.txt";
    private static final String SORT_TEMP_PRODUCT_FILE_NAME = "sort_temp_product_%d.txt";
    private int numOfTempFiles = 0;

    Sorter(ArrayList<String> tokensArray, ArrayList<String> productIdsArray, String tmpDir) {
        Collections.sort(tokensArray);
        this.tokensArray = tokensArray;
        this.tokensMap = buildHashMap(tokensArray);

        Collections.sort(productIdsArray);
        this.productIdsArray = productIdsArray;
        this.productIdMap = buildHashMap(productIdsArray);

        this.tmpDir = tmpDir;
    }

    void clear() {
        tokensMap = new HashMap<>();
        productIdMap = new HashMap<>();
    }

    private static HashMap<String, Integer> buildHashMap(ArrayList<String> toConvert) {
        HashMap<String, Integer> hmap = new HashMap<>();
        for (int i = 0; i < toConvert.size(); ++i) {
            hmap.put(toConvert.get(i), i);
        }
        return hmap;
    }

    ArrayList<String> getTokensArray() { return tokensArray; }

    ArrayList<String> getProductIdsArray() { return productIdsArray; }

    /**
     * Break a text to all it's tokens (alphanumeric).
     * @param text The text to break
     */
    private void breakText(String text) {
//        String[] tokens = text.split(SPLIT_TOKENS_REGEX);
//        for (String token: tokens) {
//            if (!token.isEmpty()) {
//                webdata.utils.Line line = createLine(tokensMap.get(token), 0);
//                if (tokenLinesFrequency.containsKey(line)) {
//                    int freq = tokenLinesFrequency.get(line);
//                    tokenLinesFrequency.put(line, freq + 1);
//                } else {
//                    tokenLinesFrequency.put(line, 1);
//                }
//            }
//        }
        ArrayList<String> tokens = new ArrayList<>(Arrays.asList(text.split(SPLIT_TOKENS_REGEX)));
        Collections.sort(tokens);
        String prevToken = "";
        int freq = 1;
        for (String token: tokens) {
            if (!token.isEmpty()) {
                if (!token.equals(prevToken)) {
                    if (!prevToken.isEmpty()) {
                        tokenLines.add(createLine(tokensMap.get(prevToken), freq));
                    }
                    prevToken = token;
                    freq = 1;
                } else {
                    ++freq;
                }
            }
        }

        if (!prevToken.isEmpty()) {
            tokenLines.add(createLine(tokensMap.get(prevToken), freq));
        }
    }

    /**
     * Create a new line object
     * @param term term of the line
     * @param freq frequency of the term
     * @return the new line object
     */
    private webdata.utils.Line createLine(int term, int freq){
        return new webdata.utils.Line(term + "#" + numOfReviews + "#" + freq);
    }

    /**
     * Parse the file
     * @param inputFile The file to parse
     */
    void firstPhase(String inputFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(inputFile)))){
            String line = reader.readLine();
            String textBuffer = "";
            boolean textFlag = false;
            while (line != null){

                if (textFlag && !line.startsWith("product/productId: ")) {
                    textBuffer = textBuffer.concat(" ").concat(line);
                    line = reader.readLine();
                    continue;
                }

                if (line.startsWith("product/productId: ")) {
                    textFlag = false;
                    if (!textBuffer.isEmpty()) {
                        breakText(textBuffer.toLowerCase());
                    }
                    ++numOfReviews;
//                    if (numOfReviews == 100001) {  // TODO: delete later
//                        break;
//                    }
//                    if (numOfReviews % 10001  == 0) {
//                        System.err.println("*************" + numOfReviews + "*************");
//                    }
                    if (numOfReviews % (NUM_OF_REVIEWS_PER_FILE + 1)  == 0) {
                        createTempFiles();
                    }
                    productIdLines.add(createLine(productIdMap.get(line.substring(19)), 1));
                    line = reader.readLine();
                    continue;
                }

                if (line.startsWith("review/text:")) {
                    textFlag = true;
                    textBuffer = line.substring(12);
                    line = reader.readLine();
                    continue;
                }

                line = reader.readLine();
            }

            if (!textBuffer.isEmpty()) {
                breakText(textBuffer.toLowerCase());
            }
            createTempFiles();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void countFrequencyHelper() {
        for (webdata.utils.Line line: tokenLinesFrequency.keySet()) {
            line.setFrequency(tokenLinesFrequency.get(line));
            tokenLines.add(line);
        }
        tokenLinesFrequency = new HashMap<>();
    }

    private void createTempFiles() {
//        countFrequencyHelper();
        Collections.sort(tokenLines);
        Collections.sort(productIdLines);
        writeMBlocks(tokenLines, SORT_TEMP_TOKEN_FILE_NAME);
        writeMBlocks(productIdLines, SORT_TEMP_PRODUCT_FILE_NAME);
        ++numOfTempFiles;
        tokenLines = new ArrayList<>();
        productIdLines = new ArrayList<>();
    }

    /**
     * Writes the blocks to a temp file with number tempNumber.
     * @param blocks The blocks to write.
     * @param fileName The file name to be used as the temp file
     */
    private void writeMBlocks(ArrayList<webdata.utils.Line> blocks, String fileName){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                new File(Paths.get(tmpDir, String.format(fileName, numOfTempFiles)).toString())), BLOCK_SIZE)){
            // Writes the block lines to the temp file
            for (webdata.utils.Line line:blocks) {
                writer.write(line.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    private static final int BLOCK_SIZE = (int) Math.pow(2, 19);
    private static final int NUMBER_LINES_IN_BLOCK = (int) Math.pow(2, 10);
    private static final int M = (int) Math.pow(2, 9);  // TODO check numbers

    /**
     * This method performs merge sort on the first column of the table in the given in file, R(A,B,C) or S(A,D,E),
     * and writes the output to the out file.
     * @param in The pathname of the file to read from.
     * @param outTokens The pathname of the token file to write to.
     * @param outProducts The pathname of the product file to write to.
     */
    public void sort(String in, String outTokens, String outProducts) {
        firstPhase(in);
        secondPhase(outTokens, tmpDir, numOfTempFiles, SORT_TEMP_TOKEN_FILE_NAME);
        secondPhase(outProducts, tmpDir, numOfTempFiles, SORT_TEMP_PRODUCT_FILE_NAME);
    }

    /**
     * This method performs the second phase of the two phase sort algorithm. It reads all lines from
     * numberOfTempFiles temp files into the output file.
     * @param out The output file name.
     * @param tmpPath The path of the temp files.
     * @param numberOfTempFiles The number of temp files.
     */
    private static void secondPhase(String out, String tmpPath, int numberOfTempFiles, String fileName){
        ArrayList<Map.Entry<File, BufferedReader>> readers = new ArrayList<>();
        Map<Integer, webdata.utils.Line> chunksLine = new HashMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(out)), BLOCK_SIZE)){
            initializeReaders(readers, chunksLine, tmpPath, numberOfTempFiles, fileName);
            int minimumChunkIndex;

            // While there are more lines left, write the next minimal line to the output
            while (!chunksLine.isEmpty()){
                minimumChunkIndex = getMinimumChunkIndex(chunksLine);
                writer.write(chunksLine.get(minimumChunkIndex).toString());
                writer.newLine();

                String newLine = readers.get(minimumChunkIndex).getValue().readLine();
                // If this was the last line in the file, close the reader and delete the temp file
                if (newLine != null)
                    chunksLine.put(minimumChunkIndex, new webdata.utils.Line(newLine));
                else {
                    chunksLine.remove(minimumChunkIndex);
                    readers.get(minimumChunkIndex).getValue().close();
                    readers.get(minimumChunkIndex).getKey().delete();
                    readers.set(minimumChunkIndex, null);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Gets the index of the chunk with the minimum value.
     * @param chunksLine None empty mapping between chunk index and current line from the chunk.
     * @return The index of the chunk with the minimal line.
     */
    private static int getMinimumChunkIndex(Map<Integer, webdata.utils.Line> chunksLine) {
        int minimumChunkIndex = Collections.min(chunksLine.keySet());
        for (int key : chunksLine.keySet()) {
            if (chunksLine.get(key).compareTo(chunksLine.get(minimumChunkIndex)) < 0)
                minimumChunkIndex = key;
        }

        return minimumChunkIndex;
    }

    /**
     * Initializes the temp file reader, and the chunk lines by reading the first line from each temp file (containing
     * a chunk).
     * @param readers The readers to initialize - arraylist of mappings between the file object and the reader
     *                reading it)
     * @param chunksLine The lines to initialize - mapping between the index of the reader and the current line
     *                   read from that file.
     * @param tmpPath The path to the temp files.
     * @param numberOfTempFiles The number of temp files.
     * @throws IOException IOException opening/reading the temp files.
     */
    private static void initializeReaders(ArrayList<Map.Entry<File, BufferedReader>> readers,
                                          Map<Integer, webdata.utils.Line> chunksLine, String tmpPath,
                                          int numberOfTempFiles, String fileName) throws IOException {
        for (int i = 0; i < numberOfTempFiles; i++) {
            File file = new File(Paths.get(tmpPath, String.format(fileName, i)).toString());
            readers.add(new AbstractMap.SimpleEntry<>(file, new BufferedReader(new FileReader(file), BLOCK_SIZE)));
            String line = readers.get(i).getValue().readLine();

            // Add the line read to the line map and update the minimum
            if (line != null)
                chunksLine.put(i, new webdata.utils.Line(line));
        }
    }
}