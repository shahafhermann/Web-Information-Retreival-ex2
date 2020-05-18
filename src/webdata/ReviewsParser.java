package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for a file of reviews.
 */
public class ReviewsParser {

//    private TreeMap<String, TreeMap<Integer, Integer>> tokenDict = new TreeMap<>();
//    private TreeMap<String, TreeMap<Integer, Integer>> productDict = new TreeMap<>();
    private ArrayList<String> reviewScore = new ArrayList<>();
    private ArrayList<String> reviewHelpfulness = new ArrayList<>();
    private ArrayList<String> productId = new ArrayList<>();
    private ArrayList<String> tokensPerReview = new ArrayList<>();
    private int numOfReviews = 0;

    private String allTokens = "";
    private int numOfUniqueTokens = 0;

    private String allProducts = "";
    private int numOfUniqueProducts = 0;

    private String tokensFilePath;
    private String productsFilePath;

    ReviewsParser(String tokensFilePath, String productsFilePath) {
        this.tokensFilePath = tokensFilePath;
        this.productsFilePath = productsFilePath;
    }

    /**
     * Returns the number of unique tokens
     */
    int getNumOfUniqueTokens() { return numOfUniqueTokens; }

    /**
     * Returns the number of unique products
     */
    int getNumOfUniqueProducts() { return numOfUniqueProducts; }

    /**
     * Return the review scores as an ArrayList of Strings
     */
    ArrayList<String> getReviewScore() {
        return reviewScore;
    }

    /**
     * Return the review helpfulness as an ArrayList of Strings
     */
    ArrayList<String> getReviewHelpfulness() {
        return reviewHelpfulness;
    }

    /**
     * Return the review product IDs as an ArrayList of Strings
     */
    ArrayList<String> getProductId() {
        return productId;
    }

    /**
     * Return the number of token per review as an ArrayList of Strings
     */
    ArrayList<String> getTokensPerReview() {
        return tokensPerReview;
    }

    /**
     * Return the number of reviews
     */
    int getNumOfReviews() { return numOfReviews;}

    /**
     * Adds a new term to the given dictionary at the correct review
     * @param termDict The dictionary to add the term to
     * @param term The term to add
     * @param reviewId The review to which the term belongs
     */
//    private void addTerm(TreeMap<String, TreeMap<Integer, Integer>> termDict, String term, int reviewId) {
//        if (!termDict.containsKey(term)) {
//            TreeMap<Integer, Integer> termData = new TreeMap<>();
//            termData.put(reviewId, 1);
//            termDict.put(term, termData);
//        }
//        else {
//            TreeMap<Integer, Integer> termData = termDict.get(term);
//            Integer lastReview = termData.lastKey();
//            Integer lastFrequency = termData.get(lastReview);
//            if (lastReview == reviewId) {
//                termData.replace(lastReview, lastFrequency + 1);
//            }
//            else {
//                termData.put(reviewId, 1);
//            }
//            termDict.replace(term, termData);
//        }
//    }

    /**
     * Break a text to all it's tokens (alphanumeric).
     * @param text The text to break
     * @param reviewId The review to which the text belongs
     */
    private void breakText(String text, int reviewId, BufferedWriter tokenWriter) throws IOException {
        String[] tokens = text.split("[^A-Za-z0-9]+");
        int tokenCounter = 0;
        for (String token: tokens) {
            if (!token.isEmpty()) {
                tokenWriter.newLine();
                tokenWriter.write(token.concat("#").concat(String.valueOf(numOfReviews)));
                countTerms(token, true);

                ++tokenCounter;
            }
        }
        tokensPerReview.add(String.valueOf(tokenCounter));
    }

    /**
     * Count the number of unique terms (tokens or products)
     * @param term The term to check if we already counted
     * @param isToken Indicates if the term is a token or a product
     */
    private void countTerms(String term, Boolean isToken) {
        if (isToken && !allTokens.contains("#" + term + "$")) {
            allTokens = allTokens.concat("#").concat(term).concat("$");
            ++numOfUniqueTokens;
        } else if(!isToken && !allProducts.contains("#" + term + "$")) {
            allProducts = allProducts.concat("#").concat(term).concat("$");
            ++numOfUniqueProducts;
        }
    }

    /**
     * Parse the file
     * @param inputFile The file to parse
     */
    void parseFile(String inputFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(inputFile)))){
            try (BufferedWriter productWriter = new BufferedWriter(new FileWriter(new File(productsFilePath)))){
                try (BufferedWriter tokenWriter = new BufferedWriter(new FileWriter(new File(tokensFilePath)))){
                    String line = reader.readLine();
                    String textBuffer = "";
                    boolean textFlag = false;
                    while (line != null){
                        Matcher term;

                        if (textFlag && !line.contains("product/productId:")) {
                            textBuffer = textBuffer.concat(" ").concat(line);
                            line = reader.readLine();
                            continue;
                        }

                        term = Pattern.compile("^product/productId: (.*)").matcher(line);
                        if (term.find()) {
                            textFlag = false;
                            if (!textBuffer.isEmpty()) {
                                breakText(textBuffer.toLowerCase(), numOfReviews, tokenWriter);
                            }
                            ++numOfReviews;
                            productId.add(term.group(1));
//                            addTerm(productDict, term.group(1), numOfReviews);  // TODO: to delete
                            productWriter.newLine();
                            productWriter.write(term.group(1).concat("#").concat(String.valueOf(numOfReviews)));
                            countTerms(term.group(1), false);
                            line = reader.readLine();
                            continue;
                        }

                        term = Pattern.compile("^review/helpfulness: (.*)").matcher(line);
                        if (term.find()) {
                            reviewHelpfulness.add(term.group(1));
                            line = reader.readLine();
                            continue;
                        }

                        term = Pattern.compile("^review/score: (.*)").matcher(line);
                        if (term.find()) {
                            reviewScore.add(term.group(1));
                            line = reader.readLine();
                            continue;
                        }

                        term = Pattern.compile("^review/text:(.*)").matcher(line);
                        if (term.find()) {
                            textFlag = true;
                            textBuffer = term.group(1);
                            line = reader.readLine();
                            continue;
                        }

                        line = reader.readLine();
                    }

                    if (!textBuffer.isEmpty()) {
                        breakText(textBuffer.toLowerCase(), numOfReviews, tokenWriter);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
