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
            //FIXME ：file text contents doesn't restore but -> blob
            copyBlobToFile(blobId, fileName);
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    /**
     * Copy the file with the same name as the commit in the commitId to the current working directory (CWD)
     * overwriting if it already exists
     * 支持短字符
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
        // delete the files unique to the original branch check out files that
        checkoutFilesOperation(getBranchCommitId(branchName));
        //set given branch to the current branch
        setBranch(branchName);
        initializeStages();
    }

    private static void filesCheckBeforeCheckOut(String fileName, String commitId) {
        boolean isUntracked = !isFileTrackedInCommit(fileName, getTheLatestCommit());
        boolean willBeOverwritten = isFileTrackedInCommit(fileName, getCommitById(commitId));
        if (isUntracked && willBeOverwritten) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
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
        //if there are untracked files in current branch and checkout will overwrite them, exit and print err
        List<String> fileNames = plainFilenamesIn(CWD);
        for (String fileName : fileNames) {
            filesCheckBeforeCheckOut(fileName, commitId);
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
            if(join(BRANCHES, branchName).exists()){
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
            if (givenBranchCommitTree.containsKey(commitId) && headCommitTree.get(commitId) < minValue) {
                minKey = commitId;
                minValue = givenBranchCommitTree.get(commitId);
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
        /**
         * depth = 1
         * queue[2]
         * marked[4,3]
         */
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
        mergeFilesOperation(getMergeResultFiles(branchName));
        createMergeCommit(branchName);
    }


    /**
     * @param branchName
     * @return
     *///TODO:暂存的目的是使得创建合并提交时从暂存区获得需要跟踪的文件
    private static TreeMap<String, String> getMergeResultFiles(String branchName) throws IOException {
        TreeMap<String, String> branchFiles = getBranchCommit(branchName).getBlobsID();
        TreeMap<String, String> splitFiles = getCommitById(findSplitPointId(branchName)).getBlobsID();
        TreeMap<String, String> headFiles = getTheLatestCommit().getBlobsID();
        TreeMap<String, String> mergeFiles = new TreeMap<>();
        HashSet<String> allFiles = new HashSet<>();
        allFiles.addAll(splitFiles.keySet());
        allFiles.addAll(headFiles.keySet());
        allFiles.addAll(branchFiles.keySet());
        for (String fileName : allFiles) {
            String splitBlobId = splitFiles.get(fileName);
            String headBlobId = headFiles.get(fileName);
            String branchBlobId = branchFiles.get(fileName);
            checkBeforeMergeFile(fileName,mergeFiles);
            if (splitBlobId != null) {
                fileHandleInSplit(fileName, splitBlobId, headBlobId, branchBlobId, mergeFiles);
            } else {
                fileHandleNotInSplit(fileName, headBlobId, branchBlobId, mergeFiles);
            }
        }
        return mergeFiles;
    }

    private static void fileHandleInSplit(String fileName, String splitBlob, String headBlob,
                                          String branchBlob, TreeMap<String, String> mergeResult) throws IOException {
        loadStage();
        boolean headModified = (headBlob != null && Objects.equals(headBlob, splitBlob));
        boolean branchModified = (branchBlob != null && Objects.equals(branchBlob, splitBlob));
        boolean headDeleted = (headBlob == null);
        boolean branchDeleted = (branchBlob == null);
        //1. split中存在，split == head , other修改 ; 检出至other中版本并暂存以供提交
        if (!headModified && branchModified) {
            mergeResult.put(fileName, branchBlob);
            addStageMap.stageSave(fileName, branchBlob);
        }
        //2. split中存在，split == other , head修改 ; 保持head原样
        else if (!branchModified && headModified) {
            mergeResult.put(fileName, headBlob);
        }
        //3. 与split相比,head与other 都以相同方式修改(修改或移除) ; 保持原样
        //如果head & other都移除了该文件,而CWD中存在同名文件，合并后依然缺省(既不暂存也不追踪)
        else if ((headModified && branchModified && headBlob.equals(branchBlob))
                || (headDeleted && branchDeleted)) {
            if (headBlob != null) {
                mergeResult.put(fileName, headBlob);
            }
        }
        //4. split中存在，split == other，head中被移除 ; 保持被移除的状态
        else if (!branchModified && headDeleted) {
            mergeResult.remove(fileName);
        }
        //5. split中存在，split == head，other中被移除 ; 从CWD中移除该文件，同时不跟踪(mergeCommit中不包含)
        else if (!headModified && branchDeleted) {
            mergeResult.remove(fileName);
            if(join(CWD,fileName).exists()){
                join(CWD,fileName).delete();
            }
        }
        //6. 与split相比，head与other都以不同方式修改(或移除或修改) ; conflict
        //1.split存在 head修改，other移除
        //2.split存在 other修改，head移除
        //3.文件在split缺失，head与other有不同内容
        else if (headModified && branchModified && !headBlob.equals(branchBlob)) {
            conflictHandling(headBlob, branchBlob, fileName);
        } else if ((headModified && branchDeleted) || (headDeleted && branchModified)) {
            conflictHandling(headBlob, branchBlob, fileName);
        }
    }

    private static void fileHandleNotInSplit(String fileName, String headBlob,
                                             String branchBlob, TreeMap<String, String> mergeResult) throws IOException {
        boolean headHas = (headBlob != null);
        boolean branchHas = (branchBlob != null);
        //7. split与head中不存在，仅存在于other ; 合并后应该检出至other版本，并将该文件暂存以便创建合并提交时包含该文件
        //8. split与other中不存在，仅存在于head ; 合并后仍然存在
        if (headHas && branchHas) {
            // 双方都新增该文件
            if (headBlob.equals(branchBlob)) {
                // 内容相同 → 正常合并
                mergeResult.put(fileName, headBlob);
            } else {
                // 内容不同 → 冲突
                conflictHandling(fileName, headBlob, branchBlob);
            }
        } else if (headHas) {
            // 仅在Head新增 → 保留
            mergeResult.put(fileName, headBlob);
        } else if (branchHas) {
            // 仅在Other新增 → 保留
            mergeResult.put(fileName, branchBlob);
        }
    }

    //如果当前提交中的未跟踪文件将被合并覆盖或删除，打印
    //There is an untracked file in the way; delete it, or add and commit it first.并退出
    private static void checkBeforeMergeFile(String fileName, TreeMap<String, String> resultFiles) {
        boolean isUntracked = !isFileTrackedInCommit(fileName, getTheLatestCommit());
        boolean isOverWritten = resultFiles.containsKey(fileName);
        if (isUntracked && isOverWritten) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
    }
    private static void mergeFilesOperation(TreeMap<String,String> mergeResult){
        for (Map.Entry<String, String> files : mergeResult.entrySet()) {
            String fileName = files.getKey();
            String contents = files.getValue();
            writeContents(join(CWD,fileName),contents);
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
    private static void conflictHandling(String headBlodId, String branchBlobId, String fileName) throws IOException {
        loadStage();
        System.out.println("Encountered a merge conflict.");
        String conflictContents = getConflictContents(headBlodId, branchBlobId);
        Blob conflictFile = new Blob(conflictContents.getBytes(StandardCharsets.UTF_8), fileName);
        saveToFile(conflictFile, fileName, BLOBS);
        addStageMap.stageSave(fileName, conflictFile.getId());
        writeContents(join(CWD, fileName), conflictContents);
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

    private static Commit createMergeCommit(String branchName) throws IOException {
        TreeMap<String, String> mergeResultFiles = getMergeResultFiles(branchName);
        String msg = "Merged " + getCurrentBranchName() + " into " + branchName + ".";
        ArrayList<String> parentsId = new ArrayList<>();
        parentsId.add(getBranchCommitId(branchName));
        parentsId.add(getCurrentBranch());
        return new Commit(msg, parentsId, mergeResultFiles);
    }
}

