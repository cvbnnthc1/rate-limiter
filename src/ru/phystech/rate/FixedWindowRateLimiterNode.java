package ru.phystech.rate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FixedWindowRateLimiterNode {
    final Map<Integer, FixedWindowRateLimiter> limitersForClients = new ConcurrentHashMap<>();

    public boolean allowForClient(int clientId) {
        return limitersForClients.get(clientId).allow();
    }

    public void addClient(int clientId, int maxRequestPerWindow, long windowSizeInMillis) {
        limitersForClients.put(clientId, new FixedWindowRateLimiter(maxRequestPerWindow, windowSizeInMillis));
    }

    public void cleanWindows() {
        limitersForClients.forEach((key, value) -> value.cleanWindows());
    }

    public void resize(float resizeCoefficient) {
        limitersForClients.forEach((key, value) -> {
            value.setMaxRequestsPerWindow((int) (value.getMaxRequestsPerWindow() * resizeCoefficient));
        });
    }
}
