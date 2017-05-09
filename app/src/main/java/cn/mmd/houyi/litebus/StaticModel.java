package cn.mmd.houyi.litebus;

import android.util.Log;

import cn.mmd.houyi.bus.annotation.SubscribeStatic;

/**
 * <p>write the description
 *
 * @author houyi
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */


public class StaticModel {
    @SubscribeStatic(type = "action")
    public static void action() {
        Log.d("dddddd", "111111111111111111111111111111111");
    }

    @SubscribeStatic(type = "action")
    public static void action1() {
        Log.d("dddddd", "333333333333333333333333333333");
    }

    @SubscribeStatic(type = "clear")
    public static void clear(String s, boolean b) {
        Log.d("dddddd", s + b);
    }
}
