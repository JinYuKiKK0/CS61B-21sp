package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

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

    private static void createDirectoriesAndFiles() throws IOException {
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

    private static void isGITLET_DIR_Exist() {
        if (!GITLET_DIR.exists()) {
            System.out.println("There is no .gitlet in your CWD");
            System.exit(0);
        }
    }

    private static void initializeStages() {
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

    //judge curFile's version ==curCommit 's version
    private static boolean isBlobIdenticalToCommit(Blob blob) {
        TreeMap<String, String> blobsID = getTheLatestCommit().getBlobsID();
        if (blobsID.containsValue(blob.getId())) {
            return true;
        }
        return false;
    }

    private static boolean isFileExistInCWD(String fileName) {
        if (!plainFilenamesIn(CWD).contains(fileName)) {
            return false;
        }
        return true;
    }

    public static void add(String fileName) throws IOException {
        isGITLET_DIR_Exist();
        //if file does not exist
        if (!isFileExistInCWD(fileName)) {
            System.out.println("File does not exist");
            return;
        }
        loadStage();
        Blob tempBlob = new Blob(readContents(join(CWD, fileName)), fileName);
        //curFile version == curCommit version , don't add
        if (isBlobIdenticalToCommit(tempBlob)) {
            System.out.println("This file is up to date");
            addStageMap.stageRestrictRemove(tempBlob.getFileName());
        }
        //if this file exist in removeStage , remove it from removeStage
        else if (removeStageMap.containsKey(tempBlob.getFileName())) {
            removeStageMap.stageRemove(tempBlob.getFileName());
        } else {
            //add the file to the addStage
            saveToFile(tempBlob, tempBlob.getId(), blobs);
            addStageMap.stageSave(tempBlob.getFileName(), tempBlob.getId());
        }
        writeStage();
    }

    private static boolean isFileExistInLatestCommit(String fileName, Commit latestCommit) {
        Set<Map.Entry<String, String>> entries = latestCommit.getBlobsID().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static void rm(String fileName) {
        isGITLET_DIR_Exist();

        loadStage();
        //file staged but no tracked
        if (addStageMap.containsKey(fileName)) {
            addStageMap.stageRemove(fileName);
        }
        //file tracked
        else {
            Commit latestCommit = getTheLatestCommit();
            if (isFileExistInLatestCommit(fileName, latestCommit)) {
                removeStageMap.stageSave(fileName, latestCommit.getBlobsID().get(fileName));
                //file exist in CWD , delete it
                if (isFileExistInCWD(fileName)) {
                    restrictedDelete(join(CWD, fileName));
                }
                return;
            }
            //if this file doesn't staged or tracked by HEAD
            System.out.println("No reason to remove the file.");
        }

    }

    //将暂存区的信息同步到Commit的blobs
    private static void stageToCommitBlobsID(Stage addStageMap, Stage removeStageMap, TreeMap<String, String> commitBlobsID) {
        addStageMap.forEach((fileName, blobID) -> commitBlobsID.put(fileName, blobID));
        removeStageMap.forEach((fileName, blobID) -> commitBlobsID.remove(fileName));
    }

    //根据暂存区信息创建一个新的Commit
    private static Commit createCommitByStage(String message) {
        String parentID = getCurrentBranch();
        Commit cloneLatestCommit = new Commit(message, getTheLatestCommit());
        cloneLatestCommit.setParent(parentID);
        stageToCommitBlobsID(addStageMap, removeStageMap, cloneLatestCommit.getBlobsID());
        return cloneLatestCommit;
    }

    public static void commit(String message) throws IOException {
        isGITLET_DIR_Exist();
        loadStage();
        //若暂存区为空，则无需提交
        if (addStageMap.isEmpty() && removeStageMap.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        //创建新的commit并写入到Commits
        Commit newCommit = createCommitByStage(message);
        saveToFile(newCommit, newCommit.getId(), Commits);
        //移动分支到最新Commit
        pointerAdvance(newCommit);
        //初始化暂存区
        initializeStages();
    }

    //获取HEAD处Commit，调用printCommit()
    //获取父提交，调用printCommit()
    //base situation:该Commit的parentID 为empty
    private static void traversalCommitTree(Commit commit) {
        commit.printCommit();
        Commit parentCommit = commit.getParentCommit();
        if(parentCommit!=null){
            traversalCommitTree(parentCommit);
        }
    }

    public static void log() {
        isGITLET_DIR_Exist();
        traversalCommitTree(getTheLatestCommit());
    }

    public static void global_log() {
        isGITLET_DIR_Exist();
        List<String> commitFileNames = plainFilenamesIn(Commits);
        commitFileNames.forEach(CommitId -> getCommitById(CommitId).printCommit());
    }

    private static ArrayList<String> findCommitsIdByMessage(String message) {
        ArrayList<String> commitsId = new ArrayList<>();
        List<String> commitFileNames = plainFilenamesIn(Commits);
        for (String CommitId : commitFileNames) {
            if (getCommitById(CommitId).getMessage().equals(message)) {
                commitsId.add(CommitId);
            }
        }
        return commitsId;
    }
    public static void find(String message) {
        isGITLET_DIR_Exist();
        ArrayList<String> CommitsId = findCommitsIdByMessage(message);
        if(CommitsId.isEmpty()){
            System.out.println("Found no commit with that message.");
            return;
        }
        CommitsId.forEach(commitId -> System.out.println(commitId));
    }
}

