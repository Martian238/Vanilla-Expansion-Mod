package MultiCraft;

import mindustry.world.blocks.storage.Unloader;

public class ImprovedUnloader extends Unloader {
    public ImprovedUnloader(String name){
        super(name);
    }

    public class ImprovedUnloaderBuild extends UnloaderBuild {

        protected float counter;
        @Override
        public void updateTile() {
            counter += edelta();

            while (counter >= speed) {
                unloadTimer = speed;
                super.updateTile();
                counter -= speed;
            }
        }

    }


}
