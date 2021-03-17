package lrg.dude.duplication;

/**
 * Created by IntelliJ IDEA.
 * User: Richard
 * Date: 13.05.2004
 * Time: 23:05:06
 * To change this template use File | Settings | File Templates.
 */
public interface Subject {
    public void attach(Observer observer);
    public void detach(Observer observer);
    public void notifyObservers();
}
