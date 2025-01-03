package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

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
    public static final File branches = join(GITLET_DIR,"branches");
    public static final File master = join(branches,"master");
    public static final File HEAD = join(GITLET_DIR,"HEAD");
    public static final File BRANCH = join(GITLET_DIR,"BRANCH");
    public static final File Stages = join(GITLET_DIR,"Stages");
    public static final File addStage = join(Stages,"addStage");
    public static final File removeStage = join(Stages,"removeStage");

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
        branches.mkdir();
        master.createNewFile();
        HEAD.createNewFile();
        BRANCH.createNewFile();
        Stages.mkdir();
        addStage.createNewFile();
        removeStage.createNewFile();

    }

}
