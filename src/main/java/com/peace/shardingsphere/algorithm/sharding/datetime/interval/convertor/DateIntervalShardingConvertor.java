package com.peace.shardingsphere.algorithm.sharding.datetime.interval.convertor;

import com.peace.shardingsphere.algorithm.sharding.datetime.interval.IntervalShardingConvertor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author zfy
 * @since 2025/5/19
 */
public class DateIntervalShardingConvertor implements IntervalShardingConvertor {

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 解析数据库date类型
     *
     * @param value value
     * @return local date time
     */
    @Override
    public LocalDateTime convert(Object value) {
        String date = value.toString().substring(0, DATE_PATTERN.length());
        return LocalDateTime.of(
                DateTimeFormatter.ofPattern(DATE_PATTERN).parse(date, LocalDate::from),
                LocalTime.MIN
        );
    }

    @Override
    public String getPropertyName() {
        return "date-interval-columns";
    }

    public static void main(String[] args) {
        DateIntervalShardingConvertor convertor = new DateIntervalShardingConvertor();

        System.out.println(convertor.convert("2020-11-01 09:44:32"));

    }

}
