package ru.adios.budgeter.jdbcrepo;

import javax.annotation.Nullable;

/**
 * Date: 10/26/15
 * Time: 9:17 PM
 *
 * @author Mikhail Kulikov
 */
public interface SqlDialect {

    static String generateWhereClause(boolean andIfTrue, String op, String... columns) {
        final StringBuilder sb = new StringBuilder(25 * columns.length);
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
        boolean first = true;
        for (final String col : columns) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(col);
        }
        return first;
    }


    String CREATE_TABLE = "CREATE TABLE ";

    String tableExistsSql(String tableName);

    String integerType();

    String textType();

    String primaryKeyWithNextValue(@Nullable String sequence);

    String createIndexSql(String indexName, String tableName, boolean unique, String... columns);

    String sequenceCurrentValueSql(@Nullable String tableName, @Nullable String sequenceName);

    String insertSql(String tableName, String... columns);

    String selectAllSql(String tableName);

    String selectSql(String tableName, @Nullable String where, String... columns);

}
