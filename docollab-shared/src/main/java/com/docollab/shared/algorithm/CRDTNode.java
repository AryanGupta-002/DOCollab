package com.docollab.shared.algorithm;

public class CRDTNode {

    private String id;
    private char value;
    private String leftId;
    private boolean isDeleted;

    private boolean isBold;
    private boolean isItalic;
    private boolean isUnderline;
    private String fontFamily;
    private int fontSize;
    private String colorHex;
    private String alignment;

    public CRDTNode() {
    }

    public CRDTNode(String id, char value, String leftId) {
        this.id = id;
        this.value = value;
        this.leftId = leftId;
        this.isDeleted = false;

        this.isBold = false;
        this.isItalic = false;
        this.isUnderline = false;
        this.fontFamily = "Arial";
        this.fontSize = 16;
        this.colorHex = "#D3D3D3";
        this.alignment = "left";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public char getValue() { return value; }
    public void setValue(char value) { this.value = value; }

    public String getLeftId() { return leftId; }
    public void setLeftId(String leftId) { this.leftId = leftId; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public boolean isBold() { return isBold; }
    public void setBold(boolean bold) { isBold = bold; }

    public boolean isItalic() { return isItalic; }
    public void setItalic(boolean italic) { isItalic = italic; }

    public boolean isUnderline() { return isUnderline; }
    public void setUnderline(boolean underline) { isUnderline = underline; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getAlignment() { return alignment; }
    public void setAlignment(String alignment) { this.alignment = alignment; }
}