package com.peace.shardingsphere.algorithm;

/**
 * @author zfy
 * @since 2025/5/21
 */
public class AlgorithmConst {

    /*
        雪花ID起始时间，Mybatis-plus与ShardingSphere默认值不同
        Mybatis-plus: 1288834974657L
        ShardingSphere: SnowflakeKeyGenerateAlgorithm.EPOCH（不支持配置）
     */
    public static final long MYBATIS_EPOCH = 1288834974657L;

}
