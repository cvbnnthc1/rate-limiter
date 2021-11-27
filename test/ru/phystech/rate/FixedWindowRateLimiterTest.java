package ru.phystech.rate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


public class FixedWindowRateLimiterTest {
    private final int amountOfIterations = 10;
    private final int amountOfThreads = 10;

    @Test
    public void allow_returnTrue_whenLimitIsNotExceeded_singleThread() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        List<Boolean> results =  new ArrayList<>();
        for (int i = 0; i < amountOfIterations; i++) {
            results.add(limiter.allow());
            results.add(limiter.allow());
            results.add(limiter.allow());
            doSleepMillis(100);
        }
        results.forEach(Assert::assertTrue);
    }

    @Test
    public void allow_returnFalse_whenLimitIsExceeded_singleThread() {
        RateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < amountOfIterations; i++) {
            limiter.allow();
            limiter.allow();
            limiter.allow();
            results.add(limiter.allow());
            results.add(limiter.allow());
            doSleepMillis(100);
        }
        results.forEach(Assert::assertFalse);
    }

    @Test
    public void allow_returnTrue_whenLimitIsNotExceeded_concurrentThreads() {
        RateLimiter limiter = new FixedWindowRateLimiter(amountOfThreads, 100);
        CountDownLatch endSignal = new CountDownLatch(amountOfThreads);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        Runnable task = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(limiter.allow());
                doSleepMillis(100);
            }
           endSignal.countDown();
        };
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < amountOfThreads; i++) {
            threads.add(new Thread(task));
        }
        threads.forEach(Thread::start);
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        results.forEach(Assert::assertTrue);
    }

    @Test
    public void allow_returnFalse_whenLimitIsExceeded_concurrentThreads() {
        RateLimiter limiter = new FixedWindowRateLimiter(amountOfThreads, 100);
        CountDownLatch endSignal = new CountDownLatch(amountOfThreads);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        Runnable task = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(limiter.allow());
                results.add(limiter.allow());
                doSleepMillis(100);
            }
            endSignal.countDown();
        };
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < amountOfThreads; i++) {
            threads.add(new Thread(task));
        }
        threads.forEach(Thread::start);
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int success = 0;
        int fail = 0;
        for (Boolean result: results) {
            if (result) success++;
            else fail++;
        }
        Assert.assertEquals(amountOfIterations * amountOfThreads, success);
        Assert.assertEquals(amountOfIterations * amountOfThreads, fail);
    }

    @Test
    public void cleanWindows_deleteAllOldWindows() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        for (int i = 0; i < amountOfIterations; i++) {
            limiter.allow();
            doSleepMillis(100);
        }
        Assert.assertEquals(amountOfIterations, limiter.windows.size());
        limiter.cleanWindows();
        Assert.assertEquals(0, limiter.windows.size());
    }

    @Test
    public void cleanWindows_deleteAllOldWindows_dontDeleteCurWindow() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 100);
        for (int i = 0; i < amountOfIterations; i++) {
            limiter.allow();
            doSleepMillis(100);
        }
        limiter.allow();
        Assert.assertEquals(amountOfIterations + 1, limiter.windows.size());
        limiter.cleanWindows();
        Assert.assertEquals(1, limiter.windows.size());
    }

    static void doSleepMillis(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}