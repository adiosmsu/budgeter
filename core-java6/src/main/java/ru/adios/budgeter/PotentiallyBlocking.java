package ru.adios.budgeter;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Annotation for marking of core methods.
 * If a method is marked a user (presumable an UI) can see that method must be processed in an async task.
 *
 * Date: 10/24/15
 * Time: 5:17 PM
 *
 * @author Mikhail Kulikov
 */
@Documented
@Retention(CLASS)
@Target({METHOD})
public @interface PotentiallyBlocking {
}
