package com.azizbgboss.ide;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Command;

public class Editor {

    private IDECanvas canvas;
    private IDEMidlet midlet;

    public Canvas getCanvas() {
        return canvas;
    }

    public int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public Editor(IDEMidlet midlet) {
        this.midlet = midlet;
        canvas = new IDECanvas();
        canvas.print("PS C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE> ant clean jar\r\n" + //
                "Buildfile: C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build.xml\r\n" + //
                "\r\n" + //
                "clean:\r\n" + //
                "   [delete] Deleting directory C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build\r\n" + //
                "   [delete] Deleting directory C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build-preverified\r\n"
                + //
                "   [delete] Deleting directory C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\dist\r\n" + //
                "\r\n" + //
                "compile:\r\n" + //
                "    [mkdir] Created dir: C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build\r\n" + //
                "    [javac] Compiling 3 source files to C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build\r\n"
                + //
                "    [javac] warning: [options] bootstrap class path not set in conjunction with -source 1.3 \r\n" + //
                "    [javac] warning: [options] source value 1.3 is obsolete and will be removed in a future release\r\n"
                + //
                "    [javac] warning: [options] target value 1.3 is obsolete and will be removed in a future release\r\n"
                + //
                "    [javac] warning: [options] To suppress warnings about obsolete options, use -Xlint:-options.\r\n" + //
                "    [javac] 4 warnings\r\n" + //
                "\r\n" + //
                "preverify:\r\n" + //
                "    [mkdir] Created dir: C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build-preverified   \r\n"
                + //
                " [proguard] ProGuard, version 6.0.3\r\n" + //
                " [proguard] Reading program directory [C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build]\r\n"
                + //
                " [proguard] Reading library jar [C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\lib\\cldcapi11-2.0.4.jar]\r\n"
                + //
                " [proguard] Reading library jar [C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\lib\\midpapi20-2.0.4.jar]\r\n"
                + //
                " [proguard] Reading library jar [C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\lib\\jsr75_file.jar]\r\n"
                + //
                " [proguard] Preparing output directory [C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build-preverified]\r\n"
                + //
                " [proguard]   Copying resources from program directory [C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\build]\r\n"
                + //
                "\r\n" + //
                "jar:\r\n" + //
                "    [mkdir] Created dir: C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\dist\r\n" + //
                "      [jar] Building jar: C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE\\dist\\BBIDE.jar     \r\n"
                + //
                "\r\n" + //
                "BUILD SUCCESSFUL\r\n" + //
                "Total time: 1 second\r\n" + //
                "PS C:\\Users\\Aziz\\Documents\\NetBeansProjects\\BBIDE>");
        canvas.repaint();
    }

    private class IDECanvas extends Canvas implements CommandListener {
        private int BACKGROUND_COLOR = 0x0F0F0F;

        final int screenW = getWidth();
        final int screenH = getHeight();
        final int cellW = TinyFont.CELL_W;
        final int cellH = TinyFont.CELL_H;
        final int charW = screenW / cellW;
        final int charH = screenH / cellH;
        final int lineSpacing = String.valueOf(charH).length() + 1; // number of digits in the charH + 1 space for the
                                                                    // line

        // Define the array of characters in the IDE
        private char[][] charArray = new char[charH][charW - lineSpacing];
        private int[][] charColorArray = new int[charH][charW - lineSpacing];
        private int cursorX = 0;
        private int cursorY = 0;
        private int color = 0;

        public int getLastCharX(int y) {
            for (int x = charW - lineSpacing - 1; x >= 0; x--) {
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
                    continue;
                } else if (c == '\r') { // carriage return (for windows, no need)
                    continue;
                } else if (c == '\b') { // backspace
                    cursorX--;
                    if (cursorX < 0) {
                        cursorY--;
                        if (cursorY < 0) {
                            cursorY = charH - 1;
                        }
                        cursorX = getLastCharX(cursorY) + 1;
                    }
                    charArray[cursorY][cursorX] = (char) 0;
                    continue;
                }
                charArray[cursorY][cursorX] = c;
                charColorArray[cursorY][cursorX] = color;
                cursorX++;
                if (cursorX == charW - lineSpacing) {
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
        }

        public IDECanvas() {
            setFullScreenMode(true);
        }

        protected void paint(Graphics g) {
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, screenW, screenH);
            g.setColor(0x3A3A3C);
            for (int y = 0; y < charH; y++) {
                TinyFont.drawString(g, String.valueOf(y + 1), 0, y * cellH);
            }
            g.drawLine(lineSpacing * cellW - cellW / 2 - 1, 0, lineSpacing * cellW - cellW / 2 - 1, screenH);
            for (int y = 0; y < charH; y++) {
                for (int x = 0; x < charW - lineSpacing; x++) {
                    if (charArray[y][x] != 0) {
                        g.setColor(charColorArray[y][x]);
                        TinyFont.drawChar(g, charArray[y][x], x * cellW + lineSpacing * cellW, y * cellH);
                    }
                }
            }
            g.setColor(0xFFFFFF);
            TinyFont.drawChar(g, '|', cursorX * cellW + lineSpacing * cellW, cursorY * cellH);
        }

        public void commandAction(Command c, Displayable d) {

        }

        public void keyPressed(int keyCode) {
            if (keyCode == -1) { // Up
                cursorY--;
                if (cursorY < 0) {
                    cursorY = charH - 1;
                }
                cursorX = clamp(cursorX, 0, getLastCharX(cursorY) + 1);
            } else if (keyCode == -2) { // Down
                cursorY++;
                if (cursorY == charH) {
                    cursorY = 0;
                }
                cursorX = clamp(cursorX, 0, getLastCharX(cursorY) + 1);
            } else if (keyCode == -3) { // Left
                cursorX--;
                if (cursorX < 0) {
                    cursorY--;
                    if (cursorY < 0) {
                        cursorY = charH - 1;
                    }
                    cursorX = getLastCharX(cursorY) + 1;
                }
            } else if (keyCode == -4) { // Right
                cursorX++;
                if (cursorX >= getLastCharX(cursorY) + 1) {
                    cursorY++;
                    if (cursorY == charH) {
                        cursorY = 0;
                    }
                    cursorX = 0;
                }
            } else if (keyCode == -5) { // Select
                print("\n");
            } else
                print(String.valueOf((char) keyCode));
            repaint();
        }

        public void keyRepeated(int keyCode) {
            keyPressed(keyCode);
        }
    }
}
