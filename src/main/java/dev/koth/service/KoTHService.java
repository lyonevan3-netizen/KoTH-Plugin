package dev.koth.service;

import dev.koth.KoTHPlugin;
import dev.koth.model.Arena;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class KoTHService {
    private static final class State { UUID capturer; int captureLeft; int eventLeft; long cooldownUntil; BossBar bar; }
    private final KoTHPlugin plugin; private final Map<String,State> states=new HashMap<>();
    public KoTHService(KoTHPlugin plugin){this.plugin=plugin;}
    private State state(Arena a){return states.computeIfAbsent(a.name,k->new State());}
    public boolean isActive(Arena a){return state(a).eventLeft>0;}
    public void startScheduled(Arena a){State s=state(a);if(System.currentTimeMillis()<s.cooldownUntil)return;start(a);}
    public void start(Arena a){if(!a.enabled){plugin.getLogger().info("Arena "+a.name+" is disabled.");return;}State s=state(a);if(s.eventLeft>0)return;s.eventLeft=a.eventSeconds;s.capturer=null;s.captureLeft=0;if(plugin.getConfig().getBoolean("settings.bossbar-enabled",true)){s.bar=Bukkit.createBossBar(KoTHPlugin.color("&6KoTH &f"+a.name+" &7- Waiting for a player"),BarColor.YELLOW,BarStyle.SOLID);s.bar.setVisible(true);for(Player p:Bukkit.getOnlinePlayers())s.bar.addPlayer(p);}plugin.broadcast("event-start",Map.of("arena",a.name));plugin.refreshHologram(a);}
    public void stop(Arena a,boolean announce){State s=state(a);if(s.eventLeft>0)s.cooldownUntil=System.currentTimeMillis()+a.cooldownSeconds*1000L;s.eventLeft=0;s.captureLeft=0;s.capturer=null;if(s.bar!=null){s.bar.removeAll();s.bar=null;}if(announce)plugin.broadcast("event-stop",Map.of("arena",a.name));plugin.refreshHologram(a);}
    public void stopAll(boolean announce){for(Arena a:plugin.arenas().values())stop(a,announce);}
    public void tick(){for(Arena a:plugin.arenas().values()){State s=state(a);if(s.eventLeft<=0)continue;s.eventLeft--;Player capturer=s.capturer==null?null:Bukkit.getPlayer(s.capturer);
            if(s.capturer!=null&&capturer==null){s.capturer=null;s.captureLeft=0;}
            if(capturer!=null&&!a.contains(capturer.getLocation())){String name=capturer.getName();s.capturer=null;s.captureLeft=0;if(a.resetOnLeave)plugin.broadcast("capture-left",Map.of("player",name));capturer=null;}
            if(s.capturer==null){for(Player p:Bukkit.getOnlinePlayers()){if(p.hasPermission("koth.play")&&a.contains(p.getLocation())){s.capturer=p.getUniqueId();s.captureLeft=a.captureSeconds;if(a.announceCapturer)plugin.broadcast("capture-start",Map.of("player",p.getName(),"arena",a.name));break;}}}
            else if(capturer!=null){s.captureLeft--;if(s.captureLeft<=0){win(a,capturer);continue;}}
            if(s.eventLeft<=0){stop(a,true);continue;}
            updateBar(a,s);plugin.refreshHologram(a);
        }}
    private void win(Arena a,Player p){plugin.broadcast("capture-win",Map.of("player",p.getName(),"arena",a.name));for(String command:a.rewards){String cmd=command.replace("%player%",p.getName()).replace("%arena%",a.name);Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd.startsWith("/")?cmd.substring(1):cmd);}stop(a,false);}
    private void updateBar(Arena a,State s){if(s.bar==null)return;Player p=s.capturer==null?null:Bukkit.getPlayer(s.capturer);String text=p==null?"&6KoTH &f"+a.name+" &7- Waiting for a player":"&6KoTH &f"+a.name+" &7- "+p.getName()+" &f"+format(s.captureLeft);s.bar.setTitle(KoTHPlugin.color(text));s.bar.setProgress(Math.max(0,Math.min(1,p==null?s.eventLeft/(double)Math.max(1,a.eventSeconds):s.captureLeft/(double)Math.max(1,a.captureSeconds))));for(Player online:Bukkit.getOnlinePlayers())if(!s.bar.getPlayers().contains(online))s.bar.addPlayer(online);}
    public String capturerName(Arena a){State s=state(a);Player p=s.capturer==null?null:Bukkit.getPlayer(s.capturer);return p==null?"Nobody":p.getName();}
    public String remaining(Arena a){State s=state(a);return s.capturer==null?"--:--":format(s.captureLeft);}
    public String format(int seconds){return String.format(Locale.ROOT,"%02d:%02d",Math.max(0,seconds)/60,Math.max(0,seconds)%60);}
}
