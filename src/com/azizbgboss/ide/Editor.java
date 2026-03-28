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
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

public class Editor implements CommandListener {

    public static final int OPEN_FILE = 0;
    public static final int SAVE_FILE = 1;
    public static final int NEW_FILE = 2;
    public static final int NEW_FOLDER = 3;

    public static final String defaultDir = "file:///";

    public int fileMode = 0;
    public int nameMode = 0;
    public String currentDir = defaultDir;
    public String currentFile = null;
    public long currentFileSize = 0;

    private IDECanvas canvas;
    private IDEMidlet midlet;

    private Command cmdNew;
    private Command cmdOpen;
    private Command cmdSave;
    private Command cmdSaveAs;
    private Command cmdExit;
    private Command cmdInsert;

    private List fileExplorer;
    private Command cmdFXSelect;
    private Command cmdFXBack;
    private Command cmdFXNewFolder;
    private Command cmdFXNewFile;

    private TextBox tbName;
    private Command cmdTBOK;
    private Command cmdTBCancel;

    private TextBox tbInsert;
    private Command cmdTBInsertOK;
    private Command cmdTBInsertCancel;

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
        cmdInsert = new Command("Insert", Command.SCREEN, 6);

        canvas.addCommand(cmdNew);
        canvas.addCommand(cmdOpen);
        canvas.addCommand(cmdSave);
        canvas.addCommand(cmdSaveAs);
        canvas.addCommand(cmdExit);
        canvas.addCommand(cmdInsert);
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
            if (!fc.exists()) {
                fc.create();
            }
            fc.truncate(0);
            OutputStream out = fc.openOutputStream();
            out.write(canvas.getText().getBytes());
            out.close();
            fc.close();
            showAlert("", "Saved " + filename + " successfully.", canvas, 2000);
            currentFile = filename;
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
            } else if (c == cmdInsert) {
                tbInsert = new TextBox("Insert", "", 256, TextField.ANY);

                cmdTBInsertOK = new Command("OK", Command.OK, 1);
                cmdTBInsertCancel = new Command("Cancel", Command.CANCEL, 2);

                tbInsert.addCommand(cmdTBInsertOK);
                tbInsert.addCommand(cmdTBInsertCancel);
                tbInsert.setCommandListener(this);
                midlet.getDisplay().setCurrent(tbInsert);
            }
        } else if (d == tbInsert) {
            if (c == cmdTBInsertOK) {
                canvas.print(tbInsert.getString());
                canvas.repaint();
                midlet.getDisplay().setCurrent(canvas);
            } else if (c == cmdTBInsertCancel) {
                midlet.getDisplay().setCurrent(canvas);
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
            } else if (c == cmdFXNewFolder) {
                newElement(NEW_FOLDER);
            } else if (c == cmdFXNewFile) {
                newElement(NEW_FILE);
            }
        } else if (d == tbName) {
            if (c == cmdTBOK) {
                String filename = tbName.getString();
                // check for illegal characters
                char[] illegalChars = { '\\', '/', ':', '*', '?', '"', '<', '>', '|', '\n' };
                for (int i = 0; i < illegalChars.length; i++) {
                    if (filename.indexOf(illegalChars[i]) != -1) {
                        showAlert("Illegal Character", "Illegal character '" + illegalChars[i] + "' found in filename",
                                fileExplorer, 2000);
                        return;
                    }
                }
                if (filename.length() == 0) {
                    showAlert("Error", "Filename cannot be empty", fileExplorer, 2000);
                    return;
                }
                try {
                    FileConnection fc = (FileConnection) Connector.open(currentDir + filename);
                    if (fc.exists()) {
                        showAlert("Error",
                                (nameMode == NEW_FOLDER ? "Folder " : "File ") + filename + " already exists",
                                fileExplorer, 2000);
                        fc.close();
                        return;
                    }
                    if (nameMode == NEW_FOLDER)
                        fc.mkdir();
                    else
                        fc.create();
                    fc.close();
                    exploreFile(fileMode);
                } catch (IOException e) {
                    showAlert("Error", e.getMessage(), canvas, 2000);
                }
            } else if (c == cmdTBCancel) {
                midlet.getDisplay().setCurrent(fileExplorer);
            }
        }
    }

    private void exploreFile(int type) {
        fileMode = type;
        try {
            FileConnection fc = (FileConnection) Connector.open(currentDir);
            Enumeration files = fc.list();
            fileExplorer = new List("File Explorer", List.IMPLICIT);
            if (!currentDir.equals(defaultDir))
                fileExplorer.append("..", null);
            while (files.hasMoreElements()) {
                String filename = (String) files.nextElement();
                fileExplorer.append(filename, null);
            }
            fc.close();

            cmdFXSelect = new Command("Select", Command.OK, 1);
            cmdFXBack = new Command("Close", Command.BACK, 2);
            cmdFXNewFolder = new Command("New Folder", Command.SCREEN, 3);
            cmdFXNewFile = new Command("New File", Command.SCREEN, 4);

            fileExplorer.addCommand(cmdFXSelect);
            fileExplorer.addCommand(cmdFXBack);
            fileExplorer.addCommand(cmdFXNewFolder);
            fileExplorer.addCommand(cmdFXNewFile);
            fileExplorer.setSelectCommand(cmdFXSelect);
            fileExplorer.setCommandListener(this);
            midlet.getDisplay().setCurrent(fileExplorer);
        } catch (Exception e) {
            if (currentDir.equals(defaultDir)) {
                showAlert("Error", e.getMessage() + "\nTrying with SDCard/", canvas, 1000);
                currentDir = "file:///SDCard/";
                exploreFile(fileMode);
            } else
                showAlert("Error", e.getMessage(), canvas, 1000);
        }
    }

    private void newElement(int type) {
        nameMode = type;
        tbName = new TextBox("New " + (type == NEW_FOLDER ? "Folder" : "File"), "", 20, TextField.ANY);

        cmdTBOK = new Command("OK", Command.OK, 1);
        cmdTBCancel = new Command("Cancel", Command.CANCEL, 2);

        tbName.addCommand(cmdTBOK);
        tbName.addCommand(cmdTBCancel);
        tbName.setCommandListener(this);
        midlet.getDisplay().setCurrent(tbName);
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
        private int[] rowStartOffsets = new int[charH];
        private int[] rowLengths = new int[charH];
        private String textBuffer = "";
        private int cursorX = 0;
        private int cursorY = 0;
        private int cursorOffset = 0;
        private int usedRowCount = 1;

        public int getCursor() {
            return cursorOffset;
        }

        public void moveCursor(int x, int y) { // increments the cursor position by x and y
            if (y != 0) {
                moveCursorVertical(y);
            }
            if (x != 0) {
                setCursor(cursorOffset + x);
            }
        }

        public void setCursor(long pos) {
            cursorOffset = clamp((int) pos, 0, (int) currentFileSize);
            rebuildScreen();
        }

        public void clear() {
            replaceText("", 0);
            repaint();
        }

        public String getText() {
            return textBuffer;
        }

        private int getRowLength(int y) {
            return rowLengths[y];
        }

        private void resetScreen() {
            for (int y = 0; y < charH; y++) {
                rowStartOffsets[y] = 0;
                rowLengths[y] = 0;
                for (int x = 0; x < charW; x++) {
                    charArray[y][x] = 0;
                    charColorArray[y][x] = 0;
                }
            }
            cursorX = 0;
            cursorY = 0;
            usedRowCount = 1;
        }

        private String fitText(String text) {
            StringBuffer fitted = new StringBuffer(text.length());
            int x = 0;
            int y = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\r') {
                    continue;
                }

                if (c == '\n') {
                    if (y == charH - 1) {
                        break;
                    }
                    fitted.append(c);
                    y++;
                    x = 0;
                    continue;
                }

                if (x == charW) {
                    if (y == charH - 1) {
                        break;
                    }
                    y++;
                    x = 0;
                }

                fitted.append(c);
                x++;
            }

            return fitted.toString();
        }

        private void replaceText(String text, int newCursorOffset) {
            textBuffer = fitText(text);
            currentFileSize = textBuffer.length();
            cursorOffset = clamp(newCursorOffset, 0, (int) currentFileSize);
            rebuildScreen();
        }

        private void rebuildScreen() {
            resetScreen();
            rowStartOffsets[0] = 0;

            int renderX = 0;
            int renderY = 0;
            int renderColor = 0xFFFFFF;
            boolean cursorPlaced = false;

            for (int i = 0; i < textBuffer.length(); i++) {
                char c = textBuffer.charAt(i);

                if (c != '\n' && renderX == charW) {
                    if (renderY == charH - 1) {
                        break;
                    }
                    renderY++;
                    renderX = 0;
                    rowStartOffsets[renderY] = i;
                    usedRowCount = renderY + 1;
                }

                if (i == cursorOffset) {
                    cursorX = renderX;
                    cursorY = renderY;
                    cursorPlaced = true;
                }

                if (c == '\n') {
                    if (renderY == charH - 1) {
                        break;
                    }
                    renderY++;
                    renderX = 0;
                    rowStartOffsets[renderY] = i + 1;
                    usedRowCount = renderY + 1;
                    continue;
                }

                charArray[renderY][renderX] = c;

                if (currentFile != null) {
                    if (currentFile.endsWith(".html") || currentFile.endsWith(".htm") || currentFile.endsWith(".xml")) {
                        if (c == '<') {
                            renderColor = 0x0000FF;
                            charColorArray[renderY][renderX] = renderColor;
                        } else if (c == '>') {
                            charColorArray[renderY][renderX] = renderColor;
                            renderColor = 0xFFFFFF;
                        } else {
                            charColorArray[renderY][renderX] = renderColor;
                        }
                    } else {
                        charColorArray[renderY][renderX] = renderColor;
                    }
                } else {
                    charColorArray[renderY][renderX] = renderColor;
                }

                renderX++;
                if (renderX > rowLengths[renderY]) {
                    rowLengths[renderY] = renderX;
                }
            }

            if (!cursorPlaced) {
                cursorX = renderX;
                cursorY = renderY;
            }

        }

        private void moveCursorVertical(int delta) {
            int targetRow = clamp(cursorY + delta, 0, usedRowCount - 1);
            int targetColumn = clamp(cursorX, 0, getRowLength(targetRow));
            cursorOffset = rowStartOffsets[targetRow] + targetColumn;
            rebuildScreen();
        }

        public void print(String text) {
            String updated = textBuffer;
            int newCursorOffset = cursorOffset;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\r') {
                    continue;
                }

                if (c == '\b') {
                    if (newCursorOffset > 0) {
                        updated = updated.substring(0, newCursorOffset - 1) + updated.substring(newCursorOffset);
                        newCursorOffset--;
                    }
                } else {
                    updated = updated.substring(0, newCursorOffset) + c + updated.substring(newCursorOffset);
                    newCursorOffset++;
                }
            }

            replaceText(updated, newCursorOffset);
        }

        public IDECanvas() {
            setFullScreenMode(true);
            rebuildScreen();
        }

        protected void paint(Graphics g) {
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, screenW, screenH);
            for (int y = 0; y < charH; y++) {
                if (y < usedRowCount)
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
                moveCursorVertical(-1);
            } else if (keyCode == -2 || keyCode == 6) { // Down
                moveCursorVertical(1);
            } else if (keyCode == -3 || keyCode == 2) { // Left
                if (cursorOffset > 0) {
                    setCursor(cursorOffset - 1);
                }
            } else if (keyCode == -4 || keyCode == 5) { // Right
                if (cursorOffset < currentFileSize) {
                    setCursor(cursorOffset + 1);
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
