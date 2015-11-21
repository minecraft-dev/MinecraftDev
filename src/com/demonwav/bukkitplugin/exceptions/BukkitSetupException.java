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

    public String getError() {
        switch (getMessage()) {
            case "empty":
                return "<html>Please fill in all required fields</html>";
            case "bad":
                return "<html>Please enter author and plugin names as a comma separated list</html>";
            default:
                return "<html>Unknown Error</html>";
        }
    }
}
