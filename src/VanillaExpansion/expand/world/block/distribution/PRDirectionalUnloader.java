package VanillaExpansion.expand.world.block.distribution;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.world.blocks.distribution.DirectionalUnloader;

import static mindustry.Vars.content;

public class PRDirectionalUnloader extends DirectionalUnloader {
    public PRDirectionalUnloader(String name) {
        super(name);
    }

    public class PRDirectionalUnloaderBuild extends DirectionalUnloaderBuild{

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.str(unloadItem == null ? "" : unloadItem.name);
            write.s(unloadItem == null ? -1 : unloadItem.id);
            write.s(offset);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int id = read.s();
            if(revision >= 3){

            } else if(revision >= 2) {
                unloadItem = id == -1 ? null : content.item(id);
                offset = read.s();
            }else{
                unloadItem = id == -1 ? null : content.items().get(id);
                offset = read.s();
            }
        }
    }
}
