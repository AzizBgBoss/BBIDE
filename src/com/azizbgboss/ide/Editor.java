package com.azizbgboss.ide;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.List;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

public class Editor implements CommandListener {

    public static final int OPEN_FILE = 0;
    public static final int SAVE_FILE = 1;

    public int fileMode = 0;
    public String currentDir = "file:///";
    public String currentFile = null;
    public long currentFileSize = 0;

    private IDECanvas canvas;
    private IDEMidlet midlet;

    private Command cmdNew;
    private Command cmdOpen;
    private Command cmdSave;
    private Command cmdSaveAs;
    private Command cmdExit;

    private List fileExplorer;
    private Command cmdFXSelect;
    private Command cmdFXBack;

    public Canvas getCanvas() {
        return canvas;
    }

    public int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public Editor(IDEMidlet midlet) {
        this.midlet = midlet;
        canvas = new IDECanvas();

        cmdNew = new Command("New", Command.SCREEN, 1);
        cmdOpen = new Command("Open", Command.SCREEN, 2);
        cmdSave = new Command("Save", Command.SCREEN, 3);
        cmdSaveAs = new Command("Save As", Command.SCREEN, 4);
        cmdExit = new Command("Exit", Command.EXIT, 5);

        canvas.addCommand(cmdNew);
        canvas.addCommand(cmdOpen);
        canvas.addCommand(cmdSave);
        canvas.addCommand(cmdSaveAs);
        canvas.addCommand(cmdExit);
        canvas.setCommandListener(this);

        canvas.print("Hello World!");
        canvas.repaint();
    }

    private void showAlert(String title, String msg, Displayable next, int timeout) {
        Alert a = new Alert(title, msg, null, AlertType.WARNING);
        a.setTimeout(timeout);
        midlet.getDisplay().setCurrent(a, next);
    }

    public void saveFile(String filename) {
        try {
            FileConnection fc = (FileConnection) Connector.open(currentDir + filename);
            OutputStream out = fc.openOutputStream();
            out.write(canvas.getText().getBytes());
            out.close();
            fc.close();
            showAlert("", "Saved " + filename + " successfully.", canvas, 2000);
        } catch (IOException e) {
            showAlert("Error", e.getMessage(), canvas, 2000);
        }
    }

    public void openFile(String filename) {
        try {
            FileConnection fc = (FileConnection) Connector.open(currentDir + filename);
            InputStream in = fc.openInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            canvas.clear();
            while ((bytesRead = in.read(buffer)) != -1) {
                canvas.print(new String(buffer, 0, bytesRead));
            }
            in.close();
            currentFileSize = fc.fileSize();
            fc.close();
            showAlert("", "Opened " + filename + " successfully.", canvas, 2000);
            currentFile = filename;
            canvas.setCursor(0);
        } catch (IOException e) {
            showAlert("Error", e.getMessage(), canvas, 2000);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (d == canvas) {
            if (c == cmdNew) {
                currentFile = null;
                currentFileSize = 0;
                canvas.clear();
            } else if (c == cmdOpen) {
                exploreFile(OPEN_FILE);
            } else if (c == cmdSave) {
                if (currentFile == null) {
                    exploreFile(SAVE_FILE);
                } else {
                    saveFile(currentFile);
                }
            } else if (c == cmdSaveAs) {
                exploreFile(SAVE_FILE);
            } else if (c == cmdExit) {
                midlet.quit();
            }
        } else if (d == fileExplorer) {
            if (c == cmdFXSelect) {
                String filename = fileExplorer.getString(fileExplorer.getSelectedIndex());
                if (filename.equals("..")) {
                    for (int i = currentDir.length() - 2; i >= 0; i--) { // remove last /
                        if (currentDir.charAt(i) == '/') {
                            currentDir = currentDir.substring(0, i + 1);
                            break;
                        }
                    }
                    exploreFile(fileMode);
                } else if (filename.endsWith("/")) {
                    currentDir += filename;
                    exploreFile(fileMode);
                } else if (fileMode == OPEN_FILE) {
                    openFile(filename);
                } else if (fileMode == SAVE_FILE) {
                    saveFile(filename);
                }
            } else if (c == cmdFXBack) {
                midlet.getDisplay().setCurrent(canvas);
            }
        }
    }

    private void exploreFile(int type) {
        fileMode = type;
        try {
            FileConnection fc = (FileConnection) Connector.open(currentDir);
            Enumeration files = fc.list();
            fileExplorer = new List("File Explorer", List.IMPLICIT);
            if (!currentDir.equals("file:///"))
                fileExplorer.append("..", null);
            while (files.hasMoreElements()) {
                String filename = (String) files.nextElement();
                fileExplorer.append(filename, null);
            }
            fc.close();

            cmdFXSelect = new Command("Select", Command.OK, 1);
            cmdFXBack = new Command("Close", Command.BACK, 2);

            fileExplorer.addCommand(cmdFXSelect);
            fileExplorer.addCommand(cmdFXBack);
            fileExplorer.setSelectCommand(cmdFXSelect);
            fileExplorer.setCommandListener(this);
            midlet.getDisplay().setCurrent(fileExplorer);
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), null, 1000);
        }
    }

    private class IDECanvas extends Canvas {
        private int BACKGROUND_COLOR = 0x0F0F0F;

        final int screenW = getWidth();
        final int screenH = getHeight();
        final int cellW = TinyFont.CELL_W;
        final int cellH = TinyFont.CELL_H;
        final int charH = screenH / cellH;
        final int lineSpacing = String.valueOf(charH).length() + 1; // number of digits in the charH + 1 space for the
                                                                    // line
        final int charW = (screenW / cellW) - lineSpacing;

        // Define the array of characters in the IDE
        private char[][] charArray = new char[charH][charW];
        private int[][] charColorArray = new int[charH][charW];
        private int cursorX = 0;
        private int cursorY = 0;
        private int color = 0;

        public int getCursor() {
            return cursorY * charW + cursorX;
        }

        public void moveCursor(int x, int y) { // increments the cursor position by x and y

        }

        public void setCursor(long pos) {
            cursorX = (int) (pos % charW);
            cursorY = (int) (pos / charW);
        }

        public void clear() {
            for (int y = 0; y < charH; y++) {
                for (int x = 0; x < charW; x++) {
                    charArray[y][x] = 0;
                    charColorArray[y][x] = 0;
                }
            }
            cursorX = 0;
            cursorY = 0;
            color = 0;
            repaint();
        }

        public String getText() {
            String text = "";
            for (int y = 0; y < charH; y++) {
                for (int x = 0; x < charW; x++) {
                    if (charArray[y][x] != 0) {
                        text += charArray[y][x];
                    }
                }
                text += "\n";
            }
            return text;
        }

        public int getLastCharX(int y) {
            for (int x = charW - 1; x >= 0; x--) {
                if (charArray[y][x] != 0) {
                    return x;
                }
            }
            return -1;
        }

        public void print(String text) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') { // new line
                    cursorX = 0;
                    cursorY++;
                    if (cursorY == charH) {
                        cursorY = 0;
                    }
                } else if (c == '\r') { // carriage return (for windows, no need to implement)
                } else if (c == '\b') { // backspace
                    boolean sync = false;
                    if (getCursor() == currentFileSize) {
                        sync = true;
                    }
                    cursorX--;
                    if (cursorX < 0) {
                        cursorY--;
                        if (cursorY < 0) {
                            cursorY = 0;
                        }
                        cursorX = getLastCharX(cursorY) + 1;
                    }
                    charArray[cursorY][cursorX] = (char) 0;
                    if (sync) {
                        currentFileSize = getCursor();
                    }
                } else {
                    charArray[cursorY][cursorX] = c;
                    charColorArray[cursorY][cursorX] = color;
                    cursorX++;
                    if (cursorX == charW) {
                        cursorX = 0;
                        cursorY++;
                        if (cursorY == charH) {
                            cursorY = 0;
                        }
                    }
                    if (c == ' ') {
                        color = (color + 0x0000FF) & 0xFFFFFF;
                    }
                }
                if (getCursor() > currentFileSize) {
                    currentFileSize = getCursor();
                }
            }
        }

        public IDECanvas() {
            setFullScreenMode(true);
        }

        protected void paint(Graphics g) {
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, screenW, screenH);
            for (int y = 0; y < charH; y++) {
                if (y <= Math.ceil(currentFileSize / charW))
                    g.setColor(0x5A5A5A);
                else
                    g.setColor(0x3A3A3C);
                TinyFont.drawString(g, String.valueOf(y + 1), 0, y * cellH);
                g.drawLine(lineSpacing * cellW - cellW / 2 - 1, y * cellH, lineSpacing * cellW - cellW / 2 - 1,
                        y * cellH + cellH - 1);
            }
            for (int y = 0; y < charH; y++) {
                for (int x = 0; x < charW; x++) {
                    if (charArray[y][x] != 0) {
                        g.setColor(charColorArray[y][x]);
                        TinyFont.drawChar(g, charArray[y][x], x * cellW + lineSpacing * cellW, y * cellH);
                    }
                }
            }
            g.setColor(0xFFFFFF);
            TinyFont.drawChar(g, '|', cursorX * cellW + lineSpacing * cellW, cursorY * cellH);
        }

        public void keyPressed(int keyCode) {
            if (keyCode == -1 || keyCode == 1) { // Up
                cursorY--;
                if (cursorY < 0) {
                    cursorY = 0;
                }
                cursorX = clamp(cursorX, 0, getLastCharX(cursorY) + 1);
                if (getCursor() > currentFileSize) {
                    setCursor(currentFileSize);
                }
            } else if (keyCode == -2 || keyCode == 6) { // Down
                cursorY++;
                if (cursorY == charH) {
                    cursorY = 0;
                }
                cursorX = clamp(cursorX, 0, getLastCharX(cursorY) + 1);
                if (getCursor() > currentFileSize) {
                    setCursor(currentFileSize);
                }
            } else if (keyCode == -3 || keyCode == 2) { // Left
                if (getCursor() > 0) {
                    cursorX--;
                    if (cursorX < 0) {
                        cursorY--;
                        if (cursorY < 0) {
                            cursorY = 0;
                        }
                        cursorX = getLastCharX(cursorY) + 1;
                    }
                    if (getCursor() > currentFileSize) {
                        setCursor(currentFileSize);
                    }
                }
            } else if (keyCode == -4 || keyCode == 5) { // Right
                cursorX++;
                if (cursorX >= getLastCharX(cursorY) + 1) {
                    cursorY++;
                    if (cursorY == charH) {
                        cursorY = 0;
                    }
                    cursorX = 0;
                }
                if (getCursor() > currentFileSize) {
                    setCursor(currentFileSize);
                }
            } else if (keyCode == -5) { // Select
                print("\n");
            } else if (keyCode < 0) { // unsupported special keys
                showAlert("Unknown key", getKeyName(keyCode) + " (" + String.valueOf(keyCode) + ")", canvas, 1000);
            } else
                print(String.valueOf((char) keyCode));
            repaint();
        }

        public void keyRepeated(int keyCode) {
            keyPressed(keyCode);
        }
    }
}
