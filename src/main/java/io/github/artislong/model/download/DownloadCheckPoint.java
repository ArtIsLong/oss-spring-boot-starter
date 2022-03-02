package io.github.artislong.model.download;

import cn.hutool.core.io.IoUtil;
import lombok.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 陈敏
 * @version DownloadCheckPoint.java, v 1.1 2022/2/21 15:14 chenmin Exp $
 * Created on 2022/2/21
 */
@Data
public class DownloadCheckPoint implements Serializable {

    private static final long serialVersionUID = 4682293344365787077L;
    public static final String DOWNLOAD_MAGIC = "92611BED-89E2-46B6-89E5-72F273D4B0A3";

    private String magic;
    private int md5;
    private String downloadFile;
    private String bucketName;
    private String key;
    private String checkPointFile;
    private DownloadObjectStat objectStat;
    private List<DownloadPart> downloadParts = Collections.synchronizedList(new ArrayList<>());
    private long originPartSize;

    /**
     * 从缓存文件中加载断点数据
     * @param checkPointFile 断点续传进度记录文件
     */
    public synchronized void load(String checkPointFile) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(checkPointFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        DownloadCheckPoint dcp = (DownloadCheckPoint) ois.readObject();
        assign(dcp);
        IoUtil.close(ois);
        IoUtil.close(fis);
    }

    /**
     * 将断点信息写入到断点缓存文件
     */
    public synchronized void dump() throws IOException {
        this.md5 = hashCode();
        FileOutputStream outputStream = new FileOutputStream(checkPointFile);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(this);
        IoUtil.close(objectOutputStream);
        IoUtil.close(outputStream);
    }

    /**
     * 获取下载缓存文件名称
     * @return 缓存文件名
     */
    public String getTempDownloadFile() {
        return downloadFile + ".tmp";
    }

    /**
     * 更新分片状态
     * @param index 分片索引
     * @param completed 对应分片是否完成
     */
    public synchronized void update(int index, boolean completed) throws IOException {
        downloadParts.get(index).setCompleted(completed);
    }

    /**
     * 校验下载文件与断点信息是否一致
     * @param objectStat 文件状态
     * @return 校验是否通过
     */
    public synchronized boolean isValid(DownloadObjectStat objectStat) {
        // 比较checkpoint的magic和md5
        if (this.magic == null || !this.magic.equals(DOWNLOAD_MAGIC) || this.md5 != hashCode()) {
            return false;
        }

        // 比较文件大小及最新修改时间
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
    }

}
