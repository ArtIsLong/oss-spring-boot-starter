package io.github.artislong.core.jdbc.adapter;

import cn.hutool.core.util.IdUtil;
import io.github.artislong.core.jdbc.constant.DbType;
import io.github.artislong.core.jdbc.model.JdbcOssInfo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

/**
 * @author 陈敏
 * @version OracleJdbcOssOperation.java, v 1.0 2022/5/25 0:38 chenmin Exp $
 * Created on 2022/5/25
 */
@NoArgsConstructor
@AllArgsConstructor
public class PostgreSQLJdbcOssOperation extends AbtractJdbcOssOperation implements JdbcOssOperation {

    public List<JdbcOssInfo> getOssInfos(String path) {
        return getJdbcTemplate().query("SELECT * FROM OSS_STORE T WHERE T.PATH LIKE concat('', ?, '%')", BeanPropertyRowMapper.newInstance(JdbcOssInfo.class), path);
    }

    public String saveOssData(InputStream inputStream) {
        String dataId = IdUtil.fastSimpleUUID();
        getJdbcTemplate().update("INSERT INTO OSS_DATA(ID, DATA) VALUES(?, ?)", ps -> {
            ps.setString(1, dataId);
            ps.setBinaryStream(2, inputStream);
        });
        return dataId;
    }

    public void updateOssData(String id, InputStream inputStream) {
        getJdbcTemplate().update("UPDATE OSS_DATA T SET T.DATA = ? WHERE T.ID = ?", ps -> {
            ps.setBinaryStream(1, inputStream);
            ps.setString(2, id);
        });
    }

    public InputStream getOssData(String id) throws SQLException {
        return getJdbcTemplate().queryForObject("SELECT DATA FROM OSS_DATA T WHERE T.ID = ?", Blob.class, id).getBinaryStream();
    }

    @Override
    public DbType getDbType() {
        return DbType.POSTGRESQL;
    }
}
