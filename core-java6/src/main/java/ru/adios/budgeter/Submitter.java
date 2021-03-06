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

package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java8.util.Optional;
import ru.adios.budgeter.api.TransactionalSupport;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.math.BigDecimal;

/**
 * Date: 6/13/15
 * Time: 9:40 PM
 *
 * @author Mikhail Kulikov
 */
public interface Submitter<T> {

    Result<T> submit();

    void setTransactional(TransactionalSupport transactional);

    TransactionalSupport getTransactional();

    void lock();

    void unlock();

    void submitAndStoreResult();

    Result<T> getStoredResult();

    @Immutable
    final class Result<T> {

        @SuppressWarnings("unchecked")
        public static <T> Result<T> success(@Nullable T submitResult) {
            return new Submitter.Result<T>(null, ImmutableList.<FieldError>of(), submitResult);
        }

        @Nullable
        public final String generalError;
        public final ImmutableList<FieldError> fieldErrors;
        @Nullable
        public final T submitResult;

        private Result(@Nullable String generalError, ImmutableList<FieldError> fieldErrors, @Nullable T submitResult) {
            this.generalError = generalError;
            this.fieldErrors = fieldErrors;
            this.submitResult = submitResult;
        }

        public boolean isSuccessful() {
            return generalError == null && fieldErrors.isEmpty();
        }

        public void raiseExceptionIfFailed() {
            if (isSuccessful())
                return;

            final StringBuilder builder = new StringBuilder(100);
            builder.append("Following errors:");
            if (generalError != null) {
                builder.append("\n\t\tGeneral error: ")
                        .append(generalError);
            }
            for (final FieldError error : fieldErrors) {
                if (builder.length() > 0) {
                    builder.append("\n\t\t");
                }
                builder.append(error.errorText).append('.');
            }
            throw new SubmitFailure(builder.toString());
        }

        public boolean containsFieldErrors(String... names) {
            for (final FieldError fieldError : fieldErrors) {
                for (final String name : names) {
                    if (fieldError.fieldInFault.equals(name)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    final class SubmitFailure extends RuntimeException {
        private SubmitFailure(String message) {
            super(message);
        }
    }

    @NotThreadSafe
    final class ResultBuilder<T> {

        public static final String FILL_IN_PRE = "Fill in";
        public static final String POSITIVE_PRE = "Fill in positive";
        public static final String SHORT_PRE = "Value too short (%d) for minimum length (%d) field";

        private static final Object[] EMPTY_ARRAY = new Object[] {};

        private ImmutableSet.Builder<FieldError> fieldErrorsBuilder = new ImmutableSet.Builder<FieldError>();
        private boolean wasFieldError = false;
        private String generalError;
        private T submitResult;

        public <FieldType> ResultBuilder<T> addFieldErrorIfAbsent(Optional<FieldType> field, String fieldName) {
            if (!field.isPresent()) {
                addFieldError(fieldName);
            }
            return this;
        }

        public ResultBuilder<T> addFieldErrorIfNull(Object objectReference, String fieldName) {
            if (objectReference == null) {
                addFieldError(fieldName);
            }
            return this;
        }

        public <Wrapper extends MoneyWrapper & MoneySettable> ResultBuilder<T> addFieldErrorIfNegative(Wrapper wrapperBean, String fieldName) {
            if (wrapperBean.isAmountSet() && wrapperBean.getAmountDecimal().compareTo(BigDecimal.ZERO) < 0) {
                addFieldError(fieldName, POSITIVE_PRE);
            }
            return this;
        }

        public ResultBuilder<T> addFieldErrorIfNotSet(MoneyWrapper wrapper, String fieldName, String unitFieldName, String amountFieldName) {
            if (!wrapper.isInitiable()) {
                if (wrapper.isAmountSet()) {
                    addFieldError(unitFieldName);
                } else if (wrapper.isUnitSet()) {
                    addFieldError(amountFieldName);
                } else {
                    addFieldError(unitFieldName);
                    addFieldError(amountFieldName);
                }
                addFieldError(fieldName);
            }
            return this;
        }

        public ResultBuilder<T> addFieldErrorIfShorter(CharSequence sequence, int minLength, String fieldName) {
            if (sequence == null) {
                addFieldError(fieldName);
            } else if (sequence.length() < minLength) {
                addFieldError(fieldName, SHORT_PRE, new Object[] {sequence.length(), minLength});
            }
            return this;
        }

        public ResultBuilder<T> addFieldError(String fieldInFault) {
            return addFieldError(fieldInFault, FILL_IN_PRE);
        }

        public ResultBuilder<T> addFieldError(String fieldInFault, String predicate) {
            return addFieldError(fieldInFault, predicate, EMPTY_ARRAY);
        }

        public ResultBuilder<T> addFieldError(String fieldInFault, String predicate, Object[] params) {
            fieldErrorsBuilder.add(new FieldError((params.length > 0 ? String.format(predicate, params) : predicate) + ' ' + fieldInFault, fieldInFault));
            wasFieldError = true;
            return this;
        }

        public ResultBuilder<T> setGeneralError(String generalError) {
            this.generalError = generalError;
            return this;
        }

        public ResultBuilder<T> addExistingResult(Result<T> result) {
            if (result.generalError != null) {
                generalError = result.generalError;
            }
            if (result.submitResult != null) {
                submitResult = result.submitResult;
            }
            for (final FieldError fieldError : result.fieldErrors) {
                fieldErrorsBuilder.add(new FieldError(fieldError.errorText, fieldError.fieldInFault));
                wasFieldError = true;
            }
            return this;
        }

        public Result<T> build() {
            return new Result<T>(generalError, ImmutableList.copyOf(fieldErrorsBuilder.build()), submitResult);
        }

        public boolean toBuildError() {
            return wasFieldError || generalError != null;
        }

    }

    @Immutable
    final class FieldError {

        public final String errorText;
        public final String fieldInFault;

        private FieldError(String errorText, String fieldInFault) {
            this.errorText = errorText;
            this.fieldInFault = fieldInFault;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FieldError that = (FieldError) o;

            return errorText.equals(that.errorText)
                    && fieldInFault.equals(that.fieldInFault);
        }

        @Override
        public int hashCode() {
            return 31 * errorText.hashCode() + fieldInFault.hashCode();
        }

        @Override
        public String toString() {
            return "FieldError{" +
                    "errorText='" + errorText + '\'' +
                    ", fieldInFault='" + fieldInFault + '\'' +
                    '}';
        }

    }

}
