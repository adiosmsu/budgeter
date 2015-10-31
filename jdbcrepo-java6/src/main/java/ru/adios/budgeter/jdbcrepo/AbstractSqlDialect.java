package ru.adios.budgeter.jdbcrepo;

import java.util.Arrays;

/**
 * Date: 11/1/15
 * Time: 12:10 AM
 *
 * @author Mikhail Kulikov
 */
public abstract class AbstractSqlDialect implements SqlDialect {

    @Override
    public String dropSeqCommand(String seqName) {
        return "DROP SEQUENCE " + seqName;
    }

    @Override
    public String insertSql(String tableName, String... columns) {
        return insertSql(tableName, Arrays.asList(columns));
    }

}
