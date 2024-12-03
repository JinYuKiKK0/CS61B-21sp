package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private Date currentTime;
    private Map<String,String > pathToBlobID = new HashMap<>();
    private String ID;
    private String Author;
    private List<String> parentCommit;
    private String timeStamp;
    /** The message of this Commit. */
    private String message;

    /* TODO: fill in the rest of this class. */
    public Commit(String summary,Date date){
        currentTime = date;
        timeStamp = date2TimeStamp(currentTime);
        message = summary;
        Author = "JinYu";
        parentCommit = new ArrayList<>();
        setHash(this);
    }

    public void setHash(Commit commit){
        byte[] bytes = Utils.serialize(commit);
        commit.ID = Utils.sha1(bytes);
    }
    public String getHash() {
        return ID;
    }

    private static String date2TimeStamp(Date date){
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
    public Map<String,String> getPathToBlobID(){
        return pathToBlobID;
    }
}
