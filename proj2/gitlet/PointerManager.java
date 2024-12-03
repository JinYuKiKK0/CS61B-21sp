package gitlet;
import static gitlet.Utils.*;
import static gitlet.Repository.*;

public class PointerManager {
    public static void initializePointers(Commit initialCommit){
        //将master指针指向initial commit
        writeContents(master,HEAD);
        //将HEAD指针指向master
        writeContents(HEAD,initialCommit.getHash());
    }

    /**
     *
     * @return 返回HEAD指针指向的commit对象的哈希值
     */
    public static String getCurrentBranch(){
        return readContentsAsString(HEAD);
    }
}
