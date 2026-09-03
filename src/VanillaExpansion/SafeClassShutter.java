package VanillaExpansion;

import rhino.ClassShutter;

import java.util.Set;

public class SafeClassShutter implements ClassShutter {
    private static final Set<String> ALLOWED = Set.of(
            "java.lang.String",
            "java.lang.Integer",
            "java.lang.Float",
            "java.lang.Math",
            "mindustry.Vars",
            "mindustry.gen.Groups",
            "arc.util.Log",
            "mindustry.type.UnitType"
    );
    @Override
    public boolean visibleToScripts(String className){
        if(className.startsWith("mindustry.")) return true;
        return ALLOWED.contains(className);
    }
}
