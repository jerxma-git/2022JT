package info.kgeorgiy.ja.zheromskij.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper;

    public IterativeParallelism() {
        this(null);
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private static <T> List<Stream<? extends T>> splitList(final List<? extends T> vals, final int parts) {
        final List<Stream<? extends T>> res = new ArrayList<>();
        final int partSize = vals.size() / parts;
        // for (int i = 0; i < vals.size(); i += partSize) {
        //     // :NOTE: Не равномерное распределение
        //     res.add(vals.subList(i, Math.min(i + partSize, vals.size())).stream());
        // }
        int rem = vals.size() % partSize;
        int step = partSize + (rem > 0 ? 1 : 0);
        for (int i = 0; i < vals.size(); i += step, rem--) {
            if (rem == 0) {
                step = partSize;
            }
            res.add(vals.subList(i, i + step).stream());
        }
       
        
        return res;
    }

    
    private <T, U, R> U parallelFun(final List<? extends T> values, final int threads, final Function<Stream<? extends T>, R> mapper, final Function<List<R>, U> combiner) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("Not enough threads");
        }

        final List<Thread> workers = new ArrayList<>();
        // :NOTE: Не работает для пустых списков
        // теперь работает
        List<Stream<? extends T>> split = splitList(values, Math.min(threads, values.size()));
        List<R> results = this.mapper == null ? map(mapper, workers, split) : this.mapper.map(mapper, split);
        return combiner.apply(results);
    }

   
    private <T, R> List<R> map(final Function<Stream<? extends T>, R> mapper, final List<Thread> workers, final List<Stream<? extends T>> split) throws InterruptedException {
        final List<R> results = new ArrayList<>(Collections.nCopies(split.size(), null));
        // IntStream.range(0, split.size()).forEach(i -> {
        //     final Thread worker = new Thread(() -> results.set(i, mapper.apply(split.get(i))));
        //     // :NOTE: Stream.peek + toList
        //     workers.add(worker);
        //     worker.start();
        // });

        workers.addAll(
            IntStream.range(0, split.size())
                .mapToObj(i -> new Thread(() -> results.set(i, mapper.apply(split.get(i)))))
                .peek(Thread::start)
                .toList()
        );


        final List<InterruptedException> exceptions = new ArrayList<>();
        for (final Thread worker : workers) {
            while (true) {
                try {
                    // :NOTE: Утечка  потока
                    worker.join();
                    break;
                } catch (final InterruptedException e) {
                    exceptions.add(e);
                    // нужно ли?
                }
            }
        }

        if (!exceptions.isEmpty()) {
            final InterruptedException ex = new InterruptedException("Execution was interrupted");
            exceptions.forEach(ex::addSuppressed);
            throw ex;
        }
        return results;
    }



    private <T, U> Optional<U> parallelReduction(final List<? extends T> values, final int threads, final Function<Stream<? extends T>, U> mapper, final BinaryOperator<U> reducer) throws InterruptedException {
        return parallelFun(values, threads, mapper, list -> list.stream().reduce(reducer));
    }

    private <T, U> List<U> combFun(final int threads, final List<? extends T> values, final Function<Stream<? extends T>, Stream<? extends U>> mapper) throws InterruptedException {
        return parallelReduction(values, threads, mapper, Stream::concat).orElse(Stream.of()).collect(Collectors.toList());
    }

    



    
    
    /** 
     * Returns the maximum value of the provided {@code List} according to the provided {@code comparator} 
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @param comparator function for comparing 2 values
     * @return the largest value according to the comparator 
     * @throws InterruptedException if the execution was interrupted
     * @throws NoSuchElementException if an empty list of values was given 
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("No values given");
        }
        return parallelReduction(values, threads, stream -> stream.max(comparator).get(), BinaryOperator.maxBy(comparator)).get();
    }

    
    /** 
     * Returns the minimum value of the provided {@List } according to the provided {@code Comparator} 
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @param comparator function for comparing 2 values
     * @return the smallest value according to the comparator 
     * @throws InterruptedException if the execution of any thread was interrupted
     * @throws NoSuchElementException if an empty list of values was given 
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

   
    
    /** 
     * Returns whether any elements of the provided {@List} match the provided {@code Predicate}. If the list is empty then returns {@code false} and the predicate is not evaluated 
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @param predicate a non-interfering, stateless predicate to apply to elements of the provided list
     * @return {@code true} true if any elements of the list match the provided predicate, otherwise false
     * @throws InterruptedException if the execution of any thread was interrupted
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelReduction(values, threads, stream -> stream.anyMatch(predicate), Boolean::logicalOr).orElse(false);
    }

    
    /** 
     * Returns whether all elements of the provided {@List} match the provided {@code Predicate}. If the list is empty then returns {@code false} and the predicate is not evaluated 
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @param predicate a non-interfering, stateless predicate to apply to elements of the provided list
     * @return {@code true} true if all elements of the list match the provided predicate, otherwise false
     * @throws InterruptedException if the execution of any thread was interrupted
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    
    /** 
     * Returns a list consisting of the elements of the provided list that match the given predicate.
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @param predicate a non-interfering, stateless predicate to apply to each element to determine if it should be included
     * @return a new ArrayList with the corresponding elements 
     * @throws InterruptedException if the execution of any thread was interrupted
     */
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        // :NOTE: Похоже на map
        return combFun(threads, values, stream -> stream.filter(predicate));
    }


    
    /** 
     * Returns a list consisting of the results of applying the given function to the elements of the given list. 
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @param f a a non-interfering, stateless function to apply to each element
     * @return a new ArrayList with the corresponding elements 
     * @throws InterruptedException if the execution of any thread was interrupted
     */
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return combFun(threads, values, stream -> stream.map(f));
    }

    


    /** 
     * Concatenates string representations of all elements of the provided list into a single String. Representations aren't separated by any character and appear in the string in the same order as they appear in the provided list.
     * @param threads maximum number of threads to use for evaluation
     * @param values list of values
     * @return the resulting string
     * @throws InterruptedException if the execution of any thread was interrupted
     */
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return parallelFun(values, 
                threads, 
                stream -> stream.map(Object::toString).collect(Collectors.joining()), 
                list -> list.stream().collect(Collectors.joining()));
    }
        
}