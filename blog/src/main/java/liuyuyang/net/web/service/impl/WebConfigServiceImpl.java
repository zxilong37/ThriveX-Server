package liuyuyang.net.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import liuyuyang.net.web.mapper.WebConfigMapper;
import liuyuyang.net.model.WebConfig;
import liuyuyang.net.web.service.WebConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WebConfigServiceImpl extends ServiceImpl<WebConfigMapper, WebConfig> implements WebConfigService {
    private static final String WEB_CONFIG_NAME = "web";
    private static final String SITE_CREATE_TIME_KEY = "create_time";
    private static final Long DEFAULT_SITE_CREATE_TIME = 1547568000000L;

    @Override
    public WebConfig getById(Integer id) {
        WebConfig webConfig = super.getById(id);
        ensureSiteCreateTime(webConfig, true);
        return webConfig;
    }

    @Override
    public List<WebConfig> list() {
        List<WebConfig> configs = super.list();
        configs.forEach(config -> ensureSiteCreateTime(config, true));
        return configs;
    }

    @Override
    public boolean updateJsonValue(Integer id, Map<String, Object> jsonValue) {
        WebConfig webConfig = super.getById(id);
        if (webConfig != null) {
            webConfig.setValue(normalizeConfigValue(webConfig, jsonValue));
            return super.updateById(webConfig);
        }
        return false;
    }

    @Override
    public WebConfig getByName(String name) {
        WebConfig webConfig = this.lambdaQuery().eq(WebConfig::getName, name).one();
        ensureSiteCreateTime(webConfig, true);
        return webConfig;
    }

    private Map<String, Object> normalizeConfigValue(WebConfig webConfig, Map<String, Object> jsonValue) {
        Map<String, Object> nextValue = new LinkedHashMap<>();
        if (jsonValue != null) {
            nextValue.putAll(jsonValue);
        }

        if (!isWebConfig(webConfig)) {
            return nextValue;
        }

        Object incomingCreateTime = nextValue.get(SITE_CREATE_TIME_KEY);
        if (isValidCreateTime(incomingCreateTime)) {
            return nextValue;
        }

        Map<String, Object> currentValue = webConfig.getValue();
        Object currentCreateTime = currentValue == null ? null : currentValue.get(SITE_CREATE_TIME_KEY);
        nextValue.put(SITE_CREATE_TIME_KEY, isValidCreateTime(currentCreateTime) ? currentCreateTime : DEFAULT_SITE_CREATE_TIME);
        return nextValue;
    }

    private void ensureSiteCreateTime(WebConfig webConfig, boolean persist) {
        if (!isWebConfig(webConfig)) {
            return;
        }

        Map<String, Object> value = webConfig.getValue();
        if (value == null) {
            value = new LinkedHashMap<>();
        } else {
            value = new LinkedHashMap<>(value);
        }

        if (isValidCreateTime(value.get(SITE_CREATE_TIME_KEY))) {
            return;
        }

        value.put(SITE_CREATE_TIME_KEY, DEFAULT_SITE_CREATE_TIME);
        webConfig.setValue(value);
        if (persist && webConfig.getId() != null) {
            super.updateById(webConfig);
        }
    }

    private boolean isWebConfig(WebConfig webConfig) {
        return webConfig != null && WEB_CONFIG_NAME.equals(webConfig.getName());
    }

    private boolean isValidCreateTime(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue() > 0;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                return false;
            }
            try {
                return Long.parseLong(text) > 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }
}
