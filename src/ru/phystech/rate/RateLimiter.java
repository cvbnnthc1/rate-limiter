package ru.phystech.rate;

interface RateLimiter {
    public boolean allow();
}