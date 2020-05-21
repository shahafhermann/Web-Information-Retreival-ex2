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

    /* Data */
    private ArrayList<Byte> reviewScore = new ArrayList<>();
    private ArrayList<Short> reviewHelpfulnessNumerator = new ArrayList<>();
    private ArrayList<Short> reviewHelpfulnessDenominator = new ArrayList<>();
    private ArrayList<Short> tokensPerReview = new ArrayList<>();
    private int numOfReviews = 0;
    private String productIds = "";

    /* String constants */
    private final String SPLIT_TOKENS_REGEX = "[^A-Za-z0-9]+";
    private final String TERM_DELIMITER_IN_FILE = "#";

    /* Auxiliary variables for counting unique tokens and productIds */
    private final String TERM_START_DELIMITER = "#";
    private final String TERM_END_DELIMITER = "$";
    private String allTokens = "";
    private int numOfUniqueTokens = 0;
    private String allProducts = "";
    private int numOfUniqueProducts = 0;

    /* File paths to save the terms lists */
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
    ArrayList<Byte> getReviewScore() {
        return reviewScore;
    }

    /**
     * Return the review helpfulness numerator as an ArrayList of Strings
     */
    ArrayList<Short> getReviewHelpfulnessNumerator() {
        return reviewHelpfulnessNumerator;
    }

    /**
     * Return the review helpfulness denominator as an ArrayList of Strings
     */
    ArrayList<Short> getReviewHelpfulnessDenominator() {
        return reviewHelpfulnessDenominator;
    }

    /**
     * Return the review product IDs as an ArrayList of Strings
     */
    String getProductIds() {
        return productIds;
    }

    /**
     * Return the number of token per review as an ArrayList of Strings
     */
    ArrayList<Short> getTokensPerReview() {
        return tokensPerReview;
    }

    /**
     * Return the number of reviews
     */
    int getNumOfReviews() { return numOfReviews;}

    /**
     * Break a text to all it's tokens (alphanumeric).
     * @param text The text to break
     * @param reviewId The review to which the text belongs
     */
    private void breakText(String text, int reviewId, BufferedWriter tokenWriter) throws IOException {
        String[] tokens = text.split(SPLIT_TOKENS_REGEX);
        int tokenCounter = 0;
        for (String token: tokens) {
            if (!token.isEmpty()) {
                tokenWriter.newLine();
                tokenWriter.write(token.concat(TERM_DELIMITER_IN_FILE).concat(String.valueOf(numOfReviews)));
                countTerms(token, true);

                ++tokenCounter;
            }
        }
        tokensPerReview.add((short) tokenCounter);
    }

    /**
     * Count the number of unique terms (tokens or products)
     * @param term The term to check if we already counted
     * @param isToken Indicates if the term is a token or a product
     */
    private void countTerms(String term, Boolean isToken) {
        if (isToken && !allTokens.contains(TERM_START_DELIMITER + term + TERM_END_DELIMITER)) {
            allTokens = allTokens.concat(TERM_START_DELIMITER).concat(term).concat(TERM_END_DELIMITER);
            ++numOfUniqueTokens;
        } else if(!isToken && !allProducts.contains(TERM_START_DELIMITER + term + TERM_END_DELIMITER)) {
            allProducts = allProducts.concat(TERM_START_DELIMITER).concat(term).concat(TERM_END_DELIMITER);
            ++numOfUniqueProducts;
        }
    }

    /**
     * Parse a string resembling a review helpfulness to it's numerator and denominator.
     * @param term The review helpfulness as String
     */
    private void writeReviewHelpfulness(String term) {
        String[] split = term.split("/");
        reviewHelpfulnessNumerator.add(Short.parseShort(split[0]));
        reviewHelpfulnessDenominator.add(Short.parseShort(split[1]));
    }

    /**
     * Parse a string resembling a review score.
     * @param term The review helpfulness as String
     */
    private void writeReviewScore(String term) {
        reviewScore.add(Byte.parseByte(term.split("\\.")[0]));
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
                            productIds = productIds.concat(term.group(1));
                            productWriter.newLine();
                            productWriter.write(term.group(1).concat(TERM_DELIMITER_IN_FILE)
                                                .concat(String.valueOf(numOfReviews)));
                            countTerms(term.group(1), false);
                            line = reader.readLine();
                            continue;
                        }

                        term = Pattern.compile("^review/helpfulness: (.*)").matcher(line);
                        if (term.find()) {
                            writeReviewHelpfulness(term.group(1));
                            line = reader.readLine();
                            continue;
                        }

                        term = Pattern.compile("^review/score: (.*)").matcher(line);
                        if (term.find()) {
                            writeReviewScore(term.group(1));
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
