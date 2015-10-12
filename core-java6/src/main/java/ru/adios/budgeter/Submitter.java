package ru.adios.budgeter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java8.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Date: 6/13/15
 * Time: 9:40 PM
 *
 * @author Mikhail Kulikov
 */
public interface Submitter<T> {

    Result<T> submit();

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

    }

    final class SubmitFailure extends RuntimeException {
        private SubmitFailure(String message) {
            super(message);
        }
    }

    @NotThreadSafe
    final class ResultBuilder<T> {

        private static final String FILL_IN_PRE = "Fill in";

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

        public ResultBuilder<T> addFieldError(String fieldInFault) {
            return addFieldError(fieldInFault, FILL_IN_PRE);
        }

        public ResultBuilder<T> addFieldError(String fieldInFault, String predicate) {
            fieldErrorsBuilder.add(new FieldError(predicate + ' ' + fieldInFault, fieldInFault));
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
