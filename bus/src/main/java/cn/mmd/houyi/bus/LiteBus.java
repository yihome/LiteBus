package cn.mmd.houyi.bus;

/**
 * <p>write the description
 *
 * @author houyi
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */


public class LiteBus {
    /**
     * send an action with a type, the static method annotated with the
     * same type will response the action.
     *
     * @param type type of the action
     * @param args args of the response method
     */
    public static void sendAction(String type, Object... args) {
        ActionFactory.sendAction(type, args);
    }
}
