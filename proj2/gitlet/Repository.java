package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import gitlet.Utils;

import static gitlet.Utils.*;

// TODO: any imports you need here

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    public static HashMap<String,String > addStageMap;
    public static HashMap<String,String > removeStageMap;
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File objects = join(GITLET_DIR, "objects");
    public static final File Commits = join(objects, "Commits");
    public static final File blobs = join(objects, "blobs");
    public static final File branches = join(GITLET_DIR,"branches");
    public static final File master = join(branches,"master");
    public static final File HEAD = join(GITLET_DIR,"HEAD");
    public static final File BRANCH = join(GITLET_DIR,"BRANCH");
    public static final File Stages = join(GITLET_DIR,"Stages");
    public static final File addStage = join(Stages,"addStage");
    public static final File removeStage = join(Stages,"removeStage");

    /* TODO: fill in the rest of this class. */

    public static <T extends Serializable> void saveToFile(T object, String fileName, File parentDIR) throws IOException{
        File saveFile = join(parentDIR,fileName);
        saveFile.createNewFile();
        writeObject(saveFile,object);
    }
    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        //create a .gitlet directory as Gitlet Repository
        GITLET_DIR.mkdir();
            /*create a Staging Area in .gitlet ; Staging Area will keep track of
            all the files that are staged for addition or removal
             */
        objects.mkdir();
        Commits.mkdir();
        blobs.mkdir();
        branches.mkdir();
        master.createNewFile();
        HEAD.createNewFile();
        BRANCH.createNewFile();
        Stages.mkdir();
        addStage.createNewFile();
        removeStage.createNewFile();

        //create addStage and removeStage
        addStageMap = new HashMap<>();
        removeStageMap = new HashMap<>();
        writeObject(addStage,addStageMap);
        writeObject(removeStage,removeStageMap);
        //create initial commit and save it in objects/Commits
        Commit initialCommit = new Commit();
        saveToFile(initialCommit, initialCommit.getId(),Commits);
    }
    public static void add(String fileName){
        //if file does not exist
        if(!plainFilenamesIn(CWD).contains(fileName)){
            System.out.println("File does not exist.");
            return ;
        }

        addStageMap = readObject(addStage,HashMap.class);
        removeStageMap = readObject(removeStage,HashMap.class);
        Blob tempBlob = new Blob(readContents(new File(fileName)),fileName);
        List<String> blobsFileName = plainFilenamesIn(blobs);

        //if the file to be added is identical to the version in the blob ,don't add it
        if(blobsFileName.contains(tempBlob.getId())){
            return ;
        }
        //if this file exist in removeStage , remove if from removeStage(rm command)
        if(removeStageMap.containsKey(tempBlob.getFileName())){
            removeStageMap.remove(tempBlob.getFileName());
        }
        //add the file to the addStage
        addStageMap.put(tempBlob.getFileName(),tempBlob.getId());
        writeObject(addStage,addStageMap);
    }
    //
    public static void rm(String fileName){

    }

}
