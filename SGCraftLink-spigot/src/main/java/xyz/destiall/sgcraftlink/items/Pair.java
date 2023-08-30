package xyz.destiall.sgcraftlink.items;

import java.util.Map;

public class Pair<K, V> implements Map.Entry<K, V> {
    private final K k;
    private V v;
    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    @Override
    public K getKey() {
        return k;
    }

    @Override
    public V getValue() {
        return v;
    }

    @Override
    public V setValue(V value) {
        V old = this.v;
        this.v = value;
        return old;
    }
}
