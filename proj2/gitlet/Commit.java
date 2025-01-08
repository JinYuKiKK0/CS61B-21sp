package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     * <p>
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private String id;
    private String message;
    private String date;
    private String  parentsID;
    //fileName -> blob.SHA-1
    private TreeMap<String,String> blobsID;

    public Commit() {
        message = "initial commit";
        parentsID = null;
        blobsID = new TreeMap<>();
        date = dateToTimeStamp(new Date(0));
        id = generateID();
    }
    public Commit(String commitMessage,Commit parentCommit) {
        message = commitMessage;
        parentsID = parentCommit.getParentsID();
        blobsID = parentCommit.getBlobsID();
        date =  dateToTimeStamp(new Date());
        id = generateID();
    }

    public String getParentsID() {
        return parentsID;
    }

    public TreeMap<String, String> getBlobsID() {
        return blobsID;
    }

    public String getId() {
        return id;
    }

    public void setBlobsID(TreeMap<String,String > file2blob){
        blobsID = file2blob;
    }
    public void setParentsID(String parentsID){
        this.parentsID = parentsID;
    }
    private String generateID(){
        return Utils.sha1(message,Utils.serialize(parentsID),Utils.serialize(blobsID),date);
    }
    private static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
}
