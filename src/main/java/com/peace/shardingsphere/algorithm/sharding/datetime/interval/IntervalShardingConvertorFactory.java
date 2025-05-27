package com.peace.shardingsphere.algorithm.sharding.datetime.interval;

import com.peace.shardingsphere.algorithm.sharding.datetime.interval.convertor.DateIntervalShardingConvertor;
import com.peace.shardingsphere.algorithm.sharding.datetime.interval.convertor.DateTimeIntervalShardingConvertor;
import com.peace.shardingsphere.algorithm.sharding.datetime.interval.convertor.SnowflakeIntervalShardingConvertor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zfy
 * @since 2025/5/16
 */
@Slf4j
public class IntervalShardingConvertorFactory {

    // 所有列转换器
    public static final Map<String, IntervalShardingConvertor> ALL_COLUMN_CONVERTOR = new HashMap<>();

    static {
        // 手动注册所有列转换器，无法使用反射（ss-proxy代理中没有反射包）
        SnowflakeIntervalShardingConvertor snowflakeConvertor = new SnowflakeIntervalShardingConvertor();
        DateTimeIntervalShardingConvertor dateTimeConvertor = new DateTimeIntervalShardingConvertor();
        DateIntervalShardingConvertor dateConvertor = new DateIntervalShardingConvertor();

        ALL_COLUMN_CONVERTOR.put(snowflakeConvertor.getPropertyName(), snowflakeConvertor);
        ALL_COLUMN_CONVERTOR.put(dateTimeConvertor.getPropertyName(), dateTimeConvertor);
        ALL_COLUMN_CONVERTOR.put(dateConvertor.getPropertyName(), dateConvertor);

    }

}
