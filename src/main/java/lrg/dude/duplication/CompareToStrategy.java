package lrg.dude.duplication;

public class CompareToStrategy implements StringCompareStrategy {
    public boolean similar(String source, String target) {
        if(source.compareTo(target) == 0)
            return true;
        return false;
    }
}
