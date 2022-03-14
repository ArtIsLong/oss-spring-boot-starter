package io.github.artislong;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author 陈敏
 * @version OssApplicationTests.java, v 1.1 2021/11/5 14:52 chenmin Exp $
 * Created on 2021/11/5
 */
//@SpringBootTest
public class OssApplicationTests {

    @Test
    public void context() {
        mkdir("/Study/test/test1/test2");
    }

    public void mkdir(String path) {
        List<String> paths = StrUtil.split(path, StrUtil.SLASH, true, true);
        StringBuilder fullPath = new StringBuilder();
        for (String p : paths) {
            fullPath.append(StrUtil.SLASH + p);
            System.out.println(p);
        }
        System.out.println(fullPath);

    }
}
