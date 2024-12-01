package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Date;

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
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

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
    public static final File refs = join(GITLET_DIR, "refs");
    public static final File heads = join(refs, "heads");
    public static final File master = join(heads, "master");
    public static final File remotes = join(refs, "remotes");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File index = join(GITLET_DIR, "index");
    public static final File removedStage = join(GITLET_DIR, "removedStage");


    /* TODO: fill in the rest of this class. */
    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        //create a .gitlet directory as Gitlet Repository
        GITLET_DIR.mkdir();
            /*create a Staging Area in .gitlet ; Staging Area will keep track of
            all the files that are staged for addition or removal
             */
        objects.mkdir();
        Commits.mkdir();
        blobs.mkdir();
        refs.mkdir();
        heads.mkdir();
        master.createNewFile();
        remotes.mkdir();
        HEAD.createNewFile();
        index.mkdir();
        removedStage.mkdir();

        Commit initCommit = new Commit(new Date(0));

        File commitFile = join(Commits, initCommit.getHash());
        commitFile.createNewFile();
        PointerManager.initializePointers(initCommit);

                /*when init,create an initial commit blob in objects and
                 create pointer "master" and "HEAD" point to the initial commit blob
                 */
    }
}
