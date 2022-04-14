package info.kgeorgiy.ja.zheromskij.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final LinkedList<Runnable> tasks = new LinkedList<>();

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
        workers = Stream.generate(() -> new Thread(workload)).limit(threads).toList();
        workers.forEach(Thread::start);

    }

    private Runnable getTask() throws InterruptedException {
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            Runnable task = tasks.poll();
            tasks.notify();
            return task;
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final List<R> results = new ArrayList<>(Collections.nCopies(args.size(), null));
        final Counter cnt = new Counter(args.size());
        final RuntimeException rt = new RuntimeException("RT thrown");

        IntStream.range(0, args.size()).forEach(index -> {
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
                    synchronized (results) {
                        results.set(index, mapped);
                        cnt.inc();
                    }
                });
                tasks.notify();
            }
        });

        if (rt.getSuppressed().length != 0) {
            throw rt;
        }
        cnt.await();
        return results;
    }

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

        public Counter(final int to) {
            this.curr = 0;
            this.to = to;
        }

        public synchronized void inc() {
            if (++curr >= to) {
                this.notify();
            }
        }

        private synchronized void await() throws InterruptedException {
            while (curr < to) {
                wait();
            }
        }
    }
}