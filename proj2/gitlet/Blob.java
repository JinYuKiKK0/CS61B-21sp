package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    private String id;
    private byte[] bytes;
    private String fileName;

    public Blob(byte[] contents, String filename){
        bytes = contents;
        fileName = filename;
        id = Utils.sha1(bytes, fileName);
    }

    public String getId() {
        return id;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getFileName() {
        return fileName;
    }
}
