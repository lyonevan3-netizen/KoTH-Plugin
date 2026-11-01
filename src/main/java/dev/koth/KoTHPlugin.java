package dev.koth;

import dev.koth.model.Arena;
import dev.koth.service.KoTHService;
import dev.koth.gui.KoTHGui;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public final class KoTHPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final Map<String,Arena> arenas=new LinkedHashMap<>();
    private final Map<UUID,Location[]> selections=new HashMap<>();
    private File arenasFile; private YamlConfiguration arenasYaml; private File statsFile; private YamlConfiguration statsYaml;
    private KoTHService service; private KoTHGui gui;
    private final Set<String> firedSchedules=new HashSet<>();
    private boolean statsDirty;
    private List<Map.Entry<String,Integer>> leaderboardCache=List.of();
    private boolean leaderboardDirty=true;

    @Override public void onEnable(){
        saveDefaultConfig(); arenasFile=new File(getDataFolder(),"arenas.yml"); statsFile=new File(getDataFolder(),"stats.yml"); loadStats(); loadArenas();
        service=new KoTHService(this); gui=new KoTHGui(this);
        Objects.requireNonNull(getCommand("koth")).setExecutor(this); getCommand("koth").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this,this);
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI")!=null){new dev.koth.integration.KoTHPlaceholders(this).register();getLogger().info("PlaceholderAPI expansion registered.");}
        // One synchronous heartbeat avoids maintaining multiple repeating tasks.
        Bukkit.getScheduler().runTaskTimer(this,()->{service.tick();checkSchedules();},20L,20L);
        // Batch disk writes instead of saving stats synchronously on every capture.
        Bukkit.getScheduler().runTaskTimer(this,this::flushStats,1200L,1200L);
        getLogger().info("KoTH enabled with "+arenas.size()+" arena(s).");
    }
    @Override public void onDisable(){if(service!=null)service.stopAll(false); saveArenas(); flushStats();}
    public Map<String,Arena> arenas(){return arenas;} public KoTHService service(){return service;} public KoTHGui gui(){return gui;}
    public String msg(String key){return color(getConfig().getString("messages.prefix","&6KoTH &8» &7")+getConfig().getString("messages."+key,key));}
    public static String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    public void tell(CommandSender s,String text){s.sendMessage(color(getConfig().getString("messages.prefix","&6KoTH &8» &7")+text));}
    public void broadcast(String key,Map<String,String> vars){String s=getConfig().getString("messages."+key,key);for(var e:vars.entrySet())s=s.replace("%"+e.getKey()+"%",e.getValue());Bukkit.broadcastMessage(color(getConfig().getString("messages.prefix","&6KoTH &8» &7")+s));}

    private void loadStats(){if(!statsFile.exists())try{getDataFolder().mkdirs();statsFile.createNewFile();}catch(IOException e){getLogger().severe(e.getMessage());}statsYaml=YamlConfiguration.loadConfiguration(statsFile);}
    public void recordWin(UUID id,String name,String arena){String key=id.toString();statsYaml.set("players."+key+".name",name);statsYaml.set("players."+key+".wins",statsYaml.getInt("players."+key+".wins")+1);String arenaKey=arena.toLowerCase(Locale.ROOT);statsYaml.set("players."+key+".arena-wins."+arenaKey,statsYaml.getInt("players."+key+".arena-wins."+arenaKey)+1);statsDirty=true;leaderboardDirty=true;}
    public int wins(UUID id){return statsYaml==null?0:statsYaml.getInt("players."+id+".wins");}
    public int wins(UUID id,String arena){return statsYaml==null?0:statsYaml.getInt("players."+id+".arena-wins."+arena.toLowerCase(Locale.ROOT));}
    public int winsByName(String name){if(statsYaml==null)return 0;ConfigurationSection players=statsYaml.getConfigurationSection("players");if(players==null)return 0;for(String id:players.getKeys(false))if(players.getString(id+".name","").equalsIgnoreCase(name))return players.getInt(id+".wins");return 0;}
    private void rebuildLeaderboard(){if(!leaderboardDirty)return;leaderboardDirty=false;List<Map.Entry<String,Integer>> rows=new ArrayList<>();ConfigurationSection players=statsYaml==null?null:statsYaml.getConfigurationSection("players");if(players!=null)for(String id:players.getKeys(false))rows.add(Map.entry(statsYaml.getString("players."+id+".name","---"),statsYaml.getInt("players."+id+".wins")));rows.sort((a,b)->Integer.compare(b.getValue(),a.getValue()));leaderboardCache=List.copyOf(rows);}
    public String topName(int rank){rebuildLeaderboard();return rank<1||rank>leaderboardCache.size()?"---":leaderboardCache.get(rank-1).getKey();}
    public int topWins(int rank){rebuildLeaderboard();return rank<1||rank>leaderboardCache.size()?0:leaderboardCache.get(rank-1).getValue();}
    private void flushStats(){if(!statsDirty||statsYaml==null)return;try{statsYaml.save(statsFile);statsDirty=false;}catch(IOException e){getLogger().severe("Could not save stats: "+e.getMessage());}}
    public long nextScheduledMillis(Arena a){if(!a.enabled||a.schedule==null||a.schedule.isBlank())return Long.MAX_VALUE;ZonedDateTime current=ZonedDateTime.now();String[] bits=a.schedule.split(" ");if(bits.length!=2)return Long.MAX_VALUE;try{DayOfWeek day=DayOfWeek.valueOf(bits[0].toUpperCase(Locale.ROOT));LocalTime time=LocalTime.parse(bits[1]);ZonedDateTime candidate=current.with(TemporalAdjusters.nextOrSame(day)).withHour(time.getHour()).withMinute(time.getMinute()).withSecond(0).withNano(0);if(!candidate.isAfter(current))candidate=candidate.plusWeeks(1);return candidate.toInstant().toEpochMilli();}catch(Exception ignored){return Long.MAX_VALUE;}}
    public long nextScheduledMillis(){long best=Long.MAX_VALUE;for(Arena a:arenas.values())best=Math.min(best,nextScheduledMillis(a));return best;}
    public long nextEventMillis(){long a=service==null?Long.MAX_VALUE:service.nextEventMillis();return Math.min(a,nextScheduledMillis());}
    public String nextEventCountdown(){long t=nextEventMillis();if(t==Long.MAX_VALUE)return "Not scheduled";long seconds=Math.max(0,(t-System.currentTimeMillis()+999)/1000);return service==null?seconds/60+"m, "+seconds%60+"s":service.format((int)seconds);}
    public String nextArenaName(){long target=nextEventMillis();if(target==Long.MAX_VALUE)return "---";for(Arena a:arenas.values()){if(service!=null&&service.nextEventMillis(a)==target)return a.name;if(nextScheduledMillis(a)==target)return a.name;}return "---";}

    private void loadArenas(){
        if(!arenasFile.exists())try{getDataFolder().mkdirs();arenasFile.createNewFile();}catch(IOException e){getLogger().severe(e.getMessage());}
        arenasYaml=YamlConfiguration.loadConfiguration(arenasFile);
        arenas.clear();
        ConfigurationSection root=arenasYaml.getConfigurationSection("arenas"); if(root==null)return;
        for(String n:root.getKeys(false)){ConfigurationSection c=root.getConfigurationSection(n);if(c==null)continue;Arena a=new Arena(n);
            a.world=c.getString("world");a.x1=c.getInt("pos1.x");a.y1=c.getInt("pos1.y");a.z1=c.getInt("pos1.z");a.x2=c.getInt("pos2.x");a.y2=c.getInt("pos2.y");a.z2=c.getInt("pos2.z");
            a.captureSeconds=c.getInt("capture-seconds",90);a.eventSeconds=c.getInt("event-seconds",1800);a.cooldownSeconds=c.getInt("cooldown-seconds",300);a.enabled=c.getBoolean("enabled",true);a.resetOnLeave=c.getBoolean("reset-on-leave",true);a.announceCapturer=c.getBoolean("announce-capturer",true);a.schedule=c.getString("schedule","");a.rewards=c.getStringList("rewards");a.hologramText=c.getString("hologram-text",a.hologramText);
            if(c.contains("hologram.world")){World w=Bukkit.getWorld(c.getString("hologram.world"));if(w!=null)a.hologram=new Location(w,c.getDouble("hologram.x"),c.getDouble("hologram.y"),c.getDouble("hologram.z"));} arenas.put(n.toLowerCase(Locale.ROOT),a);
        }
    }
    public void saveArenas(){
        if(arenasYaml==null)arenasYaml=new YamlConfiguration();arenasYaml.set("arenas",null);
        for(Arena a:arenas.values()){String p="arenas."+a.name+".";arenasYaml.set(p+"world",a.world);arenasYaml.set(p+"pos1.x",a.x1);arenasYaml.set(p+"pos1.y",a.y1);arenasYaml.set(p+"pos1.z",a.z1);arenasYaml.set(p+"pos2.x",a.x2);arenasYaml.set(p+"pos2.y",a.y2);arenasYaml.set(p+"pos2.z",a.z2);arenasYaml.set(p+"capture-seconds",a.captureSeconds);arenasYaml.set(p+"event-seconds",a.eventSeconds);arenasYaml.set(p+"cooldown-seconds",a.cooldownSeconds);arenasYaml.set(p+"enabled",a.enabled);arenasYaml.set(p+"reset-on-leave",a.resetOnLeave);arenasYaml.set(p+"announce-capturer",a.announceCapturer);arenasYaml.set(p+"schedule",a.schedule);arenasYaml.set(p+"rewards",a.rewards);arenasYaml.set(p+"hologram-text",a.hologramText);
            if(a.hologram!=null){arenasYaml.set(p+"hologram.world",a.hologram.getWorld().getName());arenasYaml.set(p+"hologram.x",a.hologram.getX());arenasYaml.set(p+"hologram.y",a.hologram.getY());arenasYaml.set(p+"hologram.z",a.hologram.getZ());}}
        try{arenasYaml.save(arenasFile);}catch(IOException e){getLogger().severe("Could not save arenas: "+e.getMessage());}
    }
    @EventHandler public void onJoin(PlayerJoinEvent e){if(service!=null)service.addPlayerToBars(e.getPlayer());}
    @EventHandler public void onQuit(PlayerQuitEvent e){if(service!=null)service.removePlayerFromBars(e.getPlayer());}
    @EventHandler public void onWand(PlayerInteractEvent e){if(e.getItem()==null||e.getItem().getType()!=Material.GOLDEN_AXE||!e.getItem().hasItemMeta())return;ItemMeta m=e.getItem().getItemMeta();if(!m.hasDisplayName()||!ChatColor.stripColor(m.getDisplayName()).equalsIgnoreCase("KoTH Selector"))return;
        if(e.getClickedBlock()==null|| (e.getAction()!=Action.LEFT_CLICK_BLOCK&&e.getAction()!=Action.RIGHT_CLICK_BLOCK))return;e.setCancelled(true);Location[] loc=selections.computeIfAbsent(e.getPlayer().getUniqueId(),k->new Location[2]);int i=e.getAction()==Action.LEFT_CLICK_BLOCK?0:1;loc[i]=e.getClickedBlock().getLocation();String k=i==0?"pos1":"pos2";tell(e.getPlayer(),getConfig().getString("messages."+k,"&aPosition selected at %x%, %y%, %z%").replace("%x%",""+loc[i].getBlockX()).replace("%y%",""+loc[i].getBlockY()).replace("%z%",""+loc[i].getBlockZ()));}
    private ItemStack wand(){ItemStack it=new ItemStack(Material.GOLDEN_AXE);ItemMeta m=it.getItemMeta();m.setDisplayName(color("&6KoTH Selector"));m.setLore(List.of(color("&7Left click: position 1"),color("&7Right click: position 2")));it.setItemMeta(m);return it;}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(args.length==0){if(sender instanceof Player p){gui.openMain(p);}else tell(sender,"Use /koth help.");return true;}
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(sub.equals("help")){tell(sender,"&e/koth &7- open GUI | &e/koth wand &7- selection axe | &e/koth define <name> | &e/koth editor <name> | &e/koth delete <name> | &e/koth start <name> [hourly] | &e/koth stop <name> | &e/koth schedule <name> <DAY> <HH:mm> | &e/koth reward <name> add <command> | remove <number> | list");return true;}
        if(sub.equals("list")){tell(sender,"&eArenas: &f"+String.join(", ",arenas.keySet()));return true;}
        if(!sender.hasPermission("koth.admin")){tell(sender,"&cYou don't have permission.");return true;}
        if(sub.equals("editor")&&args.length>=2&&sender instanceof Player p){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(p,"&cUnknown arena. Use /koth list.");return true;}gui.openArena(p,a);return true;}
        if(sub.equals("wand")&&sender instanceof Player p){p.getInventory().addItem(wand());tell(p,"&aYou received the KoTH selection axe. Left/right click two corners.");return true;}
        if(sub.equals("define")&&args.length>=2&&sender instanceof Player p){Location[] s=selections.get(p.getUniqueId());if(s==null||s[0]==null||s[1]==null){tell(p,"&cSelect both corners first with /koth wand.");return true;}if(!s[0].getWorld().equals(s[1].getWorld())){tell(p,"&cBoth corners must be in the same world.");return true;}String n=args[1].toLowerCase(Locale.ROOT);Arena a=arenas.get(n);boolean created=a==null;if(created){a=new Arena(n);a.captureSeconds=getConfig().getInt("settings.default-capture-seconds",90);a.eventSeconds=getConfig().getInt("settings.default-event-seconds",1800);a.cooldownSeconds=getConfig().getInt("settings.default-cooldown-seconds",300);arenas.put(n,a);}a.world=s[0].getWorld().getName();a.x1=s[0].getBlockX();a.y1=s[0].getBlockY();a.z1=s[0].getBlockZ();a.x2=s[1].getBlockX();a.y2=s[1].getBlockY();a.z2=s[1].getBlockZ();saveArenas();tell(p,created?"&aArena &f"+n+" &acreated.":"&aArena &f"+n+" &aarea updated.");gui.openArena(p,a);return true;}
        if(sub.equals("start")&&args.length>=2){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(sender,"&cUnknown arena.");return true;}if(args.length>=3&&args[2].equalsIgnoreCase("hourly")){service.startHourly(a);tell(sender,"&aHourly KoTH enabled for &f"+a.name+"&a. It will restart at the next hour.");}else service.start(a);return true;}
        if(sub.equals("stop")&&args.length>=2){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(sender,"&cUnknown arena.");return true;}service.stop(a,true);return true;}
        if(sub.equals("delete")&&args.length>=2){Arena a=arenas.remove(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(sender,"&cUnknown arena.");return true;}service.stop(a,false);removeHologram(a.name);saveArenas();tell(sender,"&aArena deleted.");return true;}
        if(sub.equals("schedule")&&args.length>=4){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(sender,"&cUnknown arena.");return true;}try{DayOfWeek.valueOf(args[2].toUpperCase(Locale.ROOT));LocalTime.parse(args[3]);a.schedule=args[2].toUpperCase(Locale.ROOT)+" "+args[3];saveArenas();tell(sender,"&aSchedule saved: "+a.schedule);}catch(Exception ex){tell(sender,"&cUse /koth schedule <arena> <MONDAY> <HH:mm>.");}return true;}
        if(sub.equals("unschedule")&&args.length>=2){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a!=null){a.schedule="";saveArenas();tell(sender,"&aSchedule removed.");}return true;}
        if(sub.equals("reward")&&args.length>=3){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(sender,"&cUnknown arena.");return true;}String action=args[2].toLowerCase(Locale.ROOT);if(action.equals("list")){tell(sender,"&eRewards for &f"+a.name+"&e: &f"+(a.rewards.isEmpty()?"None":String.join(" | ",a.rewards)));return true;}if(action.equals("remove")&&args.length>=4){try{int index=Integer.parseInt(args[3])-1;if(index<0||index>=a.rewards.size())throw new NumberFormatException();a.rewards.remove(index);saveArenas();tell(sender,"&aRemoved reward #"+(index+1)+".");}catch(NumberFormatException ex){tell(sender,"&cUse /koth reward <name> remove <number>.");}return true;}if(action.equals("clear")){a.rewards.clear();saveArenas();tell(sender,"&aAll rewards cleared.");return true;}int from=action.equals("add")?3:2;if(args.length<=from){tell(sender,"&cUse /koth reward <name> add <console command>.");return true;}a.rewards.add(String.join(" ",Arrays.copyOfRange(args,from,args.length)));saveArenas();tell(sender,"&aReward command added.");return true;}
        if(sub.equals("hologramtext")&&args.length>=3){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(sender,"&cUnknown arena.");return true;}a.hologramText=String.join(" ",Arrays.copyOfRange(args,2,args.length)).replace("|","\n");saveArenas();refreshHologram(a);tell(sender,"&aHologram text updated. Use | to separate lines.");return true;}
        if(sub.equals("hologram")&&args.length>=2&&sender instanceof Player p){Arena a=arenas.get(args[1].toLowerCase(Locale.ROOT));if(a==null){tell(p,"&cUnknown arena.");return true;}a.hologram=p.getLocation().clone();saveArenas();refreshHologram(a);tell(p,"&aHologram location set.");return true;}
        if(sub.equals("reload")){reloadConfig();loadArenas();tell(sender,"&aConfiguration reloaded.");return true;}
        tell(sender,"&cUnknown command. Use /koth help.");return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){if(args.length==1)return List.of("wand","define","editor","start","stop","list","delete","schedule","unschedule","reward","hologram","hologramtext","reload","help");if(args.length==2&&List.of("editor","start","stop","delete","schedule","unschedule","reward","hologram").contains(args[0].toLowerCase()))return new ArrayList<>(arenas.keySet());if(args.length==3&&args[0].equalsIgnoreCase("schedule"))return Arrays.stream(DayOfWeek.values()).map(Enum::name).toList();return Collections.emptyList();}

    private void checkSchedules(){ZonedDateTime now=ZonedDateTime.now();String minute=now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));for(Arena a:arenas.values()){if(!a.enabled||a.schedule.isBlank())continue;String[] p=a.schedule.split(" ");if(p.length!=2)continue;String key=a.name+":"+minute;if(!firedSchedules.contains(key)&&p[0].equalsIgnoreCase(now.getDayOfWeek().name())&&p[1].equals(now.format(DateTimeFormatter.ofPattern("HH:mm")))){firedSchedules.add(key);service.startScheduled(a);}}if(firedSchedules.size()>500)firedSchedules.clear();}
    /** FancyHolograms owns hologram entities; KoTH only exposes PlaceholderAPI values. */
    public void refreshHologram(Arena a){}
    public void removeHologram(String name){}
}
