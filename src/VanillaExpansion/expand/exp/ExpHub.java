package VanillaExpansion.expand.exp;

import arc.struct.IntSeq;
import mindustry.gen.Building;

/** ExpHub 占位实现，待移植完整的中枢物流逻辑 */
public class ExpHub {
    public static class ExpHubBuild extends Building {
        public IntSeq links = new IntSeq();
    }
}
