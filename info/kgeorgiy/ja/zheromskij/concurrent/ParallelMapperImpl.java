package info.kgeorgiy.ja.zheromskij.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    // :NOTE: LinkedList
    // private final LinkedList<Runnable> tasks = new LinkedList<>();
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

        // :NOTE: .peek
        // workers = Stream.generate(() -> new Thread(workload)).limit(threads).toList();
        // workers.forEach(Thread::start);
        workers = Stream.generate(() -> new Thread(workload))
                .limit(threads)
                .peek(Thread::start)
                .toList();
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
        // final RuntimeException rt = new RuntimeException("RT thrown");
        final List<RuntimeException> es = new ArrayList<>();
        IntStream.range(0, args.size()).forEach(index -> {
            synchronized (tasks) {
                tasks.add(() -> {
                    final T arg = args.get(index);
                    R mapped = null;
                    try {
                        mapped = f.apply(arg);
                        // :NOTE: Пробрасывать исходное
                    } catch (final RuntimeException e) {
                        // rt.addSuppressed(e);
                        
                        synchronized (es) {
                            es.add(e);
                        }
                    }
                    // :NOTE: Лишняя синхронизация
                    // перенес cnt.inc();
                    synchronized (results) {
                        results.set(index, mapped);
                        // cnt.inc();
                    }
                    cnt.inc();
                });
                tasks.notify();
            }
        });

        if (!es.isEmpty()) {
            RuntimeException first = es.get(0);
            es.subList(1, es.size()).forEach(first::addSuppressed);
            throw first;
        }
        cnt.await();
        return results;
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        for (final Thread thread : workers) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException ignored) {
    
                }
            }
        }
    }

    // :NOTE: Убывающий
    // теперь убывающий
    private static class Counter {
        private int cnt;

        public Counter(final int cnt) {
            this.cnt = cnt;
        }

        public synchronized void inc() {
            if (--cnt <= 0) {
                this.notify();
            }
        }

        private synchronized void await() throws InterruptedException {
            while (cnt > 0) {
                wait();
            }
        }
    }
}
