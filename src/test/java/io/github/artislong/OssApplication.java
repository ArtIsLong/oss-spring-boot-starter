package io.github.artislong;

import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author 陈敏
 * @version OssApplication.java, v 1.1 2021/11/5 14:51 chenmin Exp $
 * Created on 2021/11/5
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class OssApplication {

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    public static void main(String[] args) {
        SpringApplication.run(OssApplication.class);
    }

}
