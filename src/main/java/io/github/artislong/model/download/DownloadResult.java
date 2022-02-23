package io.github.artislong.model.download;

import lombok.Data;

import java.util.List;

/**
 * @author 陈敏
 * @version DownloadResult.java, v 1.1 2022/2/21 15:17 chenmin Exp $
 * Created on 2022/2/21
 */
@Data
public class DownloadResult {

    private List<DownloadPartResult> partResults;

}
