package gitlet;

import com.sun.source.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
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
            System.out.println("Not in an initialized Gitlet directory.");
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
        return blobsID.containsValue(blob.getId());
    }

    private static boolean isFileExistInCWD(String fileName) {
        return plainFilenamesIn(CWD).contains(fileName);
    }

    private static boolean isFileExistInGitlet(String fileName) {
        if (isFileExistInCWD(fileName)) {
            return true;
        } else {
            return isFileTrackedInCommit(fileName, getTheLatestCommit());
        }
    }

    private static Blob getBlobByFileName(String fileName) {
        for (String blobId : plainFilenamesIn(BLOBS)) {
            Blob blob = readObject(join(BLOBS, blobId), Blob.class);
            if (fileName.equals(blob.getFileName())) {
                return blob;
            }
        }
        return null;
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
            return;
        }
        //file not tracked , add to addStage
        if (!isFileTrackedInCommit(fileName, getTheLatestCommit())) {
            Blob tempBlob = new Blob(readContents(join(CWD, fileName)), fileName);
            saveToFile(tempBlob, tempBlob.getId(), BLOBS);
            addStageMap.stageSave(fileName, tempBlob.getId());
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
                saveToFile(tempBlob, tempBlob.getId(), BLOBS);
            }
        }
        writeStage();
    }

    private static boolean isFileTrackedInCommit(String fileName, Commit specifiedCommit) {
        Set<String> keySet = specifiedCommit.getBlobsID().keySet();
        for (String key : keySet) {
            if (key.equals(fileName)) {
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
        Blob blob = readObject(join(BLOBS, blobId), Blob.class);
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
            System.out.println("File does not exist in that commit.");
        }
    }

    /**
     * Copy the file with the same name as the commit in the commitId to the current working directory (CWD)
     * overwriting if it already exists
     * 支持短字符
     *
     * @param commitId
     * @param fileName
     */
    //commitId接收短Id，根据Id长度截取commit
    private static void checkoutFileFromCommit(String commitId, String fileName) {
        String blobId = getBlobIdByCommitIdAndFileName(commitId, fileName);
        if (blobId != null) {
            copyBlobToFile(blobId, fileName);
        } else {
            System.out.println("File does not exist in that commit.");
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
        String targetCommitId = getBranchCommitId(branchName);

        // 2. 先统一检查：目标commit中即将覆盖或删除的文件是否在当前工作区中且属于“未跟踪”状态
        //    若发现未跟踪文件会被覆盖或删除，则报错并提前返回
        if (hasUntrackedConflict(targetCommitId)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            return;
        }
        // delete the files unique to the original branch check out files that
        checkoutFilesOperation(getBranchCommitId(branchName));
        //set given branch to the current branch
        setBranch(branchName);
        initializeStages();
    }

    private static TreeMap<String, String> findOnlyCurCommitTracked(String commitId) {
        TreeMap<String, String> givenFiles = getCommitById(commitId).getBlobsID();
        TreeMap<String, String> curFiles = getTheLatestCommit().getBlobsID();
        for (String fileName : givenFiles.keySet()) {
            curFiles.remove(fileName);
        }
        return curFiles;
    }

    private static TreeMap<String, String> findBothCommitTracked(String commitId) {
        TreeMap<String, String> givenFiles = getCommitById(commitId).getBlobsID();
        TreeMap<String, String> curFiles = getTheLatestCommit().getBlobsID();
        TreeMap<String, String> bothTrackedFiles = new TreeMap<>();
        for (Map.Entry<String, String> entry : givenFiles.entrySet()) {
            if (curFiles.containsKey(entry.getKey())) {
                bothTrackedFiles.put(entry.getKey(), entry.getValue());
            }
        }
        return bothTrackedFiles;
    }

    private static TreeMap<String, String> findOnlyGivenCommitTracked(String commitId) {
        TreeMap<String, String> givenFiles = getCommitById(commitId).getBlobsID();
        TreeMap<String, String> curFiles = getTheLatestCommit().getBlobsID();
        TreeMap<String, String> onlyGivenFiles = new TreeMap<>(givenFiles);
        for (String curFileName : curFiles.keySet()) {
            onlyGivenFiles.remove(curFileName);
        }
        return onlyGivenFiles;
    }

    private static void overwriteFiles(TreeMap<String, String> bothTrackedFiles) {
        if (bothTrackedFiles.isEmpty()) {
            return;
        }
        for (String fileName : bothTrackedFiles.keySet()) {
            copyBlobToFile(bothTrackedFiles.get(fileName), fileName);
        }
    }

    private static void deleteFiles(TreeMap<String, String> findOnlyCurCommitTracked) {
        if (findOnlyCurCommitTracked.isEmpty()) {
            return;
        }
        for (String fileName : findOnlyCurCommitTracked.keySet()) {
            join(CWD, fileName).delete();
        }
    }

    private static void writeFiles(TreeMap<String, String> findOnlyGivenCommitTracked) {
        if (findOnlyGivenCommitTracked.isEmpty()) {
            return;
        }
        for (String fileName : findOnlyGivenCommitTracked.keySet()) {
            File file = join(CWD, fileName);
            if (file.exists()) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            overwriteFiles(findOnlyGivenCommitTracked);
        }
    }

    //如果当前分支中有未跟踪的工作文件，并且该文件将被重置覆盖，则打印
    //There is an untracked file in the way; delete it, or add and commit it first.
    private static void filesCheckBeforeCheckOut(String fileName, String commitId) {
        boolean isUntracked = !isFileTrackedInCommit(fileName, getTheLatestCommit());
        boolean willBeOverwritten = false;
        //获取给定分支该文件的blobId，与当前分支的blobId对比
        TreeMap<String, String> branchFiles = getCommitById(commitId).getBlobsID();
        TreeMap<String, String> curFiles = getTheLatestCommit().getBlobsID();

        String branchBlobId = branchFiles.get(fileName);
        String curBlobId = curFiles.get(fileName);
        if (!Objects.equals(branchBlobId, curBlobId)) {
            willBeOverwritten = true;
        }
        if (isUntracked && willBeOverwritten) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
    }

    private static boolean hasUntrackedConflict(String targetCommitId) {
        TreeMap<String, String> targetFiles = getCommitById(targetCommitId).getBlobsID();
        TreeMap<String, String> currentFiles = getTheLatestCommit().getBlobsID();

        // 收集在目标commit里出现，但内容和当前commit里不同，可能要覆盖的文件
        // 以及当前commit里有但目标commit里没有，可能要删除的文件
        Set<String> filesToOverwriteOrRemove = new HashSet<>();
        // 目标commit新增或改动的文件
        for (String fileName : targetFiles.keySet()) {
            String targetBlobId = targetFiles.get(fileName);
            String currentBlobId = currentFiles.get(fileName);
            // 如果文件在两边commit都存在且blobId不同 => 需要覆盖
            // 如果文件在目标commit存在，但当前commit不存在 => 也可能覆盖当前工作目录
            if (currentBlobId == null || !currentBlobId.equals(targetBlobId)) {
                filesToOverwriteOrRemove.add(fileName);
            }
        }
        // 当前commit有但目标commit没有 => 需要删除
        for (String fileName : currentFiles.keySet()) {
            if (!targetFiles.containsKey(fileName)) {
                filesToOverwriteOrRemove.add(fileName);
            }
        }

        // 逐个检测：如果工作目录下存在这些文件且它们在当前commit中未被跟踪 => 触发冲突
        for (String fileName : filesToOverwriteOrRemove) {
            File f = join(CWD, fileName);
            if (f.exists() && !isFileTrackedInCommit(fileName, getTheLatestCommit())) {
                return true;
            }
        }
        return false;
    }

    /**
     * checkout the files of the given commitId
     * delete the files unique to the original branch check out files that
     *
     * @param commitId
     */
    private static void checkoutFilesOperation(String commitId) {
        TreeMap<String, String> onlyGivenCommitTracked = findOnlyGivenCommitTracked(commitId);
        TreeMap<String, String> bothCommitTracked = findBothCommitTracked(commitId);
        TreeMap<String, String> onlyCurCommitTracked = findOnlyCurCommitTracked(commitId);
        deleteFiles(onlyCurCommitTracked);
        overwriteFiles(bothCommitTracked);
        overwriteFiles(onlyGivenCommitTracked);
    }

    /**
     * Find the corresponding file in the branches folder based on the given branchName, and read the CommitId from it
     * Get the commit by CommitId, and retrieve the blobs from the commit. Restore all files from the blobs to the CWD
     * Change HEAD to the current CommitId, BRANCH to branchName
     *
     * @param args
     */
    public static void handleCheckout(String[] args) {
        if (args.length == 3 && "--".equals(args[1])) {
            String fileName = args[2];
            Repository.checkoutFileFromHead(fileName);
        } else if (args.length == 4 && "--".equals(args[2])) {
            String commitId = args[1];
            String fileName = args[3];
            Repository.checkoutFileFromCommit(commitId, fileName);
        } else if (args.length == 2) {
            String branchName = args[1];
            Repository.checkoutBranch(branchName);
        } else {
            System.out.println("Incorrect operands.");
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
            if (join(BRANCHES, branchName).exists()) {
                join(BRANCHES, branchName).delete();
            }
        }
    }

    /**
     * rollback the version to the specified commitId
     */
    public static void reset(String commitId) {
        if (!plainFilenamesIn(COMMITS).contains(commitId)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        // 2. 先统一检查：目标commit中即将覆盖或删除的文件是否在当前工作区中且属于“未跟踪”状态
        //    若发现未跟踪文件会被覆盖或删除，则报错并提前返回
        if (hasUntrackedConflict(commitId)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            return;
        }
        writeContents(HEAD, commitId);
        checkoutFilesOperation(commitId);
        initializeStages();
    }

    private static boolean isStageEmpty() {
        loadStage();
        return addStageMap.isEmpty() && removeStageMap.isEmpty();
    }

    private static void sanityCheckBeforeMerge(String branchName) {
        if (!isStageEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("A branch with that name does not exists.");
            System.exit(0);
        }
        if (getCurrentBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    /**
     * 对branch A & branch B分别进行BFS遍历，将所有遍历到的commitId以及对应深度(commitId -> depth)放入Map，遍历到init commit结束
     * 获得Map A & Map B. 对MapA.keyset()进行遍历，若MapB也包含该key，记录MapA此时的key->value 为minKey以及minValue。
     * 继续遍历，如果MapB包含该key，并且对应MapA的value < minValue，将minKey与minValue更新为该键值对
     * 遍历结束，minKey对应splitCommit的commitId，minValue对应splitCommit的depth
     *
     * @param branchName
     * @return
     */
    private static String findSplitPointId(String branchName) {
        String headCommitId = getCurrentBranch();
        String givenBranchCommitId = getBranchCommitId(branchName);
        HashMap<String, Integer> headCommitTree = new HashMap<>();
        HashMap<String, Integer> givenBranchCommitTree = new HashMap<>();

        BFS(headCommitId, headCommitTree);
        BFS(givenBranchCommitId, givenBranchCommitTree);

        String minKey = "";
        int minValue = Integer.MAX_VALUE;
        for (String commitId : headCommitTree.keySet()) {
            if (givenBranchCommitTree.containsKey(commitId)) {
                int sum = headCommitTree.get(commitId) + givenBranchCommitTree.get(commitId);
                if (sum < minValue || (sum == minValue) && commitId.compareTo(minKey) > 0) {
                    minValue = sum;
                    minKey = commitId;
                }
            }
        }
        return minKey;
    }

    private static void BFS(String commitId, HashMap<String, Integer> commitTree) {
        Queue<String> queue = new LinkedList<>();
        Set<String> marked = new HashSet<>();

        int depth = 0;
        queue.offer(commitId);
        marked.add(commitId);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String commitid = queue.remove();
                commitTree.put(commitid, depth);
                ArrayList<String> parentsID = getCommitById(commitid).getParentsID();
                if (parentsID.isEmpty()) {
                    continue;
                }
                for (String parentCommitId : parentsID) {
                    if (!marked.contains(parentCommitId)) {
                        marked.add(parentCommitId);
                        queue.offer(parentCommitId);
                    }
                }
            }
            depth++;
        }
    }

    private static void mergeEasyCase(String branchName) {
        String splitPointId = findSplitPointId(branchName);
        if (splitPointId.equals(getCurrentBranch())) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            System.exit(0);
        }
        if (splitPointId.equals(getBranchCommitId(branchName))) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
    }

    /**
     * 接收一个分支
     * 找出给定分支以及当前分支的split point
     * 对sp,other以及head中的每个文件进行审查，根据三者之间的文件差异，对工作目录中的文件进行合并修改
     * 创建合并提交，合并提交保存合并后工作目录的文件快照（包括解决冲突后），并设置两个父提交
     * 对于合并提交，在commit id下添加一行：Merge: 4975af1 2c1ead1。数字依次由两个父提交ID的前七位组成
     * !!!!!在任何合并操作前执行该检查：如果当前提交中未跟踪的文件将被merge覆写或删除，打印：
     * There is an untracked file in the way; delete it, or add and commit it first.并退出
     *
     * @param branchName
     */
    public static void merge(String branchName) throws IOException {
        sanityCheckBeforeMerge(branchName);
        mergeEasyCase(branchName);
        TreeMap<String, String> mergeResultFiles = getMergeResultFiles(branchName);
        createMergeCommit(branchName,mergeResultFiles);
        initializeStages();
    }

    private static TreeMap<String,String> getMergeResultFiles(String branchName) throws IOException {
        TreeMap<String, String> splitFiles = getCommitById(findSplitPointId(branchName)).getBlobsID();
        TreeMap<String, String> branchFiles = getBranchCommit(branchName).getBlobsID();
        TreeMap<String, String> curFiles = getTheLatestCommit().getBlobsID();
        TreeMap<String, String> writeFiles = new TreeMap<>();
        TreeMap<String, String> deleteFiles = new TreeMap<>();
        Set<String> allFileNames = new HashSet<>();
        allFileNames.addAll(splitFiles.keySet());
        allFileNames.addAll(branchFiles.keySet());
        allFileNames.addAll(curFiles.keySet());
        for (String allFileName : allFileNames) {
            String splitBlobId = splitFiles.get(allFileName);
            if (splitBlobId != null) {
                filesInSplit(allFileName,splitBlobId,curFiles.get(allFileName)
                        ,branchFiles.get(allFileName),writeFiles,deleteFiles);
            }else {
                filesNotInSplit(allFileName,curFiles.get(allFileName)
                        ,branchFiles.get(allFileName)
                        ,writeFiles);
            }
        }
        TreeMap<String,String> mergeResultFiles = new TreeMap<>();
        mergeResultFiles.putAll(writeFiles);
        for (String deleteFileName : deleteFiles.keySet()) {
            if(mergeResultFiles.containsKey(deleteFileName)){
                mergeResultFiles.remove(deleteFileName);
            }
        }
        mergeFilesOperation(writeFiles,deleteFiles);
        return mergeResultFiles;
    }

    //对split中存在的文件进行操作,将最终结果fileName -> blobId 写入TreeMap files
    private static void filesInSplit(String fileName, String splitBlobId,
                                     String headBlobId, String branchBlobId,
                                     TreeMap<String, String> writeFiles,
                                     TreeMap<String, String> deleteFiles) throws IOException {
        boolean headModified = (headBlobId != null) && (!headBlobId.equals(splitBlobId));
        boolean branchModified = (branchBlobId != null) && (!branchBlobId.equals(splitBlobId));
        boolean headDeleted = headBlobId == null;
        boolean branchDeleted = branchBlobId == null;

        if (headModified && !branchModified) {
            writeFiles.put(fileName, headBlobId);
        }
        if (!headModified && branchModified) {
            writeFiles.put(fileName, branchBlobId);
        }
        if ((headModified && branchModified) && headBlobId.equals(branchBlobId)) {
            writeFiles.put(fileName, headBlobId);
        }
        if (headDeleted && branchDeleted) {
            deleteFiles.put(fileName, "delete");
        }
        if (headDeleted && !branchModified) {
            deleteFiles.put(fileName, "delete");
        }
        if (headModified && branchModified && !headBlobId.equals(branchBlobId)) {
            conflictHandling(headBlobId, branchBlobId, fileName,writeFiles);
        }
        if (headModified && branchDeleted) {
            conflictHandling(headBlobId, null, fileName,writeFiles);
        }
        if (branchModified && headDeleted) {
            conflictHandling(null, branchBlobId, fileName,writeFiles);
        }
    }
    private static void filesNotInSplit(String fileName,
                                     String headBlobId, String branchBlobId,
                                     TreeMap<String, String> writeFiles) throws IOException {
        boolean headDeleted = headBlobId == null;
        boolean branchDeleted = branchBlobId == null;

        if(branchDeleted && !headDeleted){
            writeFiles.put(fileName,headBlobId);
        }
        if(!branchDeleted && headDeleted){
            writeFiles.put(fileName,branchBlobId);
        }
        if(!headBlobId.equals(branchBlobId)){
            conflictHandling(headBlobId,branchBlobId,fileName,writeFiles);
        }
    }
    //将merge结果修改的文件写入CWD
    private static void mergeFilesOperation(TreeMap<String, String> writeFiles
            ,TreeMap<String, String>  deleteFiles) {
        for (String writeFileName : writeFiles.keySet()) {
            String blobId = writeFiles.get(writeFileName);
            Blob blob = readObject(join(CWD, blobId), Blob.class);
            writeContents(join(CWD,writeFileName), blob.getBytes());
        }
        for (String deleteFileName : deleteFiles.keySet()) {
            if(join(CWD,deleteFileName).exists()){
                join(CWD,deleteFileName).delete();
            }
        }
    }

    /**
     * 接收head分支与branch分支的内容，
     * 构成如下形式：
     * <<<<<<< HEAD
     * contents of file in current branch
     * =======
     * contents of file in given branch
     * >>>>>>>
     * 将该内容重新写回文件
     */
    private static void conflictHandling(String headBlodId, String branchBlobId, String fileName,TreeMap<String, String> writeFiles) throws IOException {
        loadStage();
        System.out.println("Encountered a merge conflict.");
        String conflictContents = getConflictContents(headBlodId, branchBlobId);
        Blob conflictFile = new Blob(conflictContents.getBytes(StandardCharsets.UTF_8), fileName);
        saveToFile(conflictFile, fileName, BLOBS);
        writeFiles.put(fileName,conflictFile.getId());
    }

    private static String getConflictContents(String headBlodId, String branchBlobId) {
        String head;
        String branch;
        if (headBlodId == null) {
            head = "";
            Blob branchBlob = readObject(join(BLOBS, branchBlobId), Blob.class);
            branch = new String(branchBlob.getBytes(), StandardCharsets.UTF_8);
            String contents = "<<<<<<< HEAD\n" + head + "=======\n"
                    + branch + ">>>>>>>\n";
            return contents;
        } else if ((branchBlobId == null)) {
            branch = "";
            Blob headBlob = readObject(join(BLOBS, headBlodId), Blob.class);
            head = new String(headBlob.getBytes(), StandardCharsets.UTF_8);
            String contents = "<<<<<<< HEAD\n" + head + "=======\n"
                    + branch + ">>>>>>>\n";
            return contents;
        } else {
            Blob headBlob = readObject(join(BLOBS, headBlodId), Blob.class);
            Blob branchBlob = readObject(join(BLOBS, branchBlobId), Blob.class);
            head = new String(headBlob.getBytes(), StandardCharsets.UTF_8);
            branch = new String(branchBlob.getBytes(), StandardCharsets.UTF_8);
            String contents = "<<<<<<< HEAD\n" + head + "=======\n"
                    + branch + ">>>>>>>\n";
            return contents;
        }
    }

    private static Commit createMergeCommit(String branchName,
                                            TreeMap<String,String> mergeResultFiles)
                                            throws IOException {
        String msg = "Merged " + getCurrentBranchName() + " into " + branchName + ".";
        ArrayList<String> parentsId = new ArrayList<>();
        parentsId.add(getBranchCommitId(branchName));
        parentsId.add(getCurrentBranch());
        return new Commit(msg, parentsId, mergeResultFiles);
    }
}

