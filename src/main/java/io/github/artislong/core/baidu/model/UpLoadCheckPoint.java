package io.github.artislong.core.baidu.model;

import com.baidubce.services.bos.model.PartETag;
import lombok.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 陈敏
 * @version UpLoadCheckPoint.java, v 1.1 2022/2/9 22:52 chenmin Exp $
 * Created on 2022/2/9
 */
@Data
public class UpLoadCheckPoint implements Serializable {

    private static final long serialVersionUID = 5424904565837227164L;

    public static final String UPLOAD_MAGIC = "FE8BB4EA-B593-4FAC-AD7A-2459A36E2E62";

    private String magic;
    private int md5;
    private String uploadFile;
    private FileStat uploadFileStat;
    private String key;
    private String bucket;
    private String checkpointFile;
    private String uploadId;
    private List<UploadPart> uploadParts = new ArrayList<>();
    private List<PartETag> partETags = new ArrayList<>();
    private long originPartSize;

    /**
     * Gets the checkpoint data from the checkpoint file.
     */
    public synchronized void load(String cpFile) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(cpFile);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        UpLoadCheckPoint ucp = (UpLoadCheckPoint) in.readObject();
        assign(ucp);
        in.close();
        fileIn.close();
    }

    /**
     * Writes the checkpoint data to the checkpoint file.
     */
    public synchronized void dump(String cpFile) throws IOException {
        this.setMd5(hashCode());
        FileOutputStream fileOut = new FileOutputStream(cpFile);
        ObjectOutputStream outStream = new ObjectOutputStream(fileOut);
        outStream.writeObject(this);
        outStream.close();
        fileOut.close();
    }

    /**
     * The part upload complete, update the status.
     *
     * @throws IOException
     */
    public synchronized void update(int partIndex, PartETag partETag, boolean completed) throws IOException {
        this.getPartETags().add(partETag);
        this.getUploadParts().get(partIndex).setCompleted(completed);
    }

    /**
     * Check if the local file matches the checkpoint.
     */
    public synchronized boolean isValid(File uploadFile) {
        // 比较checkpoint的magic和md5
        // Compares the magic field in checkpoint and the file's md5.
        if (this.getMagic() == null || !this.getMagic().equals(UPLOAD_MAGIC) || this.getMd5() != hashCode()) {
            return false;
        }

        // Checks if the file exists.
        if (!uploadFile.exists()) {
            return false;
        }

        // The file name, size and last modified time must be same as the
        // checkpoint.
        // If any item is changed, return false (re-upload the file).
        if (!this.getUploadFile().equals(uploadFile) || this.getUploadFileStat().getSize() != uploadFile.length()
                || this.getUploadFileStat().getLastModified() != uploadFile.lastModified()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((magic == null) ? 0 : magic.hashCode());
        result = prime * result + ((partETags == null) ? 0 : partETags.hashCode());
        result = prime * result + ((uploadFile == null) ? 0 : uploadFile.hashCode());
        result = prime * result + ((uploadFileStat == null) ? 0 : uploadFileStat.hashCode());
        result = prime * result + ((uploadId == null) ? 0 : uploadId.hashCode());
        result = prime * result + ((uploadParts == null) ? 0 : uploadParts.hashCode());
        result = prime * result + (int) originPartSize;
        return result;
    }

    public void assign(UpLoadCheckPoint ucp) {
        this.setMagic(ucp.magic);
        this.setMd5(ucp.md5);
        this.setUploadFile(ucp.uploadFile);
        this.setUploadFileStat(ucp.uploadFileStat);
        this.setKey(ucp.key);
        this.setBucket(ucp.bucket);
        this.setCheckpointFile(ucp.checkpointFile);
        this.setUploadId(ucp.uploadId);
        this.setUploadParts(ucp.uploadParts);
        this.setPartETags(ucp.partETags);
        this.setOriginPartSize(ucp.originPartSize);
    }

}
