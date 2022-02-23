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

    /**
     * part index (starting from 0).
     */
    private int index;
    /**
     * start index;
     */
    private long start;
    /**
     * end index;
     */
    private long end;
    /**
     * flag of part download finished or not;
     */
    private boolean isCompleted;
    /**
     * length of part
     */
    private long length;
    /**
     * part crc.
     */
    private long crc;
    /**
     *  start index in file, for range get
     */
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
