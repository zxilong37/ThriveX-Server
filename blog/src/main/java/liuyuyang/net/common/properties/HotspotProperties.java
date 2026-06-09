package liuyuyang.net.common.properties;

import liuyuyang.net.dto.hotspot.HotspotSourceDTO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "hotspot")
public class HotspotProperties {
    private static final String DEFAULT_SOURCE_BASE_URL = "https://api-hot.imsyy.top";

    private Boolean enabled = true;
    private String sourceBaseUrl = DEFAULT_SOURCE_BASE_URL;
    private Boolean allowLocalSource = false;
    private Integer connectTimeoutMillis = 5000;
    private Integer readTimeoutMillis = 10000;
    private Integer maxItemsPerSource = 50;
    private List<HotspotSourceDTO> sources = new ArrayList<>();

    @PostConstruct
    public void initDefaultSources() {
        if (sources != null && !sources.isEmpty()) {
            return;
        }
        String baseUrl = normalizeBaseUrl(sourceBaseUrl);
        sources = new ArrayList<>(Arrays.asList(
                source("zhihu", "知乎", baseUrl + "/zhihu"),
                source("douyin", "抖音", baseUrl + "/douyin"),
                source("weibo", "微博", "https://v2.xxapi.cn/api/weibohot"),
                source("bilibili", "哔哩哔哩", baseUrl + "/bilibili"),
                source("baidu", "百度", baseUrl + "/baidu"),
                source("toutiao", "今日头条", baseUrl + "/toutiao"),
                disabledSource("xiaohongshu", "小红书"),
                disabledSource("tencent_video", "腾讯视频")
        ));
    }

    private static HotspotSourceDTO source(String platform, String platformName, String url) {
        HotspotSourceDTO source = new HotspotSourceDTO();
        source.setPlatform(platform);
        source.setPlatformName(platformName);
        source.setUrl(url);
        source.setEnabled(true);
        return source;
    }

    private static HotspotSourceDTO disabledSource(String platform, String platformName) {
        HotspotSourceDTO source = new HotspotSourceDTO();
        source.setPlatform(platform);
        source.setPlatformName(platformName);
        source.setEnabled(false);
        return source;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String safeBaseUrl = baseUrl == null || baseUrl.trim().isEmpty() ? DEFAULT_SOURCE_BASE_URL : baseUrl.trim();
        while (safeBaseUrl.endsWith("/")) {
            safeBaseUrl = safeBaseUrl.substring(0, safeBaseUrl.length() - 1);
        }
        return safeBaseUrl;
    }
}
