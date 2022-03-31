package info.kgeorgiy.ja.zheromskij.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {


    private static <T> List<Stream<? extends T>> splitList(final List<? extends T> vals, final int parts) {
        final List<Stream<? extends T>> res = new ArrayList<>();
        final int partSize = vals.size() / parts;
        for (int i = 0; i < vals.size(); i += partSize) {
            res.add(vals.subList(i, Math.min(i + partSize, vals.size())).stream());
        }
        return res;
    }

    private <T, U, R> U parallelFun(final List<? extends T> values, final int threads, final Function<Stream<? extends T>, R> mapper, final Function<List<R>, U> combiner) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("Not enough threads");
        }

        final List<Thread> workers = new ArrayList<>();
        final List<Stream<? extends T>> split = splitList(values, Math.min(threads, values.size()));
        final List<R> results = map(mapper, workers, split);
        return combiner.apply(results);
    }

    private <T, R> List<R> map(final Function<Stream<? extends T>, R> mapper, final List<Thread> workers, final List<Stream<? extends T>> split) throws InterruptedException {
        final List<R> results = new ArrayList<>(Collections.nCopies(split.size(), null));
        IntStream.range(0, split.size()).forEach(i -> {
            final Thread worker = new Thread(() -> results.set(i, mapper.apply(split.get(i))));
            workers.add(worker);
            worker.start();
        });

        final List<InterruptedException> exceptions = new ArrayList<>();
        for (final Thread worker : workers) {
            try {
                worker.join();
            } catch (final InterruptedException e) {
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            final InterruptedException ex = new InterruptedException("Execution was interrupted");
            exceptions.forEach(ex::addSuppressed);
            throw ex;
        }
        return results;
    }


    private <T, U> U parallelReduction(final List<? extends T> values, final int threads, final Function<Stream<? extends T>, U> mapper, final BinaryOperator<U> reducer) throws InterruptedException {
        return parallelFun(values, threads, mapper, list -> list.stream().reduce(reducer).get());
    }


    
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("No values given");
        }
        return parallelReduction(values, threads, stream -> stream.max(comparator).get(), BinaryOperator.maxBy(comparator));
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

   
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelReduction(values, threads, stream -> stream.anyMatch(predicate), (a, b) -> a || b);
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelFun(values, 
                threads, 
                stream -> stream.filter(predicate), 
                list -> list.stream().reduce(Stream::concat).get().collect(Collectors.toList()));
    }


    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelFun(values, 
                threads, 
                stream -> stream.map(f), 
                list -> list.stream().reduce(Stream::concat).get().collect(Collectors.toList()));
    }

    public String join(final int threads, final List<?> values) throws InterruptedException {
        return parallelFun(values, 
                threads, 
                stream -> stream.map(Object::toString).collect(Collectors.joining()), 
                list -> list.stream().collect(Collectors.joining()));
    }
        
}