package lrg.dude.duplication;

import java.io.File;
import java.util.ArrayList;

public class DirectoryReader {
    private File root;
    private ArrayList<File> files;

    public DirectoryReader(String rootName) {
        try {
            root = new File(rootName);
        } catch (NullPointerException npe) {
            System.out.println("Error: empty String as path!");
        }
        files = new ArrayList<File>();
    }

    public ArrayList<File> getFilesRecursive() {
        File[] list = root.listFiles();
        if (list == null) {
            System.out.println("Error: no files here!");
            return null;
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i].isFile())
                files.add(list[i]);
            else if (list[i].isDirectory()) {
                DirectoryReader dir = new DirectoryReader(list[i].getAbsolutePath());
                files.addAll(dir.getFilesRecursive());
            }
        }
        return files;
    }
}
