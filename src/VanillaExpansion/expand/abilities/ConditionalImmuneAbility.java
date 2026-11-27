package VanillaExpansion.expand.abilities;
import arc.struct.Seq;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;
public class ConditionalImmuneAbility extends Ability {
    public ConditionalImmuneAbility() {display = false;}
    public Seq<StatusEffect> immunities = new Seq<>();
    public boolean playerImmune = true;
    @Override
    public void update(Unit u){
        if(playerImmune && u.isPlayer()){
            for(StatusEffect effect : immunities){
                if(u.hasEffect(effect)){
                    u.unapply(effect);
                }
            }
        }
    }
}
