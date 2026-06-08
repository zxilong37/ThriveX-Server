package liuyuyang.net.common.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class WorkReportSchemaInitializer {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `work_report` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`user_id` int NOT NULL," +
                "`type` varchar(20) NOT NULL," +
                "`period` varchar(32) NOT NULL," +
                "`title` varchar(255) DEFAULT NULL," +
                "`summary` text," +
                "`details` text," +
                "`next_plan` text," +
                "`attachment_note` text," +
                "`status` varchar(20) DEFAULT 'draft'," +
                "`draft_version` int DEFAULT 1," +
                "`create_time` varchar(32) DEFAULT NULL," +
                "`update_time` varchar(32) DEFAULT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_work_report_user_type_period` (`user_id`,`type`,`period`)," +
                "KEY `idx_work_report_user_type` (`user_id`,`type`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `work_report_export` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`user_id` int NOT NULL," +
                "`report_id` int DEFAULT NULL," +
                "`type` varchar(20) NOT NULL," +
                "`period` varchar(32) NOT NULL," +
                "`file_name` varchar(255) NOT NULL," +
                "`file_path` text NOT NULL," +
                "`source` varchar(20) DEFAULT 'manual'," +
                "`export_time` varchar(32) DEFAULT NULL," +
                "`create_time` varchar(32) DEFAULT NULL," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_work_report_export_user` (`user_id`,`export_time`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `work_report_schedule` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`user_id` int NOT NULL," +
                "`type` varchar(20) NOT NULL," +
                "`enabled` tinyint(1) DEFAULT 0," +
                "`export_time` varchar(8) DEFAULT NULL," +
                "`weekly_day` int DEFAULT 5," +
                "`monthly_mode` varchar(20) DEFAULT 'last_day'," +
                "`last_export_period` varchar(32) DEFAULT NULL," +
                "`create_time` varchar(32) DEFAULT NULL," +
                "`update_time` varchar(32) DEFAULT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_work_report_schedule_user_type` (`user_id`,`type`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("INSERT INTO `cate` (`name`,`icon`,`url`,`mark`,`level`,`order`,`type`) " +
                "SELECT '工作报表','📋','/reports','work_reports',0,7,'nav' FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM `cate` WHERE `mark` = 'work_reports' OR `url` = '/reports')");
    }
}
