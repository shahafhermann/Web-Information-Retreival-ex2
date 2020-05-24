package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for a file of reviews.
 */
public class ReviewsParser {

    /* Data */
    private HashSet<String> tokenSet = new HashSet<>();
    private HashSet<String> productIdSet = new HashSet<>();

    private ArrayList<Byte> reviewScore = new ArrayList<>();
    private ArrayList<Short> reviewHelpfulnessNumerator = new ArrayList<>();
    private ArrayList<Short> reviewHelpfulnessDenominator = new ArrayList<>();
    private ArrayList<Short> tokensPerReview = new ArrayList<>();
    private int numOfReviews = 0;
    private String productIds = "";

    /* String constants */
    private final String SPLIT_TOKENS_REGEX = "[^A-Za-z0-9]+";

    /**
     * Empty the data structures stored in this instance.
     */
    void clear() {
        this.reviewScore.clear();
        this.reviewHelpfulnessNumerator.clear();
        this.reviewHelpfulnessDenominator.clear();
        this.tokensPerReview.clear();
        numOfReviews = 0;
        productIds = "";
    }

    /**
     * Return the tokens HashSet
     */
    HashSet<String> getTokenSet() { return tokenSet; }

    /**
     * Return the productIds HashSet
     */
    HashSet<String> getProductIdSet() { return productIdSet; }

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
     * Return the number unique tokens
     */
    int getNumOfTokens() { return tokenSet.size(); }

    /**
     * Return the number of unique products
     */
    int getNumOfproducts() { return productIdSet.size(); }

    /**
     * Break a text to all it's tokens (alphanumeric).
     * @param text The text to break
     */
    private void breakText(String text) {
        String[] tokens = text.split(SPLIT_TOKENS_REGEX);
        int tokenCounter = 0;
        for (String token: tokens) {
            if (!token.isEmpty()) {
                tokenSet.add(token);

                ++tokenCounter;
            }
        }
        tokensPerReview.add((short) tokenCounter);
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
                        breakText(textBuffer.toLowerCase());
                    }
                    ++numOfReviews;
//                            if (numOfReviews == 100001) {  // TODO: delete later
//                                break;
//                            }
                    productIds = productIds.concat(term.group(1));
                    productIdSet.add(term.group(1));
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
                breakText(textBuffer.toLowerCase());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
