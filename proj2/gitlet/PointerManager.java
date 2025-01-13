package gitlet;
import java.io.File;
import java.util.List;

import static gitlet.Utils.*;
import static gitlet.Repository.*;

public class PointerManager {
    public static void initializePointers(Commit initialCommit){
        //Point the master pointer to initial commit
        writeContents(master,initialCommit.getId());
        //Point the HEAD pointer to master
        writeContents(HEAD,initialCommit.getId());
        writeContents(BRANCH,"master");
    }
    // any pointer
    public static void pointerAdvance(Commit nextCommit){
        String curBranchName = getCurrentBranchName();
        File curBranchFile = findBranchFile(curBranchName);
        writeContents(curBranchFile,nextCommit.getId());
        writeContents(HEAD,nextCommit.getId());
    }
    /**
     *
     * @return Returns the hash value of the commit object pointed to by the HEAD pointer
     */
    private static File findBranchFile(String branchName){
        return join(branches,branchName);
    }
    public static String getCurrentBranch(){
        return readContentsAsString(HEAD);
    }
    public static String getCurrentBranchName(){return readContentsAsString(BRANCH);}
    /**
     *
     * @return Return the latestCommit that HEAD pointer point to
     */
    public static Commit getTheLatestCommit(){
        return readObject(join(Commits, getCurrentBranch()), Commit.class);
    }
}
