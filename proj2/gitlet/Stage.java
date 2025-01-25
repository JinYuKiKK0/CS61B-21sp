package gitlet;

import java.io.File;
import java.util.HashMap;


public class Stage extends HashMap<String, String> {
    private final File stageFile;

    public Stage(File stageFile) {
        this.stageFile = stageFile;
    }

    //Synchronize staged changes to the file
    public void stageSave(String fileName, String blobID) {
        this.put(fileName, blobID);
        Utils.writeObject(this.stageFile, this);
    }

    //Synchronize staged changes to the file
    public void stageRemove(String fileName) {
        this.remove(fileName);
        Utils.writeObject(this.stageFile, this);
    }

    public void stageRestrictRemove(String fileName) {
        if (this.containsKey(fileName)) {
            this.stageRemove(fileName);
        }
    }
}
