package liuyuyang.net.common.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class AgentSchemaInitializer {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `knowledge_source` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`name` varchar(255) NOT NULL COMMENT '知识来源名称'," +
                "`source_type` varchar(50) NOT NULL COMMENT '知识来源类型'," +
                "`source_path` varchar(500) NOT NULL COMMENT '知识来源路径'," +
                "`description` varchar(500) DEFAULT NULL COMMENT '知识来源说明'," +
                "`enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用'," +
                "`chunk_count` int NOT NULL DEFAULT '0' COMMENT '分块数量'," +
                "`index_status` varchar(30) NOT NULL DEFAULT 'pending' COMMENT '索引状态'," +
                "`last_indexed_time` varchar(32) DEFAULT NULL COMMENT '最后索引时间'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "`update_time` varchar(32) DEFAULT NULL COMMENT '更新时间'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_knowledge_source_path` (`source_path`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库来源'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `knowledge_chunk` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`source_id` int NOT NULL COMMENT '知识来源 ID'," +
                "`title` varchar(255) DEFAULT NULL COMMENT '分块标题'," +
                "`content` longtext NOT NULL COMMENT '分块正文'," +
                "`source_path` varchar(500) NOT NULL COMMENT '知识来源路径'," +
                "`source_type` varchar(50) NOT NULL COMMENT '知识来源类型'," +
                "`tags` varchar(255) DEFAULT NULL COMMENT '标签'," +
                "`chunk_order` int NOT NULL DEFAULT '0' COMMENT '分块顺序'," +
                "`content_hash` char(32) DEFAULT NULL COMMENT '内容摘要 Hash'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_knowledge_chunk_source` (`source_id`)," +
                "KEY `idx_knowledge_chunk_hash` (`content_hash`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分块'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `agent_session` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`user_id` int NOT NULL COMMENT '用户 ID'," +
                "`title` varchar(255) NOT NULL COMMENT '会话标题'," +
                "`mode` varchar(50) DEFAULT NULL COMMENT '会话模式'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "`update_time` varchar(32) DEFAULT NULL COMMENT '更新时间'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_agent_session_user` (`user_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体会话'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `agent_message` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`session_id` int NOT NULL COMMENT '会话 ID'," +
                "`user_id` int NOT NULL COMMENT '用户 ID'," +
                "`message_role` varchar(30) NOT NULL COMMENT '消息角色'," +
                "`intent` varchar(50) DEFAULT NULL COMMENT '识别意图'," +
                "`content` longtext NOT NULL COMMENT '消息内容'," +
                "`citations` longtext COMMENT '引用来源 JSON'," +
                "`result_id` int DEFAULT NULL COMMENT '关联结果 ID'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_agent_message_session` (`session_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体消息'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `document_task` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`user_id` int NOT NULL COMMENT '用户 ID'," +
                "`session_id` int DEFAULT NULL COMMENT '会话 ID'," +
                "`title` varchar(255) NOT NULL COMMENT '文档标题'," +
                "`doc_type` varchar(50) NOT NULL COMMENT '文档类型'," +
                "`status` varchar(30) NOT NULL DEFAULT 'draft' COMMENT '任务状态'," +
                "`prompt` longtext NOT NULL COMMENT '用户提示词'," +
                "`outline` longtext COMMENT '文档大纲'," +
                "`citations` longtext COMMENT '引用来源 JSON'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "`update_time` varchar(32) DEFAULT NULL COMMENT '更新时间'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_document_task_user` (`user_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档生成任务'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `document_result` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`task_id` int NOT NULL COMMENT '任务 ID'," +
                "`user_id` int NOT NULL COMMENT '用户 ID'," +
                "`title` varchar(255) NOT NULL COMMENT '文档标题'," +
                "`doc_type` varchar(50) NOT NULL COMMENT '文档类型'," +
                "`format` varchar(30) NOT NULL DEFAULT 'markdown' COMMENT '输出格式'," +
                "`content` longtext NOT NULL COMMENT '文档正文'," +
                "`citations` longtext COMMENT '引用来源 JSON'," +
                "`review_score` int DEFAULT NULL COMMENT '审校分数'," +
                "`review_passed` tinyint DEFAULT '0' COMMENT '审校是否通过'," +
                "`review_issues` longtext COMMENT '审校问题 JSON'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "`update_time` varchar(32) DEFAULT NULL COMMENT '更新时间'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_document_result_user` (`user_id`)," +
                "KEY `idx_document_result_task` (`task_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档生成结果'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `agent_tool_log` (" +
                "`id` int NOT NULL AUTO_INCREMENT," +
                "`user_id` int DEFAULT NULL COMMENT '用户 ID'," +
                "`task_id` int DEFAULT NULL COMMENT '任务 ID'," +
                "`tool_name` varchar(100) NOT NULL COMMENT '工具名称'," +
                "`params_summary` varchar(500) DEFAULT NULL COMMENT '参数摘要'," +
                "`status` varchar(30) NOT NULL COMMENT '执行状态'," +
                "`error_message` varchar(500) DEFAULT NULL COMMENT '错误信息'," +
                "`create_time` varchar(32) DEFAULT NULL COMMENT '创建时间'," +
                "PRIMARY KEY (`id`)," +
                "KEY `idx_agent_tool_log_task` (`task_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体工具日志'");
    }
}
