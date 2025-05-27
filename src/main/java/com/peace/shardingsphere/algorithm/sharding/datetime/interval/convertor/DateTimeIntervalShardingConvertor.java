package com.peace.shardingsphere.algorithm.sharding.datetime.interval.convertor;

import com.peace.shardingsphere.algorithm.sharding.datetime.interval.IntervalShardingConvertor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author zfy
 * @since 2025/5/19
 */
public class DateTimeIntervalShardingConvertor implements IntervalShardingConvertor {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 解析数据库datetime类型
     *
     * @param value value
     * @return local date time
     */
    @Override
    public LocalDateTime convert(Object value) {
        String date = value.toString().substring(0, DATE_TIME_PATTERN.length());
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    @Override
    public String getPropertyName() {
        return "date-time-interval-columns";
    }

    public static void main(String[] args) {
        DateTimeIntervalShardingConvertor convertor = new DateTimeIntervalShardingConvertor();

        System.out.println(convertor.convert("2020-11-01 00:00:00.212"));

    }

}
