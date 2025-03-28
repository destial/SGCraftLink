package xyz.destiall.sgcraftlink.utils;

import java.util.HashSet;

public class ClassSet<T> extends HashSet<T> {
    @Override
    public boolean add(T t) {
        if (stream().anyMatch(c -> c.getClass().equals(t.getClass())))
            return false;
        return super.add(t);
    }
}
