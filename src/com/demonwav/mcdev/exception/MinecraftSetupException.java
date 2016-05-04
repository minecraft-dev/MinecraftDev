package com.demonwav.mcdev.exception;

import javax.swing.JComponent;

public class MinecraftSetupException extends Exception {
    private JComponent j;

    public MinecraftSetupException(String msg, JComponent j) {
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
            case "sponge":
                return "<html>Sorry, Sponge support isn't currently working yet.</html>";
            case "forge":
                return "<html>Sorry, Forge support isn't currently working yet.</html>";
            case "fillAll":
                return "<html>Please fill in all fields</html>";
            case "gradle":
                return "<html>Sorry, Gradle support isn't currently working yet.</html>";
            default:
                return "<html>Unknown Error</html>";
        }
    }
}
