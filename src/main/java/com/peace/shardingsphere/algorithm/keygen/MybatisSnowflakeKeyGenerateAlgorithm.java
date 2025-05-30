package com.peace.shardingsphere.algorithm.keygen;

import com.peace.shardingsphere.algorithm.AlgorithmConst;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.shardingsphere.infra.instance.InstanceContext;
import org.apache.shardingsphere.infra.instance.InstanceContextAware;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.algorithm.keygen.TimeService;
import org.apache.shardingsphere.sharding.exception.algorithm.keygen.KeyGenerateAlgorithmInitializationException;
import org.apache.shardingsphere.sharding.exception.algorithm.keygen.SnowflakeClockMoveBackException;
import org.apache.shardingsphere.sharding.spi.KeyGenerateAlgorithm;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zfy
 * @since 2025/5/21
 */
public class MybatisSnowflakeKeyGenerateAlgorithm implements KeyGenerateAlgorithm, InstanceContextAware {

    public static final long EPOCH = AlgorithmConst.MYBATIS_EPOCH;

    private static final String MAX_VIBRATION_OFFSET_KEY = "max-vibration-offset";

    private static final String MAX_TOLERATE_TIME_DIFFERENCE_MILLIS_KEY = "max-tolerate-time-difference-milliseconds";

    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_BITS = 10L;

    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1L;

    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;

    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;

    private static final int DEFAULT_VIBRATION_VALUE = 1;

    private static final int MAX_TOLERATE_TIME_DIFFERENCE_MILLIS = 10;

    private static final int DEFAULT_WORKER_ID = 0;

    @Setter
    private static TimeService timeService = new TimeService();

    private final AtomicReference<InstanceContext> instanceContext = new AtomicReference<>();

    private final AtomicInteger sequenceOffset = new AtomicInteger(-1);

    private final AtomicLong sequence = new AtomicLong();

    private final AtomicLong lastMillis = new AtomicLong();

    private Properties props;

    private int maxVibrationOffset;

    private int maxTolerateTimeDifferenceMillis;

    @Override
    public void init(final Properties props) {
        this.props = props;
        maxVibrationOffset = getMaxVibrationOffset(props);
        maxTolerateTimeDifferenceMillis = getMaxTolerateTimeDifferenceMillis(props);
    }

    private int getMaxVibrationOffset(final Properties props) {
        int result = Integer.parseInt(props.getOrDefault(MAX_VIBRATION_OFFSET_KEY, DEFAULT_VIBRATION_VALUE).toString());
        ShardingSpherePreconditions.checkState(result >= 0 && result <= SEQUENCE_MASK, () -> new KeyGenerateAlgorithmInitializationException(getType(), "Illegal max vibration offset."));
        return result;
    }

    private int getMaxTolerateTimeDifferenceMillis(final Properties props) {
        int result = Integer.parseInt(props.getOrDefault(MAX_TOLERATE_TIME_DIFFERENCE_MILLIS_KEY, MAX_TOLERATE_TIME_DIFFERENCE_MILLIS).toString());
        ShardingSpherePreconditions.checkState(result >= 0, () -> new KeyGenerateAlgorithmInitializationException(getType(), "Illegal max tolerate time difference milliseconds."));
        return result;
    }

    @Override
    public void setInstanceContext(final InstanceContext instanceContext) {
        this.instanceContext.set(instanceContext);
        if (null != instanceContext) {
            instanceContext.generateWorkerId(props);
        }
    }

    @Override
    public synchronized Long generateKey() {
        long currentMillis = timeService.getCurrentMillis();
        if (waitTolerateTimeDifferenceIfNeed(currentMillis)) {
            currentMillis = timeService.getCurrentMillis();
        }
        if (lastMillis.get() == currentMillis) {
            sequence.set(sequence.incrementAndGet() & SEQUENCE_MASK);
            if (0L == sequence.get()) {
                currentMillis = waitUntilNextTime(currentMillis);
            }
        } else {
            vibrateSequenceOffset();
            sequence.set(sequenceOffset.get());
        }
        lastMillis.set(currentMillis);
        return ((currentMillis - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | ((long) getWorkerId() << WORKER_ID_LEFT_SHIFT_BITS) | sequence.get();
    }

    @SneakyThrows(InterruptedException.class)
    private boolean waitTolerateTimeDifferenceIfNeed(final long currentMillis) {
        if (lastMillis.get() <= currentMillis) {
            return false;
        }
        long timeDifferenceMillis = lastMillis.get() - currentMillis;
        ShardingSpherePreconditions.checkState(timeDifferenceMillis < maxTolerateTimeDifferenceMillis, () -> new SnowflakeClockMoveBackException(lastMillis.get(), currentMillis));
        Thread.sleep(timeDifferenceMillis);
        return true;
    }

    private long waitUntilNextTime(final long lastTime) {
        long result = timeService.getCurrentMillis();
        while (result <= lastTime) {
            result = timeService.getCurrentMillis();
        }
        return result;
    }

    private void vibrateSequenceOffset() {
        if (!sequenceOffset.compareAndSet(maxVibrationOffset, 0)) {
            sequenceOffset.incrementAndGet();
        }
    }

    private int getWorkerId() {
        return null == instanceContext.get() ? DEFAULT_WORKER_ID : instanceContext.get().getWorkerId();
    }

    @Override
    public String getType() {
        return "MYBATIS_SNOWFLAKE";
    }

}
