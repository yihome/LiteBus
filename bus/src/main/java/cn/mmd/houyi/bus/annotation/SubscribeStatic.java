package cn.mmd.houyi.bus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>write the description
 *
 * @author houyi
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface SubscribeStatic {
    public String action();
}
