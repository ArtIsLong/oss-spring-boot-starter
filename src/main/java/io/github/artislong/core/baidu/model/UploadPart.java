package io.github.artislong.core.baidu.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 陈敏
 * @version UploadPart.java, v 1.1 2022/2/9 23:01 chenmin Exp $
 * Created on 2022/2/9
 */
@Data
public class UploadPart implements Serializable {

    private static final long serialVersionUID = 6692863980224332199L;

    /**
     * part number
     */
    private int number;
    /**
     * the offset in the file
     */
    private long offset;
    /**
     * part size
     */
    private long size;
    /**
     * upload completeness flag.
     */
    private boolean isCompleted;
    /**
     * part crc
     */
    private long crc;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isCompleted ? 1231 : 1237);
        result = prime * result + number;
        result = prime * result + (int) (offset ^ (offset >>> 32));
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + (int) (crc ^ (crc >>> 32));
        return result;
    }

}
