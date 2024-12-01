package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date; // TODO: You'll likely use this in this class

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private Date date;
    private ArrayList<blob> blobs;
    private String ID;
    private String Author;
    private Commit parentCommit;
    /** The message of this Commit. */
    private String message;

    /* TODO: fill in the rest of this class. */
    public Commit(String summary,Commit parent){
        blobs = new ArrayList<>();
        date = new Date();
        message = summary;
        Author = "JinYu";
        parentCommit = parent;
        ID = Utils.sha1(date,blobs,message,Author,parentCommit);
    }
    public Commit(Date date){
        blobs = new ArrayList<>();
        this.date = date;
        message = "initial commit";
        Author = "JinYu";
        ID = Utils.sha1(this.date,blobs,message,Author,parentCommit);
    }

    public String getHash() {
        return ID;
    }
}
