package lrg.dude.duplication;

public abstract class CleaningDecorator {
    protected CleaningDecorator nextComponent;

    public CleaningDecorator(CleaningDecorator next) {
        nextComponent = next;
    }

    public StringList clean(StringList str) {
        StringList newStr = specificClean(str);
        if (nextComponent != null)
            return nextComponent.clean(newStr);
        return newStr;
    }

    protected abstract StringList specificClean(StringList s);
}
