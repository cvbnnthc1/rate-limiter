package ru.phystech.rate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter implements RateLimiter{
    private long windowSizeInMillis;
    volatile private int maxRequestsPerWindow;
    final ConcurrentMap<Long, AtomicInteger> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequestsPerWindow, long windowSizeInMillis) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSizeInMillis = windowSizeInMillis;
    }

    @Override
    public boolean allow() {
        long windowKey = System.currentTimeMillis() / windowSizeInMillis;
        windows.putIfAbsent(windowKey, new AtomicInteger(0));
        return windows.get(windowKey).incrementAndGet() <= maxRequestsPerWindow
                && windowKey == System.currentTimeMillis() / windowSizeInMillis;
    }


    public void cleanWindows() {
        long curWindowKey = System.currentTimeMillis() / windowSizeInMillis;
        windows.keySet().forEach(key -> {
            if (key < curWindowKey) windows.remove(key);
        });
    }

    public void setMaxRequestsPerWindow(int maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    public int getMaxRequestsPerWindow() {
        return maxRequestsPerWindow;
    }
}
