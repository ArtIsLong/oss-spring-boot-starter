package io.github.artislong.model.download;

import lombok.Data;

/**
 * @author 陈敏
 * @version PartResult.java, v 1.1 2022/2/21 15:17 chenmin Exp $
 * Created on 2022/2/21
 */
@Data
public class DownloadPartResult {

    private int number;
    private long start;
    private long end;
    private boolean failed = false;
    private Exception exception;
    private Long clientCrc;
    private Long serverCrc;
    private long length;

    public DownloadPartResult(int number, long start, long end) {
        this.number = number;
        this.start = start;
        this.end = end;
    }

    public DownloadPartResult(int number, long start, long end, long length, long clientCrc) {
        this.number = number;
        this.start = start;
        this.end = end;
        this.length = length;
        this.clientCrc = clientCrc;
    }

}
