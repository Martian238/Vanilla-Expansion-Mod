package VanillaExpansion.ui.bossrush;

import VanillaExpansion.VanillaExpansionMod;
import arc.Core;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.maps.Map;

public class BossRushData{
    public Seq<BossRushNode> nodes = new Seq<>();
    public BossRushNode root;

    public void load(){
        Fi modRoot = Vars.mods.getMod(VanillaExpansionMod.class).root;
        if(modRoot == null){
            //Log.err("BossRushData: mod root is null");
            return;
        }

        Fi file = findFile(modRoot, "nodes.json");
        if(file == null){
            file = Core.files.internal("bossrush/nodes.json");
        }

        if(file == null || !file.exists()){
            //Log.err("BossRushData: nodes.json not found");
            return;
        }

        try{
            JsonReader reader = new JsonReader();
            JsonValue json = reader.parse(file.readString());

            JsonValue nodesArr = json.get("nodes");
            if(nodesArr == null){
                //Log.err("BossRushData: nodes array missing");
                return;
            }

            ObjectMap<String, BossRushNode> map = new ObjectMap<>();
            for(JsonValue n : nodesArr){
                BossRushNode node = new BossRushNode();
                node.id = n.getString("id");
                node.titleKey = n.getString("title");
                node.descriptionKey = n.getString("description");
                node.title = Core.bundle.get(node.titleKey, node.titleKey);
                node.description = Core.bundle.get(node.descriptionKey, node.descriptionKey);
                node.imagePath = n.getString("imagePath");
                node.type = BossRushCardType.valueOf(n.getString("type"));
                node.mapPath = n.getString("mapPath", null);

                JsonValue prereq = n.get("prerequisites");
                if(prereq != null){
                    for(JsonValue p : prereq){
                        node.prerequisites.add(p.asString());
                    }
                }

                nodes.add(node);
                map.put(node.id, node);
            }

            for(BossRushNode node : nodes){
                for(String pid : node.prerequisites){
                    BossRushNode parent = map.get(pid);
                    if(parent != null){
                        node.parent = parent;
                        parent.children.add(node);
                    }
                }
            }

            root = nodes.find(n -> n.parent == null);
            if(root == null && nodes.size > 0){
                root = nodes.find(n -> n.prerequisites.size == 0);
            }

            //Log.info("BossRushData: loaded @ nodes, root = @", nodes.size, root == null ? "null" : root.id);
        }catch(Exception e){
            //Log.err("BossRushData: failed to load nodes.json", e);
        }
    }

    public Map findMap(String mapPath){
        if(mapPath == null || mapPath.isEmpty()) return null;

        String fileName = mapPath.substring(mapPath.lastIndexOf('/') + 1);
        String nameNoExt = fileName.endsWith(".msav")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;

        for(Map map : Vars.maps.all()){
            if(map.file.name().equals(fileName)) return map;
            if(map.file.nameWithoutExtension().equals(nameNoExt)) return map;
        }
        return null;
    }

    public void bindMaps(){
        for(BossRushNode node : nodes){
            Map map = null;
            if(node.savedMapPath != null && !node.savedMapPath.isEmpty()){
                map = findMap(node.savedMapPath);
            }
            if(map == null && node.mapPath != null && !node.mapPath.isEmpty()){
                map = findMap(node.mapPath);
            }
            node.map = map;
            Log.info("BossRushData.bindMaps: @ -> @ (saved=@, path=@)",
                    node.id,
                    map == null ? "null" : map.file.name(),
                    node.savedMapPath,
                    node.mapPath);
        }
    }

    public void relocalize(){
        for(BossRushNode node : nodes){
            if(node.titleKey != null) node.title = Core.bundle.get(node.titleKey, node.titleKey);
            if(node.descriptionKey != null) node.description = Core.bundle.get(node.descriptionKey, node.descriptionKey);
        }
    }

    private static Fi findFile(Fi dir, String name){
        if(dir == null || !dir.exists() || !dir.isDirectory()) return null;
        for(Fi f : dir.list()){
            if(f.isDirectory()){
                Fi found = findFile(f, name);
                if(found != null) return found;
            }else if(f.name().equals(name)){
                return f;
            }
        }
        return null;
    }

    public void loadProgress(){
        ObjectMap<String, Boolean> completed = Core.settings.getJson("bossrush-completed", ObjectMap.class, ObjectMap::new);
        ObjectMap<String, String> savedMaps = Core.settings.getJson("bossrush-savedmaps", ObjectMap.class, ObjectMap::new);

        for(BossRushNode node : nodes){
            node.completed = completed.get(node.id, false);
            String saved = savedMaps.get(node.id);
            if(saved != null && !saved.equals(node.mapPath) && findMap(saved) != null){
                node.savedMapPath = saved;
            }else{
                node.savedMapPath = null;
            }
        }

        updateUnlocks();
    }

    public void save(){
        ObjectMap<String, Boolean> completed = new ObjectMap<>();
        ObjectMap<String, String> savedMaps = new ObjectMap<>();

        for(BossRushNode node : nodes){
            completed.put(node.id, node.completed);
            if(node.savedMapPath != null && !node.savedMapPath.equals(node.mapPath)){
                savedMaps.put(node.id, node.savedMapPath);
            }
        }

        Core.settings.putJson("bossrush-completed", ObjectMap.class, completed);
        Core.settings.putJson("bossrush-savedmaps", ObjectMap.class, savedMaps);
        Core.settings.forceSave();
    }
    public void updateUnlocks(){
        for(BossRushNode node : nodes){
            if(node.parent == null){
                node.unlocked = true;
            }else{
                BossRushNode p = node.parent;
                boolean parentDone = p.completed || (p.mapPath == null || p.mapPath.isEmpty());
                node.unlocked = parentDone;
            }
        }
        // root 始终解锁
        if(root != null) root.unlocked = true;
    }
    public void complete(String nodeId){
        BossRushNode node = nodes.find(n -> n.id.equals(nodeId));
        if(node == null){
            //Log.warn("BossRushData: complete: node @ not found", nodeId);
            return;
        }
        node.completed = true;
        save();
        updateUnlocks();

        //Log.info("BossRushData: completed @, nodes:", nodeId);
        for(BossRushNode n : nodes){
            //Log.info("  node @: unlocked=@, completed=@", n.id, n.unlocked, n.completed);
        }
    }

    public void complete(BossRushNode node){
        if(node != null) complete(node.id);
    }

    public void clearProgress(){
        for(BossRushNode node : nodes){
            node.completed = false;
            node.savedMapPath = null;
            node.map = null;
        }
        updateUnlocks();

        Core.settings.remove("bossrush-completed");
        Core.settings.remove("bossrush-savedmaps");
        Core.settings.forceSave();
        try{
            java.io.File bossrushDir = new java.io.File(Vars.saveDirectory.absolutePath(), "bossrush");
            if(bossrushDir.exists()){
                deleteDir(bossrushDir);
                //Log.info("BossRushData: deleted save dir @", bossrushDir.getAbsolutePath());
            }
        }catch(Exception e){
            //Log.err("BossRushData: failed to delete save dir", e);
        }

        //Log.info("BossRushData: cleared all progress");
        for(BossRushNode n : nodes){
            //Log.info("  node @: unlocked=@, completed=@", n.id, n.unlocked, n.completed);
        }
    }

    private static void deleteDir(java.io.File dir){
        java.io.File[] files = dir.listFiles();
        if(files != null){
            for(java.io.File f : files){
                if(f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}