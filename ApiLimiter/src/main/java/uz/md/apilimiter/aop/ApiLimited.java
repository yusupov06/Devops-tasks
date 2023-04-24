package uz.md.apilimiter.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLimited {

    /**
     * @return What is this API it is patterned or not
     * ex.: /api/services/1, /api/services/2 apis are one api limit
     */
    String pattern() default "";
}