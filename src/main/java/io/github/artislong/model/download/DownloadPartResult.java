package io.github.artislong.model.download;

import lombok.Data;

/**
 * @author 陈敏
 * @version PartResult.java, v 1.1 2022/2/21 15:17 chenmin Exp $
 * Created on 2022/2/21
 */
@Data
public class DownloadPartResult {

    /**
     * part number, starting from 1.
      */
    private int number;
    /**
     * start index in the part.
      */
    private long start;
    /**
     * end index in the part.
      */
    private long end;
    /**
     * flag of part upload failure.
      */
    private boolean failed = false;
    /**
     * Exception during part upload.
      */
    private Exception exception;
    /**
     * client crc of this part
      */
    private Long clientCrc;
    /**
     * server crc of this file
      */
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
