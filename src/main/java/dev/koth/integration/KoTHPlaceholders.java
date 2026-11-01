package dev.koth.integration;

import dev.koth.KoTHPlugin;
import dev.koth.model.Arena;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class KoTHPlaceholders extends PlaceholderExpansion {
    private final KoTHPlugin plugin;
    public KoTHPlaceholders(KoTHPlugin plugin){this.plugin=plugin;}
    @Override public @NotNull String getIdentifier(){return "koth";}
    @Override public @NotNull String getAuthor(){return "Robblestone Network";}
    @Override public @NotNull String getVersion(){return plugin.getDescription().getVersion();}
    @Override public boolean persist(){return true;}
    @Override public String onPlaceholderRequest(Player player,String params){
        if(params==null)return "";String p=params.toLowerCase(java.util.Locale.ROOT);
        if(p.equals("active"))return plugin.service().activeArena();
        if(p.equals("arena"))return plugin.service().activeArena();
        if(p.equals("next_event"))return plugin.service().nextEvent();
        if(p.equals("capturer")){Arena a=firstActive();return a==null?"---":plugin.service().capturerName(a);}
        if(p.equals("time")){Arena a=firstActive();return a==null?"---":plugin.service().remaining(a);}
        if(p.equals("status")){Arena a=firstActive();return a==null?"Inactive":plugin.service().status(a);}
        if(p.equals("players")){Arena a=firstActive();if(a==null)return "0";int count=0;for(Player online:plugin.getServer().getOnlinePlayers())if(a.contains(online.getLocation()))count++;return Integer.toString(count);}
        if(p.startsWith("arena_")&&p.endsWith("_active")){String n=p.substring(6,p.length()-7);Arena a=plugin.arenas().get(n);return a!=null&&plugin.service().isActive(a)?"true":"false";}
        if(p.startsWith("arena_")&&p.endsWith("_capturer")){String n=p.substring(6,p.length()-9);Arena a=plugin.arenas().get(n);return a==null?"---":plugin.service().capturerName(a);}
        if(p.startsWith("arena_")&&p.endsWith("_time")){String n=p.substring(6,p.length()-5);Arena a=plugin.arenas().get(n);return a==null?"---":plugin.service().remaining(a);}
        return "";
    }
    private Arena firstActive(){for(Arena a:plugin.arenas().values())if(plugin.service().isActive(a))return a;return null;}
}
