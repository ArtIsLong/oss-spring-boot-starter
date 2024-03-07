package io.github.artislong.core.jdbc.adapter;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.artislong.core.jdbc.model.JdbcOssInfo;
import io.github.artislong.utils.OssPathUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * @author 陈敏
 * @version MySQLJdbcOssOperation.java, v 1.0 2022/5/25 0:38 chenmin Exp $
 * Created on 2022/5/25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbtractJdbcOssOperation implements JdbcOssOperation {

    private JdbcTemplate jdbcTemplate;

    public JdbcOssInfo saveOssInfo(String key, Long size, String parentId, String type, String dateId) {
        String name = StrUtil.equals(key, StrUtil.SLASH) ? key : FileNameUtil.getName(key);
        DateTime now = DateUtil.date();

        JdbcOssInfo jdbcOssInfo = new JdbcOssInfo();
        jdbcOssInfo.setId(IdUtil.fastSimpleUUID());
        jdbcOssInfo.setName(name);
        jdbcOssInfo.setPath(OssPathUtil.replaceKey(key, name, true));
        jdbcOssInfo.setSize(size);
        jdbcOssInfo.setCreateTime(now.toJdkDate());
        jdbcOssInfo.setLastUpdateTime(now.toJdkDate());
        jdbcOssInfo.setParentId(parentId);
        jdbcOssInfo.setType(type);
        jdbcOssInfo.setDataId(dateId);

        jdbcTemplate.update("INSERT INTO OSS_STORE (ID, NAME, PATH, LENGTH, CREATE_TIME, LAST_UPDATE_TIME, PARENT_ID, TYPE, DATA_ID)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, jdbcOssInfo.getId());
            ps.setString(2, jdbcOssInfo.getName());
            ps.setString(3, jdbcOssInfo.getPath());
            ps.setLong(4, jdbcOssInfo.getSize());
            ps.setDate(5, now.toSqlDate());
            ps.setDate(6, now.toSqlDate());
            ps.setString(7, jdbcOssInfo.getParentId());
            ps.setString(8, jdbcOssInfo.getType());
            ps.setString(9, jdbcOssInfo.getDataId());
        });
        return jdbcOssInfo;
    }

    public void deleteOssInfo(String id) {
        jdbcTemplate.update("DELETE FROM OSS_STORE T WHERE T.ID = ?", id);
    }

    public void updateOssInfo(String id, String key, Long size, String parentId) {
        String name = StrUtil.equals(key, StrUtil.SLASH) ? key : FileNameUtil.getName(key);
        DateTime now = DateUtil.date();
        String path = OssPathUtil.replaceKey(key, name, true);
        jdbcTemplate.update("UPDATE OSS_STORE T SET T.NAME = ?, T.PATH = ?, T.LENGTH = ?, T.LAST_UPDATE_TIME = ?, T.PARENT_ID = ? WHERE T.ID = ?", ps -> {
            ps.setString(1, name);
            ps.setString(2, path);
            ps.setLong(3, size);
            ps.setDate(4, now.toSqlDate());
            ps.setString(5, parentId);
            ps.setString(6, id);
        });
    }

    public JdbcOssInfo getOssInfo(String key) {
        String name = StrUtil.equals(key, StrUtil.SLASH) ? key : FileNameUtil.getName(key);
        String path = OssPathUtil.replaceKey(key, name, true);
        List<JdbcOssInfo> jdbcOssInfos = jdbcTemplate.query("SELECT * FROM OSS_STORE T WHERE T.NAME = ? AND T.PATH = ?", BeanPropertyRowMapper.newInstance(JdbcOssInfo.class), name, path);
        if (ObjectUtil.isNotEmpty(jdbcOssInfos) && jdbcOssInfos.size() == 1) {
            return jdbcOssInfos.get(0);
        } else {
            return null;
        }
    }

    public void copyOssInfo(String sourceId, String targetKey, String targetDataId) {
        String targetId = IdUtil.fastSimpleUUID();
        String name = StrUtil.equals(targetKey, StrUtil.SLASH) ? targetKey : FileNameUtil.getName(targetKey);
        String targetPath = OssPathUtil.replaceKey(targetKey, name, true);
        DateTime now = DateUtil.date();
        jdbcTemplate.update("INSERT INTO OSS_STORE (ID, NAME, PATH, LENGTH, CREATE_TIME, LAST_UPDATE_TIME, PARENT_ID, TYPE, DATA_ID) " +
                        "SELECT ?, ?, ?, SIZE, ?, ?, PARENT_ID, TYPE, ? FROM OSS_STORE T WHERE T.ID = ?",
                targetId, name, targetPath, now.toSqlDate(), now.toSqlDate(), targetDataId, sourceId);
    }

    public void deleteOssData(String id) {
        jdbcTemplate.update("DELETE FROM OSS_DATA T WHERE T.ID = ?", id);
    }

    public String copyOssData(String sourceDataId) {
        String targetDataId = IdUtil.fastSimpleUUID();
        jdbcTemplate.update("INSERT INTO OSS_DATA (ID, DATA) SELECT ?, DATA FROM OSS_DATA T WHERE T.ID = ?", targetDataId, sourceDataId);
        return targetDataId;
    }

}
