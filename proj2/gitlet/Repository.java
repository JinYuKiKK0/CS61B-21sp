package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static gitlet.Commit.*;
import static gitlet.PointerManager.*;
import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author JinYu
 */
public class Repository {
    /**
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
    public static final File OBJECTS = join(GITLET_DIR, "objects");
    public static final File COMMITS = join(OBJECTS, "Commits");
    public static final File BLOBS = join(OBJECTS, "blobs");
    public static final File BRANCHES = join(GITLET_DIR, "branches");
    public static final File MASTER = join(BRANCHES, "master");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File BRANCH = join(GITLET_DIR, "BRANCH");
    public static final File STAGES = join(GITLET_DIR, "Stages");
    public static final File ADD_STAGE = join(STAGES, "addStage");
    public static final File REMOVE_STAGE = join(STAGES, "removeStage");


    private static void createDirectoriesAndFiles() throws IOException {
        //create a .gitlet directory as Gitlet Repository
        GITLET_DIR.mkdir();
        /*create a Staging Area in .gitlet ; Staging Area will keep track of
            all the files that are staged for addition or removal
        */
        OBJECTS.mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
        BRANCHES.mkdir();
        MASTER.createNewFile();
        HEAD.createNewFile();
        BRANCH.createNewFile();
        STAGES.mkdir();
        ADD_STAGE.createNewFile();
        REMOVE_STAGE.createNewFile();

    }

    private static void isGiltetDirExist() {
        if (!GITLET_DIR.exists()) {
            System.out.println("There is no .gitlet in your CWD");
            System.exit(0);
        }
    }

    private static void initializeStages() {
        //create addStage and removeStage
        addStageMap = new Stage(ADD_STAGE);
        removeStageMap = new Stage(REMOVE_STAGE);
        writeStage();
    }

    private static <T extends Serializable> void saveToFile
            (T object, String fileName, File parentDIR)
            throws IOException {
        File saveFile = join(parentDIR, fileName);
        saveFile.createNewFile();
        writeObject(saveFile, object);
    }

    //read Stage from file
    private static void loadStage() {
        addStageMap = readObject(ADD_STAGE, Stage.class);
        removeStageMap = readObject(REMOVE_STAGE, Stage.class);
    }

    //write Stage into  file
    private static void writeStage() {
        writeObject(ADD_STAGE, addStageMap);
        writeObject(REMOVE_STAGE, removeStageMap);
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
        saveToFile(initialCommit, initialCommit.getId(), COMMITS);
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

    private static boolean isFileExistInGitlet(String fileName) {
        if (isFileExistInCWD(fileName)) {
            return true;
        } else {
            if (isFileTrackedInCommit(fileName, getTheLatestCommit())) {
                return true;
            }
        }
        return false;
    }

    public static void add(String fileName) throws IOException {
        isGiltetDirExist();
        if (!isFileExistInGitlet(fileName)) {
            System.out.println("File does not exist");
            return;
        }
        loadStage();
        if (removeStageMap.containsKey(fileName)) {
            removeStageMap.stageRemove(fileName);
            writeStage();
            return;
        }

        //file not tracked , add to addStage
        if (!isFileTrackedInCommit(fileName, getTheLatestCommit())) {
            Blob tempBlob = new Blob(readContents(join(CWD, fileName)), fileName);
            addStageMap.stageSave(fileName, tempBlob.getId());
            writeStage();
            return;
        }
        //file tracked
        if (!isFileExistInCWD(fileName)) {
            // file not exist in CWD, remove from removeStage
            removeStageMap.stageRemove(fileName);
        } else {
            //  file exist in CWD
            Blob tempBlob = new Blob(readContents(join(CWD, fileName)), fileName);
            if (isBlobIdenticalToCommit(tempBlob)) {
                // curFile == commit
                addStageMap.stageRestrictRemove(tempBlob.getFileName());
            } else {
                // file has some modification ,stage it
                addStageMap.stageSave(fileName, tempBlob.getId());
            }
        }
        writeStage();
    }

    private static boolean isFileTrackedInCommit(String fileName, Commit specifiedCommit) {
        Set<Map.Entry<String, String>> entries = specifiedCommit.getBlobsID().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * rm FILES
     * 1. files neither staged nor tracked by HEAD commit , print err
     * 2. files tracked    staged for removal , delete from CWD , display section "Removed Files" in status command
     * 3. files staged  remove it from addStage
     *
     * @param fileName
     */
    public static void rm(String fileName) {
        isGiltetDirExist();
        loadStage();
        if (addStageMap.containsKey(fileName)) {
            addStageMap.stageRemove(fileName);
        } else if (isFileTrackedInCommit(fileName, getTheLatestCommit())) {
            // remove from commit and delete from CWD
            removeStageMap.stageSave(fileName, getTheLatestCommit().getBlobsID().get(fileName));
            if (isFileExistInCWD(fileName)) {
                restrictedDelete(join(CWD, fileName));
            }
        } else {
            System.out.println("No reason to remove the file.");
        }
        writeStage();
    }


    //Synchronizing staged area information to Commit blobs
    private static void stageToCommitBlobsID(Stage add_Stage_Map, Stage remove_Stage_Map,
                                             TreeMap<String, String> commitBlobsID) {
        add_Stage_Map.forEach((fileName, blobID) -> commitBlobsID.put(fileName, blobID));
        remove_Stage_Map.forEach((fileName, blobID) -> commitBlobsID.remove(fileName));
    }

    //Create a new commit based on the staging area information
    private static Commit createCommitByStage(String message) {
        String parentID = getCurrentBranch();
        Commit cloneLatestCommit = new Commit(message, getTheLatestCommit());
        cloneLatestCommit.setParent(parentID);
        stageToCommitBlobsID(addStageMap, removeStageMap, cloneLatestCommit.getBlobsID());
        return cloneLatestCommit;
    }

    public static void commit(String message) throws IOException {
        isGiltetDirExist();
        loadStage();
        //if staging area is empty , no need to commit
        if (addStageMap.isEmpty() && removeStageMap.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        // create new commit and write to Commits
        Commit newCommit = createCommitByStage(message);
        saveToFile(newCommit, newCommit.getId(), COMMITS);
        //Commit advance Branch to the latest Commit
        pointerAdvance(newCommit);
        initializeStages();
    }

    //Get the HEAD commit, call printCommit()
    //Get the parent commit, call printCommit()
    //Base situation: The commit's parentID is empty
    private static void traversalCommitTree(Commit commit) {
        commit.printCommit();
        Commit parentCommit = commit.getParentCommit();
        if (parentCommit != null) {
            traversalCommitTree(parentCommit);
        }
    }

    public static void log() {
        isGiltetDirExist();
        traversalCommitTree(getTheLatestCommit());
    }

    public static void globalLog() {
        isGiltetDirExist();
        List<String> commitFileNames = plainFilenamesIn(COMMITS);
        commitFileNames.forEach(CommitId -> getCommitById(CommitId).printCommit());
    }

    private static ArrayList<String> findCommitsIdByMessage(String message) {
        ArrayList<String> commitsId = new ArrayList<>();
        List<String> commitFileNames = plainFilenamesIn(COMMITS);
        for (String commitId : commitFileNames) {
            if (getCommitById(commitId).getMessage().equals(message)) {
                commitsId.add(commitId);
            }
        }
        return commitsId;
    }

    public static void find(String message) {
        isGiltetDirExist();
        ArrayList<String> commitsId = findCommitsIdByMessage(message);
        if (commitsId.isEmpty()) {
            System.out.println("Found no commit with that message.");
            return;
        }
        commitsId.forEach(System.out::println);
    }

    private static void branches() {
        message("=== Branches ===");
        List<String> branchNames = plainFilenamesIn(BRANCHES);
        for (String branchName : branchNames) {
            if (readContentsAsString(BRANCH).equals(branchName)) {
                message("*%s", branchName);
                continue;
            }
            message(branchName);
        }
    }

    private static void stagedFiles() {
        loadStage();
        message("=== Staged Files ===");
        addStageMap.keySet().forEach(Utils::message);
        writeStage();
    }

    private static void removedFile() {
        loadStage();
        message("=== Removed Files ===");
        removeStageMap.keySet().forEach(Utils::message);
    }

    private static void modificationsNotStaged() {
        message("=== Modifications Not Staged For Commit ===");
    }

    private static void untrackedFiles() {
        message("=== Untracked Files ===");
    }

    public static void status() {
        isGiltetDirExist();
        branches();
        System.out.println();
        stagedFiles();
        System.out.println();
        removedFile();
        System.out.println();
        modificationsNotStaged();
        System.out.println();
        untrackedFiles();
        System.out.println();
    }

    /**
     * Copy the contents of the blob corresponding to blobId to the file located at fileName
     * Read the blob from the blobs, obtaining the blob object
     * Get the byte array from the blob and write it to the file corresponding to fileName
     *
     * @param blobId   Copy the content of the blob
     * @param fileName Copy to this file
     */
    private static void copyBlobToFile(String blobId, String fileName) {
        Blob blob = new Blob(readContents(join(BLOBS, blobId)), blobId);
        writeContents(join(CWD, fileName), blob.getBytes());
    }

    /**
     * Find the blob corresponding to the file named fileName in the specified commit
     *
     * @param commitId The specified commitId
     * @param fileName
     * @return Found, return blobId; not found, return null
     */
    private static String getBlobIdByCommitIdAndFileName(String commitId, String fileName) {
        Commit commit = getCommitById(commitId);
        return commit.getFileBlobId(fileName);
    }

    /**
     * Copy the file with the same name from the HEAD commit to the CWD, overwriting if it exists
     * Read the HEAD to get the current commit ID, then get the commit based on the ID
     * Get blobs from a commit, then find a file named fileName within those blobs
     * If found, retrieve the blobId corresponding to the key, read the blob file with the same name as the blobs
     * Read the blob object and write the blob's byte array to a file in the current working directory
     * If not found, print: File does not exist in that commit.
     *
     * @param fileName The copied file name
     */
    private static void checkoutFileFromHead(String fileName) {
        String headCommitId = getCurrentBranch();
        String blobId = getBlobIdByCommitIdAndFileName(headCommitId, fileName);
        if (blobId != null) {
            copyBlobToFile(blobId, fileName);
        } else {
            throw new IllegalArgumentException("File does not exist in that commit.");
        }
    }

    /**
     * Copy the file with the same name as the commit in the commitId to the current working directory (CWD)
     * overwriting if it already exists
     *
     * @param commitId
     * @param fileName
     */
    private static void checkoutFileFromCommit(String commitId, String fileName) {
        String blobId = getBlobIdByCommitIdAndFileName(commitId, fileName);
        if (blobId != null) {
            copyBlobToFile(blobId, fileName);
        } else {
            throw new IllegalArgumentException("File does not exist in that commit.");
        }
    }

    /**
     * File name and corresponding blobId for each file tracked by the given branch
     *
     * @param branchName The given branch
     * @return TreeMap<fileName, blobId>
     */
    private static TreeMap<String, String> filesTrackedByBranch(String branchName) {
        Commit branchCommit = getBranchCommit(branchName);
        return branchCommit.getBlobsID();
    }

    private static TreeMap<String, String> filesTrackedByCommit(String commitId) {
        return getCommitById(commitId).getBlobsID();
    }

    /**
     * Check if a branch exists and if it's the same as the current branch
     * Get all file names (String) tracked by the checked branch
     * Check if a file with the same name as the checked branch exists in the current working directory
     * If present, print: There is an untracked file in the way; delete it, or add and commit it first. Exit
     * The checked-out branch's files are restored to the current working directory (CWD).
     * If a file already exists, it will be overwritten
     * Get all file names (String) tracked by the current commit, filter for files tracked only by the current commit
     * and delete them from the CWD
     * Treat the given branch as the current HEAD branch
     * Clear the staging area
     *
     * @param branchName
     */
    private static void checkoutBranch(String branchName) {
        if (!plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (readContentsAsString(BRANCH).equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        //checkout the files of the given commitId
        // delete the files unique to the original branch check out files that
        checkoutFilesOperation(getBranchCommitId(branchName));
        //set given branch to the current branch
        setBranch(branchName);
        initializeStages();
    }

    /**
     * checkout the files of the given commitId
     * delete the files unique to the original branch check out files that
     *
     * @param commitId
     */
    private static void checkoutFilesOperation(String commitId) {
        //Retrieve all files -> blobs for the commits of the given branch
        TreeMap<String, String> checkedBranchFiles = filesTrackedByCommit(commitId);
        //if there is a file with same name exist in CWD , exit and print err
        List<String> fileNames = plainFilenamesIn(CWD);
        for (String fileName : fileNames) {
            if (checkedBranchFiles.keySet().contains(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        //restore each file to CWD
        checkedBranchFiles.forEach((filename, blobId) -> copyBlobToFile(blobId, filename));
        //Remove the unique files from the original branch from the current working directory
        TreeMap<String, String> curCommitFiles = filesTrackedByBranch(getCurrentBranchName());
        Set<String> curCommitFileName = curCommitFiles.keySet();
        curCommitFileName.removeIf(filename -> checkedBranchFiles.keySet().contains(filename));
        curCommitFileName.forEach(filename -> restrictedDelete(filename));
    }

    /**
     * Find the corresponding file in the branches folder based on the given branchName, and read the CommitId from it
     * Get the commit by CommitId, and retrieve the blobs from the commit. Restore all files from the blobs to the CWD
     * Change HEAD to the current CommitId, BRANCH to branchName
     *
     * @param args
     */
    public static void handleCheckout(String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            String fileName = args[2];
            Repository.checkoutFileFromHead(fileName);
        } else if (args.length == 4 && args[2].equals("--")) {
            String commitId = args[1];
            String fileName = args[3];
            Repository.checkoutFileFromCommit(commitId, fileName);
        } else if (args.length == 2) {
            String branchName = args[1];
            Repository.checkoutBranch(branchName);
        } else {
            System.out.println("Invalid checkout command.");
            System.exit(0);
        }
    }

    public static void branch(String branchName) throws IOException {
        if (plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            join(BRANCHES, branchName).createNewFile();
            writeContents(join(BRANCHES, branchName), getCurrentBranch());
        }
    }

    public static void removeBranch(String branchName) {
        if (getCurrentBranchName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else {
            restrictedDelete(join(BRANCHES, branchName));
        }
    }

    /**
     * rollback the version to the specified commitId
     */
    public static void reset(String commitId) {
        if (!plainFilenamesIn(COMMITS).contains(commitId)) {
            throw new IllegalArgumentException("No commit with that id exists.");
        }
        writeContents(HEAD, commitId);
        checkoutFilesOperation(commitId);
        initializeStages();
    }
}

