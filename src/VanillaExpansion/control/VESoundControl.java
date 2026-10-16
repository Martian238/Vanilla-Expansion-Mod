package VanillaExpansion.control;

import arc.*;
import arc.audio.*;
import arc.audio.Filters.*;
import arc.files.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.audio.AmbientSource;
import mindustry.audio.MusicContainer;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;

/** Controls playback of multiple audio tracks.*/
public class VESoundControl{
    protected Interval timer = new Interval(4);

    protected @Nullable AudioThread ambientThread;
    protected Seq<SoundData> localData = new Seq<>();

    protected ObjectMap<Sound, SoundData> sounds = new ObjectMap<>();

    public VESoundControl(){
    }

    /** Update and play the right music track.*/
    public void update(){
        updateLoops();
    }

    //loop system

    public void loop(Sound sound, float volume){
        if(Vars.headless) return;

        loop(sound, Core.camera.position, volume);
    }

    public void loop(Sound sound, Position pos, float volume){
        loop(sound, pos, volume, 1f);
    }

    public void loop(Sound sound, Position pos, float volume, float pitch){
        if(Vars.headless || sound == Sounds.none || volume <= 0.00001f) return;

        loop(sounds.get(sound, SoundData::new), sound, pos, volume, pitch);
    }

    static void loop(SoundData data, Sound sound, Position pos, float volume, float pitch){
        float baseVol = sound.calcFalloff(pos.getX(), pos.getY());
        float vol = baseVol * volume;

        data.volume += vol;
        data.pitch += pitch * vol;
        data.volume = Mathf.clamp(data.volume, 0f, 1f);
        data.total += baseVol;
        data.totalVolume += vol;
        data.sumX += pos.getX() * baseVol;
        data.sumY += pos.getY() * baseVol;
    }

    protected void updateLoops(){
        //clear loops when in menu
        if(!state.isGame()){
            sounds.clear();
            return;
        }

        if(state.isPaused()) return;

        float avol = Core.settings.getInt("ambientvol", 100) / 100f;

        sounds.each((sound, data) -> {
            data.curVolume = Mathf.lerpDelta(data.curVolume, data.volume * avol, 1f);

            boolean play = data.curVolume > 0.001f;
            float pan = Mathf.zero(data.total, 0.0001f) ? 0f : sound.calcPan(data.sumX / data.total, data.sumY / data.total);
            float pitch = Mathf.zero(data.totalVolume, 0.0001f) ? 1f : data.pitch / data.totalVolume;
            if(data.soundID <= 0 || !Core.audio.isPlaying(data.soundID)){
                if(play){
                    data.soundID = sound.loop(data.curVolume, pitch, pan);
                    Core.audio.protect(data.soundID, true);
                }
            }else{
                if(data.curVolume <= 0.001f){
                    sound.stop();
                    data.soundID = -1;
                    return;
                }
                Core.audio.set(data.soundID, pan, data.curVolume);
                if(!Mathf.equal(pitch, 1f, 0.001f)){
                    Core.audio.setPitch(data.soundID, pitch);
                }
            }

            data.pitch = 0f;
            data.volume = 0f;
            data.total = 0f;
            data.totalVolume = 0f;
            data.sumX = 0f;
            data.sumY = 0f;
        });

        //grab data from ambient thread
        if(ambientThread != null){
            synchronized(ambientThread.outputData){
                localData.set(ambientThread.outputData);
            }

            for(var data : localData){
                var target = sounds.get(data.sound, SoundData::new);

                target.pitch = data.pitch;
                target.volume = data.volume;
                target.total = data.total;
                target.totalVolume = data.totalVolume;
                target.sumX = data.sumX;
                target.sumY = data.sumY;
            }
        }
    }

    protected static class SoundData implements Cloneable{
        float volume, pitch;
        float total;
        float sumX, sumY;

        int soundID;
        float curVolume, totalVolume;
        Sound sound;

        @Override
        public SoundData clone(){
            try{
                return (SoundData)super.clone();
            }catch(CloneNotSupportedException e){
                throw new AssertionError("death");
            }
        }
    }

    static class AudioThread extends Thread{
        static final int targetFps = 20;
        static final long targetNanos = Time.millisToNanos(1000) / targetFps;

        volatile boolean running = true;
        Seq<AmbientSource> sources = new Seq<>(false, 16, AmbientSource.class);

        LinkedBlockingQueue<AmbientSource> inputSources = new LinkedBlockingQueue<>();
        ObjectMap<Sound, SoundData> sounds = new ObjectMap<>();

        //buffer that is built up on the audio thread
        final Seq<SoundData> localData = new Seq<>();
        //buffer for output sound data
        final Seq<SoundData> outputData = new Seq<>();

        void doLoop(){
            var items = sources.items;
            int size = sources.size;
            localData.size = 0;

            AmbientSource source;
            while((source = inputSources.poll()) != null){
                sources.add(source);
            }

            for(int i = 0; i < size; i++){
                var item = items[i];
                if(item.isValid()){
                    if(item.shouldAmbientSound()){
                        float volume = item.getAmbientVolume();
                        if(volume > 0.00001f){

                            Sound sound = item.getAmbientSound();
                            var data = sounds.get(sound, SoundData::new);
                            data.sound = sound;

                            boolean silent = data.volume == 0f;

                            loop(data, sound, item, volume, 1f);

                            if(silent && data.volume > 0f){
                                localData.add(data);
                            }
                        }
                    }
                }else{
                    sources.remove(i); //unordered swap
                    i --;
                    size --;
                }
            }

            //copy built-up data to output buffer
            synchronized(outputData){
                outputData.size = 0;
                for(var data : localData){

                    //this allocates, which is bad, but it only happens at 20fps with a few audio types at most
                    outputData.add(data.clone());
                }
            }

            //reset data for next iteration
            sounds.each((sound, data) -> {
                data.pitch = 0f;
                data.volume = 0f;
                data.total = 0f;
                data.totalVolume = 0f;
                data.sumX = 0f;
                data.sumY = 0f;
            });
        }

        @Override
        public void run(){
            try{
                while(running){
                    long nanos = Time.nanos();

                    if(state.isMenu()) break;
                    if(state.isPlaying()){
                        doLoop();
                    }

                    long elapsed = Time.timeSinceNanos(nanos);
                    if(elapsed < targetNanos){
                        long remaining = targetNanos - elapsed;
                        Thread.sleep(remaining / Time.nanosPerMilli, (int)(remaining % Time.nanosPerMilli));
                    }
                }
            }catch(InterruptedException e){
                //exit
            }
        }
    }
}
