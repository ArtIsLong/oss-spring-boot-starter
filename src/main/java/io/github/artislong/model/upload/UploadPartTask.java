package io.github.artislong.model.upload;

/**
 * @author 陈敏
 * @version UploadPartTask.java, v 1.0 2022/10/12 23:42 chenmin Exp $
 * Created on 2022/10/12
 */

import cn.hutool.core.io.FileUtil;
import io.github.artislong.core.StandardOssClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * 分片上传Task
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadPartTask implements Callable<UploadPartResult> {
    /**
     * OSS客户端
     */
    private StandardOssClient ossClient;
    /**
     * 断点续传对象
     */
    private UploadCheckpoint upLoadCheckPoint;
    /**
     * 分片索引
     */
    private int partNum;

    @Override
    public UploadPartResult call() {
        InputStream inputStream = FileUtil.getInputStream(upLoadCheckPoint.getUploadFile());
        UploadPartResult uploadPartResult = ossClient.uploadPart(upLoadCheckPoint, partNum, inputStream);
        if (!uploadPartResult.isFailed()) {
            upLoadCheckPoint.update(partNum, uploadPartResult.getEntityTag(), true);
            upLoadCheckPoint.dump();
        }
        return uploadPartResult;
    }
}
