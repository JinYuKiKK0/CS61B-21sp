package gitlet;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Repository.COMMITS;
import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 * <p>
 * does at a high level.
 *
 * @author JinYu
 */
public class Commit implements Serializable {
    /**
     * <p>
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private String id;
    private String message;
    private String date;
    private ArrayList<String> parentsID;
    //fileName -> blob.SHA-1
    private TreeMap<String, String> blobsID;

    public Commit() {
        message = "initial commit";
        parentsID = new ArrayList<>();
        blobsID = new TreeMap<>();
        date = dateToTimeStamp(new Date(0));
        id = generateID();
    }

    public Commit(String commitMessage, Commit parentCommit) {
        message = commitMessage;
        parentsID = new ArrayList<>();
        blobsID = parentCommit.getBlobsID();
        date = dateToTimeStamp(new Date());
        id = generateID();
    }

    public Commit(String commitMessage, ArrayList<String> parentsID, TreeMap<String, String> result) {
        message = commitMessage;
        this.parentsID = parentsID;
        blobsID = result;
        date = dateToTimeStamp(new Date());
        id = generateID();
    }

    public ArrayList<String> getParentsID() {
        return parentsID;
    }

    public TreeMap<String, String> getBlobsID() {
        return blobsID;
    }

    public String getId() {
        return id;
    }

    public void setBlobsID(TreeMap<String, String> file2blob) {
        blobsID = file2blob;
    }

    //FIXME:before merge
    public void setParent(String parentID) {
        this.parentsID.add(parentID);
    }

    private String generateID() {
        return sha1(message, date, parentsID.toString(), blobsID.toString());
    }

    public Commit getParentCommit() {
        if (parentsID.isEmpty()) {
            return null;
        }
        String parentId = parentsID.get(0);
        return getCommitById(parentId);
    }

    public String getMessage() {
        return message;
    }

    private static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }

    public void printCommit() {
        message("===");
        message("commit %s", id);
        if (parentsID.size() > 1) {
            String parent1 = parentsID.get(0).substring(0, 7);
            String parent2 = parentsID.get(1).substring(0, 7);
            message("Merge: %s %s", parent1, parent2);
        }
        message("Date: %s", date);
        message(message);
        message("");
    }

    /**
     * 获取给定文件名的blobId
     */
    public String getFileBlobId(String fileName) {
        Set<Map.Entry<String, String>> fileName2blobId = blobsID.entrySet();
        for (Map.Entry<String, String> fileNameBlobIdEntry : fileName2blobId) {
            if (fileNameBlobIdEntry.getKey().equals(fileName)) {
                return fileNameBlobIdEntry.getValue();
            }
        }
        return null;
    }


    public static Commit getCommitById(String commitId) {
        List<String> commitFiles = plainFilenamesIn(COMMITS);
        String foundFile = null;
        if(commitId.length() < 40){
            for (String commitid : commitFiles) {
                if(commitid.length() < commitId.length()){
                    continue;
                }
                String shortId = commitid.substring(0,commitid.length());
                if(commitId.equals(shortId)){
                    if(foundFile != null){
                        System.out.println("Multiple commits with that prefix exist.");
                        System.exit(0);
                    }
                    foundFile = commitid;
                }
                if(foundFile != null){
                    return readObject(join(COMMITS,commitId), Commit.class);
                }
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
        }
        if(!commitFiles.contains(commitId)){
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return readObject(join(COMMITS,commitId), Commit.class);
    }

}
