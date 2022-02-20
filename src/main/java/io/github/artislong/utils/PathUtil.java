package io.github.artislong.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 陈敏
 * @version PathUtil.java, v 1.1 2022/2/18 17:01 chenmin Exp $
 * Created on 2022/2/18
 */
@Slf4j
public class PathUtil {

    public static String valid(String basePath) {
        // 根路径未配置时，默认路径为 /
        if (ObjectUtil.isEmpty(basePath)) {
            basePath = StrUtil.SLASH;
        }
        // 将路径分隔符统一转为 /
        basePath = basePath.replaceAll("\\\\", StrUtil.SLASH).replaceAll("//", StrUtil.SLASH);

        // 将配置默认转为绝对路径
        if (!basePath.startsWith(StrUtil.SLASH)) {
            basePath = StrUtil.SLASH + basePath;
        }
        if (!basePath.endsWith(StrUtil.SLASH)) {
            basePath = basePath + StrUtil.SLASH;
        }
        return basePath;
    }
}
