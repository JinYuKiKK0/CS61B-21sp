package gitlet;
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
    public static void pointerAdvance(Commit nextCommit){
        writeContents(master,nextCommit.getId());
        writeContents(HEAD,nextCommit.getId());
    }
    /**
     *
     * @return Returns the hash value of the commit object pointed to by the HEAD pointer
     */
    public static String getCurrentBranch(){
        return readContentsAsString(HEAD);
    }

    /**
     *
     * @return Return the latestCommit that HEAD pointer point to
     */
    public static Commit getTheLatestCommit(){
        return readObject(join(Commits, getCurrentBranch()), Commit.class);
    }
    public static Commit getCommitById(String commitId){
        return readObject(join(Commits , commitId), Commit.class);
    }
}
