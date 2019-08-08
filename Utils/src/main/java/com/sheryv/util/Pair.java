package com.sheryv.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class Pair<K, V> {
    private final K key;
    private final V value;

    public static <K, V> Pair of(K key, V value) {
        return new Pair<>(key, value);
    }
}
