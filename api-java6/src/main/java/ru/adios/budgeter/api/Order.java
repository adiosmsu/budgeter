package ru.adios.budgeter.api;

import java.io.Serializable;

/**
 * Date: 10/24/15
 * Time: 7:51 PM
 *
 * @author Mikhail Kulikov
 */
public enum Order implements Serializable {

    ASC {
        @Override
        public int applyToCompareResult(int compareResult) {
            return compareResult;
        }

        @Override
        public Order other() {
            return DESC;
        }
    }, DESC {
        @Override
        public int applyToCompareResult(int compareResult) {
            return -compareResult;
        }

        @Override
        public Order other() {
            return ASC;
        }
    };

    public abstract int applyToCompareResult(int compareResult);

    public abstract Order other();

}
