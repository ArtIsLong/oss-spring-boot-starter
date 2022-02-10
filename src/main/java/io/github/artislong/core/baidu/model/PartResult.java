package io.github.artislong.core.baidu.model;

import lombok.Data;

/**
 * @author 陈敏
 * @version PartResult.java, v 1.1 2022/2/10 10:01 chenmin Exp $
 * Created on 2022/2/10
 */
@Data
public class PartResult {

    /**
     * part number
     */
    private int number;
    /**
     * offset in the file
     */
    private long offset;
    /**
     * part size
     */
    private long length;
    /**
     * part upload failure flag
     */
    private boolean failed;
    /**
     * part upload exception
     */
    private Exception exception;
    private Long partCrc;

    public PartResult(int number, long offset, long length) {
        this.number = number;
        this.offset = offset;
        this.length = length;
    }

    public PartResult(int number, long offset, long length, long partCRC) {
        this.number = number;
        this.offset = offset;
        this.length = length;
        this.partCrc = partCRC;
    }

}
