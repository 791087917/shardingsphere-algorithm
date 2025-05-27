//package com.panzhi.tech.common.distdb.sharding;
//
//import com.google.common.base.Preconditions;
//import com.google.common.collect.BoundType;
//import com.google.common.collect.Range;
//import lombok.Getter;
//import lombok.Setter;
//import me.ahoo.cosid.snowflake.MillisecondSnowflakeId;
//import me.ahoo.cosid.snowflake.MillisecondSnowflakeIdStateParser;
//import me.ahoo.cosid.snowflake.SnowflakeIdState;
//import me.ahoo.cosid.snowflake.SnowflakeIdStateParser;
//import org.apache.shardingsphere.infra.config.exception.ShardingSphereConfigurationException;
//import org.apache.shardingsphere.sharding.algorithm.keygen.CosIdSnowflakeKeyGenerateAlgorithm;
//import org.apache.shardingsphere.sharding.api.sharding.ShardingAutoTableAlgorithm;
//import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
//import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
//import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.stream.Collectors;
//
//
///**
// * 基于shardingsphere的分布式数据库中间件的分表分库算法
// *
// * 时间：分表
// * 雪花序列：分库
// *
// * @author marker
// *
// */
//@Getter
//@Setter
//public class TimeHashModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>>, ShardingAutoTableAlgorithm {
//
//
//    private static final String DATE_TIME_PATTERN_KEY = "datetime-pattern";
//
//    private static final String DATE_TIME_LOWER_KEY = "datetime-lower";
//
//    private static final String DATE_TIME_UPPER_KEY = "datetime-upper";
//
//    private static final String SHARDING_SUFFIX_FORMAT_KEY = "sharding-suffix-pattern";
//
//    private static final String INTERVAL_AMOUNT_KEY = "datetime-interval-amount";
//
//    private static final String INTERVAL_UNIT_KEY = "datetime-interval-unit";
//
//    // 判断使用数据源的第一个名称
//    private static final String DATABASE_NAME_PREFIX = "datasourceNameFirst";
//
//    @Getter
//    @Setter
//    private Properties props = new Properties();
//
//    private DateTimeFormatter dateTimeFormatter;
//
//    private int dateTimePatternLength;
//
//    private LocalDateTime dateTimeLower;
//
//    private LocalDateTime dateTimeUpper;
//
//    private DateTimeFormatter tableSuffixPattern;
//
//    private int stepAmount;
//
//    private ChronoUnit stepUnit;
//
//
//    private static final String SHARDING_COUNT_KEY = "sharding-count";
//
//
//    private int shardingCount;
//
//    @Override
//    public void init() {
//        shardingCount = getShardingCount();
//
//        String dateTimePattern = getDateTimePattern();
//        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
//        dateTimePatternLength = dateTimePattern.length();
//        dateTimeLower = getDateTimeLower(dateTimePattern);
//        dateTimeUpper = getDateTimeUpper(dateTimePattern);
//        tableSuffixPattern = getTableSuffixPattern();
//        stepAmount = Integer.parseInt(props.getOrDefault(INTERVAL_AMOUNT_KEY, 1).toString());
//        stepUnit = props.containsKey(INTERVAL_UNIT_KEY) ? getStepUnit(props.getProperty(INTERVAL_UNIT_KEY)) : ChronoUnit.DAYS;
//
//
//        long epoch = CosIdSnowflakeKeyGenerateAlgorithm.DEFAULT_EPOCH;
//
//        snowflakeIdStateParser = new MillisecondSnowflakeIdStateParser(
//                epoch,
//                MillisecondSnowflakeId.DEFAULT_TIMESTAMP_BIT,
//                MillisecondSnowflakeId.DEFAULT_MACHINE_BIT,
//                MillisecondSnowflakeId.DEFAULT_SEQUENCE_BIT,
//                getZoneId()
//        );
//
//    }
//
//    private ZoneId zoneId = ZoneId.systemDefault();
//    SnowflakeIdStateParser snowflakeIdStateParser;
//
//    /**
//     *
//     * @param availableTargetNames 有效的表目标
//     * @param shardingValue 分片的值
//     * @return
//     */
//    @Override
//    public String doSharding(final Collection<String> availableTargetNames, final PreciseShardingValue<Comparable<?>> shardingValue) {
//        // 拆解雪花id
//
//        SnowflakeIdState snowflakeIdState = snowflakeIdStateParser.parse(getLongValue(shardingValue.getValue()));
//        LocalDateTime localDateTime = snowflakeIdState.getTimestamp();
//        String val = dateTimeFormatter.format(localDateTime);
//
////        StringSnowflakeId snowflakeId = new StringSnowflakeId(clockSyncSnowflakeId, Radix62IdConverter.PAD_START);
//
//
//        if(!availableTargetNames.contains(getDatabaseNamePrefix())){
//            return doSharding(availableTargetNames, Range.singleton(val)).stream().findFirst().orElse(null);
//        }
//
//        Long mId = snowflakeIdState.getSequence();
//
//
//        int a= mId.hashCode();
//        String suffix = String.valueOf(a % shardingCount);
//        for (String each : availableTargetNames) {
//            if (each.endsWith(suffix)) {
//                return each;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public Collection<String> doSharding(final Collection<String> availableTargetNames, final RangeShardingValue<Comparable<?>> shardingValue) {
//
//        if(!availableTargetNames.contains(getDatabaseNamePrefix())){ // 使用分表策略
//            return doSharding(availableTargetNames, shardingValue.getValueRange());
//        }
//
//        return isContainAllTargets(shardingValue) ? availableTargetNames : getAvailableTargetNames(availableTargetNames, shardingValue);
//    }
//
//
//
//    private Collection<String> doSharding(final Collection<String> availableTargetNames, final Range<Comparable<?>> range) {
//        Set<String> result = new HashSet<>();
//        LocalDateTime calculateTime = dateTimeLower;
//        while (!calculateTime.isAfter(dateTimeUpper)) {
//            if (hasIntersection(Range.closedOpen(calculateTime, calculateTime.plus(stepAmount, stepUnit)), range)) {
//                result.addAll(getMatchedTables(calculateTime, availableTargetNames));
//            }
//            calculateTime = calculateTime.plus(stepAmount, stepUnit);
//        }
//        return result;
//    }
//
//
//
//
//    private int getShardingCount() {
//        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");
//        return Integer.parseInt(props.get(SHARDING_COUNT_KEY).toString());
//    }
//
//    private boolean isContainAllTargets(final RangeShardingValue<Comparable<?>> shardingValue) {
//        return !shardingValue.getValueRange().hasUpperBound() || shardingValue.getValueRange().hasLowerBound()
//                && getLongValue(shardingValue.getValueRange().upperEndpoint()) - getLongValue(shardingValue.getValueRange().lowerEndpoint()) >= shardingCount - 1;
//    }
//
//    private Collection<String> getAvailableTargetNames(final Collection<String> availableTargetNames, final RangeShardingValue<Comparable<?>> shardingValue) {
//        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
//
//
//
//        for (long i = getLongValue(shardingValue.getValueRange().lowerEndpoint()); i <= getLongValue(shardingValue.getValueRange().upperEndpoint()); i++) {
//            for (String each : availableTargetNames) {
////                SnowflakeIdState snowflakeIdState = snowflakeIdStateParser.parse(getLongValue(each));
////                LocalDateTime localDateTime = snowflakeIdState.getTimestamp();
////                String val = dateTimeFormatter.format(localDateTime);
////
////
////
////
//
//                if (each.endsWith(String.valueOf(i % shardingCount))) {
//                    result.add(each);
//                }
//            }
//        }
//        return result;
//    }
//
//    private long getLongValue(final Comparable<?> value) {
//        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
//    }
//
//    @Override
//    public int getAutoTablesAmount() {
//        return shardingCount;
//    }
//
//    @Override
//    public String getType() {
//        return "INTERVAL_MOD";
//    }
//
//    @Override
//    public Collection<String> getAllPropertyKeys() {
//        return Collections.singletonList(SHARDING_COUNT_KEY);
//    }
//
//
//
//
//
//
//    private String getDateTimePattern() {
//        Preconditions.checkArgument(props.containsKey(DATE_TIME_PATTERN_KEY), "%s can not be null.", DATE_TIME_PATTERN_KEY);
//        return props.getProperty(DATE_TIME_PATTERN_KEY);
//    }
//
//    private LocalDateTime getDateTimeLower(final String dateTimePattern) {
//        Preconditions.checkArgument(props.containsKey(DATE_TIME_LOWER_KEY), "%s can not be null.", DATE_TIME_LOWER_KEY);
//        return getDateTime(DATE_TIME_LOWER_KEY, props.getProperty(DATE_TIME_LOWER_KEY), dateTimePattern);
//    }
//
//    private LocalDateTime getDateTimeUpper(final String dateTimePattern) {
//        return props.containsKey(DATE_TIME_UPPER_KEY) ? getDateTime(DATE_TIME_UPPER_KEY, props.getProperty(DATE_TIME_UPPER_KEY), dateTimePattern) : LocalDateTime.now();
//    }
//
//    private String getDatabaseNamePrefix() {
//        return props.containsKey(DATABASE_NAME_PREFIX) ?  props.getProperty(DATABASE_NAME_PREFIX) : "db_";
//    }
//
//    private LocalDateTime getDateTime(final String dateTimeKey, final String dateTimeValue, final String dateTimePattern) {
//        try {
//            return LocalDateTime.parse(dateTimeValue, dateTimeFormatter);
//        } catch (final DateTimeParseException ex) {
//            throw new ShardingSphereConfigurationException("Invalid %s, datetime pattern should be `%s`, value is `%s`", dateTimeKey, dateTimePattern, dateTimeValue);
//        }
//    }
//
//    private DateTimeFormatter getTableSuffixPattern() {
//        Preconditions.checkArgument(props.containsKey(SHARDING_SUFFIX_FORMAT_KEY), "%s can not be null.", SHARDING_SUFFIX_FORMAT_KEY);
//        return DateTimeFormatter.ofPattern(props.getProperty(SHARDING_SUFFIX_FORMAT_KEY));
//    }
//
//    private ChronoUnit getStepUnit(final String stepUnit) {
//        for (ChronoUnit each : ChronoUnit.values()) {
//            if (each.toString().equalsIgnoreCase(stepUnit)) {
//                return each;
//            }
//        }
//        throw new UnsupportedOperationException(String.format("Cannot find step unit for specified %s property: `%s`", INTERVAL_UNIT_KEY, stepUnit));
//    }
//
//
//
//
//    private boolean hasIntersection(final Range<LocalDateTime> calculateRange, final Range<Comparable<?>> range) {
//        LocalDateTime lower = range.hasLowerBound() ? parseDateTime(range.lowerEndpoint().toString()) : dateTimeLower;
//        LocalDateTime upper = range.hasUpperBound() ? parseDateTime(range.upperEndpoint().toString()) : dateTimeUpper;
//        BoundType lowerBoundType = range.hasLowerBound() ? range.lowerBoundType() : BoundType.CLOSED;
//        BoundType upperBoundType = range.hasUpperBound() ? range.upperBoundType() : BoundType.CLOSED;
//        Range<LocalDateTime> dateTimeRange = Range.range(lower, lowerBoundType, upper, upperBoundType);
//        return calculateRange.isConnected(dateTimeRange) && !calculateRange.intersection(dateTimeRange).isEmpty();
//    }
//
//    private LocalDateTime parseDateTime(final String value) {
//        return LocalDateTime.parse(value.substring(0, dateTimePatternLength), dateTimeFormatter);
//    }
//
//    private Collection<String> getMatchedTables(final LocalDateTime dateTime, final Collection<String> availableTargetNames) {
//        String tableSuffix = dateTime.format(tableSuffixPattern);
//        return availableTargetNames.parallelStream().filter(each -> each.endsWith(tableSuffix)).collect(Collectors.toSet());
//    }
//
//
//}
