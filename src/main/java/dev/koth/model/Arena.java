package dev.koth.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Arena {
    public final String name;
    public String world;
    public int x1,y1,z1,x2,y2,z2;
    public int captureSeconds=90, eventSeconds=1800, cooldownSeconds=300;
    public boolean enabled=true, resetOnLeave=true, announceCapturer=true;
    public Location hologram;
    public String hologramText="&6&l%arena%\n&fCapturer: &e%capturer%\n&fTime: &e%time%";
    public String schedule="";
    public java.util.List<String> rewards=new java.util.ArrayList<>();
    public Arena(String name){this.name=name;}
    public World getWorld(){return Bukkit.getWorld(world);}
    public boolean contains(Location l){
        if(l.getWorld()==null || world==null || !l.getWorld().getName().equals(world)) return false;
        return l.getBlockX()>=Math.min(x1,x2)&&l.getBlockX()<=Math.max(x1,x2)
            && l.getBlockY()>=Math.min(y1,y2)&&l.getBlockY()<=Math.max(y1,y2)
            && l.getBlockZ()>=Math.min(z1,z2)&&l.getBlockZ()<=Math.max(z1,z2);
    }
}
