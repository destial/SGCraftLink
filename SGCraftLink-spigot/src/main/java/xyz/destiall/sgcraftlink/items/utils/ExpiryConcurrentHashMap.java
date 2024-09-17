package xyz.destiall.sgcraftlink.items.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExpiryConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
    private final long timeDifferenceMs;
    private final ConcurrentHashMap<Long, K> keyTimeMap;

    public ExpiryConcurrentHashMap(long timeDifferenceMs) {
        this.timeDifferenceMs = timeDifferenceMs;
        keyTimeMap = new ConcurrentHashMap<>();
    }

    @Override
    public V put(@NotNull K key, @NotNull V value) {
        long now = System.currentTimeMillis();
        Collection<Long> expired = keyTimeMap.keySet().stream().filter(l -> l + timeDifferenceMs <= now).collect(Collectors.toList());
        for (Long time : expired) {
            K k = keyTimeMap.remove(time);
            remove(k);
        }

        keyTimeMap.put(now, key);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        long now = System.currentTimeMillis();
        Collection<Long> expired = keyTimeMap.keySet().stream().filter(l -> l + timeDifferenceMs <= now).collect(Collectors.toList());
        for (Long time : expired) {
            K k = keyTimeMap.remove(time);
            remove(k);
        }

        for (K ks : m.keySet()) {
            keyTimeMap.put(now, ks);
        }
        super.putAll(m);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        long now = System.currentTimeMillis();
        Collection<Long> expired = keyTimeMap.keySet().stream().filter(l -> l + timeDifferenceMs <= now).collect(Collectors.toList());
        for (Long time : expired) {
            K k = keyTimeMap.remove(time);
            remove(k);
        }

        V v = super.putIfAbsent(key, value);
        if (v != null)
            keyTimeMap.put(now, key);
        return v;
    }
}
