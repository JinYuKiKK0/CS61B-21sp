package gitlet;
import static gitlet.Utils.*;
import static gitlet.Repository.*;

public class PointerManager {
    public static void initializePointers(Commit initialCommit){
        //Point the master pointer to initial commit
        writeContents(master,initialCommit.getId());
        //Point the HEAD pointer to master
        writeContents(HEAD,initialCommit.getId());
    }

    /**
     *
     * @return Returns the hash value of the commit object pointed to by the HEAD pointer
     */
    public static String getCurrentBranch(){
        return readContentsAsString(HEAD);
    }
}
