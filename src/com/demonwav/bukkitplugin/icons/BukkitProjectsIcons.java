/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public class BukkitProjectsIcons {

  private static Icon load(String path) {
    return IconLoader.getIcon(path, BukkitProjectsIcons.class);
  }

  public static final Icon BukkitProject = load("/icons/Bukkit.png");
  public static final Icon BukkitProjectBig = load("/icons/Bukkit@2x.png");
  public static final Icon SpigotProject = load("/icons/Spigot.png");
  public static final Icon SpigotProjectBig = load("/icons/Spigot@2x.png");
}
