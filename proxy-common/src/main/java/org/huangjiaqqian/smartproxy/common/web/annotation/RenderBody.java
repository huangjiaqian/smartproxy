package org.huangjiaqqian.smartproxy.common.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.huangjiaqqian.smartproxy.common.web.enums.RenderBodyType;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RenderBody {
	RenderBodyType value() default RenderBodyType.TEXT;
}