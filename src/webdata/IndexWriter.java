package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 *
 */
public class IndexWriter{

    static final String tokenDictFileName = "tokenDict";
    static final String productDictFileName = "productDict";
    static final String reviewDataFileName = "reviewData";
    static final String productPostingListFileName = "productPostingList";
    static final String tokenPostingListFileName = "tokenPostingList";
    private final String tokensFileName = "tokenFile";
    private final String productsFileName = "productFile";
    private final String sortedIndicator = "_sorted";

    static void takeTime(String msg) {
        String timeStamp =  new SimpleDateFormat("HH.mm.ss.SS").format(new java.util.Date());
        System.err.println(msg + " at " + timeStamp);
    }

    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     * @param inputFile The path to the file containing the review data.
     * @param dir the directory in which all index files will be created if the directory does not exist, it should be
     *            created.
     */
    public void write(String inputFile, String dir) {
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

        String sortedTokensFilePath = dir + File.separator + tokensFileName + sortedIndicator;
        String sortedProductsFilePath = dir + File.separator + productsFileName + sortedIndicator;

        takeTime("<<<<<<<<<<< *STARTED FIRST PASS* >>>>>>>>>>");
        ReviewsParser parser = new ReviewsParser();
        parser.parseFile(inputFile);
        takeTime("<<<<<<<<<<< *DONE FIRST PASS* >>>>>>>>>>");

        takeTime("<<<<<<<<<<< *STARTED BUILDING REVIEW DATA* >>>>>>>>>>");
        ReviewData rd = new ReviewData(parser.getProductIds(), parser.getReviewHelpfulnessNumerator(),
                parser.getReviewHelpfulnessDenominator(), parser.getReviewScore(),
                parser.getTokensPerReview(), parser.getNumOfReviews());
        takeTime("<<<<<<<<<<< *DONE BUILDING REVIEW DATA* >>>>>>>>>>");

        takeTime("<<<<<<<<<<< *START WRITING REVIEW DATA TO FILE* >>>>>>>>>>");
        try (ObjectOutputStream reviewDataWriter = new ObjectOutputStream(
                new FileOutputStream(dir + File.separator + reviewDataFileName))) {
            reviewDataWriter.writeObject(rd);
            takeTime("<<<<<<<<<<< *DONE WRITING REVIEW DATA TO FILE* >>>>>>>>>>");
        } catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        rd.clear();
        parser.clear();

        String tmpDirName = createTempDir(dir);
        takeTime("<<<<<<<<<<< *START SECOND PASS + SORT* >>>>>>>>>>");
        Sorter sorter = new Sorter(new ArrayList<>(parser.getTokenSet()),
                                   new ArrayList<>(parser.getProductIdSet()),
                                   tmpDirName);
        sorter.sort(inputFile, sortedTokensFilePath, sortedProductsFilePath);
        removeIndex(tmpDirName);
//        sorter.clear();
        takeTime("<<<<<<<<<<< *DONE SECOND PASS + SORT* >>>>>>>>>>");

        takeTime("<<<<<<<<<<< *START BUILDING TOKEN DICTIONARY* >>>>>>>>>>");
        Dictionary tokenDict = buildDictionary(parser.getNumOfTokens(), sortedTokensFilePath,
                false, dir, sorter.getTokensArray());
        takeTime("<<<<<<<<<<< *DONE BUILDING TOKEN DICTIONARY* >>>>>>>>>>");
        takeTime("<<<<<<<<<<< *START BUILDING PRODUCT DICTIONARY* >>>>>>>>>>");
        Dictionary productDict = buildDictionary(parser.getNumOfproducts(), sortedProductsFilePath,
                true, dir, sorter.getProductIdsArray());
        takeTime("<<<<<<<<<<< *DONE BUILDING PRODUCT DICTIONARY* >>>>>>>>>>");

        try {
            /* Write the new files */
            takeTime("<<<<<<<<<<< *START WRITING TOKEN DICT TO FILE* >>>>>>>>>>");
            ObjectOutputStream tokenDictWriter = new ObjectOutputStream(new FileOutputStream(dir + File.separator + tokenDictFileName));
            tokenDictWriter.writeObject(tokenDict);
            tokenDictWriter.close();
            takeTime("<<<<<<<<<<< *DONE WRITING TOKEN DICT TO FILE* >>>>>>>>>>");

            takeTime("<<<<<<<<<<< *START WRITING PRODUCT DICT TO FILE* >>>>>>>>>>");
            ObjectOutputStream productDictWriter = new ObjectOutputStream(new FileOutputStream(dir + File.separator + productDictFileName));
            productDictWriter.writeObject(productDict);
            productDictWriter.close();
            takeTime("<<<<<<<<<<< *DONE WRITING PRODUCT DICT TO FILE* >>>>>>>>>>");
        } catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private String createTempDir(String dir) {
        String tmpDirName = dir + File.separator + "tmp";
        removeIndex(tmpDirName);
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
        return tmpDirName;
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

    private Dictionary buildDictionary(int numOfTerms, String out, Boolean isProduct, String dir,
                                       ArrayList<String> mapping) {
        Dictionary dict = new Dictionary(numOfTerms, out, isProduct, dir, mapping);
        /* Delete sorted */
        try {
            Files.deleteIfExists(Paths.get(out));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dict;
    }
}
