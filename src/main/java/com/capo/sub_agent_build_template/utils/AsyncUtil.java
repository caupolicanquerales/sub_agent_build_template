package com.capo.sub_agent_build_template.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncUtil {

    public static <T> CompletableFuture<T> executeAsync(Supplier<T> supplier, String logPrefix) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                System.err.println(logPrefix + " - " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

}
