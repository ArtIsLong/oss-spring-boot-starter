package io.github.artislong.core.baidu.model;

import lombok.Data;

import java.io.File;
import java.io.Serializable;

/**
 * @author 陈敏
 * @version FileStat.java, v 1.1 2022/2/9 22:54 chenmin Exp $
 * Created on 2022/2/9
 */
@Data
public class FileStat implements Serializable {

    private static final long serialVersionUID = -1223810339796425415L;

    /**
     * file size
     */
    private long size;
    /**
     * file last modified time.
     */
    private long lastModified;
    /**
     * file content's digest (signature).
     */
    private String digest;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((digest == null) ? 0 : digest.hashCode());
        result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
        result = prime * result + (int) (size ^ (size >>> 32));
        return result;
    }

    public static FileStat getFileStat(String uploadFile) {
        FileStat fileStat = new FileStat();
        File file = new File(uploadFile);
        fileStat.setSize(file.length());
        fileStat.setLastModified(file.lastModified());
        return fileStat;
    }

}
