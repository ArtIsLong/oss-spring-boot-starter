package io.github.artislong.model.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.json.JSONUtil;
import io.github.artislong.exception.OssException;
import lombok.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 断点对象
 * @author 陈敏
 * @version UploadCheckPoint.java, v 1.1 2022/2/9 22:52 chenmin Exp $
 * Created on 2022/2/9
 */
@Data
public class UploadCheckpoint implements Serializable {

    private static final long serialVersionUID = 5424904565837227164L;

    public static final String UPLOAD_MAGIC = "FE8BB4EA-B593-4FAC-AD7A-2459A36E2E62";

    private String magic;
    private int md5;
    private String uploadFile;
    private UploadFileStat uploadFileStat;
    private String key;
    private String bucket;
    private String checkpointFile;
    private String uploadId;
    private List<UploadPart> uploadParts = Collections.synchronizedList(new ArrayList<>());
    private List<UploadPartEntityTag> partEntityTags = Collections.synchronizedList(new ArrayList<>());
    private long originPartSize;

    /**
     * 从缓存文件中加载断点数据
     * @param checkpointFile 断点缓存文件
     */
    public synchronized void load(String checkpointFile) {
        try {
            // TODO 缓存数据进行压缩
            UploadCheckpoint ucp = JSONUtil.readJSONObject(new File(checkpointFile), CharsetUtil.CHARSET_UTF_8).toBean(this.getClass());
            assign(ucp);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    /**
     * 将断点信息写入到断点缓存文件
     */
    public synchronized void dump() {
        this.setMd5(hashCode());
        try {
            FileUtil.writeUtf8String(JSONUtil.toJsonStr(this), checkpointFile);
        } catch (Exception e) {
            throw new OssException(e);
        }
    }

    /**
     * 更新分块状态
     * @param partIndex 分片索引
     * @param partEntityTag 分片Tag
     * @param completed 分片是否完成
     */
    public synchronized void update(int partIndex, UploadPartEntityTag partEntityTag, boolean completed) {
        this.getPartEntityTags().add(partEntityTag);
        this.getUploadParts().get(partIndex).setCompleted(completed);
    }

    /**
     * 检查断点缓存文件是否与断点一致
     * @return 校验是否通过
     */
    public synchronized boolean isValid() {
        // 比较checkpoint的magic和md5
        if (this.getMagic() == null || !this.getMagic().equals(UPLOAD_MAGIC) || this.getMd5() != hashCode()) {
            return false;
        }
        // 检查断点缓存文件是否存在
        if (!FileUtil.exist(checkpointFile)) {
            return false;
        }

        File file = new File(uploadFile);
        // 文件名，大小和上次修改时间必须与当前断点相同。
        // 如果有任何改变，则重新上传
        return this.getUploadFileStat().getSize() == file.length()
                && this.getUploadFileStat().getLastModified() == file.lastModified();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
        result = prime * result + ((checkpointFile == null) ? 0 : checkpointFile.hashCode());
        result = prime * result + ((magic == null) ? 0 : magic.hashCode());
        result = prime * result + ((partEntityTags == null) ? 0 : partEntityTags.hashCode());
        result = prime * result + ((uploadFile == null) ? 0 : uploadFile.hashCode());
        result = prime * result + ((uploadFileStat == null) ? 0 : uploadFileStat.hashCode());
        result = prime * result + ((uploadId == null) ? 0 : uploadId.hashCode());
        result = prime * result + ((uploadParts == null) ? 0 : uploadParts.hashCode());
        result = prime * result + (int) originPartSize;
        return result;
    }

    public void assign(UploadCheckpoint ucp) {
        this.setMagic(ucp.magic);
        this.setMd5(ucp.md5);
        this.setUploadFile(ucp.uploadFile);
        this.setUploadFileStat(ucp.uploadFileStat);
        this.setKey(ucp.key);
        this.setBucket(ucp.bucket);
        this.setCheckpointFile(ucp.checkpointFile);
        this.setUploadId(ucp.uploadId);
        this.setUploadParts(ucp.uploadParts);
        this.setPartEntityTags(ucp.partEntityTags);
        this.setOriginPartSize(ucp.originPartSize);
    }

}
