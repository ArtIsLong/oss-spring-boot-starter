package io.github.artislong.model.download;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 陈敏
 * @version DownloadPart.java, v 1.1 2022/2/21 15:15 chenmin Exp $
 * Created on 2022/2/21
 */
@Data
public class DownloadPart implements Serializable {

    private static final long serialVersionUID = -3655925846487976207L;

    private int index;
    private long start;
    private long end;
    private boolean isCompleted;
    private long length;
    private long crc;
    private long fileStart;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + (isCompleted ? 1231 : 1237);
        result = prime * result + (int) (end ^ (end >>> 32));
        result = prime * result + (int) (start ^ (start >>> 32));
        result = prime * result + (int) (crc ^ (crc >>> 32));
        result = prime * result + (int) (fileStart ^ (fileStart >>> 32));
        return result;
    }

}
