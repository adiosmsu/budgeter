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
public class ClosingOnTerminalOpsStream<T> implements Stream<T>, Wrappable {

    static <T> ClosingOnTerminalOpsStream<T> stream(Stream<T> stream) {
        return new ClosingOnTerminalOpsStream<>(stream);
    }


    private static final Logger logger = LoggerFactory.getLogger(ClosingOnTerminalOpsStream.class);

    static <T> void closeSilently(AutoCloseable delegate) {
        try {
            delegate.close();
        } catch (Exception ignore) {
            logger.debug("Delegate stream close exception", ignore);
        }
    }

    private static <ReturnType, Param> ReturnType wrapForCloseRethrow(AutoCloseable s, Function<Param, ReturnType> f, Param p) {
        try {
            return f.apply(p);
        } catch (RuntimeException rt) {
            closeSilently(s);
            throw rt;
        }
    }
    private static <ReturnType> ReturnType wrapForCloseRethrow(AutoCloseable s, Supplier<ReturnType> f) {
        try {
            return f.get();
        } catch (RuntimeException rt) {
            closeSilently(s);
            throw rt;
        }
    }
    private static <Param> boolean wrapBooleanFunct(final WrappingAutoCloseable s, final Function<Param, Boolean> f, Param p) {
        return wrapForCloseRethrow(s, param -> {
            final boolean b = f.apply(param);
            if (!b) {
                s.close();
            }
            return b;
        }, p);
    }
    private static boolean wrapBooleanSup(final WrappingAutoCloseable s, final Supplier<Boolean> f) {
        return wrapForCloseRethrow(s, () -> {
            final boolean b = f.get();
            if (!b) {
                s.close();
            }
            return b;
        });
    }
    private static <ReturnType> ReturnType wrapNullableSup(final WrappingAutoCloseable s, final Supplier<ReturnType> f) {
        return wrapForCloseRethrow(s, () -> {
            final ReturnType r = f.get();
            if (r == null) {
                s.close();
            }
            return r;
        });
    }

    private final Stream<T> delegate;


    ClosingOnTerminalOpsStream(Stream<T> delegate) {
        this.delegate = delegate;
    }


    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return wrap(delegate::allMatch, predicate);
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return wrap(delegate::anyMatch, predicate);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return wrap(delegate::collect, collector);
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
        return wrap(delegate::count);
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
        return wrap(delegate::findAny);
    }

    @Override
    public Optional<T> findFirst() {
        return wrap(delegate::findFirst);
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
        wrap(action, delegate::forEach);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        wrap(action, delegate::forEachOrdered);
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
        return wrap(delegate::max, comparator);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return wrap(delegate::min, comparator);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return wrap(delegate::noneMatch, predicate);
    }

    @Override
    public ClosingOnTerminalOpsStream<T> peek(Consumer<? super T> action) {
        return new ClosingOnTerminalOpsStream<>(delegate.peek(action));
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return wrap(delegate::reduce, accumulator);
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
        return wrap(delegate::toArray);
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return wrap(delegate::toArray, generator);
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public AutoClosingIterator<T> iterator() {
        return new AutoClosingIterator<>(delegate.iterator(), delegate);
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
        return new AutoClosingSpliterator<>(delegate.spliterator(), delegate);
    }

    @Override
    public ClosingOnTerminalOpsStream<T> unordered() {
        return new ClosingOnTerminalOpsStream<>(delegate.unordered());
    }

    @Override
    public void close() {
        delegate.close();
    }


    public static final class AutoClosingSpliterator<T> extends CloseableFinalisingDelegate<Stream<T>> implements Spliterator<T>, WrappingAutoCloseable {

        private final Spliterator<T> d;

        private AutoClosingSpliterator(Spliterator<T> d, Stream<T> streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public int characteristics() {
            return wrap(d::characteristics);
        }

        @Override
        public long estimateSize() {
            return wrap(d::estimateSize);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return wrapBool(d::tryAdvance, action);
        }

        @Override
        public AutoClosingSpliterator<T> trySplit() {
            return new AutoClosingSpliterator<>(wrapNullable(d::trySplit), delegate);
        }

    }

    public static final class AutoClosingIterator<T> extends CloseableFinalisingDelegate<Stream<T>> implements Iterator<T>, WrappingAutoCloseable {

        private final Iterator<T> d;

        private AutoClosingIterator(Iterator<T> d, Stream<T> streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public boolean hasNext() {
            return wrapBool(d::hasNext);
        }

        @Override
        public T next() {
            return wrap(d::next);
        }

    }

    public static final class AutoClosingIntStream implements IntStream, Wrappable {

        private final IntStream d;

        public AutoClosingIntStream(IntStream d) {
            this.d = d;
        }

        @Override
        public boolean allMatch(IntPredicate predicate) {
            return wrap(d::allMatch, predicate);
        }

        @Override
        public boolean anyMatch(IntPredicate predicate) {
            return wrap(d::anyMatch, predicate);
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
            return wrap(d::average);
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
            return wrap(d::count);
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
            return wrap(d::findAny);
        }

        @Override
        public OptionalInt findFirst() {
            return wrap(d::findFirst);
        }

        @Override
        public AutoClosingIntStream flatMap(IntFunction<? extends IntStream> mapper) {
            return new AutoClosingIntStream(d.flatMap(mapper));
        }

        @Override
        public void forEach(IntConsumer action) {
            wrap(action, d::forEach);
        }

        @Override
        public void forEachOrdered(IntConsumer action) {
            wrap(action, d::forEachOrdered);
        }

        @Override
        public AutoClosingIntIterator iterator() {
            return new AutoClosingIntIterator(d.iterator(), d);
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
            return wrap(d::max);
        }

        @Override
        public OptionalInt min() {
            return wrap(d::min);
        }

        @Override
        public boolean noneMatch(IntPredicate predicate) {
            return wrap(d::noneMatch, predicate);
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
            return wrap(d::reduce, op);
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
            return new AutoClosingIntSpliterator(d.spliterator(), d);
        }

        @Override
        public int sum() {
            return wrap(d::sum);
        }

        @Override
        public IntSummaryStatistics summaryStatistics() {
            return wrap(d::summaryStatistics);
        }

        @Override
        public int[] toArray() {
            return wrap(d::toArray);
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

    public static final class AutoClosingLongStream implements LongStream, Wrappable {

        private final LongStream d;

        public AutoClosingLongStream(LongStream d) {
            this.d = d;
        }

        @Override
        public boolean allMatch(LongPredicate predicate) {
            return wrap(d::allMatch, predicate);
        }

        @Override
        public boolean anyMatch(LongPredicate predicate) {
            return wrap(d::anyMatch, predicate);
        }

        @Override
        public AutoClosingDoubleStream asDoubleStream() {
            return new AutoClosingDoubleStream(d.asDoubleStream());
        }

        @Override
        public OptionalDouble average() {
            return wrap(d::average);
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
            return wrap(d::count);
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
            return wrap(d::findAny);
        }

        @Override
        public OptionalLong findFirst() {
            return wrap(d::findFirst);
        }

        @Override
        public AutoClosingLongStream flatMap(LongFunction<? extends LongStream> mapper) {
            return new AutoClosingLongStream(d.flatMap(mapper));
        }

        @Override
        public void forEach(LongConsumer action) {
            wrap(action, d::forEach);
        }

        @Override
        public void forEachOrdered(LongConsumer action) {
            wrap(action, d::forEachOrdered);
        }

        @Override
        public AutoClosingLongIterator iterator() {
            return new AutoClosingLongIterator(d.iterator(), d);
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
            return wrap(d::max);
        }

        @Override
        public OptionalLong min() {
            return wrap(d::min);
        }

        @Override
        public boolean noneMatch(LongPredicate predicate) {
            return wrap(d::noneMatch, predicate);
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
            return wrap(d::reduce, op);
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
            return new AutoClosingLongSpliterator(d.spliterator(), d);
        }

        @Override
        public long sum() {
            return wrap(d::sum);
        }

        @Override
        public LongSummaryStatistics summaryStatistics() {
            return wrap(d::summaryStatistics);
        }

        @Override
        public long[] toArray() {
            return wrap(d::toArray);
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

    public static final class AutoClosingDoubleStream implements DoubleStream, Wrappable {

        private final DoubleStream d;

        public AutoClosingDoubleStream(DoubleStream d) {
            this.d = d;
        }

        @Override
        public boolean allMatch(DoublePredicate predicate) {
            return wrap(d::allMatch, predicate);
        }

        @Override
        public boolean anyMatch(DoublePredicate predicate) {
            return wrap(d::anyMatch, predicate);
        }

        @Override
        public OptionalDouble average() {
            return wrap(d::average);
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
            return wrap(d::count);
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
            return wrap(d::findAny);
        }

        @Override
        public OptionalDouble findFirst() {
            return wrap(d::findFirst);
        }

        @Override
        public AutoClosingDoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
            return new AutoClosingDoubleStream(d.flatMap(mapper));
        }

        @Override
        public void forEach(DoubleConsumer action) {
            wrap(action, d::forEach);
        }

        @Override
        public void forEachOrdered(DoubleConsumer action) {
            wrap(action, d::forEachOrdered);
        }

        @Override
        public AutoClosingDoubleIterator iterator() {
            return new AutoClosingDoubleIterator(d.iterator(), d);
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
            return wrap(d::max);
        }

        @Override
        public OptionalDouble min() {
            return wrap(d::min);
        }

        @Override
        public boolean noneMatch(DoublePredicate predicate) {
            return wrap(d::noneMatch, predicate);
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
            return wrap(d::reduce, op);
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
            return new AutoClosingDoubleSpliterator(d.spliterator(), d);
        }

        @Override
        public double sum() {
            return wrap(d::sum);
        }

        @Override
        public DoubleSummaryStatistics summaryStatistics() {
            return wrap(d::summaryStatistics);
        }

        @Override
        public double[] toArray() {
            return wrap(d::toArray);
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

    public static final class AutoClosingIntSpliterator extends CloseableFinalisingDelegate<IntStream> implements Spliterator.OfInt, WrappingAutoCloseable {

        private final Spliterator.OfInt d;

        private AutoClosingIntSpliterator(OfInt d, IntStream streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public int characteristics() {
            return wrap(d::characteristics);
        }

        @Override
        public long estimateSize() {
            return wrap(d::estimateSize);
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            return wrapBool(d::tryAdvance, action);
        }

        @Override
        public AutoClosingIntSpliterator trySplit() {
            return new AutoClosingIntSpliterator(wrapNullable(d::trySplit), delegate);
        }

    }

    public static final class AutoClosingLongSpliterator extends CloseableFinalisingDelegate<LongStream> implements Spliterator.OfLong, WrappingAutoCloseable {

        private final Spliterator.OfLong d;

        private AutoClosingLongSpliterator(OfLong d, LongStream streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public int characteristics() {
            return wrap(d::characteristics);
        }

        @Override
        public long estimateSize() {
            return wrap(d::estimateSize);
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            return wrapBool(d::tryAdvance, action);
        }

        @Override
        public AutoClosingLongSpliterator trySplit() {
            return new AutoClosingLongSpliterator(wrapNullable(d::trySplit), delegate);
        }

    }

    public static final class AutoClosingDoubleSpliterator extends CloseableFinalisingDelegate<DoubleStream> implements Spliterator.OfDouble, WrappingAutoCloseable {

        private final Spliterator.OfDouble d;

        private AutoClosingDoubleSpliterator(OfDouble d, DoubleStream streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public int characteristics() {
            return wrap(d::characteristics);
        }

        @Override
        public long estimateSize() {
            return wrap(d::estimateSize);
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            return wrapBool(d::tryAdvance, action);
        }

        @Override
        public AutoClosingDoubleSpliterator trySplit() {
            return new AutoClosingDoubleSpliterator(wrapNullable(d::trySplit), delegate);
        }

    }

    public static final class AutoClosingIntIterator extends CloseableFinalisingDelegate<IntStream> implements PrimitiveIterator.OfInt, WrappingAutoCloseable {

        private final PrimitiveIterator.OfInt d;

        private AutoClosingIntIterator(OfInt d, IntStream streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public int nextInt() {
            return wrap(d::nextInt);
        }

        @Override
        public boolean hasNext() {
            return wrapBool(d::hasNext);
        }

    }

    public static final class AutoClosingLongIterator extends CloseableFinalisingDelegate<LongStream> implements PrimitiveIterator.OfLong, WrappingAutoCloseable {

        private final PrimitiveIterator.OfLong d;

        private AutoClosingLongIterator(OfLong d, LongStream streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public long nextLong() {
            return wrap(d::nextLong);
        }

        @Override
        public boolean hasNext() {
            return wrapBool(d::hasNext);
        }

    }

    public static final class AutoClosingDoubleIterator extends CloseableFinalisingDelegate<DoubleStream> implements PrimitiveIterator.OfDouble, WrappingAutoCloseable {

        private final PrimitiveIterator.OfDouble d;

        private AutoClosingDoubleIterator(OfDouble d, DoubleStream streamDelegate) {
            super(streamDelegate);
            this.d = d;
        }

        @Override
        public double nextDouble() {
            return wrap(d::nextDouble);
        }

        @Override
        public boolean hasNext() {
            return wrapBool(d::hasNext);
        }

    }

    public interface WrappingAutoCloseable extends AutoCloseable {

        default <ReturnType, Param> ReturnType wrap(Function<Param, ReturnType> f, Param p) {
            return wrapForCloseRethrow(this, f, p);
        }

        default <ReturnType> ReturnType wrap(Supplier<ReturnType> f) {
            return wrapForCloseRethrow(this, f);
        }

        default <Param> boolean wrapBool(Function<Param, Boolean> f, Param p) {
            return wrapBooleanFunct(this, f, p);
        }

        default boolean wrapBool(Supplier<Boolean> f) {
            return wrapBooleanSup(this, f);
        }

        default <ReturnType> ReturnType wrapNullable(Supplier<ReturnType> f) {
            return wrapNullableSup(this, f);
        }

        @Override
        void close();

    }

    private static abstract class CloseableFinalisingDelegate<DelegateType extends AutoCloseable> implements AutoCloseable {

        protected final DelegateType delegate;

        private volatile boolean open = true;

        private CloseableFinalisingDelegate(DelegateType delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            open = false;
            closeSilently(delegate);
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
