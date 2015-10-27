package ru.adios.budgeter.jdbcrepo;

import javax.annotation.Nullable;
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

    static String generateWhereClause(boolean andIfTrue, String op, String... columns) {
        return generateWhereClause(andIfTrue, op, Arrays.asList(columns));
    }

    static String generateWhereClause(boolean andIfTrue, String op, List<String> columns) {
        final StringBuilder sb = new StringBuilder(25 * columns.size());
        boolean first = true;
        for (final String c : columns) {
            if (first) {
                first = false;
            } else {
                sb.append(andIfTrue ? " AND" : " OR");
            }
            sb.append(' ').append(c).append(' ').append(op).append(" ?");
        }
        return sb.toString();
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
        return appendColumns(sb, columns, null);
    }

    static boolean appendColumns(StringBuilder sb, List<String> columns, @Nullable String suffix) {
        boolean first = true;
        for (final String col : columns) {
            first = appendCol(sb, col, first);
            if (suffix != null) {
                sb.append(suffix);
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

    static String getUpdateSql(String tableName, List<String> changingColumns, List<String> selectingColumns, int rowsNumber, boolean opt) {
        final int numberOfSelectingColumns = selectingColumns.size();
        final StringBuilder builder = new StringBuilder(20 + tableName.length() + 16 * changingColumns.size() + 20 * numberOfSelectingColumns);

        builder.append("UPDATE ").append(tableName).append(" SET ");

        appendColumns(builder, changingColumns, "=?");

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
                appendSuccession(sb, numberOfColumns, sb2 -> sb.append('?'));
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


    enum Op {

        EQUAL("="), NOT_EQUAL("<>"), LIKE("LIKE"), MORE(">"), MORE_EQ(">="), LESS("<"), LESS_EQ("<=");

        public final String spelling;

        Op(String spelling) {
            this.spelling = spelling;
        }

    }

    String CREATE_TABLE = "CREATE TABLE ";
    String OPTIMIZED_PSEUDO_NAMED_PARAM = "ids";


    String tableExistsSql(String tableName);

    String integerType();

    String textType();

    String primaryKeyWithNextValue(@Nullable String sequence);

    String createIndexSql(String indexName, String tableName, boolean unique, String... columns);

    String sequenceCurrentValueSql(@Nullable String tableName, @Nullable String sequenceName);

    default String insertSql(String tableName, String... columns) {
        return insertSql(tableName, Arrays.asList(columns));
    }

    String insertSql(String tableName, List<String> columns);

    String selectAllSql(String tableName);

    default String selectSql(String tableName, @Nullable String where, String... columns) {
        return selectSql(tableName, where, Arrays.asList(columns));
    }

    String selectSql(String tableName, @Nullable String where, List<String> columns);

}
