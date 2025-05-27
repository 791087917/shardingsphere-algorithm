package com.peace.shardingsphere.algorithm.sharding.datetime.interval;

import java.time.LocalDateTime;

/**
 * @author zfy
 * @since 2025/5/16
 */
public interface IntervalShardingConvertor {

    /**
     * SQL中的分片值转换成时间
     *
     * @param value value
     * @return local date time
     */
    LocalDateTime convert(Object value);

    /**
     * 转换器prop配置名称
     */
    String getPropertyName();

}
