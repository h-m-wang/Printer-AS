package com.industry.printer.data;

public class ZIntSymbol {

    public int symbology;
    public int height;
    public int output_options;
    public String fgcolour;
    public int fgColor;
    public String bgcolour;
    public int bgColor;
    public float scale;
    public int option_1;
    public int option_2;
    public int option_3;
    public int show_hrt;
    public int fontsize;
    public int input_mode;
    public int eci;
    public String text; /* UTF-8 */
    public int width;
    public String errtxt;
    public int bitmap_width;
    public int bitmap_height;
    public float dot_size;
    public int debug;
    public int bReduction;
    public int autoSize;
    public char ecc_level;
    public int errorNumber;
    public int[] pixels;

    public ZIntSymbol() {
        this.symbology = 0;
        this.height = 0;
        this.width = 0;
        this.output_options = 0;
        this.fgColor = 0xff000000;
        this.bgColor = 0xffffffff;
//        this.fgcolour = "000000";
//        this.bgcolour = "ffffff";
        this.option_1 = -1;
//        this.option_2 = 1;
        this.option_3 = 0;
        this.show_hrt = 1;
        this.fontsize = 8;
        this.input_mode = 0;
        this.eci = 0;
        this.dot_size = 4.0f / 5.0f;
        this.debug = 0;
    }
}
