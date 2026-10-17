package VanillaExpansion.ui.bossrush;

import arc.Events;
import arc.files.Fi;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.game.Saves.SaveSlot;
import mindustry.type.Planet;
import VanillaExpansion.content.VEPlanets;

import java.io.File;

public class BossRushRegistry{
    public static BossRushView view;
    public static BossRushView pendingView;
    public static BossRushNode currentNode;
    public static BossRushData data;
    private static boolean registered = false;

    private static Element hiddenMainView;
    private static Table hiddenButtons;
    private static Table hiddenExpand;

    private static float saveTimer = 0f;
    private static final float SAVE_INTERVAL = 60f * 10f;
    private static Fi currentSaveFile = null;

    public static void register(){
        if(registered) return;
        registered = true;
        //Log.info("BossRushRegistry: register called");

        Events.on(EventType.WinEvent.class, e -> {
            //Log.info("BossRushRegistry: WinEvent fired, currentNode=@", currentNode == null ? "null" : currentNode.id);

            if(currentNode != null){
                saveCurrentProgress();

                BossRushData d = data;
                if(d == null && pendingView != null) d = pendingView.data;
                if(d == null && view != null) d = view.data;

                if(d != null){
                    d.complete(currentNode.id);   // ★ 用 id 定位
                }else{
                    //Log.warn("BossRushRegistry: data is null, cannot complete");
                }

                currentNode = null;
                pendingView = null;
                currentSaveFile = null;
                saveTimer = 0f;
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            if(currentNode == null || currentSaveFile == null) return;
            if(!Vars.state.isGame()) return;
            if(Vars.state.isMenu()) return;

            SaveSlot slot = Vars.control.saves.getCurrent();
            if(slot != null && !currentSaveFile.equals(slot.file)){
                ensureParent(currentSaveFile);
                slot.file = currentSaveFile;
            }

            saveTimer += Time.delta;
            if(saveTimer >= SAVE_INTERVAL){
                saveTimer = 0f;
                saveCurrentProgress();
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            if(Vars.ui.planet == null) return;

            if(!Vars.ui.planet.isShown()){
                if(view != null || hiddenMainView != null || hiddenButtons != null || hiddenExpand != null){
                    cleanup();
                }
                return;
            }

            Planet current = Vars.ui.planet.state.planet;
            boolean bossRushSelected = current == VEPlanets.bossRush;

            if(bossRushSelected && view == null){
                try{
                    Group stack = findStack();
                    if(stack == null){
                        //Log.warn("BossRushRegistry: stack not found");
                        return;
                    }

                    if(stack.getChildren().size >= 5){
                        hiddenMainView = stack.getChildren().get(0);
                        hiddenButtons = (Table) stack.getChildren().get(2);
                        hiddenExpand = (Table) stack.getChildren().get(4);
                        if(hiddenMainView != null) hiddenMainView.visible = false;
                        if(hiddenButtons != null) hiddenButtons.visible = false;
                        if(hiddenExpand != null) hiddenExpand.visible = false;
                    }

                    view = new BossRushView();
                    stack.addChildAt(3, view);
                    //Log.info("BossRushRegistry: view inserted at index 3");
                }catch(Exception e){
                    Log.err(e);
                }
            }else if(!bossRushSelected && view != null){
                cleanup();
            }
        });
    }

    private static void ensureParent(Fi file){
        if(file == null) return;
        Fi parentFi = file.parent();
        if(parentFi == null) return;
        File parent = new File(parentFi.absolutePath());
        if(!parent.exists()){
            parent.mkdirs();
        }
    }

    private static void saveCurrentProgress(){
        if(currentNode == null) return;
        if(!Vars.state.isGame()) return;

        try{
            File nodeDir = new File(Vars.saveDirectory.absolutePath(), "bossrush/" + currentNode.id);
            if(!nodeDir.exists()) nodeDir.mkdirs();

            if(currentSaveFile == null){
                currentSaveFile = Vars.saveDirectory.child("bossrush").child(currentNode.id).child(System.currentTimeMillis() + "." + Vars.saveExtension);
            }

            ensureParent(currentSaveFile);

            SaveSlot slot = Vars.control.saves.getCurrent();
            if(slot != null){
                slot.file = currentSaveFile;
                slot.save();
            }else{
                slot = Vars.control.saves.addSave(currentNode.id);
                slot.file = currentSaveFile;
                slot.save();
            }
        }catch(Exception e){
            //Log.err("BossRushRegistry: failed to save progress", e);
        }
    }

    public static void startNewGame(BossRushNode node, BossRushView parent){
        currentNode = node;
        pendingView = parent;
        saveTimer = 0f;

        File nodeDir = new File(Vars.saveDirectory.absolutePath(), "bossrush/" + node.id);
        if(!nodeDir.exists()) nodeDir.mkdirs();

        Fi nodeDirFi = Vars.saveDirectory.child("bossrush").child(node.id);
        Fi latest = null;
        if(nodeDirFi.exists()){
            Fi[] files = nodeDirFi.list();
            long latestTime = 0;
            for(Fi f : files){
                if(!f.name().endsWith(".msav")) continue;
                if(f.lastModified() > latestTime){
                    latestTime = f.lastModified();
                    latest = f;
                }
            }
        }
        currentSaveFile = latest;

        if(currentSaveFile == null){
            currentSaveFile = nodeDirFi.child(System.currentTimeMillis() + "." + Vars.saveExtension);
        }
    }

    public static void setCurrentSaveFile(Fi file){
        currentSaveFile = file;
        ensureParent(file);
    }

    public static void returnToMenu(){
        cleanup();
        if(Vars.ui.planet != null) Vars.ui.planet.hide();
        Vars.state.set(GameState.State.menu);
    }

    public static void cleanupForLoad(){
        cleanup();
    }

    public static void cleanup(){
        boolean didSomething = false;

        if(view != null && view.parent != null){
            view.remove();
            didSomething = true;
        }
        view = null;

        if(hiddenMainView != null){ hiddenMainView.visible = true; didSomething = true; }
        if(hiddenButtons != null){ hiddenButtons.visible = true; didSomething = true; }
        if(hiddenExpand != null){ hiddenExpand.visible = true; didSomething = true; }
        hiddenMainView = null;
        hiddenButtons = null;
        hiddenExpand = null;

        if(didSomething){
            //Log.info("BossRushRegistry: cleaned up");
        }
    }

    private static Group findStack(){
        for(Element e : Vars.ui.planet.getChildren()){
            if(e instanceof Group){
                Group g = (Group) e;
                if(g.getChildren().size >= 4){
                    return g;
                }
            }
        }
        return null;
    }
}