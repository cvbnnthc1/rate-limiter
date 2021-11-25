package ru.phystech.rate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter implements RateLimiter{
    private final long windowSizeInMillis;
    private final int maxRequestPerSec;
    final ConcurrentMap<Long, AtomicInteger> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequestPerSec, long windowSizeInMillis) {
        this.maxRequestPerSec = maxRequestPerSec;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    @Override
    public boolean allow() {
        long windowKey = System.currentTimeMillis() / windowSizeInMillis;
        windows.putIfAbsent(windowKey, new AtomicInteger(0));
        return windows.get(windowKey).incrementAndGet() <= maxRequestPerSec
                && windowKey == System.currentTimeMillis() / windowSizeInMillis;
    }


    public void cleanWindows() {
        long curWindowKey = System.currentTimeMillis() / windowSizeInMillis;
        for (Long windowKey: windows.keySet()) {
            if (windowKey < curWindowKey) {
                windows.remove(windowKey);
            }
        }
    }
}
