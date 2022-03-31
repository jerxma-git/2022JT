package info.kgeorgiy.ja.zheromskij.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> list;
    private final Comparator<E> comparator;

    public ArraySet() {
        this(List.of());
    }

    public ArraySet(final Collection<E> c) {
        this(c, null);
    }


    public ArraySet(final Collection<E> c, final Comparator<E> comparator) {
        this.comparator = comparator;
        final TreeSet<E> tmpSet = new TreeSet<>(comparator);
        tmpSet.addAll(c);
        list = List.copyOf(tmpSet);
    }

    private ArraySet(final ArraySet<E> set, final int from, final int to) {
        assert 0 <= from && to <= set.list.size();
        this.comparator = set.comparator();
        this.list = set.list.subList(from, to);
    }


    @Override
    public E first() {
        if (list.size() == 0) {
            throw new NoSuchElementException();
        }
        return list.get(0);
    }

    @Override
    public E last() {
        if (list.size() == 0) {
            throw new NoSuchElementException();
        }
        return list.get(list.size() - 1);
    }

    private int binSearch(final E element) {
        return Collections.binarySearch(list, element, comparator);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return binSearch((E) o) >= 0;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public Comparator<E> comparator() {
        return comparator;
    }

    private int insertionPos(final E el) {
        final int pos = binSearch(el);
        return pos < 0 ? -(pos + 1) : pos;
    }

    @Override
    public ArraySet<E> headSet(final E el) {
        return new ArraySet<>(this, 0, insertionPos(el));
    }

    @Override
    public ArraySet<E> tailSet(final E el) {
        return new ArraySet<>(this, insertionPos(el), list.size());
    }

    @Override
    public ArraySet<E> subSet(final E from, final E to) {
        if (compare(from, to) > 0) {
            throw new IllegalArgumentException();
        }
        return tailSet(from).headSet(to);
    }

    @SuppressWarnings("unchecked")
    private int compare(final E o1, final E o2) {
        return comparator == null ? ((Comparable<? super E>) o1).compareTo(o2) : comparator.compare(o1, o2);
    }
}