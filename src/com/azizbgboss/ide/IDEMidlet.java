package com.azizbgboss.ide;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;

public class IDEMidlet extends MIDlet {
    private Display   display;
    private Editor editor;

    public void startApp() {
        display = Display.getDisplay(this);
        editor = new Editor(this);
        display.setCurrent(editor.getCanvas());
    }

    public void pauseApp() {

    }

    public void destroyApp(boolean unconditional) {
        
    }

    public void quit() {
        
    }

    public Display getDisplay() {
        return display;
    }
}