package io.github.artislong.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;

/**
 * @author 陈敏
 * @version OssRequest.java, v 1.0 2024/2/18 17:18 chenmin Exp $
 * Created on 2024/2/18
 */
@Data
@Accessors(chain = true)
public class OssGenericRequest {

    private String targetName;
    private InputStream inputStream;
    private Boolean override;

    private String bucketName;

}
