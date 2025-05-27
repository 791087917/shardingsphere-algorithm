package com.peace.shardingsphere.algorithm.sharding.datetime.interval.convertor;

import com.peace.shardingsphere.algorithm.AlgorithmConst;
import com.peace.shardingsphere.algorithm.sharding.datetime.interval.IntervalShardingConvertor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author zfy
 * @since 2025/5/16
 */
public class SnowflakeIntervalShardingConvertor implements IntervalShardingConvertor {

    /**
     * 雪花ID解析时间
     *
     * @param snowflake snowflake
     * @return date
     */
    @Override
    public LocalDateTime convert(Object snowflake) {
        long timestampPart = Long.parseLong(snowflake + "") >> 22;
        long timestamp = timestampPart + AlgorithmConst.MYBATIS_EPOCH;
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public String getPropertyName() {
        return "snowflake-interval-columns";
    }

    public static void main(String[] args) {
        SnowflakeIntervalShardingConvertor convertor = new SnowflakeIntervalShardingConvertor();
        System.out.println(convertor.convert(1131267535920959489L));

    }

}
