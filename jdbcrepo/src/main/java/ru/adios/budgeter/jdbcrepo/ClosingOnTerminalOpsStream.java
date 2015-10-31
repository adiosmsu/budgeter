package ru.adios.budgeter.jdbcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Date: 10/26/15
 * Time: 11:54 PM
 *
 * @author Mikhail Kulikov
 */
@Immutable
public class ClosingOnTerminalOpsStream<T> implements Stream<T> {

    static <T> ClosingOnTerminalOpsStream<T> stream(Stream<T> stream) {
        return new ClosingOnTerminalOpsStream<>(stream);
    }


    private static final Logger logger = LoggerFactory.getLogger(ClosingOnTerminalOpsStream.class);

    private static <T> void closeSilently(AutoCloseable delegate) {
        try {
            delegate.close();
        } catch (Exception ignore) {
            logger.warn("Delegate stream close exception", ignore);
        }
    }


    private final Stream<T> delegate;


    ClosingOnTerminalOpsStream(Stream<T> delegate) {
        this.delegate = delegate;
    }


    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        try {
            return delegate.allMatch(predicate);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        try {
            return delegate.anyMatch(predicate);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        try {
            return delegate.collect(collector);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public long count() {
        try {
            return delegate.count();
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public ClosingOnTerminalOpsStream<T> distinct() {
        return new ClosingOnTerminalOpsStream<>(delegate.distinct());
    }

    @Override
    public ClosingOnTerminalOpsStream<T> filter(Predicate<? super T> predicate) {
        return new ClosingOnTerminalOpsStream<>(delegate.filter(predicate));
    }

    @Override
    public Optional<T> findAny() {
        try {
            return delegate.findAny();
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public Optional<T> findFirst() {
        try {
            return delegate.findFirst();
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public <R> ClosingOnTerminalOpsStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return new ClosingOnTerminalOpsStream<>(delegate.flatMap(mapper));
    }

    @Override
    public AutoClosingDoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return new AutoClosingDoubleStream(delegate.flatMapToDouble(mapper));
    }

    @Override
    public AutoClosingIntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return new AutoClosingIntStream(delegate.flatMapToInt(mapper));
    }

    @Override
    public AutoClosingLongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return new AutoClosingLongStream(delegate.flatMapToLong(mapper));
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        try {
            delegate.forEach(action);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        try {
            delegate.forEachOrdered(action);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public ClosingOnTerminalOpsStream<T> limit(long maxSize) {
        return new ClosingOnTerminalOpsStream<>(delegate.limit(maxSize));
    }

    @Override
    public <R> ClosingOnTerminalOpsStream<R> map(Function<? super T, ? extends R> mapper) {
        return new ClosingOnTerminalOpsStream<>(delegate.map(mapper));
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return delegate.mapToDouble(mapper);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return delegate.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return delegate.mapToLong(mapper);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        try {
            return delegate.max(comparator);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        try {
            return delegate.min(comparator);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        try {
            return delegate.noneMatch(predicate);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public ClosingOnTerminalOpsStream<T> peek(Consumer<? super T> action) {
        return new ClosingOnTerminalOpsStream<>(delegate.peek(action));
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        try {
            return delegate.reduce(accumulator);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        try {
            return delegate.reduce(identity, accumulator);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        try {
            return delegate.reduce(identity, accumulator, combiner);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public ClosingOnTerminalOpsStream<T> skip(long n) {
        return new ClosingOnTerminalOpsStream<>(delegate.skip(n));
    }

    @Override
    public ClosingOnTerminalOpsStream<T> sorted() {
        return new ClosingOnTerminalOpsStream<>(delegate.sorted());
    }

    @Override
    public ClosingOnTerminalOpsStream<T> sorted(Comparator<? super T> comparator) {
        return new ClosingOnTerminalOpsStream<>(delegate.sorted(comparator));
    }

    @Override
    public Object[] toArray() {
        try {
            return delegate.toArray();
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        try {
            return delegate.toArray(generator);
        } finally {
            closeSilently(delegate);
        }
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public AutoClosingIterator<T> iterator() {
        return new AutoClosingIterator<>(delegate.iterator(), this);
    }

    @Override
    public ClosingOnTerminalOpsStream<T> onClose(Runnable closeHandler) {
        return new ClosingOnTerminalOpsStream<>(delegate.onClose(closeHandler));
    }

    @Override
    public ClosingOnTerminalOpsStream<T> parallel() {
        return new ClosingOnTerminalOpsStream<>(delegate.parallel());
    }

    @Override
    public ClosingOnTerminalOpsStream<T> sequential() {
        return new ClosingOnTerminalOpsStream<>(delegate.sequential());
    }

    @Override
    public AutoClosingSpliterator<T> spliterator() {
        return new AutoClosingSpliterator<>(delegate.spliterator(), this);
    }

    @Override
    public ClosingOnTerminalOpsStream<T> unordered() {
        return new ClosingOnTerminalOpsStream<>(delegate.unordered());
    }

    @Override
    public void close() {
        delegate.close();
    }

    public static final class AutoClosingSpliterator<T> implements Spliterator<T>, AutoCloseable {

        private final Spliterator<T> d;
        private final ClosingOnTerminalOpsStream<T> streamDelegate;

        private volatile boolean open = true;

        private AutoClosingSpliterator(Spliterator<T> d, ClosingOnTerminalOpsStream<T> streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public int characteristics() {
            try {
                return d.characteristics();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public long estimateSize() {
            try {
                return d.estimateSize();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                final boolean advance = d.tryAdvance(action);
                if (!advance) {
                    close();
                }
                return advance;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public AutoClosingSpliterator<T> trySplit() {
            Spliterator<T> d;
            try {
                d = this.d.trySplit();
                if (d == null) {
                    close();
                    return null;
                }
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
            return new AutoClosingSpliterator<>(d, streamDelegate);
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.delegate);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingIterator<T> implements Iterator<T>, AutoCloseable {

        private final Iterator<T> d;
        private final ClosingOnTerminalOpsStream<T> streamDelegate;

        private volatile boolean open = true;

        private AutoClosingIterator(Iterator<T> d, ClosingOnTerminalOpsStream<T> streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public boolean hasNext() {
            try {
                final boolean next = d.hasNext();
                if (!next) {
                    close();
                }
                return next;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public T next() {
            try {
                return d.next();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.delegate);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingIntStream implements IntStream {

        private final IntStream d;

        public AutoClosingIntStream(IntStream d) {
            this.d = d;
        }

        @Override
        public boolean allMatch(IntPredicate predicate) {
            try {
                return d.allMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public boolean anyMatch(IntPredicate predicate) {
            try {
                return d.anyMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleStream asDoubleStream() {
            return new AutoClosingDoubleStream(d.asDoubleStream());
        }

        @Override
        public AutoClosingLongStream asLongStream() {
            return new AutoClosingLongStream(d.asLongStream());
        }

        @Override
        public OptionalDouble average() {
            try {
                return d.average();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public ClosingOnTerminalOpsStream<Integer> boxed() {
            return new ClosingOnTerminalOpsStream<>(d.boxed());
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            try {
                return d.collect(supplier, accumulator, combiner);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public long count() {
            try {
                return d.count();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingIntStream distinct() {
            return new AutoClosingIntStream(d.distinct());
        }

        @Override
        public AutoClosingIntStream filter(IntPredicate predicate) {
            return new AutoClosingIntStream(d.filter(predicate));
        }

        @Override
        public OptionalInt findAny() {
            try {
                return d.findAny();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalInt findFirst() {
            try {
                return d.findFirst();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingIntStream flatMap(IntFunction<? extends IntStream> mapper) {
            return new AutoClosingIntStream(d.flatMap(mapper));
        }

        @Override
        public void forEach(IntConsumer action) {
            try {
                d.forEach(action);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public void forEachOrdered(IntConsumer action) {
            try {
                d.forEachOrdered(action);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingIntIterator iterator() {
            return new AutoClosingIntIterator(d.iterator(), this);
        }

        @Override
        public AutoClosingIntStream limit(long maxSize) {
            return new AutoClosingIntStream(d.limit(maxSize));
        }

        @Override
        public AutoClosingIntStream map(IntUnaryOperator mapper) {
            return new AutoClosingIntStream(d.map(mapper));
        }

        @Override
        public AutoClosingDoubleStream mapToDouble(IntToDoubleFunction mapper) {
            return new AutoClosingDoubleStream(d.mapToDouble(mapper));
        }

        @Override
        public AutoClosingLongStream mapToLong(IntToLongFunction mapper) {
            return new AutoClosingLongStream(d.mapToLong(mapper));
        }

        @Override
        public <U> ClosingOnTerminalOpsStream<U> mapToObj(IntFunction<? extends U> mapper) {
            return new ClosingOnTerminalOpsStream<>(d.mapToObj(mapper));
        }

        @Override
        public OptionalInt max() {
            try {
                return d.max();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalInt min() {
            try {
                return d.min();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public boolean noneMatch(IntPredicate predicate) {
            try {
                return d.noneMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingIntStream parallel() {
            return new AutoClosingIntStream(d.parallel());
        }

        @Override
        public AutoClosingIntStream peek(IntConsumer action) {
            return new AutoClosingIntStream(d.peek(action));
        }

        @Override
        public int reduce(int identity, IntBinaryOperator op) {
            try {
                return d.reduce(identity, op);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalInt reduce(IntBinaryOperator op) {
            try {
                return d.reduce(op);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingIntStream sequential() {
            return new AutoClosingIntStream(d.sequential());
        }

        @Override
        public AutoClosingIntStream skip(long n) {
            return new AutoClosingIntStream(d.skip(n));
        }

        @Override
        public AutoClosingIntStream sorted() {
            return new AutoClosingIntStream(d.sorted());
        }

        @Override
        public AutoClosingIntSpliterator spliterator() {
            return new AutoClosingIntSpliterator(d.spliterator(), this);
        }

        @Override
        public int sum() {
            try {
                return d.sum();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public IntSummaryStatistics summaryStatistics() {
            try {
                return d.summaryStatistics();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public int[] toArray() {
            try {
                return d.toArray();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public void close() {
            d.close();
        }

        @Override
        public boolean isParallel() {
            return d.isParallel();
        }

        @Override
        public AutoClosingIntStream onClose(Runnable closeHandler) {
            return new AutoClosingIntStream(d.onClose(closeHandler));
        }

        @Override
        public AutoClosingIntStream unordered() {
            return new AutoClosingIntStream(d.unordered());
        }

    }

    public static final class AutoClosingLongStream implements LongStream {

        private final LongStream d;

        public AutoClosingLongStream(LongStream d) {
            this.d = d;
        }

        @Override
        public boolean allMatch(LongPredicate predicate) {
            try {
                return d.allMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public boolean anyMatch(LongPredicate predicate) {
            try {
                return d.anyMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleStream asDoubleStream() {
            return new AutoClosingDoubleStream(d.asDoubleStream());
        }

        @Override
        public OptionalDouble average() {
            try {
                return d.average();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public ClosingOnTerminalOpsStream<Long> boxed() {
            return new ClosingOnTerminalOpsStream<>(d.boxed());
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            try {
                return d.collect(supplier, accumulator, combiner);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public long count() {
            try {
                return d.count();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingLongStream distinct() {
            return new AutoClosingLongStream(d.distinct());
        }

        @Override
        public AutoClosingLongStream filter(LongPredicate predicate) {
            return new AutoClosingLongStream(d.filter(predicate));
        }

        @Override
        public OptionalLong findAny() {
            try {
                return d.findAny();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalLong findFirst() {
            try {
                return d.findFirst();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingLongStream flatMap(LongFunction<? extends LongStream> mapper) {
            return new AutoClosingLongStream(d.flatMap(mapper));
        }

        @Override
        public void forEach(LongConsumer action) {
            try {
                d.forEach(action);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public void forEachOrdered(LongConsumer action) {
            try {
                d.forEachOrdered(action);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingLongIterator iterator() {
            return new AutoClosingLongIterator(d.iterator(), this);
        }

        @Override
        public AutoClosingLongStream limit(long maxSize) {
            return new AutoClosingLongStream(d.limit(maxSize));
        }

        @Override
        public AutoClosingLongStream map(LongUnaryOperator mapper) {
            return new AutoClosingLongStream(d.map(mapper));
        }

        @Override
        public AutoClosingDoubleStream mapToDouble(LongToDoubleFunction mapper) {
            return new AutoClosingDoubleStream(d.mapToDouble(mapper));
        }

        @Override
        public AutoClosingIntStream mapToInt(LongToIntFunction mapper) {
            return new AutoClosingIntStream(d.mapToInt(mapper));
        }

        @Override
        public <U> ClosingOnTerminalOpsStream<U> mapToObj(LongFunction<? extends U> mapper) {
            return new ClosingOnTerminalOpsStream<>(d.mapToObj(mapper));
        }

        @Override
        public OptionalLong max() {
            try {
                return d.max();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalLong min() {
            try {
                return d.min();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public boolean noneMatch(LongPredicate predicate) {
            try {
                return d.noneMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingLongStream parallel() {
            return new AutoClosingLongStream(d.parallel());
        }

        @Override
        public AutoClosingLongStream peek(LongConsumer action) {
            return new AutoClosingLongStream(d.peek(action));
        }

        @Override
        public long reduce(long identity, LongBinaryOperator op) {
            try {
                return d.reduce(identity, op);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalLong reduce(LongBinaryOperator op) {
            try {
                return d.reduce(op);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingLongStream sequential() {
            return new AutoClosingLongStream(d.sequential());
        }

        @Override
        public AutoClosingLongStream skip(long n) {
            return new AutoClosingLongStream(d.skip(n));
        }

        @Override
        public AutoClosingLongStream sorted() {
            return new AutoClosingLongStream(d.sorted());
        }

        @Override
        public AutoClosingLongSpliterator spliterator() {
            return new AutoClosingLongSpliterator(d.spliterator(), this);
        }

        @Override
        public long sum() {
            try {
                return d.sum();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public LongSummaryStatistics summaryStatistics() {
            try {
                return d.summaryStatistics();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public long[] toArray() {
            try {
                return d.toArray();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public void close() {
            d.close();
        }

        @Override
        public boolean isParallel() {
            return d.isParallel();
        }

        @Override
        public AutoClosingLongStream onClose(Runnable closeHandler) {
            return new AutoClosingLongStream(d.onClose(closeHandler));
        }

        @Override
        public AutoClosingLongStream unordered() {
            return new AutoClosingLongStream(d.unordered());
        }

    }

    public static final class AutoClosingDoubleStream implements DoubleStream {

        private final DoubleStream d;

        public AutoClosingDoubleStream(DoubleStream d) {
            this.d = d;
        }

        @Override
        public boolean allMatch(DoublePredicate predicate) {
            try {
                return d.allMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public boolean anyMatch(DoublePredicate predicate) {
            try {
                return d.anyMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalDouble average() {
            try {
                return d.average();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public ClosingOnTerminalOpsStream<Double> boxed() {
            return new ClosingOnTerminalOpsStream<>(d.boxed());
        }

        @Override
        public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            try {
                return d.collect(supplier, accumulator, combiner);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public long count() {
            try {
                return d.count();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleStream distinct() {
            return new AutoClosingDoubleStream(d.distinct());
        }

        @Override
        public AutoClosingDoubleStream filter(DoublePredicate predicate) {
            return new AutoClosingDoubleStream(d.filter(predicate));
        }

        @Override
        public OptionalDouble findAny() {
            try {
                return d.findAny();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalDouble findFirst() {
            try {
                return d.findFirst();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
            return new AutoClosingDoubleStream(d.flatMap(mapper));
        }

        @Override
        public void forEach(DoubleConsumer action) {
            try {
                d.forEach(action);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public void forEachOrdered(DoubleConsumer action) {
            try {
                d.forEachOrdered(action);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleIterator iterator() {
            return new AutoClosingDoubleIterator(d.iterator(), this);
        }

        @Override
        public AutoClosingDoubleStream limit(long maxSize) {
            return new AutoClosingDoubleStream(d.limit(maxSize));
        }

        @Override
        public AutoClosingDoubleStream map(DoubleUnaryOperator mapper) {
            return new AutoClosingDoubleStream(d.map(mapper));
        }

        @Override
        public AutoClosingLongStream mapToLong(DoubleToLongFunction mapper) {
            return new AutoClosingLongStream(d.mapToLong(mapper));
        }

        @Override
        public AutoClosingIntStream mapToInt(DoubleToIntFunction mapper) {
            return new AutoClosingIntStream(d.mapToInt(mapper));
        }

        @Override
        public <U> ClosingOnTerminalOpsStream<U> mapToObj(DoubleFunction<? extends U> mapper) {
            return new ClosingOnTerminalOpsStream<>(d.mapToObj(mapper));
        }

        @Override
        public OptionalDouble max() {
            try {
                return d.max();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalDouble min() {
            try {
                return d.min();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public boolean noneMatch(DoublePredicate predicate) {
            try {
                return d.noneMatch(predicate);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleStream parallel() {
            return new AutoClosingDoubleStream(d.parallel());
        }

        @Override
        public AutoClosingDoubleStream peek(DoubleConsumer action) {
            return new AutoClosingDoubleStream(d.peek(action));
        }

        @Override
        public double reduce(double identity, DoubleBinaryOperator op) {
            try {
                return d.reduce(identity, op);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public OptionalDouble reduce(DoubleBinaryOperator op) {
            try {
                return d.reduce(op);
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public AutoClosingDoubleStream sequential() {
            return new AutoClosingDoubleStream(d.sequential());
        }

        @Override
        public AutoClosingDoubleStream skip(long n) {
            return new AutoClosingDoubleStream(d.skip(n));
        }

        @Override
        public AutoClosingDoubleStream sorted() {
            return new AutoClosingDoubleStream(d.sorted());
        }

        @Override
        public AutoClosingDoubleSpliterator spliterator() {
            return new AutoClosingDoubleSpliterator(d.spliterator(), this);
        }

        @Override
        public double sum() {
            try {
                return d.sum();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public DoubleSummaryStatistics summaryStatistics() {
            try {
                return d.summaryStatistics();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public double[] toArray() {
            try {
                return d.toArray();
            } finally {
                closeSilently(d);
            }
        }

        @Override
        public void close() {
            d.close();
        }

        @Override
        public boolean isParallel() {
            return d.isParallel();
        }

        @Override
        public AutoClosingDoubleStream onClose(Runnable closeHandler) {
            return new AutoClosingDoubleStream(d.onClose(closeHandler));
        }

        @Override
        public AutoClosingDoubleStream unordered() {
            return new AutoClosingDoubleStream(d.unordered());
        }

    }

    public static final class AutoClosingIntSpliterator implements Spliterator.OfInt, AutoCloseable {

        private final Spliterator.OfInt d;
        private final AutoClosingIntStream streamDelegate;

        private volatile boolean open = true;

        private AutoClosingIntSpliterator(OfInt d, AutoClosingIntStream streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public int characteristics() {
            try {
                return d.characteristics();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public long estimateSize() {
            try {
                return d.estimateSize();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            try {
                final boolean ret = d.tryAdvance(action);
                if (!ret) {
                    close();
                }
                return ret;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public AutoClosingIntSpliterator trySplit() {
            OfInt d2;
            try {
                d2 = this.d.trySplit();
                if (d2 == null) {
                    close();
                    return null;
                }
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
            return new AutoClosingIntSpliterator(d2, streamDelegate);
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.d);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingLongSpliterator implements Spliterator.OfLong, AutoCloseable {

        private final Spliterator.OfLong d;
        private final AutoClosingLongStream streamDelegate;

        private volatile boolean open = true;

        private AutoClosingLongSpliterator(OfLong d, AutoClosingLongStream streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public int characteristics() {
            try {
                return d.characteristics();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public long estimateSize() {
            try {
                return d.estimateSize();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            try {
                final boolean ret = d.tryAdvance(action);
                if (!ret) {
                    close();
                }
                return ret;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public AutoClosingLongSpliterator trySplit() {
            OfLong d2;
            try {
                d2 = this.d.trySplit();
                if (d2 == null) {
                    close();
                    return null;
                }
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
            return new AutoClosingLongSpliterator(d2, streamDelegate);
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.d);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingDoubleSpliterator implements Spliterator.OfDouble, AutoCloseable {

        private final Spliterator.OfDouble d;
        private final AutoClosingDoubleStream streamDelegate;

        private volatile boolean open = true;

        private AutoClosingDoubleSpliterator(OfDouble d, AutoClosingDoubleStream streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public int characteristics() {
            try {
                return d.characteristics();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public long estimateSize() {
            try {
                return d.estimateSize();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            try {
                final boolean ret = d.tryAdvance(action);
                if (!ret) {
                    close();
                }
                return ret;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public AutoClosingDoubleSpliterator trySplit() {
            OfDouble d2;
            try {
                d2 = this.d.trySplit();
                if (d2 == null) {
                    close();
                    return null;
                }
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
            return new AutoClosingDoubleSpliterator(d2, streamDelegate);
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.d);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingIntIterator implements PrimitiveIterator.OfInt, AutoCloseable {

        private final PrimitiveIterator.OfInt d;
        private final AutoClosingIntStream streamDelegate;

        private volatile boolean open = true;

        private AutoClosingIntIterator(OfInt d, AutoClosingIntStream streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public int nextInt() {
            try {
                return d.nextInt();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean hasNext() {
            try {
                final boolean n = d.hasNext();
                if (!n) {
                    close();
                }
                return n;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public void close()  {
            open = false;
            closeSilently(streamDelegate.d);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingLongIterator implements PrimitiveIterator.OfLong, AutoCloseable {

        private final PrimitiveIterator.OfLong d;
        private final AutoClosingLongStream streamDelegate;

        private volatile boolean open = true;

        private AutoClosingLongIterator(OfLong d, AutoClosingLongStream streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public long nextLong() {
            try {
                return d.nextLong();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean hasNext() {
            try {
                final boolean n = d.hasNext();
                if (!n) {
                    close();
                }
                return n;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.d);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

    public static final class AutoClosingDoubleIterator implements PrimitiveIterator.OfDouble, AutoCloseable {

        private final PrimitiveIterator.OfDouble d;
        private final AutoClosingDoubleStream streamDelegate;

        private volatile boolean open = true;

        private AutoClosingDoubleIterator(OfDouble d, AutoClosingDoubleStream streamDelegate) {
            this.d = d;
            this.streamDelegate = streamDelegate;
        }

        @Override
        public double nextDouble() {
            try {
                return d.nextDouble();
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public boolean hasNext() {
            try {
                final boolean n = d.hasNext();
                if (!n) {
                    close();
                }
                return n;
            } catch (RuntimeException rt) {
                close();
                throw rt;
            }
        }

        @Override
        public void close() {
            open = false;
            closeSilently(streamDelegate.d);
        }

        @Override
        protected void finalize() throws Throwable {
            if (open) {
                close();
            }
            super.finalize();
        }

    }

}
