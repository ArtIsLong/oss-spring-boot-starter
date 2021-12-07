package io.github.artislong;

import cn.hutool.system.SystemUtil;
import cn.hutool.system.UserInfo;
import org.junit.jupiter.api.Test;

/**
 * @author 陈敏
 * @version OssApplicationTests.java, v 1.1 2021/11/5 14:52 chenmin Exp $
 * Created on 2021/11/5
 */
//@SpringBootTest
public class OssApplicationTests {

    @Test
    public void context() {
        UserInfo userInfo = SystemUtil.getUserInfo();
        System.out.println(userInfo);
    }
}
