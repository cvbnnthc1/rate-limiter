package ru.phystech.rate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FixedWindowRateLimiterNode {
    final Map<Integer, FixedWindowRateLimiter> limitersForUsers = new ConcurrentHashMap<>();

    public boolean allowFoUser(int userId) {
        return limitersForUsers.get(userId).allow();
    }

    public void addUser(int userId, int maxRequestPerWindow, long windowSizeInMillis) {
        limitersForUsers.put(userId, new FixedWindowRateLimiter(maxRequestPerWindow, windowSizeInMillis));
    }

    public void cleanWindows() {
        limitersForUsers.forEach((key, value) -> value.cleanWindows());
    }

    public void resize(float resizeCoefficient) {
        limitersForUsers.forEach((key, value) -> {
            value.setMaxRequestsPerWindow((int) (value.getMaxRequestsPerWindow() * resizeCoefficient));
        });
    }
}
