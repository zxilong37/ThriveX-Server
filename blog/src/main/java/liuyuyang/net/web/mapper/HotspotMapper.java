package liuyuyang.net.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import liuyuyang.net.model.Hotspot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotspotMapper extends BaseMapper<Hotspot> {
    @Insert("INSERT INTO `hotspot` (" +
            "`platform`,`platform_name`,`title`,`url`,`cover`,`summary`,`rank_no`,`hot_value`,`fetched_at`,`raw_json`,`title_hash`,`link_hash`,`created_at`,`updated_at`" +
            ") VALUES (" +
            "#{platform},#{platformName},#{title},#{url},#{cover},#{summary},#{rankNo},#{hotValue},#{fetchedAt},#{rawJson},#{titleHash},#{linkHash},#{createdAt},#{updatedAt}" +
            ") ON DUPLICATE KEY UPDATE " +
            "`platform_name`=VALUES(`platform_name`)," +
            "`title`=VALUES(`title`)," +
            "`url`=VALUES(`url`)," +
            "`cover`=VALUES(`cover`)," +
            "`summary`=VALUES(`summary`)," +
            "`rank_no`=VALUES(`rank_no`)," +
            "`hot_value`=VALUES(`hot_value`)," +
            "`fetched_at`=VALUES(`fetched_at`)," +
            "`raw_json`=VALUES(`raw_json`)," +
            "`title_hash`=VALUES(`title_hash`)," +
            "`link_hash`=VALUES(`link_hash`)," +
            "`updated_at`=VALUES(`updated_at`)")
    int upsert(Hotspot hotspot);
}
