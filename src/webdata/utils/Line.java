package webdata.utils;

/**
 * This class represents a line in the relation files with idX and two additional columns separated by spaces.
 */
public class Line implements Comparable<Line>{
    private static final String COLUMN_DELIMINATOR = "#";
    private static final int TERM_INDEX = 0;
    private static final int REVIEW_ID_INDEX = 1;

    // Data members
    private String term;
    private int reviewId;
    private String line;

    /**
     * Initializes the line with the given line string.
     * @param line Line string.
     */
    public Line(String line){
        this.line = line;
        this.initLineParts();
    }

    /**
     * Initializes the different line parts.
     */
    private void initLineParts(){
        String[] lineParts = this.line.split(COLUMN_DELIMINATOR);
        this.term = lineParts[TERM_INDEX];
        this.reviewId = Integer.parseInt(lineParts[REVIEW_ID_INDEX]);
    }

    /**
     * Get the line's term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Get the line's reviewId
     */
    public int getReviewId() {
        return reviewId;
    }

    /**
     * Returns an int representing the order between this line and the given other line.
     * @param o The other line to compare to.
     * @return Negative number if this line is smaller than other, 0 if equal, and positive if it is greater.
     */
    @Override
    public int compareTo(Line o) {
        if (this.term.equals(o.term)){
            return this.reviewId - o.reviewId;
        }
        return this.term.compareTo(o.term);
    }

    /**
     * Returns a string representing the line.
     * @return The line string representation.
     */
    @Override
    public String toString() {
        return this.line;
    }
}
