package io.github.artislong.model.download;

import com.aliyun.oss.common.utils.BinaryUtil;
import lombok.Data;

import java.io.*;
import java.util.ArrayList;

/**
 * @author 陈敏
 * @version DownloadCheckPoint.java, v 1.1 2022/2/21 15:14 chenmin Exp $
 * Created on 2022/2/21
 */
@Data
public class DownloadCheckPoint implements Serializable {

    private static final long serialVersionUID = 4682293344365787077L;
    public static final String DOWNLOAD_MAGIC = "92611BED-89E2-46B6-89E5-72F273D4B0A3";

    /**
     * magic
     */
    private String magic;
    /**
     * the md5 of checkpoint data.
     */
    private int md5;
    /**
     * local path for the download.
     */
    private String downloadFile;
    /**
     * bucket name
     */
    private String bucketName;
    /**
     * object key
     */
    private String key;

    private String checkPointFile;
    /**
     * object state
     */
    private DownloadObjectStat objectStat;
    /**
     * download parts list.
     */
    private ArrayList<DownloadPart> downloadParts;

    private long originPartSize;

    private String versionId;

    /**
     * Loads the checkpoint data from the checkpoint file.
     */
    public synchronized void load(String cpFile) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(cpFile);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        DownloadCheckPoint dcp = (DownloadCheckPoint) in.readObject();
        assign(dcp);
        in.close();
        fileIn.close();
    }

    /**
     * Writes the checkpoint data to the checkpoint file.
     */
    public synchronized void dump() throws IOException {
        this.md5 = hashCode();
        FileOutputStream fileOut = new FileOutputStream(checkPointFile);
        ObjectOutputStream outStream = new ObjectOutputStream(fileOut);
        outStream.writeObject(this);
        outStream.close();
        fileOut.close();
    }

    public String getTempDownloadFile() {
        if (getVersionId() != null) {
            return downloadFile + "." + BinaryUtil.encodeMD5(getVersionId().getBytes()) + ".tmp";
        } else {
            return downloadFile + ".tmp";
        }
    }

    /**
     * Updates the part's download status.
     *
     * @throws IOException
     */
    public synchronized void update(int index, boolean completed) throws IOException {
        downloadParts.get(index).setCompleted(completed);
    }

    /**
     * Check if the object matches the checkpoint information.
     */
    public synchronized boolean isValid(DownloadObjectStat objectStat) {
        // 比较checkpoint的magic和md5
        if (this.magic == null || !this.magic.equals(DOWNLOAD_MAGIC) || this.md5 != hashCode()) {
            return false;
        }

        // Object's size, last modified time or ETAG are not same as the one
        // in the checkpoint.
        if (this.objectStat.getSize() != objectStat.getSize() || !this.objectStat.getLastModified().equals(objectStat.getLastModified())
                || !this.objectStat.getDigest().equals(objectStat.getDigest())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bucketName == null) ? 0 : bucketName.hashCode());
        result = prime * result + ((downloadFile == null) ? 0 : downloadFile.hashCode());
        result = prime * result + ((checkPointFile == null) ? 0 : checkPointFile.hashCode());
        result = prime * result + ((magic == null) ? 0 : magic.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((objectStat == null) ? 0 : objectStat.hashCode());
        result = prime * result + ((downloadParts == null) ? 0 : downloadParts.hashCode());
        result = prime * result + ((versionId == null) ? 0 : versionId.hashCode());
        return result;
    }

    private void assign(DownloadCheckPoint dcp) {
        this.setMagic(dcp.getMagic());
        this.setMd5(dcp.getMd5());
        this.setDownloadFile(dcp.getDownloadFile());
        this.setCheckPointFile(dcp.getCheckPointFile());
        this.setBucketName(dcp.getBucketName());
        this.setKey(dcp.getKey());
        this.setObjectStat(dcp.getObjectStat());
        this.setDownloadParts(dcp.getDownloadParts());
        this.setOriginPartSize(dcp.getOriginPartSize());
        this.setVersionId(dcp.getVersionId());
    }

}
