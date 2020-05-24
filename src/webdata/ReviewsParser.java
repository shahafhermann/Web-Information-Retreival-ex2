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

    /* File paths to save the terms lists */
    private String tokensFilePath;
    private String productsFilePath;

    ReviewsParser(String tokensFilePath, String productsFilePath) {
        this.tokensFilePath = tokensFilePath;
        this.productsFilePath = productsFilePath;
    }

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
                            if (numOfReviews == 100001) {
                                break;
                            }
                            productIds = productIds.concat(term.group(1));
                            productWriter.newLine();
                            productWriter.write(term.group(1).concat(TERM_DELIMITER_IN_FILE)
                                                .concat(String.valueOf(numOfReviews)));
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
