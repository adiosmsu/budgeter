package ru.adios.budgeter.api;

/**
 * Date: 10/24/15
 * Time: 7:51 PM
 *
 * @author Mikhail Kulikov
 */
public enum Order {

    ASC {
        @Override
        public int applyToCompareResult(int compareResult) {
            return compareResult;
        }
    }, DESC {
        @Override
        public int applyToCompareResult(int compareResult) {
            return -compareResult;
        }
    };

    public abstract int applyToCompareResult(int compareResult);

}
