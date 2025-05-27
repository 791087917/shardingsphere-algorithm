package com.peace.shardingsphere.algorithm.sharding.datetime;

import com.peace.shardingsphere.algorithm.sharding.datetime.interval.IntervalShardingConvertor;
import com.peace.shardingsphere.algorithm.sharding.datetime.interval.IntervalShardingConvertorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.locationtech.jts.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zfy
 * @since 2025/5/16
 */
@Slf4j
public class ComplexIntervalShardingAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    // ZK中prop配置的列转换器
    private static final Map<String, IntervalShardingConvertor> COLUMN_CONVERTOR = new HashMap<>();

    @Override
    public void init(Properties props) {
        this.initColumnConvertor(props);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Set<String> shardingTables = getShardingTables(availableTargetNames, shardingValue);
        // 有精确查询返回精确查询命中分表
        if (!shardingTables.isEmpty()) {
            return shardingTables;
        }
        // 有范围查询返回范围查询命中分表
        Set<String> rangeTables = getRangeTables(availableTargetNames, shardingValue);
        if (!rangeTables.isEmpty()) {
            return rangeTables;
        }
        // 默认返回所有分表
        log.warn("Complex interval sharding algorithm return all sub table : {}", shardingValue.getLogicTableName());
        return availableTargetNames;
    }

    /**
     * 处理精确分片，如：in、=
     */
    private Set<String> getShardingTables(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Set<String> tables = new HashSet<>();
        shardingValue.getColumnNameAndShardingValuesMap()
                .forEach((column, values) -> {
                    // 获取分片算法
                    IntervalShardingConvertor convertor = COLUMN_CONVERTOR.get(column);
                    if (convertor != null) {
                        values.forEach(v -> {
                            // 解析出精确条件对应时间LocalDateTime
                            LocalDateTime date = convertor.convert(v);
                            // 时间匹配的物理表
                            Set<String> matchedTable = getMatchedTable(availableTargetNames, date);
                            tables.addAll(matchedTable);
                        });
                    }
                });
        return tables;
    }

    /**
     * 处理区间分片，如：BETWEEN、>、<
     */
    private Set<String> getRangeTables(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Set<String> tables = new HashSet<>();
        shardingValue.getColumnNameAndRangeValuesMap()
                .forEach((column, range) -> {
                    IntervalShardingConvertor convertor = COLUMN_CONVERTOR.get(column);
                    if (convertor != null) {
                        // 当前时间
                        LocalDateTime now = LocalDateTime.now();
                        // SQL条件开始时间，默认2020-01-01
                        LocalDateTime lower = range.hasLowerBound()
                                ? convertor.convert(range.lowerEndpoint())
                                : LocalDateTime.of(2020, 1, 1, 0, 0, 0);
                        // SQL条件结束时间，默认当前时间
                        LocalDateTime upper = range.hasUpperBound()
                                ? convertor.convert(range.upperEndpoint())
                                : now;

                        // 结束时间不超过当前时间，减少命中空表
                        LocalDateTime end = upper.isBefore(now) ? upper : now;
                        // 按步长匹配时间命中物理表，使用 非After兼容开始结束时间相等情况
                        while (!lower.isAfter(end)) {
                            Set<String> matchedTable = getMatchedTable(availableTargetNames, lower);
                            tables.addAll(matchedTable);
                            lower = lower.plusMonths(1);
                        }
                    }
                });
        return tables;
    }

    private Set<String> getMatchedTable(Collection<String> availableTargetNames, LocalDateTime date) {
        String suffix = date.format(DATE_TIME_FORMATTER);
        // 时间对应物理表
        return availableTargetNames.stream().filter(t -> t.endsWith(suffix)).collect(Collectors.toSet());
    }

    /**
     * 1.初始化列转换器
     * 2.REFRESH TABLE METADATA;会重新初始化
     */
    private void initColumnConvertor(final Properties props) {
        //清空原配置信息，重新按配置初始化列转换器。
        COLUMN_CONVERTOR.clear();
        IntervalShardingConvertorFactory.ALL_COLUMN_CONVERTOR.forEach((name, convertor) -> {
            String columns = props.getProperty(name);
            // 排除未配置的转换器
            if (StringUtils.isNotBlank(columns)) {
                for (String column : columns.split(",")) {
                    COLUMN_CONVERTOR.putIfAbsent(column, convertor);
                }
            }
        });
        Assert.isTrue(!COLUMN_CONVERTOR.isEmpty(), "Complex columns convertor null");
        log.info("Complex columns convertor init success {}", COLUMN_CONVERTOR);
    }

    @Override
    public String getType() {
        return "COMPLEX_INTERVAL";
    }

}
