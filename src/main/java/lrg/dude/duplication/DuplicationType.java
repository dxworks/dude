/**
 * Created by IntelliJ IDEA.
 * User: Richard
 * Date: 03.03.2004
 * Time: 14:42:37
 * To change this template use Options | File Templates.
 */
package lrg.dude.duplication;

import java.io.Serializable;

public class DuplicationType implements Serializable{
	private static final long serialVersionUID = -4994852310049586480L;
	public static final DuplicationType EXACT = new DuplicationType("EXACT");
    public static final DuplicationType INSERT = new DuplicationType("INSERT");
    public static final DuplicationType DELETE = new DuplicationType("DELETE");
    public static final DuplicationType MODIFIED = new DuplicationType("MODIFIED");
    public static final DuplicationType COMPOSED = new DuplicationType("COMPOSED");

    private final String myName; // for debug only

    private DuplicationType(String name) {
        myName = name;
    }

    public String toString() {
        return myName;
    }
}
