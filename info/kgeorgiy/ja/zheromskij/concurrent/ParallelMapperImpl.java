package info.kgeorgiy.ja.zheromskij.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    // :NOTE: LinkedList?
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    public ParallelMapperImpl(final int threads) {
        final Runnable workload = () -> {
            try {
                while (!Thread.interrupted()) {
                    getTask().run();
                }
            } catch (final InterruptedException ignored) {

            } finally {
                Thread.currentThread().interrupt();
            }
        };
        // :NOTE: toCollection(ArrayList::new)
        workers = Stream.generate(() -> new Thread(workload)).limit(threads).collect(Collectors.toList());
        workers.forEach(Thread::start);

    }

    private Runnable getTask() throws InterruptedException {
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            return tasks.poll();
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final List<R> results = new ArrayList<>(Collections.nCopies(args.size(), null));
        final Counter cnt = new Counter(args.size());
        final RuntimeException rt = new RuntimeException("RT thrown");

        // :NOTE: IntStream
        IntStream.range(0, args.size()).map(index -> {
            synchronized (tasks) {
                tasks.add(() -> {
                    final T arg = args.get(index);
                    R mapped = null;
                    try {
                        mapped = f.apply(arg);
                    } catch (final RuntimeException e) {
                        synchronized (rt) {
                            rt.addSuppressed(e);
                        }
                    }
                    // :NOTE: Несинхронизированная запись
                    synchronized (results) {
                        results.set(index, mapped);
                        cnt.inc();
                    }
                });
                tasks.notify();
            }
            return 0;
        });

        // for (int i = 0; i < args.size(); i++) {
        //     final int index = i;
        //     synchronized (tasks) {
        //         tasks.add(() -> {
        //             final T arg = args.get(index);
        //             R mapped = null;
        //             try {
        //                 mapped = f.apply(arg);
        //             } catch (final RuntimeException e) {
        //                 synchronized (rt) {
        //                     rt.addSuppressed(e);
        //                 }
        //             }
        //             // :NOTE: Несинхронизированная запись
        //             results.set(index, mapped);
        //             cnt.inc();
        //         });
        //         tasks.notify();
        //     }
        // }
        if (rt.getSuppressed().length != 0) {
            throw rt;
        }
        cnt.await();
        return results;
    }

    // :NOTE: "Подвисшие" задания
    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        for (final Thread thread : workers) {
            try {
                thread.join();
            } catch (final InterruptedException ignored) {

            }
        }
    }

    private static class Counter {
        private int curr;
        private final int to;
        private boolean broken;

        public Counter(final int to) {
            this.curr = 0;
            this.to = to;
        }

        public void breakAndInc() {
            broken = true;
            inc();
        }
        
        public void inc() {
            if (++curr >= to) {
                // :NOTE: synchronized (this)
                synchronized (this) {
                    this.notify();
                }
            }
        }

        private synchronized void await() throws InterruptedException {
            while (curr < to) {
                wait();
            }
        }
    }
}