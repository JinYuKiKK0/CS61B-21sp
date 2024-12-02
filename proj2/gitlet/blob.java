package gitlet;

public class blob {
    private String id;
    private byte[] bytes;
    private String refs;

    public blob(byte[] contents,String filePath){
        bytes = contents;
        refs = filePath;
        id = Utils.sha1(bytes,refs);
    }

    public String getId() {
        return id;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getRefs() {
        return refs;
    }
}
