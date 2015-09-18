/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.exceptions;

import javax.swing.JComponent;

public class BukkitSetupException extends Exception {
    private JComponent j;
    public BukkitSetupException(String msg, JComponent j) {
        super(msg);
        this.j = j;
    }
    public JComponent getJ() {
        return j;
    }
}
