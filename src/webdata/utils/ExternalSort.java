//package webdata.utils;
//
//import java.io.*;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class ExternalSort {
//
//    private static final int BLOCK_SIZE = (int) Math.pow(2, 19);
//    private static final int NUMBER_LINES_IN_BLOCK = (int) Math.pow(2, 10);
//    private static final int M = (int) Math.pow(2, 9);  // TODO check numbers
//    private static final String SORT_TEMP_FILE_NAME = "sort_temp_%d.txt";
//
//    /***
//     * This method performs merge sort on the first column of the table in the given in file, R(A,B,C) or S(A,D,E),
//     * and writes the output to the out file.
//     * @param in The pathname of the file to read from.
//     * @param out The pathname of the file to write to.
//     * @param tmpPath The path for saving temporary files.
//     */
//    public static void sort(String in, String out, String tmpPath) {
//        secondPhase(out, tmpPath, firstPhase(in, tmpPath));
//    }
//
//    /**
//     * This method performs the second phase of the two phase sort algorithm. It reads all lines from
//     * numberOfTempFiles temp files into the output file.
//     * @param out The output file name.
//     * @param tmpPath The path of the temp files.
//     * @param numberOfTempFiles The number of temp files.
//     */
//    private static void secondPhase(String out, String tmpPath, int numberOfTempFiles){
//        ArrayList<Map.Entry<File, BufferedReader>> readers = new ArrayList<>();
//        Map<Integer, webdata.utils.Line> chunksLine = new HashMap<>();
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(out)), BLOCK_SIZE)){
//            initializeReaders(readers, chunksLine, tmpPath, numberOfTempFiles);
//            int minimumChunkIndex;
//
//            // While there are more lines left, write the next minimal line to the output
//            while (!chunksLine.isEmpty()){
//                minimumChunkIndex = getMinimumChunkIndex(chunksLine);
//                writer.write(chunksLine.get(minimumChunkIndex).toString());
//                writer.newLine();
//
//                String newLine = readers.get(minimumChunkIndex).getValue().readLine();
//                // If this was the last line in the file, close the reader and delete the temp file
//                if (newLine != null)
//                    chunksLine.put(minimumChunkIndex, new webdata.utils.Line(newLine));
//                else {
//                    chunksLine.remove(minimumChunkIndex);
//                    readers.get(minimumChunkIndex).getValue().close();
//                    readers.get(minimumChunkIndex).getKey().delete();
//                    readers.set(minimumChunkIndex, null);
//                }
//            }
//        } catch (IOException e) {
//            System.err.println(e.getMessage());
//        }
//    }
//
//    /**
//     * Gets the index of the chunk with the minimum value.
//     * @param chunksLine None empty mapping between chunk index and current line from the chunk.
//     * @return The index of the chunk with the minimal line.
//     */
//    private static int getMinimumChunkIndex(Map<Integer, webdata.utils.Line> chunksLine) {
//        int minimumChunkIndex = Collections.min(chunksLine.keySet());
//        for (int key : chunksLine.keySet()) {
//            if (chunksLine.get(key).compareTo(chunksLine.get(minimumChunkIndex)) < 0)
//                minimumChunkIndex = key;
//        }
//
//        return minimumChunkIndex;
//    }
//
//    /**
//     * Initializes the temp file reader, and the chunk lines by reading the first line from each temp file (containing
//     * a chunk).
//     * @param readers The readers to initialize - arraylist of mappings between the file object and the reader
//     *                reading it)
//     * @param chunksLine The lines to initialize - mapping between the index of the reader and the current line
//     *                   read from that file.
//     * @param tmpPath The path to the temp files.
//     * @param numberOfTempFiles The number of temp files.
//     * @throws IOException IOException opening/reading the temp files.
//     */
//    private static void initializeReaders(ArrayList<Map.Entry<File, BufferedReader>> readers,
//                                   Map<Integer, webdata.utils.Line> chunksLine, String tmpPath, int numberOfTempFiles)
//            throws IOException {
//        for (int i = 0; i < numberOfTempFiles; i++) {
//            File file = new File(Paths.get(tmpPath, String.format(SORT_TEMP_FILE_NAME, i)).toString());
//            readers.add(new AbstractMap.SimpleEntry<>(file, new BufferedReader(new FileReader(file), BLOCK_SIZE)));
//            String line = readers.get(i).getValue().readLine();
//
//            // Add the line read to the line map and update the minimum
//            if (line != null)
//                chunksLine.put(i, new webdata.utils.Line(line));
//        }
//    }
//}