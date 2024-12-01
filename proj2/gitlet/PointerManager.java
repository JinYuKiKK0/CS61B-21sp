package gitlet;

public class PointerManager {
    public static void initializePointers(Commit initialCommit){
        //将master指针指向initial commit
        Utils.writeContents(Repository.master,initialCommit.getHash());
        //将HEAD指针指向master
        Utils.writeContents(Repository.HEAD, "ref: refs/heads/master");
    }
}
