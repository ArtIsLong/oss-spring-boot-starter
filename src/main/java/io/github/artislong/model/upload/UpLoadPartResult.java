package io.github.artislong.model.upload;

import lombok.Data;

/**
 * 分块结果集
 * @author 陈敏
 * @version PartResult.java, v 1.1 2022/2/10 10:01 chenmin Exp $
 * Created on 2022/2/10
 */
@Data
public class UpLoadPartResult {

    /**
     * 分块号
     */
    private int number;
    /**
     * 分块在文件中的偏移量
     */
    private long offset;
    /**
     * 分块大小
     */
    private long length;
    /**
     * 分块失败标识
     */
    private boolean failed = false;
    /**
     * 分块上传失败异常
     */
    private Exception exception;
    /**
     * 分块crc
     */
    private Long partCrc;

    public UpLoadPartResult(int number, long offset, long length) {
        this.number = number;
        this.offset = offset;
        this.length = length;
    }

    public UpLoadPartResult(int number, long offset, long length, long partCRC) {
        this.number = number;
        this.offset = offset;
        this.length = length;
        this.partCrc = partCRC;
    }

}
