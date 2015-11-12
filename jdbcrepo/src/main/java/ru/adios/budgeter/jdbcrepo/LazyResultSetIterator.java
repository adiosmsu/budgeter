/*
 *
 *  * Copyright 2015 Michael Kulikov
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package ru.adios.budgeter.jdbcrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Date: 10/26/15
 * Time: 10:46 PM
 *
 * @author Mikhail Kulikov
 */
@NotThreadSafe
public class LazyResultSetIterator<T> implements Iterator<T>, AutoCloseable {

    static <T> Stream<T> stream(ResultSetSupplier resultSetSupplier, Function<ResultSet, T> retriever) {
        final LazyResultSetIterator<T> iterator = new LazyResultSetIterator<>(resultSetSupplier, retriever);
        return stream(iterator);
    }

    static <T> Stream<T> stream(ResultSetSupplier resultSetSupplier, Function<ResultSet, T> retriever, String sqlForException) {
        final LazyResultSetIterator<T> iterator = new LazyResultSetIterator<>(resultSetSupplier, retriever, sqlForException);
        return stream(iterator);
    }

    static <T> Stream<T> stream(LazyResultSetIterator<T> iterator) {
        return ClosingOnTerminalOpsStream.stream(
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                        .onClose(iterator::close)
        );
    }

    static <T> LazyResultSetIterator<T> of(ResultSetSupplier resultSetSupplier, Function<ResultSet, T> retriever, String sqlForException) {
        return new LazyResultSetIterator<>(resultSetSupplier, retriever, sqlForException);
    }

    private static final Logger logger = LoggerFactory.getLogger(LazyResultSetIterator.class);


    private final ResultSetSupplier resultSetSupplier;
    private final Function<ResultSet, T> retriever;
    @Nullable
    private final String sql;

    private ResultSet resultSet;
    private boolean didNext = false;
    private boolean hasNext = false;
    private boolean closed = false;

    LazyResultSetIterator(ResultSetSupplier resultSetSupplier, Function<ResultSet, T> retriever) {
        this(resultSetSupplier, retriever, null);
    }

    LazyResultSetIterator(ResultSetSupplier resultSetSupplier, Function<ResultSet, T> retriever, @Nullable String sql) {
        checkNotNull(resultSetSupplier, "resultSetSupplier");
        checkNotNull(retriever, "retriever");
        this.resultSetSupplier = resultSetSupplier;
        this.retriever = retriever;
        this.sql = sql;
    }

    Stream<T> stream() {
        return stream(this);
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
        closeResultSet(false);
    }

    private boolean askNext() {
        if (resultSet == null) {
            resultSet = resultSetSupplier.get();
        }

        try {
            final boolean next = resultSet.next();
            if (!next) {
                closeResultSet(false);
            }
            return next;
        } catch (SQLException ex) {
            closeResultSet(true);
            throw Common.EXCEPTION_TRANSLATOR.translate("LazyResultSetIterator", sql, ex);
        } catch (RuntimeException ex) {
            closeResultSet(true);
            throw new DataAccessResourceFailureException("Driver/wrapper threw unchecked exception", ex);
        }
    }

    private void closeResultSet(boolean eatException) {
        if (resultSet != null && !closed) {
            closed = true;
            try {
                resultSet.close();
            } catch (SQLException ignore) {
                logger.warn("ResultSet close exception", ignore);
            }
            if (eatException) {
                try {
                    resultSetSupplier.close();
                } catch (RuntimeException eaten) {
                    logger.warn(
                            "Statement and connection close exception; unable to rethrow it because another exception already caught up the stack",
                            eaten
                    );
                }
            } else {
                resultSetSupplier.close();
            }
        }
    }

}
