package org.example.transactionapp.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

public class UuidModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private int shardingCount = 2;

    @Override
    public void init(Properties properties) {
        String count = properties.getProperty("sharding-count");
        if (count != null && !count.isEmpty()) {
            this.shardingCount = Integer.parseInt(count);
        }
    }

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Comparable<?>> preciseShardingValue) {
        UUID uuid = UUID.fromString(preciseShardingValue.getValue().toString());
        long hash = uuid.getLeastSignificantBits();
        int shardIndex = (int) Math.abs(hash & shardingCount);
        return "ds" + shardIndex;
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Comparable<?>> rangeShardingValue) {
        return collection;
    }
    @Override
    public String getType(){
        return "UUID_MOD";
    }

}
