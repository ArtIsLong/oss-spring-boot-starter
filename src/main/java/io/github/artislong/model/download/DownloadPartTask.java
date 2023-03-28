package io.github.artislong.model.download;

/**
 * @author 陈敏
 * @version DownloadPartTask.java, v 1.0 2022/10/12 23:41 chenmin Exp $
 * Created on 2022/10/12
 */

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import io.github.artislong.core.StandardOssClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

/**
 * 分片下载Task
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadPartTask implements Callable<DownloadPartResult> {

    /**
     * Oss客户端
     */
    StandardOssClient ossClient;
    /**
     * 断点续传对象
     */
    DownloadCheckPoint downloadCheckPoint;
    /**
     * 分片索引
     */
    int partNum;

    @Override
    public DownloadPartResult call() {
        DownloadPartResult partResult = null;
        RandomAccessFile output = null;
        InputStream content = null;
        try {
            DownloadPart downloadPart = downloadCheckPoint.getDownloadParts().get(partNum);

            partResult = new DownloadPartResult(partNum + 1, downloadPart.getStart(), downloadPart.getEnd());

            output = new RandomAccessFile(downloadCheckPoint.getTempDownloadFile(), "rw");
            output.seek(downloadPart.getFileStart());

            content = ossClient.downloadPart(downloadCheckPoint.getKey(), downloadPart.getStart(), downloadPart.getEnd());

            long partSize = downloadPart.getEnd() - downloadPart.getStart();
            byte[] buffer = new byte[Convert.toInt(partSize)];
            int bytesRead = 0;
            while ((bytesRead = content.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            partResult.setLength(downloadPart.getLength());
            downloadCheckPoint.update(partNum, true);
            downloadCheckPoint.dump();
        } catch (Exception e) {
            partResult.setException(e);
            partResult.setFailed(true);
        } finally {
            IoUtil.close(output);
            IoUtil.close(content);
        }
        return partResult;
    }
}