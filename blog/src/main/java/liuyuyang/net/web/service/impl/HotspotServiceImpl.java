package liuyuyang.net.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import liuyuyang.net.common.execption.CustomException;
import liuyuyang.net.common.properties.HotspotProperties;
import liuyuyang.net.common.utils.UrlSecurityUtils;
import liuyuyang.net.dto.hotspot.HotspotRefreshResultDTO;
import liuyuyang.net.dto.hotspot.HotspotSourceDTO;
import liuyuyang.net.dto.hotspot.HotspotSummaryDTO;
import liuyuyang.net.model.Hotspot;
import liuyuyang.net.vo.hotspot.HotspotFilterVo;
import liuyuyang.net.web.mapper.HotspotMapper;
import liuyuyang.net.web.service.HotspotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.context.event.EventListener;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotspotServiceImpl extends ServiceImpl<HotspotMapper, Hotspot> implements HotspotService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_SUMMARY_TOP = 5;
    private static final String[] ARRAY_FIELDS = {"data", "list", "items", "records", "result", "results"};

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    @Resource
    private HotspotMapper hotspotMapper;
    @Resource
    private HotspotProperties hotspotProperties;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public List<HotspotSourceDTO> getSources() {
        return configuredSources().stream()
                .map(this::publicSource)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Hotspot> getHotspots(HotspotFilterVo filterVo) {
        HotspotFilterVo safeFilter = filterVo == null ? new HotspotFilterVo() : filterVo;
        int pageNo = safeFilter.getPage() == null || safeFilter.getPage() <= 0 ? DEFAULT_PAGE : safeFilter.getPage();
        int pageSize = safeFilter.getSize() == null || safeFilter.getSize() <= 0 ? DEFAULT_SIZE : safeFilter.getSize();
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        QueryWrapper<Hotspot> wrapper = new QueryWrapper<>();
        if (!Boolean.TRUE.equals(safeFilter.getIncludeRawJson())) {
            wrapper.select("id", "platform", "platform_name", "title", "url", "cover", "summary",
                    "rank_no", "hot_value", "fetched_at", "created_at", "updated_at");
        }
        if (StringUtils.hasText(safeFilter.getPlatform())) {
            wrapper.eq("platform", safeFilter.getPlatform().trim());
        }
        if (StringUtils.hasText(safeFilter.getKey())) {
            String key = safeFilter.getKey().trim();
            wrapper.and(item -> item.like("title", key).or().like("summary", key));
        }
        wrapper.orderByAsc("platform").orderByAsc("rank_no").orderByDesc("fetched_at");
        return hotspotMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    public HotspotSummaryDTO getSummary(Integer top) {
        int topSize = top == null || top <= 0 ? DEFAULT_SUMMARY_TOP : top;
        QueryWrapper<Hotspot> wrapper = new QueryWrapper<>();
        wrapper.select("id", "platform", "platform_name", "title", "url", "cover", "summary",
                "rank_no", "hot_value", "fetched_at", "created_at", "updated_at");
        wrapper.orderByAsc("platform").orderByAsc("rank_no").orderByDesc("fetched_at");
        List<Hotspot> allItems = hotspotMapper.selectList(wrapper);

        HotspotSummaryDTO summary = new HotspotSummaryDTO();
        summary.setGeneratedAt(nowMillis());
        summary.setTotalItems(allItems.size());
        summary.setLastFetchedAt(allItems.stream()
                .map(Hotspot::getFetchedAt)
                .filter(StringUtils::hasText)
                .max(String::compareTo)
                .orElse(null));

        Map<String, HotspotSummaryDTO.PlatformSummary> platformMap = new LinkedHashMap<>();
        for (Hotspot item : allItems) {
            String platform = item.getPlatform();
            HotspotSummaryDTO.PlatformSummary platformSummary = platformMap.get(platform);
            if (platformSummary == null) {
                platformSummary = new HotspotSummaryDTO.PlatformSummary();
                platformSummary.setPlatform(platform);
                platformSummary.setPlatformName(item.getPlatformName());
                platformMap.put(platform, platformSummary);
            }
            platformSummary.setCount(platformSummary.getCount() + 1);
            String fetchedAt = item.getFetchedAt();
            if (StringUtils.hasText(fetchedAt)
                    && (platformSummary.getLatestFetchedAt() == null || fetchedAt.compareTo(platformSummary.getLatestFetchedAt()) > 0)) {
                platformSummary.setLatestFetchedAt(fetchedAt);
            }
            if (platformSummary.getTopItems().size() < topSize) {
                platformSummary.getTopItems().add(item);
            }
        }
        summary.setTotalPlatforms(platformMap.size());
        summary.setPlatforms(new ArrayList<>(platformMap.values()));
        return summary;
    }

    @Override
    public HotspotRefreshResultDTO refresh() {
        HotspotRefreshResultDTO result = new HotspotRefreshResultDTO();
        result.setStartedAt(nowMillis());
        if (!Boolean.TRUE.equals(hotspotProperties.getEnabled())) {
            result.setFinishedAt(nowMillis());
            return result;
        }
        if (!refreshing.compareAndSet(false, true)) {
            result.setBusy(true);
            result.setFinishedAt(nowMillis());
            return result;
        }
        try {
            List<HotspotSourceDTO> enabledSources = configuredSources().stream()
                    .filter(source -> source != null && (source.getEnabled() == null || Boolean.TRUE.equals(source.getEnabled())))
                    .collect(Collectors.toList());
            result.setTotalSources(enabledSources.size());
            for (HotspotSourceDTO source : enabledSources) {
                HotspotRefreshResultDTO.SourceResult sourceResult = refreshSource(source);
                result.getSources().add(sourceResult);
                if (Boolean.TRUE.equals(sourceResult.getSuccess())) {
                    result.setSuccessSources(result.getSuccessSources() + 1);
                } else {
                    result.setFailedSources(result.getFailedSources() + 1);
                }
                result.setFetchedItems(result.getFetchedItems() + safeInt(sourceResult.getFetchedItems()));
                result.setSavedItems(result.getSavedItems() + safeInt(sourceResult.getSavedItems()));
            }
        } finally {
            result.setFinishedAt(nowMillis());
            refreshing.set(false);
        }
        return result;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void refreshAfterStartup() {
        try {
            HotspotRefreshResultDTO result = refresh();
            log.info("startup_hotspot_refresh successSources={} failedSources={} savedItems={}",
                    result.getSuccessSources(), result.getFailedSources(), result.getSavedItems());
        } catch (Exception ex) {
            log.warn("startup_hotspot_refresh_failed message={}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelay = 1800000)
    public void scheduledRefresh() {
        try {
            HotspotRefreshResultDTO result = refresh();
            log.info("scheduled_hotspot_refresh successSources={} failedSources={} savedItems={}",
                    result.getSuccessSources(), result.getFailedSources(), result.getSavedItems());
        } catch (Exception ex) {
            log.warn("scheduled_hotspot_refresh_failed message={}", ex.getMessage(), ex);
        }
    }

    private HotspotRefreshResultDTO.SourceResult refreshSource(HotspotSourceDTO source) {
        HotspotRefreshResultDTO.SourceResult result = new HotspotRefreshResultDTO.SourceResult();
        if (source == null) {
            result.setMessage("热点源配置为空");
            return result;
        }
        result.setPlatform(source.getPlatform());
        result.setPlatformName(source.getPlatformName());
        try {
            validateSource(source);
            String response = fetch(source.getUrl(), source.getHeaders());
            List<Hotspot> items = parseItems(source, response);
            result.setFetchedItems(items.size());
            int saved = 0;
            for (Hotspot item : items) {
                saved += hotspotMapper.upsert(item);
            }
            result.setSavedItems(saved);
            result.setSuccess(true);
            result.setMessage("ok");
        } catch (Exception ex) {
            result.setSuccess(false);
            result.setMessage(ex.getMessage());
            log.warn("hotspot_source_refresh_failed platform={} url={} message={}",
                    source.getPlatform(), sanitizeUrl(source.getUrl()), ex.getMessage());
        }
        return result;
    }

    private void validateSource(HotspotSourceDTO source) {
        if (!StringUtils.hasText(source.getPlatform())) {
            throw new CustomException(400, "平台标识不能为空");
        }
        if (!StringUtils.hasText(source.getPlatformName())) {
            throw new CustomException(400, "平台名称不能为空");
        }
        if (!StringUtils.hasText(source.getUrl())) {
            throw new CustomException(400, "热点源URL不能为空");
        }
        UrlSecurityUtils.validateHttpUrl("热点源URL", source.getUrl(), Boolean.TRUE.equals(hotspotProperties.getAllowLocalSource()));
    }

    private String fetch(String sourceUrl, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(sourceUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(hotspotProperties.getConnectTimeoutMillis());
        connection.setReadTimeout(hotspotProperties.getReadTimeoutMillis());
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        connection.setRequestProperty("User-Agent", "ThriveX-Hotspot/1.0");
        applyHeaders(connection, headers);
        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (!StringUtils.hasText(location)) {
                throw new CustomException(502, "热点源跳转地址为空：" + status);
            }
            UrlSecurityUtils.validateHttpUrl("热点源跳转URL", location, Boolean.TRUE.equals(hotspotProperties.getAllowLocalSource()));
            return fetch(location, headers);
        }
        if (status < 200 || status >= 300) {
            throw new CustomException(502, "热点源响应异常：" + status);
        }
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } finally {
            connection.disconnect();
        }
    }

    private List<Hotspot> parseItems(HotspotSourceDTO source, String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        ArrayNode itemNodes = findItemArray(root);
        List<Hotspot> items = new ArrayList<>();
        int index = 0;
        int maxItems = hotspotProperties.getMaxItemsPerSource() == null ? 50 : hotspotProperties.getMaxItemsPerSource();
        for (JsonNode itemNode : itemNodes) {
            if (items.size() >= maxItems) {
                break;
            }
            index++;
            Hotspot item = toHotspot(source, itemNode, index);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private Hotspot toHotspot(HotspotSourceDTO source, JsonNode itemNode, int fallbackRank) {
        JsonNode contentNode = preferredContentNode(itemNode);
        String title = firstText(contentNode, "title", "name", "word", "query", "keyword", "sentence", "topic", "desc");
        if (!StringUtils.hasText(title)) {
            title = firstText(itemNode, "title", "name", "word", "query", "keyword", "sentence", "topic", "desc");
        }
        if (!StringUtils.hasText(title)) {
            return null;
        }
        String now = nowMillis();
        String url = firstText(contentNode, "url", "link", "href", "mobileUrl", "shareUrl", "uri");
        if (!StringUtils.hasText(url)) {
            url = firstText(itemNode, "url", "link", "href", "mobileUrl", "shareUrl", "uri");
        }
        if (!StringUtils.hasText(url)) {
            url = buildKnownPlatformUrl(source, contentNode);
        }
        String cover = firstText(contentNode, "cover", "pic", "image", "img", "avatar", "thumbnail", "poster", "coverUrl");
        if (!StringUtils.hasText(cover)) {
            cover = firstText(itemNode, "cover", "pic", "image", "img", "avatar", "thumbnail", "poster", "coverUrl");
        }
        if (!StringUtils.hasText(cover)) {
            cover = firstImageUrl(contentNode);
        }
        String summary = firstText(contentNode, "summary", "desc", "description", "content", "abstract", "excerpt", "intro");
        if (!StringUtils.hasText(summary)) {
            summary = firstText(itemNode, "summary", "desc", "description", "content", "abstract", "excerpt", "intro");
        }
        Hotspot item = new Hotspot();
        item.setPlatform(source.getPlatform().trim());
        item.setPlatformName(source.getPlatformName().trim());
        item.setTitle(limit(title.trim(), 500));
        item.setUrl(limit(url, 1000));
        item.setCover(limit(cover, 1000));
        item.setSummary(summary);
        Integer rankNo = firstInteger(contentNode, null, "rankNo", "rank", "index", "sort", "order", "position", "no");
        item.setRankNo(rankNo == null ? firstInteger(itemNode, fallbackRank, "rankNo", "rank", "index", "sort", "order", "position", "no") : rankNo);
        String hotValue = firstText(contentNode, "hotValue", "hot", "heat", "score", "value", "views", "metrics", "readNum", "viewNum", "likeNum", "likedCount");
        if (!StringUtils.hasText(hotValue)) {
            hotValue = firstText(itemNode, "hotValue", "hot", "heat", "score", "value", "views", "metrics", "readNum", "viewNum", "likeNum", "likedCount");
        }
        item.setHotValue(limit(hotValue, 100));
        item.setFetchedAt(now);
        item.setRawJson(itemNode.toString());
        item.setTitleHash(md5(item.getPlatform() + "|" + item.getTitle().toLowerCase(Locale.ROOT)));
        item.setLinkHash(StringUtils.hasText(item.getUrl()) ? md5(item.getPlatform() + "|" + item.getUrl().trim()) : item.getTitleHash());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    private ArrayNode findItemArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return objectMapper.createArrayNode();
        }
        if (root.isArray()) {
            return (ArrayNode) root;
        }
        for (String field : ARRAY_FIELDS) {
            JsonNode node = root.get(field);
            ArrayNode arrayNode = findArrayInNode(node);
            if (arrayNode.size() > 0) {
                return arrayNode;
            }
        }
        ArrayNode nestedArray = findArrayInNode(root);
        return nestedArray == null ? objectMapper.createArrayNode() : nestedArray;
    }

    private ArrayNode findArrayInNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return objectMapper.createArrayNode();
        }
        if (node.isArray()) {
            return (ArrayNode) node;
        }
        if (!node.isObject()) {
            return objectMapper.createArrayNode();
        }
        ArrayNode firstArray = objectMapper.createArrayNode();
        for (String field : ARRAY_FIELDS) {
            JsonNode child = node.get(field);
            ArrayNode arrayNode = findArrayInNode(child);
            if (isHotspotArray(arrayNode)) {
                return arrayNode;
            }
            if (firstArray.size() == 0 && arrayNode.size() > 0) {
                firstArray = arrayNode;
            }
        }
        java.util.Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            JsonNode child = iterator.next();
            ArrayNode arrayNode = findArrayInNode(child);
            if (isHotspotArray(arrayNode)) {
                return arrayNode;
            }
            if (firstArray.size() == 0 && arrayNode.size() > 0) {
                firstArray = arrayNode;
            }
        }
        return firstArray;
    }

    private boolean isHotspotArray(ArrayNode arrayNode) {
        if (arrayNode == null || arrayNode.size() == 0) {
            return false;
        }
        for (JsonNode item : arrayNode) {
            JsonNode contentNode = preferredContentNode(item);
            if (StringUtils.hasText(firstText(contentNode, "title", "name", "word", "query", "keyword", "sentence", "topic", "desc"))
                    || StringUtils.hasText(firstText(item, "title", "name", "word", "query", "keyword", "sentence", "topic", "desc"))) {
                return true;
            }
        }
        return false;
    }

    private void applyHeaders(HttpURLConnection connection, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry == null || !StringUtils.hasText(entry.getKey()) || !StringUtils.hasText(entry.getValue())) {
                continue;
            }
            connection.setRequestProperty(entry.getKey().trim(), entry.getValue().trim());
        }
    }

    private List<HotspotSourceDTO> configuredSources() {
        List<HotspotSourceDTO> sources = hotspotProperties.getSources();
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources;
    }

    private HotspotSourceDTO publicSource(HotspotSourceDTO source) {
        HotspotSourceDTO publicSource = new HotspotSourceDTO();
        if (source == null) {
            return publicSource;
        }
        publicSource.setPlatform(source.getPlatform());
        publicSource.setPlatformName(source.getPlatformName());
        publicSource.setUrl(sanitizeUrl(source.getUrl()));
        publicSource.setEnabled(source.getEnabled());
        return publicSource;
    }

    private JsonNode preferredContentNode(JsonNode itemNode) {
        if (itemNode == null || itemNode.isNull()) {
            return itemNode;
        }
        for (String field : Arrays.asList("noteInfo", "videoInfo", "awemeInfo", "item", "info", "content", "data")) {
            JsonNode node = findIgnoreCase(itemNode, field);
            if (node != null && node.isObject()) {
                return node;
            }
        }
        return itemNode;
    }

    private String buildKnownPlatformUrl(HotspotSourceDTO source, JsonNode contentNode) {
        if (source == null || contentNode == null) {
            return null;
        }
        String platform = source.getPlatform();
        if ("xiaohongshu".equals(platform)) {
            String noteId = firstText(contentNode, "noteId", "id");
            return StringUtils.hasText(noteId) ? "https://www.xiaohongshu.com/explore/" + noteId.trim() : null;
        }
        return null;
    }

    private String firstImageUrl(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String field : Arrays.asList("noteImages", "images", "imageList", "coverList")) {
            JsonNode images = findIgnoreCase(node, field);
            if (images != null && images.isArray() && images.size() > 0) {
                for (JsonNode image : images) {
                    String imageUrl = firstText(image, "imageUrl", "url", "src", "cover");
                    if (StringUtils.hasText(imageUrl)) {
                        return imageUrl;
                    }
                }
            }
        }
        return null;
    }

    private String sanitizeUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return rawUrl;
        }
        return rawUrl.replaceAll("(?i)([?&](?:token|access_token|apikey|api_key|appkey|key)=)[^&]*", "$1***");
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = findIgnoreCase(node, field);
            String text = nodeToText(value);
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return null;
    }

    private Integer firstInteger(JsonNode node, Integer defaultValue, String... fields) {
        for (String field : fields) {
            JsonNode value = findIgnoreCase(node, field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.asInt();
            }
            String text = nodeToText(value);
            if (StringUtils.hasText(text)) {
                try {
                    return Integer.parseInt(text.replaceAll("[^0-9-]", ""));
                } catch (Exception ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private JsonNode findIgnoreCase(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode direct = node.get(field);
        if (direct != null) {
            return direct;
        }
        for (String name : iterableToList(node.fieldNames())) {
            if (field.equalsIgnoreCase(name)) {
                return node.get(name);
            }
        }
        return null;
    }

    private List<String> iterableToList(java.util.Iterator<String> iterator) {
        List<String> names = new ArrayList<>();
        while (iterator != null && iterator.hasNext()) {
            names.add(iterator.next());
        }
        return names;
    }

    private String nodeToText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                String value = nodeToText(item);
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
            return String.join(",", values);
        }
        if (node.isObject()) {
            for (String field : Arrays.asList("text", "title", "name", "value", "url")) {
                String value = firstText(node, field);
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
            return node.toString();
        }
        return null;
    }

    private String md5(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new CustomException(500, "生成热点哈希失败");
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String nowMillis() {
        return String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
