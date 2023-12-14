package io.github.artislong.core.jdbc.adapter;

import io.github.artislong.core.jdbc.constant.DbType;
import io.github.artislong.core.jdbc.model.JdbcOssInfo;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * @author 陈敏
 * @version AbstractJdbcOssOperation.java, v 1.0 2022/5/25 0:23 chenmin Exp $
 * Created on 2022/5/25
 */
public interface JdbcOssOperation {

    JdbcOssInfo saveOssInfo(String key, Long size, String parentId, String type, String dateId);

    void deleteOssInfo(String id);

    void updateOssInfo(String id, String key, Long size, String parentId);

    JdbcOssInfo getOssInfo(String key);

    List<JdbcOssInfo> getOssInfos(String path);

    String copyOssInfo(String sourceId, String targetKey, String targetDataId);

    String saveOssData(InputStream inputStream);

    void deleteOssData(String id);

    void updateOssData(String id, InputStream inputStream);

    InputStream getOssData(String id) throws SQLException;

    String copyOssData(String sourceDataId);

    JdbcTemplate getJdbcTemplate();

    void setJdbcTemplate(JdbcTemplate jdbcTemplate);

    DbType getDbType();
}
