package ru.adios.budgeter.jdbcrepo;

import org.springframework.jdbc.core.SingleColumnRowMapper;
import ru.adios.budgeter.api.OptLimit;
import ru.adios.budgeter.api.OrderBy;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Date: 10/26/15
 * Time: 9:17 PM
 *
 * @author Mikhail Kulikov
 */
public interface SqlDialect {

    static String countAllSql(String tableName) {
        return countSql(tableName, "*");
    }

    static String countAllSql(String tableName, @Nullable String whereClause, Join... joins) {
        return countSql(tableName, "*", whereClause, joins);
    }

    static String countSql(String tableName, String columnName) {
        return countSqlBuilder(tableName, columnName).toString();
    }

    static String countSql(String tableName, String columnName, @Nullable String whereClause, Join... joins) {
        return appendJoins(
                appendWhereClause(
                        countSqlBuilder(tableName, columnName),
                        whereClause
                ),
                joins
        ).toString();
    }

    static StringBuilder countSqlBuilder(String tableName, String columnName) {
        return new StringBuilder(200)
                .append("SELECT COUNT(")
                .append(columnName)
                .append(')')
                .append(" FROM ")
                .append(tableName);
    }

    static String selectSql(String tableName, @Nullable String whereClause, String... columns) {
        return selectSql(tableName, whereClause, Arrays.asList(columns));
    }

    static String selectSql(String tableName, @Nullable String whereClause, List<String> columns, Join... joins) {
        return appendWhereClause(selectSqlBuilder(tableName, columns, joins), whereClause).toString();
    }

    static StringBuilder selectSqlBuilder(String tableName, @Nullable String whereClause, String... columns) {
        return appendWhereClause(selectSqlBuilder(tableName, Arrays.asList(columns)), whereClause);
    }

    static StringBuilder appendWhereClause(StringBuilder sb, @Nullable String whereClause) {
        if (whereClause != null) {
            sb.append(" WHERE ");
            sb.append(whereClause);
        }
        return sb;
    }

    static StringBuilder selectSqlBuilder(String tableName, List<String> columns, Join... joins) {
        final StringBuilder sb = new StringBuilder(20 + tableName.length() + columns.size() * 15);

        sb.append("SELECT ");
        if (appendColumns(sb, columns)) {
            sb.append('*');
        }

        return appendJoins(sb.append(" FROM ").append(tableName), joins);
    }

    static StringBuilder appendJoins(StringBuilder sb, Join[] joins) {
        for (final Join join : joins) {
            sb.append(' ');
            join.appendToBuilder(sb);
        }
        return sb;
    }

    static String getWhereClausePostfix(SqlDialect sqlDialect, @Nullable OptLimit limit, OrderBy... orders) {
        final StringBuilder sb = new StringBuilder(50);
        appendWhereClausePostfix(sb, sqlDialect, limit, orders);
        return sb.toString();
    }

    static String getWhereClausePostfix(SqlDialect sqlDialect, @Nullable OptLimit limit, List<OrderBy> orders) {
        final StringBuilder sb = new StringBuilder(50);
        appendWhereClausePostfix(sb, sqlDialect, limit, orders);
        return sb.toString();
    }

    static void appendWhereClausePostfix(StringBuilder sb, SqlDialect sqlDialect, @Nullable OptLimit limit, OrderBy... orders) {
        appendWhereClausePostfix(sb, sqlDialect, limit, Arrays.asList(orders));
    }

    static void appendWhereClausePostfix(StringBuilder sb, SqlDialect sqlDialect, @Nullable OptLimit limit, List<OrderBy> orders) {
        if (orders.size() > 0) {
            sb.append(" ORDER BY ");
            boolean first = true;
            for (final OrderBy orderBy : orders) {
                first = appendCol(sb, sqlDialect.checkNameCase(orderBy.field.name()), first);
                sb.append(' ').append(orderBy.order.name());
            }
        }
        if (limit != null) {
            final boolean wasLimit = limit.limit > 0;
            if (wasLimit) {
                sb.append(" LIMIT ").append(limit.limit);
            }
            if (limit.offset > 0) {
                if (!wasLimit) {
                    sb.append(" LIMIT -1");
                }
                sb.append(" OFFSET ").append(limit.offset);
            }
        }
    }

    static String generateWhereClausePart(boolean andIfTrue, Op op, String... columns) {
        return generateWhereClausePart(andIfTrue, op, Arrays.asList(columns));
    }

    static String generateWhereClausePart(boolean andIfTrue, Op op, List<String> columns) {
        return generateWhereClausePartBuilder(andIfTrue, op, columns).toString();
    }

    static StringBuilder generateWhereClausePartBuilder(boolean andIfTrue, Op op, String... columns) {
        return generateWhereClausePartBuilder(andIfTrue, op, Arrays.asList(columns));
    }

    static StringBuilder generateWhereClausePartBuilder(boolean andIfTrue, Op op, List<String> columns) {
        final StringBuilder sb = new StringBuilder(25 * columns.size());
        appendWhereClausePart(sb, andIfTrue, op, columns);
        return sb;
    }

    static void appendWhereClausePart(StringBuilder sb, boolean andIfTrue, Op op, String... columns) {
        appendWhereClausePart(sb, andIfTrue, op, Arrays.asList(columns));
    }

    static void appendWhereClausePart(StringBuilder sb, boolean andIfTrue, Op op, List<String> columns) {
        appendWhereClausePart(true, sb, andIfTrue, op, columns);
    }

    static boolean appendWhereClausePart(boolean first, StringBuilder sb, boolean andIfTrue, Op op, String... columns) {
        return appendWhereClausePart(first, sb, andIfTrue, op, Arrays.asList(columns));
    }

    static boolean appendWhereClausePart(boolean first, StringBuilder sb, boolean andIfTrue, Op op, List<String> columns) {
        for (final String c : columns) {
            if (first) {
                first = false;
            } else {
                sb.append(andIfTrue ? " AND" : " OR");
            }
            sb.append(' ').append(c).append(' ').append(op.spelling).append(" ?");
        }
        return first;
    }

    static boolean appendColumns(StringBuilder sb, String[] columns) {
        return appendColumns(sb, columns, null);
    }

    static boolean appendColumns(StringBuilder sb, String[] columns, @Nullable String suffix) {
        boolean first = true;
        for (final String col : columns) {
            first = appendCol(sb, col, first);
            if (suffix != null) {
                sb.append(suffix);
            }
        }
        return first;
    }

    static boolean appendColumns(StringBuilder sb, List<String> columns) {
        return appendColumns(sb, columns, null, null, null);
    }

    static boolean appendColumns(StringBuilder sb, List<String> columns, @Nullable String suffix, @Nullable Op op, @Nullable Integer ix) {
        boolean first = true;
        int i = 0;
        for (final String col : columns) {
            first = appendCol(sb, col, first);
            if (suffix != null) {
                if (op != null && ix != null && ix == i++) {
                    sb.append('=').append(col).append(op.spelling).append('?');
                } else {
                    sb.append(suffix);
                }
            }
        }
        return first;
    }

    static boolean appendCol(StringBuilder sb, String col, boolean first) {
        if (!first) {
            sb.append(',');
        }
        sb.append(col);
        return false;
    }

    static String getUpdateSqlStandard(String tableName, List<String> changingColumns, List<String> selectingColumns, Op op, int ix) {
        return getUpdateSql(tableName, changingColumns, selectingColumns, 1, false, op, ix);
    }
    static String getUpdateSqlStandard(String tableName, List<String> changingColumns, List<String> selectingColumns) {
        return getUpdateSql(tableName, changingColumns, selectingColumns, 1, false, null, null);
    }

    static String getUpdateSql(String tableName, List<String> changingColumns, List<String> selectingColumns, int rowsNumber, boolean opt, @Nullable Op op, @Nullable Integer ix) {
        final int numberOfSelectingColumns = selectingColumns.size();
        final StringBuilder builder = new StringBuilder(20 + tableName.length() + 16 * changingColumns.size() + 20 * numberOfSelectingColumns);

        builder.append("UPDATE ").append(tableName).append(" SET ");

        appendColumns(builder, changingColumns, "=?", op, ix);

        builder.append(" WHERE ");

        if (numberOfSelectingColumns > 1) builder.append('(');

        appendColumns(builder, selectingColumns);

        if (numberOfSelectingColumns > 1) builder.append(')');

        if (rowsNumber == 1)
            builder.append(" = ");
        else
            builder.append(" IN (");

        if (opt && rowsNumber > 1) {
            builder.append(':').append(OPTIMIZED_PSEUDO_NAMED_PARAM);
        } else {
            appendParams(builder, numberOfSelectingColumns, rowsNumber);
        }

        if (rowsNumber > 1)
            builder.append(')');

        builder.append(';');

        return builder.toString();
    }

    static void appendParams(StringBuilder builder, final int numberOfColumns, int paramsNumber) {
        appendSuccession(builder, paramsNumber, sb -> {
            if (numberOfColumns > 1) {
                sb.append('(');
                appendSuccession(sb, numberOfColumns, sb2 -> sb2.append('?'));
                sb.append(')');
            } else {
                sb.append('?');
            }
        });
    }

    static void appendSuccession(StringBuilder builder, int number, Consumer<StringBuilder> strPopulator) {
        if (number < 1) {
            builder.append("null");
            return;
        }

        int i = 0;
        do {
            if (i > 0)
                builder.append(", ");

            strPopulator.accept(builder);

            i++;
        } while (i < number);
    }

    static String dropIndexCommand(String indexName) {
        return "DROP INDEX IF EXISTS " + indexName;
    }

    static String dropTableCommand(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName;
    }


    enum Op {

        EQUAL("="), NOT_EQUAL("<>"), LIKE("LIKE"), MORE(">"), MORE_EQ(">="), LESS("<"), LESS_EQ("<="),
        ADD("+"), SUBTRACT("-"), MULTIPLY("*"), DIVIDE("/");

        public final String spelling;

        Op(String spelling) {
            this.spelling = spelling;
        }

    }

    final class Join {

        enum Type {
            LEFT, INNER
        }

        private final Type type;
        private final String mainTable;
        private final String joinTable;
        private final String alias;
        private final String mainColumn;
        private final String joinColumn;

        Join(Type type, String mainTable, String joinTable, String alias, String mainColumn, String joinColumn) {
            this.type = type;
            this.mainTable = mainTable;
            this.joinTable = joinTable;
            this.alias = alias;
            this.mainColumn = mainColumn;
            this.joinColumn = joinColumn;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(joinTable.length() + alias.length() + mainColumn.length() + joinColumn.length() + 20);
            appendToBuilder(sb);
            return sb.toString();
        }

        void appendToBuilder(StringBuilder sb) {
            sb.append(type.name()).append(" JOIN ")
                    .append(joinTable).append(' ').append(alias)
                    .append(" ON ")
                    .append(mainTable).append('.').append(mainColumn)
                    .append(" = ")
                    .append(alias).append('.').append(joinColumn);
        }

    }

    static Join innerJoin(String mainTable, String joinTable, String alias, String mainColumn, String joinColumn) {
        return new Join(Join.Type.INNER, mainTable, joinTable, alias, mainColumn, joinColumn);
    }

    static Join leftJoin(String mainTable, String joinTable, String alias, String mainColumn, String joinColumn) {
        return new Join(Join.Type.LEFT, mainTable, joinTable, alias, mainColumn, joinColumn);
    }

    String CREATE_TABLE = "CREATE TABLE ";
    String OPTIMIZED_PSEUDO_NAMED_PARAM = "ids";


    String checkNameCase(String nameCapitalized);

    String tableExistsSql(String tableName);

    String textType();

    String decimalType();

    String bigIntType();

    String timestampType();

    String timestampWithoutTimezoneType();

    String primaryKeyWithNextValue(@Nullable String sequence);

    String foreignKey(String[] columns, String otherTable, String[] otherColumns, String keyName);

    String createIndexSql(String indexName, String tableName, boolean unique, String... columns);

    String createSeq(String seqName, String tableName);

    default String dropSeqCommand(String seqName) {
        return "DROP SEQUENCE " + seqName;
    }

    String sequenceCurrentValueSql(@Nullable String tableName, @Nullable String sequenceName);

    String sequenceSetValueSql(@Nullable String tableName, @Nullable String sequenceName);

    default String insertSql(String tableName, String... columns) {
        return insertSql(tableName, Arrays.asList(columns));
    }

    String insertSql(String tableName, List<String> columns);

    String selectAllSql(String tableName);

    Object translateForDb(Object object);

    <T> T translateFromDb(Object object, Class<T> type);

    @SuppressWarnings("unchecked")
    default <T> SingleColumnRowMapper<T> getRowMapperForType(Class<T> type) {
        if (type.equals(Long.class)) {
            return (SingleColumnRowMapper<T>) Common.LONG_ROW_MAPPER;
        } else if (type.equals(Integer.class)) {
            return (SingleColumnRowMapper<T>) Common.INTEGER_ROW_MAPPER;
        }  else if (type.equals(String.class)) {
            return (SingleColumnRowMapper<T>) Common.STRING_ROW_MAPPER;
        } else if (type.equals(BigDecimal.class) || type.equals(UtcDay.class) || type.equals(OffsetDateTime.class)) {
            return new ArbitrarySingleColumnRowMapper<>(type, this);
        }
        throw new IllegalArgumentException(type.toString() + " unsupported");
    }

    final class ArbitrarySingleColumnRowMapper<T> extends SingleColumnRowMapper<T> {

        private final SqlDialect sqlDialect;

        private ArbitrarySingleColumnRowMapper(Class<T> requiredType, SqlDialect sqlDialect) {
            super(requiredType);
            this.sqlDialect = sqlDialect;
        }

        @Override
        protected Object getColumnValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
            return sqlDialect.translateFromDb(super.getColumnValue(rs, index, requiredType), requiredType);
        }

    }

}
