package liuyuyang.net.common.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class HotspotSchemaInitializer {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `hotspot` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`platform` varchar(64) NOT NULL COMMENT '平台标识'," +
                "`platform_name` varchar(64) NOT NULL COMMENT '平台名称'," +
                "`title` varchar(500) NOT NULL COMMENT '热点标题'," +
                "`url` varchar(1000) DEFAULT NULL COMMENT '热点链接'," +
                "`cover` varchar(1000) DEFAULT NULL COMMENT '封面图'," +
                "`summary` text COMMENT '摘要'," +
                "`rank_no` int DEFAULT NULL COMMENT '榜单排名'," +
                "`hot_value` varchar(100) DEFAULT NULL COMMENT '热度值'," +
                "`fetched_at` varchar(32) NOT NULL COMMENT '抓取时间戳'," +
                "`raw_json` json DEFAULT NULL COMMENT '原始JSON'," +
                "`title_hash` char(32) NOT NULL COMMENT '标题MD5'," +
                "`link_hash` char(32) DEFAULT NULL COMMENT '链接MD5'," +
                "`created_at` varchar(32) DEFAULT NULL COMMENT '创建时间戳'," +
                "`updated_at` varchar(32) DEFAULT NULL COMMENT '更新时间戳'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_hotspot_platform_title` (`platform`,`title_hash`)," +
                "UNIQUE KEY `uk_hotspot_platform_link` (`platform`,`link_hash`)," +
                "KEY `idx_hotspot_platform_rank` (`platform`,`rank_no`)," +
                "KEY `idx_hotspot_fetched_at` (`fetched_at`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时热点'");

        jdbcTemplate.execute("INSERT INTO `cate` (`name`,`icon`,`url`,`mark`,`level`,`order`,`type`) " +
                "SELECT '实时热点','⚡','/hotspot','hotspot',0,6,'nav' FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM `cate` WHERE `mark` = 'hotspot' OR `url` = '/hotspot')");
    }
}
