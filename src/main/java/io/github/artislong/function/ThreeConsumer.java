package io.github.artislong.function;

/**
 * @author 陈敏
 * @version TriConsumer.java, v 1.0 2023/12/5 0:45 chenmin Exp $
 * Created on 2023/12/5
 */
@FunctionalInterface
public interface ThreeConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
