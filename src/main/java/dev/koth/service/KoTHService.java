package dev.koth.service;

import dev.koth.KoTHPlugin;
import dev.koth.model.Arena;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import java.time.*;
import java.util.*;

/** Main-thread KoTH state machine. No chunk loading or per-player repeating tasks. */
public final class KoTHService {
    private static final class State { UUID capturer; int captureLeft; int eventLeft; long cooldownUntil; long nextHourlyMillis; boolean hourly; BossBar bar; }
    private final KoTHPlugin plugin; private final Map<String,State> states=new HashMap<>();
    public KoTHService(KoTHPlugin plugin){this.plugin=plugin;}
    private State state(Arena a){return states.computeIfAbsent(a.name.toLowerCase(Locale.ROOT),k->new State());}
    public boolean isActive(Arena a){return state(a).eventLeft!=0;}
    public boolean isHourly(Arena a){return state(a).hourly;}
    public long nextHourlyMillis(Arena a){return state(a).nextHourlyMillis;}
    public void startScheduled(Arena a){start(a);}
    public void start(Arena a){startInternal(a,false);}
    public void startHourly(Arena a){startInternal(a,true);}
    private void startInternal(Arena a,boolean hourly){
        if(!a.enabled){plugin.getLogger().warning("Arena "+a.name+" is disabled.");return;} State s=state(a);
        if(s.eventLeft!=0){if(s.hourly==hourly)return;stop(a,false);}
        s.hourly=hourly;s.eventLeft=hourly?-1:Math.max(1,a.eventSeconds);s.capturer=null;s.captureLeft=0;
        if(hourly)s.nextHourlyMillis=nextHour(System.currentTimeMillis());
        if(plugin.getConfig().getBoolean("settings.bossbar-enabled",true)){s.bar=Bukkit.createBossBar("",BarColor.YELLOW,BarStyle.SOLID);s.bar.setVisible(true);}
        plugin.broadcast("event-start",Map.of("arena",a.name));updateBar(a,s);plugin.refreshHologram(a);
    }
    private long nextHour(long now){ZonedDateTime z=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).withMinute(0).withSecond(0).withNano(0).plusHours(1);return z.toInstant().toEpochMilli();}
    public void stop(Arena a,boolean announce){State s=state(a);boolean was=s.eventLeft!=0;s.eventLeft=0;s.hourly=false;s.nextHourlyMillis=0;s.captureLeft=0;s.capturer=null;if(s.bar!=null){s.bar.removeAll();s.bar=null;}if(announce&&was)plugin.broadcast("event-stop",Map.of("arena",a.name));plugin.refreshHologram(a);}
    public void stopAll(boolean announce){for(Arena a:plugin.arenas().values())stop(a,announce);}
    public void tick(){long now=System.currentTimeMillis();for(Arena a:plugin.arenas().values()){State s=states.get(a.name.toLowerCase(Locale.ROOT));if(s==null||s.eventLeft==0)continue;
        if(s.hourly&&now>=s.nextHourlyMillis){stop(a,true);startHourly(a);continue;}
        Player capturer=s.capturer==null?null:Bukkit.getPlayer(s.capturer);
        if(s.capturer!=null&&(capturer==null||!capturer.isOnline())){s.capturer=null;s.captureLeft=0;capturer=null;}
        if(capturer!=null&&!a.contains(capturer.getLocation())){String n=capturer.getName();s.capturer=null;s.captureLeft=0;if(a.resetOnLeave)plugin.broadcast("capture-left",Map.of("player",n));capturer=null;}
        if(s.capturer==null){for(Player p:Bukkit.getOnlinePlayers()){if(p.hasPermission("koth.play")&&a.contains(p.getLocation())){s.capturer=p.getUniqueId();s.captureLeft=Math.max(1,a.captureSeconds);if(a.announceCapturer)plugin.broadcast("capture-start",Map.of("player",p.getName(),"arena",a.name));break;}}}
        else if(capturer!=null){s.captureLeft--;if(s.captureLeft<=0){win(a,capturer);continue;}}
        if(!s.hourly&&s.eventLeft>0&&--s.eventLeft<=0){stop(a,true);continue;}
        updateBar(a,s);
    }}
    private void win(Arena a,Player p){plugin.broadcast("capture-win",Map.of("player",p.getName(),"arena",a.name));for(String command:a.rewards){String cmd=command.replace("%player%",p.getName()).replace("%arena%",a.name);Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd.startsWith("/")?cmd.substring(1):cmd);}stop(a,false);}
    private void updateBar(Arena a,State s){if(s.bar==null)return;Player p=s.capturer==null?null:Bukkit.getPlayer(s.capturer);String text="&6KoTH &f"+a.name+" &7- "+(p==null?"--- ("+duration(Math.max(1,a.captureSeconds))+")":p.getName()+" ("+duration(s.captureLeft)+")");s.bar.setTitle(KoTHPlugin.color(text));s.bar.setProgress(p==null?1.0:Math.max(0,Math.min(1,s.captureLeft/(double)Math.max(1,a.captureSeconds))));for(Player online:Bukkit.getOnlinePlayers())if(!s.bar.getPlayers().contains(online))s.bar.addPlayer(online);}
    private String duration(int seconds){return (seconds/60)+"m, "+(seconds%60)+"s";}
    public String capturerName(Arena a){State s=state(a);Player p=s.capturer==null?null:Bukkit.getPlayer(s.capturer);return p==null?"---":p.getName();}
    public String remaining(Arena a){State s=state(a);return s.capturer==null?"---":duration(s.captureLeft);}
    public String status(Arena a){State s=state(a);return s.eventLeft==0?"Inactive":s.capturer==null?"Waiting":"Capturing";}
    public String format(int seconds){return duration(Math.max(0,seconds));}
    public String activeArena(){return states.entrySet().stream().filter(e->e.getValue().eventLeft!=0).map(e->e.getKey()).findFirst().orElse("None");}
    public String nextEvent(){return states.values().stream().filter(s->s.hourly&&s.eventLeft!=0).mapToLong(s->s.nextHourlyMillis).min().stream().mapToObj(t->duration((int)Math.max(0,(t-System.currentTimeMillis())/1000))).findFirst().orElse("Not scheduled");}
}
