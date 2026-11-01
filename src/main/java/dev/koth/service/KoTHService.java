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
    private static final class State { UUID capturer; int captureLeft; int eventLeft; long nextHourlyMillis; boolean hourly; BossBar bar; }
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
        if(!a.enabled){plugin.getLogger().warning("Arena "+a.name+" is disabled.");return;}
        State s=state(a);
        if(s.eventLeft!=0 && s.hourly==hourly)return;
        if(s.eventLeft!=0)stop(a,false);
        s.hourly=hourly;
        s.eventLeft=hourly?-1:Math.max(1,a.eventSeconds);
        s.capturer=null;s.captureLeft=0;
        if(hourly && s.nextHourlyMillis<=System.currentTimeMillis())s.nextHourlyMillis=nextHour(System.currentTimeMillis());
        if(plugin.getConfig().getBoolean("settings.bossbar-enabled",true)){s.bar=Bukkit.createBossBar("",BarColor.YELLOW,BarStyle.SOLID);s.bar.setVisible(true);for(Player online:Bukkit.getOnlinePlayers())s.bar.addPlayer(online);}
        plugin.broadcast("event-start",Map.of("arena",a.name));updateBar(a,s);plugin.refreshHologram(a);
    }
    private long nextHour(long now){ZonedDateTime z=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).withMinute(0).withSecond(0).withNano(0).plusHours(1);return z.toInstant().toEpochMilli();}
    public void stop(Arena a,boolean announce){State s=state(a);boolean was=s.eventLeft!=0;s.eventLeft=0;s.hourly=false;s.nextHourlyMillis=0;s.captureLeft=0;s.capturer=null;removeBar(s);if(announce&&was)plugin.broadcast("event-stop",Map.of("arena",a.name));plugin.refreshHologram(a);}
    private void removeBar(State s){if(s.bar!=null){s.bar.removeAll();s.bar=null;}}
    private void finishHourlyWin(Arena a,State s){s.eventLeft=0;s.capturer=null;s.captureLeft=0;removeBar(s);plugin.refreshHologram(a);}
    public void stopAll(boolean announce){for(Arena a:plugin.arenas().values())stop(a,announce);}
    public void tick(){long now=System.currentTimeMillis();for(Arena a:plugin.arenas().values()){State s=states.get(a.name.toLowerCase(Locale.ROOT));if(s==null)continue;
        if(s.hourly&&s.nextHourlyMillis>0&&now>=s.nextHourlyMillis){boolean wasActive=s.eventLeft!=0;if(wasActive)stopForHourlyRestart(a,s);s.nextHourlyMillis=nextHour(now);startHourlyRound(a,s);continue;}
        if(s.eventLeft==0)continue;
        Player capturer=s.capturer==null?null:Bukkit.getPlayer(s.capturer);
        if(s.capturer!=null&&(capturer==null||!capturer.isOnline())){s.capturer=null;s.captureLeft=0;capturer=null;}
        if(capturer!=null&&!a.contains(capturer.getLocation())){String n=capturer.getName();s.capturer=null;s.captureLeft=0;if(a.resetOnLeave)plugin.broadcast("capture-left",Map.of("player",n));capturer=null;}
        if(s.capturer==null){for(Player p:Bukkit.getOnlinePlayers()){if(p.hasPermission("koth.play")&&a.contains(p.getLocation())){s.capturer=p.getUniqueId();s.captureLeft=Math.max(1,a.captureSeconds);if(a.announceCapturer)plugin.broadcast("capture-start",Map.of("player",p.getName(),"arena",a.name));break;}}}
        else if(capturer!=null){s.captureLeft--;if(s.captureLeft<=0){win(a,capturer);continue;}}
        if(!s.hourly&&s.eventLeft>0&&--s.eventLeft<=0){stop(a,true);continue;}
        updateBar(a,s);
    }}
    private void win(Arena a,Player p){plugin.broadcast("capture-win",Map.of("player",p.getName(),"arena",a.name));for(String command:a.rewards){String cmd=command.replace("%player%",p.getName()).replace("%arena%",a.name);Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd.startsWith("/")?cmd.substring(1):cmd);}plugin.recordWin(p.getUniqueId(),p.getName(),a.name);State s=state(a);if(s.hourly)finishHourlyWin(a,s);else stop(a,false);}
    private void stopForHourlyRestart(Arena a,State s){boolean was=s.eventLeft!=0;s.eventLeft=0;s.capturer=null;s.captureLeft=0;removeBar(s);if(was)plugin.broadcast("event-stop",Map.of("arena",a.name));}
    private void startHourlyRound(Arena a,State s){if(!a.enabled)return;s.hourly=true;s.eventLeft=-1;s.capturer=null;s.captureLeft=0;if(plugin.getConfig().getBoolean("settings.bossbar-enabled",true)){s.bar=Bukkit.createBossBar("",BarColor.YELLOW,BarStyle.SOLID);s.bar.setVisible(true);for(Player online:Bukkit.getOnlinePlayers())s.bar.addPlayer(online);}plugin.broadcast("event-start",Map.of("arena",a.name));updateBar(a,s);plugin.refreshHologram(a);}
    private void updateBar(Arena a,State s){if(s.bar==null)return;Player p=s.capturer==null?null:Bukkit.getPlayer(s.capturer);String text="&6KoTH &f"+a.name+" &7- "+(p==null?"--- ("+duration(Math.max(1,a.captureSeconds))+")":p.getName()+" ("+duration(s.captureLeft)+")");s.bar.setTitle(KoTHPlugin.color(text));s.bar.setProgress(p==null?1.0:Math.max(0,Math.min(1,s.captureLeft/(double)Math.max(1,a.captureSeconds))));}
    public void addPlayerToBars(Player player){for(State s:states.values())if(s.eventLeft!=0&&s.bar!=null&&!s.bar.getPlayers().contains(player))s.bar.addPlayer(player);}
    public void removePlayerFromBars(Player player){for(State s:states.values())if(s.bar!=null)s.bar.removePlayer(player);}
    private String duration(int seconds){return (seconds/60)+"m, "+(seconds%60)+"s";}
    public String capturerName(Arena a){State s=state(a);Player p=s.capturer==null?null:Bukkit.getPlayer(s.capturer);return p==null?"---":p.getName();}
    public String remaining(Arena a){State s=state(a);return s.capturer==null?"---":duration(s.captureLeft);}
    public String status(Arena a){State s=state(a);return s.eventLeft==0?"Inactive":s.capturer==null?"Waiting":"Capturing";}
    public String format(int seconds){return duration(Math.max(0,seconds));}
    public String activeArena(){return states.entrySet().stream().filter(e->e.getValue().eventLeft!=0).map(e->e.getKey()).findFirst().orElse("None");}
    public long nextEventMillis(Arena a){State s=states.get(a.name.toLowerCase(Locale.ROOT));return s!=null&&s.hourly?s.nextHourlyMillis:Long.MAX_VALUE;}
    public long nextEventMillis(){return states.values().stream().filter(s->s.hourly&&s.nextHourlyMillis>0).mapToLong(s->s.nextHourlyMillis).min().orElse(Long.MAX_VALUE);}
    public String nextEvent(){long t=nextEventMillis();return t==Long.MAX_VALUE?"Not scheduled":duration((int)Math.max(0,(t-System.currentTimeMillis()+999)/1000));}
}
