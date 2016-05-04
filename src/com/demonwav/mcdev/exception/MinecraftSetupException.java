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
            default:
                return "<html>Unknown Error</html>";
        }
    }
}
