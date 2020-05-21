package webdata;

import webdata.utils.ExternalSort;

import java.io.*;
import java.text.SimpleDateFormat;

/**
 *
 */
public class SlowIndexWriter{

    static final String tokenDictFileName = "tokenDict";
    static final String productDictFileName = "productDict";
    static final String reviewDataFileName = "reviewData";
    static final String productPostingListFileName = "productPostingList";
    static final String tokenPostingListFileName = "tokenPostingList";
    private final String tokensFileName = "tokenFile";
    private final String productsFileName = "productFile";
    private final String sortedIndicator = "_sorted";

    private void takeTime(String msg) {
        String timeStamp =  new SimpleDateFormat("HH.mm.ss.SS").format(new java.util.Date());
        System.err.println(msg + " at " + timeStamp);
    }

    /**
     * Given product review data, creates an on disk index.
     * @param inputFile The path to the file containing the review data.
     * @param dir the directory in which all index files will be created if the directory does not exist, it should be
     *            created.
     */
    public void slowWrite(String inputFile, String dir) {
        File dirFile = new File(dir);
        if (dirFile.exists()) {
            removeFiles(dir);
        } else {  // Create it
            try{
                dirFile.mkdir();
            }
            catch(SecurityException e){
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        String tokensFilePath = dir + File.separator + tokensFileName;
        String productsFilePath = dir + File.separator + productsFileName;
        String sortedTokensFilePath = dir + File.separator + tokensFileName + sortedIndicator;
        String sortedProductsFilePath = dir + File.separator + productsFileName + sortedIndicator;

        takeTime("<<<<<<<<<<< *STARTED PARSING* >>>>>>>>>>");
        ReviewsParser parser = new ReviewsParser(tokensFilePath, productsFilePath);
        parser.parseFile(inputFile);
        takeTime("<<<<<<<<<<< *PARSE DONE* >>>>>>>>>>");

        Dictionary tokenDict = buildDictionary(tokensFilePath, sortedTokensFilePath, dir, false);
        takeTime("<<<<<<<<<<< *DONE BUILDING TOKEN DICTIONARY* >>>>>>>>>>");
        Dictionary productDict = buildDictionary(productsFilePath, sortedProductsFilePath, dir, true);
        takeTime("<<<<<<<<<<< *DONE BUILDING PRODUCT DICTIONARY* >>>>>>>>>>");

        ReviewData rd = new ReviewData(parser.getProductIds(), parser.getReviewHelpfulnessNumerator(),
                                       parser.getReviewHelpfulnessDenominator(), parser.getReviewScore(),
                                       parser.getTokensPerReview(), parser.getNumOfReviews());
        takeTime("<<<<<<<<<<< *DONE BUILDING REVIEW DATA* >>>>>>>>>>");

        try {
            /* Write the new files */
            ObjectOutputStream tokenDictWriter = new ObjectOutputStream(new FileOutputStream(dir + File.separator + tokenDictFileName));
            tokenDictWriter.writeObject(tokenDict);
            tokenDictWriter.close();
            takeTime("<<<<<<<<<<< *DONE WRITING TOKEN DICT TO FILE* >>>>>>>>>>");

            ObjectOutputStream productDictWriter = new ObjectOutputStream(new FileOutputStream(dir + File.separator + productDictFileName));
            productDictWriter.writeObject(productDict);
            productDictWriter.close();
            takeTime("<<<<<<<<<<< *DONE WRITING PRODUCT DICT TO FILE* >>>>>>>>>>");

            ObjectOutputStream reviewDataWriter = new ObjectOutputStream(new FileOutputStream(dir + File.separator + reviewDataFileName));
            reviewDataWriter.writeObject(rd);
            reviewDataWriter.close();
            takeTime("<<<<<<<<<<< *DONE WRITING REVIEW DATA TO FILE* >>>>>>>>>>");
        } catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Delete all index files by removing the given directory.
     * @param dir The directory to remove the index from.
     */
    public void removeIndex(String dir) {
        File dirFile = new File(dir);
        if (dirFile.exists()) {
            String[] entries = dirFile.list();
            if (entries != null) {
                for(String s: entries){
                    File currentFile = new File(dirFile, s);
                    currentFile.delete();
                }
            }
            dirFile.delete();
        }
    }

    /**
     * Delete all index files (and only the files).
     * @param dir The directory to remove the index from.
     */
    private void removeFiles(String dir) {
        deleteFile(dir, tokenDictFileName);
        deleteFile(dir, productDictFileName);
        deleteFile(dir, reviewDataFileName);
        deleteFile(dir, productPostingListFileName);
        deleteFile(dir, tokenPostingListFileName);
    }

    /**
     * Delete a single file
     * @param dir The directory to delete from
     * @param fileName The file name
     */
    private void deleteFile(String dir, String fileName) {
        File f = new File(dir + File.separator + fileName);
        if (f.exists()) {
            f.delete();
        }
    }

    private Dictionary buildDictionary(String in, String out, String dir, Boolean isProduct) {
        String tmpDirName = dir + File.separator + "tmp";
        File tmpDir = new File(tmpDirName);
        if (!tmpDir.exists()) {
            try{
                tmpDir.mkdir();
            }
            catch(SecurityException e){
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        /* Sort */
        takeTime("<<<<<<<<<<< *STARTED SORTING* >>>>>>>>>>");
        ExternalSort.sort(in, out, tmpDirName);
        takeTime("<<<<<<<<<<< *FINISHED SORTING* >>>>>>>>>>");
        deleteFile(dir, in);
        removeIndex(tmpDirName);

        int numOfTerms = countTerms(out);

        takeTime("<<<<<<<<<<< *STARTED BUILDING DICT* >>>>>>>>>>");
        Dictionary dict = new Dictionary(numOfTerms, out, isProduct, dir);
        takeTime("<<<<<<<<<<< *DONE BUILDING DICT* >>>>>>>>>>");

        /* Delete sorted */
        deleteFile(dir, out);

        return dict;
    }

    private int countTerms(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)))){
            String line;
            String prevTerm = "";
            int i = 0;

            while ((line = reader.readLine()) != null) {  // TODO: Might be super bugged
                if (line.isEmpty()) {
                    continue;
                }

                String term = line.split("#")[0];

                if (!term.equals(prevTerm)) {
                    ++i;
                    prevTerm = term;
                }
            }

            return i;

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return -1;  // Will never reach
    }
}
