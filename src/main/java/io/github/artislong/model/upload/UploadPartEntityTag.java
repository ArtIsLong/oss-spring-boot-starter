package io.github.artislong.model.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 分块标签
 * @author 陈敏
 * @version PartETag.java, v 1.1 2022/2/10 23:38 chenmin Exp $
 * Created on 2022/2/10
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class UploadPartEntityTag implements Serializable {

    private static final long serialVersionUID = 2471854027355307627L;

    /**
     * 分块号
     */
    private int partNumber;

    /**
     * 标签
     */
    private String eTag;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.eTag == null) ? 0 : this.eTag.hashCode());
        result = prime * result + this.partNumber;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        UploadPartEntityTag other = (UploadPartEntityTag) obj;
        if (this.eTag == null) {
            if (other.eTag != null) {
                return false;
            }
        } else if (!this.eTag.equals(other.eTag)) {
            return false;
        }
        if (this.partNumber != other.partNumber) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PartETag [partNumber=" + this.partNumber + ", eTag=" + this.eTag + "]";
    }
}
