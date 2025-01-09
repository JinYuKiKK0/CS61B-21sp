package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;

import gitlet.PointerManager;

import static gitlet.PointerManager.*;
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
     * <p>
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    private static Stage addStageMap;
    private static Stage removeStageMap;
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
    public static final File branches = join(GITLET_DIR, "branches");
    public static final File master = join(branches, "master");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File BRANCH = join(GITLET_DIR, "BRANCH");
    public static final File Stages = join(GITLET_DIR, "Stages");
    public static final File addStage = join(Stages, "addStage");
    public static final File removeStage = join(Stages, "removeStage");

    /* TODO: fill in the rest of this class. */

    private static void createDirectoriesAndFiles() throws IOException{
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

    }
    private static void initializeStages(){
        //create addStage and removeStage
        addStageMap = new Stage(addStage);
        removeStageMap = new Stage(removeStage);
        writeStage();
    }
    private static <T extends Serializable> void saveToFile(T object, String fileName, File parentDIR) throws IOException {
        File saveFile = join(parentDIR, fileName);
        saveFile.createNewFile();
        writeObject(saveFile, object);
    }
    //read Stage from file
    private static void loadStage() {
        addStageMap = readObject(addStage, Stage.class);
        removeStageMap = readObject(removeStage, Stage.class);
    }

    //write Stage into  file
    private static void writeStage() {
        writeObject(addStage, addStageMap);
        writeObject(removeStage, removeStageMap);
    }

    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        createDirectoriesAndFiles();
        initializeStages();
        //create initial commit and save it in objects/Commits
        Commit initialCommit = new Commit();
        saveToFile(initialCommit, initialCommit.getId(), Commits);
        //create HEAD and master pointer point to initial commit
        initializePointers(initialCommit);
    }


    private static boolean isBlobIdentical(Blob tempBlob){
        return plainFilenamesIn(blobs).contains(tempBlob.getId());
    }
    public static void add(String fileName) throws IOException {
        //if file does not exist
        if (!plainFilenamesIn(CWD).contains(fileName)) {
            System.out.println("File does not exist.");
            return;
        }
        loadStage();
        Blob tempBlob = new Blob(readContents(join(CWD,fileName)), fileName);
        //if the file to be added is identical to the version in the blob ,don't add it
        if (isBlobIdentical(tempBlob)) {return;}
        //if this file exist in removeStage , remove if from removeStage(rm command)
        else if (removeStageMap.containsKey(tempBlob.getFileName())) {
            removeStageMap.stageRemove(tempBlob.getFileName());
        }else {
            //add the file to the addStage
            saveToFile(tempBlob, tempBlob.getId(), blobs);
            addStageMap.stageSave(tempBlob.getFileName(),tempBlob.getId());
        }
        writeStage();
    }
    //TODO:clone the commit that HEAD point and modify its metadata with message and other info user provide
    //TODO:modify new commit's refs to blob by Mapping relationship(e.g hello.txt->blob0.getID())
    //TODO:modify commit's refs in addStage and removeStage
    //TODO:give parent commit to the new commit and advance HEAD and master to the latest commit
    public static void commit(String message) throws IOException{
        String parentID = getCurrentBranch();
        Commit cloneLatestCommit = new Commit(message, getTheLatestCommit());
        TreeMap<String, String> commitBlobsID = cloneLatestCommit.getBlobsID();
        loadStage();
        if(addStageMap.isEmpty()&&removeStageMap.isEmpty()){
            System.out.println("No changes added to the commit.");
        }
        addStageMap.forEach((fileName, blobID) -> commitBlobsID.put(fileName, blobID));
        removeStageMap.forEach((fileName, blobID) -> {
            commitBlobsID.remove(fileName);
            //delete rm file from CWD
            restrictedDelete(join(CWD, fileName));
            //delete rm file -> blob from blobs
            restrictedDelete(join(blobs, blobID));
        });
        //modify the clone commit
        cloneLatestCommit.setBlobsID(commitBlobsID);
        cloneLatestCommit.setParent(parentID);
        //advance the pointer to new latest commit
        pointerAdvance(cloneLatestCommit);
        //clear stage
        addStageMap.clear();
        removeStageMap.clear();
        writeStage();
        saveToFile(cloneLatestCommit, cloneLatestCommit.getId(), blobs);
    }

    public static void rm(String fileName) {
        loadStage();
        //remove it from addStage if this file has staged in addStage
        if (addStageMap.containsKey(fileName)) {
            addStageMap.stageRemove(fileName);
            return;
        }
        /* if this file doesn't in addStage but in the Latest commit that HEAD point to ,
         * than add it in removeStage and remove it when commit command execute
         * //TODO:for this circumstance,this file should delete from CWD
         */
        else {
            Commit latestCommit = getTheLatestCommit();
            Set<Map.Entry<String, String>> entries = latestCommit.getBlobsID().entrySet();
            for (Map.Entry<String, String> entry : entries) {
                if (entry.getKey().equals(fileName)) {
                    removeStageMap.stageSave(fileName, entry.getValue());
                    return;
                }
            }
        }
        //if this file doesn't staged or tracked by HEAD
        System.out.println("No reason to remove the file.");
    }
}

