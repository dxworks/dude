package lrg.dude.duplication;

/**
 * Strategy based on usual string compare in java.
 * Needed to comply to the interface (uniformity reasons)
 */
public class IdenticalCompareStrategy implements StringCompareStrategy {
    public boolean similar(String source, String target) {
        return source.equals(target);
    }
}
