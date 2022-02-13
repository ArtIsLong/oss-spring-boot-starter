package io.github.artislong.model.slice;

import lombok.Data;

import java.io.Serializable;

/**
 * 分块
 * @author 陈敏
 * @version UploadPart.java, v 1.1 2022/2/9 23:01 chenmin Exp $
 * Created on 2022/2/9
 */
@Data
public class UploadPart implements Serializable {

    private static final long serialVersionUID = 6692863980224332199L;

    /**
     * 分块号(顺序)
     */
    private int number;
    /**
     * 分块在文件中的偏移量
     */
    private long offset;
    /**
     * 分块大小
     */
    private long size;
    /**
     * 分块成功标识
     */
    private boolean isCompleted = false;
    /**
     * 分块crc
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
