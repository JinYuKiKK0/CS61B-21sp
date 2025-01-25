package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

public class Blob implements Serializable {
    private String id;
    private byte[] bytes;
    private String fileName;

    public Blob(byte[] contents, String filename) {
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

    public boolean isContentChanged(File file) {
        byte[] readContents = Utils.readContents(file);
        return !Arrays.equals(this.bytes, readContents);
    }

    public boolean isContentChanged(Blob blob)  {
        return !id.equals(blob.getId());
    }
}
