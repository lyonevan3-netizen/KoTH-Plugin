package dev.koth.integration;

import dev.koth.KoTHPlugin;
import dev.koth.model.Arena;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Locale;

public final class KoTHPlaceholders extends PlaceholderExpansion {
    private final KoTHPlugin plugin;
    public KoTHPlaceholders(KoTHPlugin plugin){this.plugin=plugin;}
    @Override public @NotNull String getIdentifier(){return "koth";}
    @Override public @NotNull String getAuthor(){return "Robblestone Network";}
    @Override public @NotNull String getVersion(){return plugin.getDescription().getVersion();}
    @Override public boolean persist(){return true;}

    @Override public String onPlaceholderRequest(Player player,String params){
        if(params==null)return "";
        String p=params.toLowerCase(Locale.ROOT);
        Arena active=firstActive();
        if(p.equals("active")||p.equals("arena"))return active==null?"None":active.name;
        if(p.equals("next_event")||p.equals("event_time")||p.equals("next_start")||p.equals("time_until_next"))return plugin.nextEventCountdown();
        if(p.equals("next_event_seconds")){long t=plugin.nextEventMillis();return t==Long.MAX_VALUE?"-1":Long.toString(Math.max(0,(t-System.currentTimeMillis()+999)/1000));}
        if(p.equals("next_event_minutes")){long t=plugin.nextEventMillis();return t==Long.MAX_VALUE?"-1":Long.toString(Math.max(0,(t-System.currentTimeMillis()+59999)/60000));}
        if(p.equals("next_arena")||p.equals("next_event_arena"))return plugin.nextArenaName();
        if(p.equals("capturer"))return active==null?"---":plugin.service().capturerName(active);
        if(p.equals("time")||p.equals("capture_time"))return active==null?"---":plugin.service().remaining(active);
        if(p.equals("status"))return active==null?"Inactive":plugin.service().status(active);
        if(p.equals("players"))return active==null?"0":Integer.toString(playersIn(active));
        if(p.equals("wins")||p.equals("player_wins"))return player==null?"0":Integer.toString(plugin.wins(player.getUniqueId()));
        if(p.startsWith("wins_"))return Integer.toString(plugin.winsByName(params.substring(5)));
        if(p.startsWith("top_")&&p.endsWith("_name")){int rank=parseRank(p.substring(4,p.length()-5));return plugin.topName(rank);}
        if(p.startsWith("top_")&&p.endsWith("_wins")){int rank=parseRank(p.substring(4,p.length()-5));return Integer.toString(plugin.topWins(rank));}
        if(p.startsWith("arena_"))return arenaPlaceholder(p.substring(6),player);
        return "";
    }
    private String arenaPlaceholder(String value,Player player){
        String[] suffixes={"_active","_capturer","_time","_status","_players","_next_event","_next_start","_wins"};
        for(String suffix:suffixes){if(value.endsWith(suffix)){String name=value.substring(0,value.length()-suffix.length());Arena a=plugin.arenas().get(name);if(a==null)return "---";
            return switch(suffix){case "_active"->plugin.service().isActive(a)?"true":"false";case "_capturer"->plugin.service().capturerName(a);case "_time"->plugin.service().remaining(a);case "_status"->plugin.service().status(a);case "_players"->Integer.toString(playersIn(a));case "_wins"->player==null?"0":Integer.toString(plugin.wins(player.getUniqueId(),a.name));case "_next_event","_next_start"->{long t=Math.min(plugin.service().nextEventMillis(a),plugin.nextScheduledMillis(a));if(t==Long.MAX_VALUE)yield "Not scheduled";yield plugin.service().format((int)Math.max(0,(t-System.currentTimeMillis()+999)/1000));}default->"---";};
        }}return "";
    }
    private int playersIn(Arena a){int count=0;for(Player online:plugin.getServer().getOnlinePlayers())if(a.contains(online.getLocation()))count++;return count;}
    private int parseRank(String s){try{return Integer.parseInt(s);}catch(NumberFormatException e){return -1;}}
    private Arena firstActive(){for(Arena a:plugin.arenas().values())if(plugin.service().isActive(a))return a;return null;}
}
