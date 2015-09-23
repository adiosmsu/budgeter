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
public interface Submitter {

    Result submit();

    @Immutable
    final class Result {

        public static final Submitter.Result SUCCESS = new Submitter.Result(null, ImmutableList.<FieldError>of());

        @Nullable
        public final String generalError;
        public final ImmutableList<FieldError> fieldErrors;

        private Result(@Nullable String generalError, ImmutableList<FieldError> fieldErrors) {
            this.generalError = generalError;
            this.fieldErrors = fieldErrors;
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
    final class ResultBuilder {

        private ImmutableSet.Builder<FieldError> fieldErrorsBuilder = new ImmutableSet.Builder<FieldError>();
        private boolean wasFieldError = false;
        private String generalError;

        public <T> ResultBuilder addFieldErrorIfAbsent(Optional<T> field, String fieldName) {
            if (!field.isPresent()) {
                addFieldError(fieldName);
            }
            return this;
        }

        public ResultBuilder addFieldErrorIfNull(Object objectReference, String fieldName) {
            if (objectReference == null) {
                addFieldError(fieldName);
            }
            return this;
        }

        public ResultBuilder addFieldError(String fieldInFault) {
            fieldErrorsBuilder.add(new FieldError("Fill in " + fieldInFault, fieldInFault));
            wasFieldError = true;
            return this;
        }

        public ResultBuilder setGeneralError(String generalError) {
            this.generalError = generalError;
            return this;
        }

        public Result build() {
            return new Result(generalError, ImmutableList.copyOf(fieldErrorsBuilder.build()));
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
