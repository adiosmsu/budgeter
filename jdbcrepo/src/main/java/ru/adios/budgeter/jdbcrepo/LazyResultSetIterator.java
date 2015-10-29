package ru.adios.budgeter.jdbcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 10/26/15
 * Time: 10:46 PM
 *
 * @author Mikhail Kulikov
 */
public class LazyResultSetIterator<T> implements Iterator<T>, AutoCloseable {

    static <T> Stream<T> stream(Supplier<ResultSet> resultSetSupplier, Function<ResultSet, T> retriever) {
        final LazyResultSetIterator<T> iterator = new LazyResultSetIterator<>(resultSetSupplier, retriever);
        return innerStream(iterator);
    }

    static <T> Stream<T> stream(Supplier<ResultSet> resultSetSupplier, Function<ResultSet, T> retriever, String sqlForException) {
        final LazyResultSetIterator<T> iterator = new LazyResultSetIterator<>(resultSetSupplier, retriever, sqlForException);
        return innerStream(iterator);
    }

    private static <T> Stream<T> innerStream(LazyResultSetIterator<T> iterator) {
        return ClosingOnTerminalOpsStream.stream(
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                        .onClose(iterator::close)
        );
    }

    private static final Logger logger = LoggerFactory.getLogger(LazyResultSetIterator.class);

    private final Supplier<ResultSet> resultSetSupplier;
    private final Function<ResultSet, T> retriever;
    @Nullable
    private final String sql;

    private ResultSet resultSet;
    private boolean didNext = false;
    private boolean hasNext = false;
    private boolean closed = false;

    LazyResultSetIterator(Supplier<ResultSet> resultSetSupplier, Function<ResultSet, T> retriever) {
        this(resultSetSupplier, retriever, null);
    }

    LazyResultSetIterator(Supplier<ResultSet> resultSetSupplier, Function<ResultSet, T> retriever, @Nullable String sql) {
        checkNotNull(resultSetSupplier, "resultSetSupplier");
        checkNotNull(retriever, "retriever");
        this.resultSetSupplier = resultSetSupplier;
        this.retriever = retriever;
        this.sql = sql;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        didNext = false;
        return retriever.apply(resultSet);
    }

    @Override
    public boolean hasNext() {
        if (closed) return false;

        if (!didNext) {
            hasNext = askNext();
            didNext = true;
        }
        return hasNext;
    }

    @Override
    public void close() {
        hasNext = false;
        closeResultSet();
    }

    private boolean askNext() {
        if (resultSet == null) {
            resultSet = resultSetSupplier.get();
        }

        try {
            final boolean next = resultSet.next();
            if (!next) {
                closeResultSet();
            }
            return next;
        } catch (SQLException ex) {
            closeResultSet();
            throw Common.EXCEPTION_TRANSLATOR.translate("LazyResultSetIterator", sql, ex);
        } catch (RuntimeException ex) {
            closeResultSet();
            throw new DataAccessResourceFailureException("Driver/wrapper threw unchecked exception", ex);
        }
    }

    private void closeResultSet() {
        if (resultSet != null && !closed) {
            closed = true;
            try {
                resultSet.close();
            } catch (SQLException ignore) {
                logger.warn("ResultSet close exception", ignore);
            }
        }
    }

}
