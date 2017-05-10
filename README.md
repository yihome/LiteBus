# LiteBus
an LiteBus In Android to invoke static method by annotation.The lib can be used in multi-model 
project.


##Simple Usage
use the `@SubscribeStatic` above you target static method,the `type` is the key used to find the 
method, it is necessary.

    @SubscribeStatic(type = "clear")
    public static void clear(String s, boolean b) {
        Log.d("dddddd", s + b);
    }
then where you want to invoke the method, you just call like

    LiteBus.sendAction("clear","2222222222",false);
the first parameter is `type` value same with you set on `@SubscribeStatic`, the others is the 
parameters of the target method.