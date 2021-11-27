package ru.phystech.rate;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FixedWindowRateLimiterNodeTest {
    private final int amountOfIterations = 5;
    private final int amountOfThreads = 5;

    @Test
    public void allowFoUser_returnTrue_whenLimitForUserIsNotExceeded() {
        FixedWindowRateLimiterNode node = new FixedWindowRateLimiterNode();
        node.addUser(0, 3 * amountOfThreads, 100);
        node.addUser(1, 5 * amountOfThreads, 100);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        List<Thread> threads = new ArrayList<>();
        CountDownLatch endSignal = new CountDownLatch(amountOfThreads);
        Runnable task1 = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(node.allowFoUser(0));
                results.add(node.allowFoUser(0));
                results.add(node.allowFoUser(0));
                doSleepMillis(100);
            }
            endSignal.countDown();
        };
        Runnable task2 = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(node.allowFoUser(1));
                results.add(node.allowFoUser(1));
                results.add(node.allowFoUser(1));
                results.add(node.allowFoUser(1));
                results.add(node.allowFoUser(1));
                doSleepMillis(100);
            }
            endSignal.countDown();
        };
        for (int i = 0; i < amountOfThreads; i++) {
            threads.add(new Thread(task1));
            threads.add(new Thread(task2));
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
    public void allowFoUser_returnTrue_whenLimitForUserIsExceeded() {
        FixedWindowRateLimiterNode node = new FixedWindowRateLimiterNode();
        node.addUser(0, amountOfThreads, 100);
        node.addUser(1, amountOfThreads, 100);
        Collection<Boolean> results = new ConcurrentLinkedDeque<>();
        List<Thread> threads = new ArrayList<>();
        CountDownLatch endSignal = new CountDownLatch(amountOfThreads);
        Runnable task1 = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(node.allowFoUser(0));
                results.add(node.allowFoUser(0));
                doSleepMillis(100);
            }
            endSignal.countDown();
        };
        Runnable task2 = () -> {
            for (int i = 0; i < amountOfIterations; i++) {
                results.add(node.allowFoUser(1));
                results.add(node.allowFoUser(1));
                doSleepMillis(100);
            }
            endSignal.countDown();
        };
        for (int i = 0; i < amountOfThreads; i++) {
            threads.add(new Thread(task1));
            threads.add(new Thread(task2));
        }
        threads.forEach(Thread::start);
        try {
            endSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int success = 0;
        int fail = 0;
       for (boolean result: results) {
           if (result) success++;
           else fail++;
       }
       assertEquals(success, 2 * amountOfThreads * amountOfIterations);
       assertEquals(fail, 2 * amountOfThreads * amountOfIterations);
    }

    @Test
    public void addUser() {
        FixedWindowRateLimiterNode node = new FixedWindowRateLimiterNode();
        node.addUser(0, 5, 100);
        assertNotNull(node.limitersForUsers.get(0));
    }

    @Test
    public void cleanWindows_deleteAllOldWindows() {
        FixedWindowRateLimiterNode node = new FixedWindowRateLimiterNode();
        node.addUser(0, 3, 100);
        node.addUser(1, 3, 100);
        for (int i = 0; i < amountOfIterations; i++) {
            node.allowFoUser(0);
            node.allowFoUser(1);
            doSleepMillis(100);
        }
        assertEquals(amountOfIterations, node.limitersForUsers.get(0).windows.size());
        assertEquals(amountOfIterations, node.limitersForUsers.get(1).windows.size());
        node.cleanWindows();
        assertEquals(0, node.limitersForUsers.get(0).windows.size());
        assertEquals(0, node.limitersForUsers.get(1).windows.size());
    }

    @Test
    public void cleanWindows_deleteAllOldWindows_andNotDeleteNewWindows() {
        FixedWindowRateLimiterNode node = new FixedWindowRateLimiterNode();
        node.addUser(0, 3, 100);
        node.addUser(1, 3, 100);
        for (int i = 0; i < amountOfIterations; i++) {
            node.allowFoUser(0);
            node.allowFoUser(1);
            doSleepMillis(100);
        }
        node.allowFoUser(0);
        node.allowFoUser(1);
        assertEquals(amountOfIterations + 1, node.limitersForUsers.get(0).windows.size());
        assertEquals(amountOfIterations + 1, node.limitersForUsers.get(1).windows.size());
        node.cleanWindows();
        assertEquals(1, node.limitersForUsers.get(0).windows.size());
        assertEquals(1, node.limitersForUsers.get(1).windows.size());
    }

    @Test
    public void resize() {
        FixedWindowRateLimiterNode node = new FixedWindowRateLimiterNode();
        node.addUser(0, 2, 100);
        node.addUser(1, 2, 100);
        List<Boolean> successResults = new ArrayList<>();
        List<Boolean> failResults = new ArrayList<>();
        for (int i = 0; i < amountOfIterations; i++) {
            successResults.add(node.allowFoUser(0));
            successResults.add(node.allowFoUser(0));
            successResults.add(node.allowFoUser(1));
            successResults.add(node.allowFoUser(1));
            doSleepMillis(100);
        }
        node.resize(0.5f);
        for (int i = 0; i < amountOfIterations; i++) {
            successResults.add(node.allowFoUser(0));
            failResults.add(node.allowFoUser(0));
            successResults.add(node.allowFoUser(1));
            failResults.add(node.allowFoUser(1));
            doSleepMillis(100);
        }
        successResults.forEach(Assert::assertTrue);
        failResults.forEach(Assert::assertFalse);
        assertEquals(6 * amountOfIterations, successResults.size());
        assertEquals(2 * amountOfIterations, failResults.size());
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