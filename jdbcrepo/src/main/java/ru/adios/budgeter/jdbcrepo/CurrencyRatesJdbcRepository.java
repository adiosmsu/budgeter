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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.*;
import ru.adios.budgeter.api.data.ConversionPair;
import ru.adios.budgeter.api.data.ConversionRate;

import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Date: 10/28/15
 * Time: 1:15 AM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class CurrencyRatesJdbcRepository implements CurrencyRatesRepository, JdbcRepository<ConversionRate> {

    public static final String TABLE_NAME = "currency_rate";
    public static final String SEQ_NAME = "seq_currency_rate";
    public static final String INDEX_DAY = "ix_currency_rate_day";
    public static final String INDEX_FROM_TO = "ix_currency_rate_from_to";
    public static final String COL_ID = "id";
    public static final String COL_DAY = "day";
    public static final String COL_FROM = "from_unit";
    public static final String COL_TO = "to_unit";
    public static final String COL_RATE = "rate";

    private static final Logger logger = LoggerFactory.getLogger(CurrencyRatesJdbcRepository.class);
    private static final ImmutableList<String> COLS = ImmutableList.of(COL_DAY, COL_FROM, COL_TO, COL_RATE);

    private static final String SQL_CONV_MULTI_STRAIGHT = getConversionMultiStraightSql();
    private static String getConversionMultiStraightSql() {
        final StringBuilder sb = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_RATE);
        SqlDialect.appendWhereClausePart(sb.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY, COL_FROM, COL_TO);
        return sb.toString();
    }
    private static final String SQL_RATE_STALE = getRateStaleSql();
    private static String getRateStaleSql() {
        final StringBuilder sb = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_ID);
        SqlDialect.appendWhereClausePart(sb.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY, COL_FROM);
        SqlDialect.appendWhereClausePart(false, sb, false, SqlDialect.Op.EQUAL, COL_TO);
        return sb.toString();
    }
    private static final String SQL_INDEXED_4D = getIndexedDaySql();
    private static String getIndexedDaySql() {
        final StringBuilder sb = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_ID);
        SqlDialect.appendWhereClausePart(sb.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_DAY);
        return sb.toString();
    }


    private final SafeJdbcConnector jdbcConnector;
    private final RateRowMapper rowMapper = new RateRowMapper();
    private volatile SqlDialect sqlDialect = SqliteDialect.INSTANCE;
    private final LazySupplier supIdSql = new LazySupplier();
    private final String insertSql = JdbcRepository.super.getInsertSql(false);
    private final String sqlLatestOptConvMulti;

    CurrencyRatesJdbcRepository(SafeJdbcConnector jdbcConnector) {
        this.jdbcConnector = jdbcConnector;
        final StringBuilder sqlBuilder = SqlDialect.selectSqlBuilder(TABLE_NAME, null, COL_RATE);
        SqlDialect.appendWhereClausePart(sqlBuilder.append(" WHERE"), true, SqlDialect.Op.EQUAL, COL_FROM, COL_TO);
        SqlDialect.appendWhereClausePostfix(sqlBuilder, sqlDialect, OptLimit.createLimit(1), OrderBy.getDefault(COL_DAY, Order.DESC));
        sqlLatestOptConvMulti = sqlBuilder.toString();
    }

    @Override
    public void setSqlDialect(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    @Override
    public SqlDialect getSqlDialect() {
        return sqlDialect;
    }

    @Override
    public AgnosticRowMapper<ConversionRate> getRowMapper() {
        return rowMapper;
    }

    @Override
    public SafeJdbcConnector getJdbcConnector() {
        return jdbcConnector;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getIdColumnName() {
        return COL_ID;
    }

    @Override
    public String getSeqName() {
        return SEQ_NAME;
    }

    @Override
    public ImmutableList<String> getColumnNames() {
        return COLS;
    }

    @Override
    public SqlDialect.Join[] getJoins() {
        return Common.EMPTY_JOINS;
    }

    @Override
    public ImmutableList<?> decomposeObject(ConversionRate object) {
        return ImmutableList.of(object.day, object.pair.from.getNumericCode(), object.pair.to.getNumericCode(), object.rate);
    }

    @Override
    public LazySupplier getIdLazySupplier() {
        return supIdSql;
    }

    @Override
    public String getInsertSql(boolean withId) {
        return insertSql;
    }


    @Override
    public boolean addRate(UtcDay dayUtc, CurrencyUnit from, CurrencyUnit to, BigDecimal rate) {
        try {
            return Common.insert(this, new ConversionRate(dayUtc, new ConversionPair(from, to), rate)).getKey() != null;
        } catch (RuntimeException ex) {
            logger.warn(String.format("addRate(%s, %s, %s, %s) exception", dayUtc, from, to, rate), ex);
            return false;
        }
    }

    @Override
    public Optional<BigDecimal> getConversionMultiplierStraight(UtcDay day, CurrencyUnit from, CurrencyUnit to) {
        return Common.getSingleColumnOptional(
                this,
                SQL_CONV_MULTI_STRAIGHT,
                sqlDialect.getRowMapperForType(BigDecimal.class),
                sqlDialect.translateForDb(day), from.getNumericCode(), to.getNumericCode()
        );
    }

    @Override
    public Optional<BigDecimal> getLatestOptionalConversionMultiplier(CurrencyUnit from, CurrencyUnit to) {
        return Common.getSingleColumnOptional(
                this,
                sqlLatestOptConvMulti,
                sqlDialect.getRowMapperForType(BigDecimal.class),
                from.getNumericCode(), to.getNumericCode()
        );
    }

    @Override
    public boolean isRateStale(CurrencyUnit to) {
        final int toTrans = to.getNumericCode();
        return !Common.getSingleColumnOptional(
                this,
                SQL_RATE_STALE,
                sqlDialect.getRowMapperForType(Long.class),
                sqlDialect.translateForDb(new UtcDay()), toTrans, toTrans
        ).isPresent();
    }

    @Override
    public ImmutableSet<Long> getIndexedForDay(UtcDay day) {
        return ImmutableSet.copyOf(
                Common.getSingleColumnList(
                        this,
                        SQL_INDEXED_4D,
                        sqlDialect.getRowMapperForType(Long.class),
                        sqlDialect.translateForDb(day)
                )
        );
    }


    @Override
    public String[] getCreateTableSql() {
        return new String[] {
                getActualCreateTableSql(),
                sqlDialect.createSeq(SEQ_NAME, TABLE_NAME),
                sqlDialect.createIndexSql(INDEX_DAY, TABLE_NAME, false, COL_DAY),
                sqlDialect.createIndexSql(INDEX_FROM_TO, TABLE_NAME, true, COL_DAY, COL_FROM, COL_TO)
        };
    }

    @Override
    public String[] getDropTableSql() {
        return new String[] {
                sqlDialect.dropSeqCommand(SEQ_NAME),
                SqlDialect.dropIndexCommand(INDEX_DAY),
                SqlDialect.dropIndexCommand(INDEX_FROM_TO),
                SqlDialect.dropTableCommand(TABLE_NAME)
        };
    }

    private String getActualCreateTableSql() {
        return SqlDialect.CREATE_TABLE + TABLE_NAME+ " ("
                    + COL_ID + ' ' + sqlDialect.bigIntType() + ' ' + sqlDialect.primaryKeyWithNextValue(SEQ_NAME) + ", "
                    + COL_DAY + ' ' + sqlDialect.timestampWithoutTimezoneType() + ", "
                    + COL_FROM + " INT, "
                    + COL_TO + " INT, "
                    + COL_RATE + ' ' + sqlDialect.decimalType()
                + ')';
    }


    private final class RateRowMapper implements AgnosticRowMapper<ConversionRate> {

        @Override
        public ConversionRate mapRow(ResultSet rs) throws SQLException {
            final UtcDay day = sqlDialect.translateFromDb(rs.getObject(1), UtcDay.class);
            final int from = rs.getInt(2);
            final int to = rs.getInt(3);
            final BigDecimal rate = sqlDialect.translateFromDb(rs.getObject(4), BigDecimal.class);

            return new ConversionRate(day, new ConversionPair(CurrencyUnit.ofNumericCode(from), CurrencyUnit.ofNumericCode(to)), rate);
        }

    }

}
