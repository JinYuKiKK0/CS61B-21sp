package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /* TODO: fill in the rest of this class. */
    public static void init(){
        //create a .gitlet directory as Gitlet Repository
        GITLET_DIR.mkdir();
            /*create a Staging Area in .gitlet ; Staging Area will keep track of
            all the files that are staged for addition or removal
             */
        File StagingArea = join(GITLET_DIR,"StagingArea");
        File objects = join(GITLET_DIR,"objects");
        File Commits = join(objects,"Commits");
        File blobs = join(objects,"blobs");
        File refs = join(GITLET_DIR,"refs");
        File master = join(refs,"heads");
        try {
                //Area.1:Staged for addition
                //Area.2:Staged for removal
            StagingArea.createNewFile();
            //create a objects to keep track of our commit tree
            objects.createNewFile();
            //create a dir for pointer
            Commits.createNewFile();
            blobs.createNewFile();
            refs.mkdir();
            master.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
            Commit initCommit = new Commit(new Date(0));
        writeObject(Commits,initCommit);
                /*when init,create an initial commit blob in objects and
                 create pointer "master" and "HEAD" point to the initial commit blob
                 */
    }
}
