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
    private String author;
    private ArrayList<String> parentsID;
    private ArrayList<String> blobsID;

    public Commit() {
        message = "initial commit";
        author = "JinYu";
        parentsID = new ArrayList<>();
        blobsID = new ArrayList<>();
        date = dateToTimeStamp(new Date(0));
        id = generateID();
    }
    public Commit(String commitMessage) {
        message = commitMessage;
        author = "JinYu";
        parentsID = new ArrayList<>();
        blobsID = new ArrayList<>();
        date = dateToTimeStamp(new Date());
        id = generateID();
    }

    public ArrayList<String> getParentsID() {
        return parentsID;
    }

    public ArrayList<String> getBlobsID() {
        return blobsID;
    }

    public String getId() {
        return id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDate(String date) {
        this.date = date;
    }

    private String generateID(){
        return Utils.sha1(message,author,Utils.serialize(parentsID),Utils.serialize(blobsID),date);
    }
    private static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
}
