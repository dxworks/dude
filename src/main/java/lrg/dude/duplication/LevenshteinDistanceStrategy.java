package lrg.dude.duplication;

/**
 * The Levenshtein Distance Metric, a particular implementation of the Dynamic Programming
 * algorithm, permits three types of operation for transforming the source word x to the target word y:
 *      The substitution of one character of x by a character in y
 *      The deletion of a character of x
 *      The insertion of a character in y
 */
public class LevenshteinDistanceStrategy implements StringCompareStrategy {
    private double threshold;

    public LevenshteinDistanceStrategy(double threshold) {
        this.threshold = threshold;
    }

    public boolean similar(String source, String target) {
        if(normalizedDistance(source, target) <= threshold)
            return true;
        return false;
    }

    private double normalizedDistance(String source, String target) {
        return (double)levenshteinDistance(source, target) / Math.max(source.length(), target.length());
    }

    private int levenshteinDistance(String source, String target) {
        int m = source.length();
        int n = target.length();
        int[][] T = new int[m + 1][n + 1];

        T[0][0] = 0;
        for (int j = 0; j < n; j++) {
            T[0][j + 1] = T[0][j] + ins(target, j);
        }
        for (int i = 0; i < m; i++) {
            T[i + 1][0] = T[i][0] + del(source, i);
            for (int j = 0; j < n; j++) {
                T[i + 1][j + 1] =  min( T[i][j] + sub(source, i, target, j),
                                        T[i][j + 1] + del(source, i),
                                        T[i + 1][j] + ins(target, j));
            }
        }
        return T[m-1][n-1];
    }

    private int sub(String x, int xi, String y, int yi) {
        return x.charAt(xi) == y.charAt(yi) ? 0 : 1;
    }

    private int ins(String x, int xi) {
        return 1;
    }

    private int del(String x, int xi) {
        return 1;
    }

    private int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}
