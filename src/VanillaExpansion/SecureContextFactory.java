package VanillaExpansion;

import rhino.Context;
import rhino.ContextFactory;
import rhino.Scriptable;

public class SecureContextFactory extends ContextFactory {
    @Override
    protected Context makeContext(){
        Context cx = super.makeContext();
        cx.setClassShutter(new SafeClassShutter());
        return cx;
    }

    @Override
    protected void onContextCreated(Context cx){
        super.onContextCreated(cx);
        try{
            java.lang.reflect.Method getScopeMethod = Context.class.getDeclaredMethod("getCurrentScope");
            getScopeMethod.setAccessible(true);
            Scriptable scope = (Scriptable) getScopeMethod.invoke(cx);
            if(scope != null){
                scope.delete("Packages");
                scope.delete("java");
                scope.delete("javax");
                scope.delete("org");
                scope.delete("com");
                scope.delete("Java");
                scope.delete("JavaAdapter");
                scope.delete("JavaImporter");

                Scriptable classProto = (Scriptable) scope.get("Class", scope);
                if(classProto != null){
                    classProto.delete("forName");
                    classProto.delete("getDeclaredField");
                    classProto.delete("getDeclaredMethod");
                    classProto.delete("getMethod");
                    classProto.delete("getField");
                    classProto.delete("getDeclaredConstructor");
                }
            }
        }catch(Exception ignored){}
    }
}
