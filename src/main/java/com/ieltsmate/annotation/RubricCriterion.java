package com.ieltsmate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RubricCriterion {
    String name();

    String task1Name() default "";

    String description();

    double minScore() default 0.0;

    double maxScore() default 9.0;

    double step() default 0.5;
}
