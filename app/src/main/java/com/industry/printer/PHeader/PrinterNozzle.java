package com.industry.printer.PHeader;

import com.industry.printer.FileFormat.SystemConfigFile;
import com.industry.printer.hardware.Hp22mm;

public enum PrinterNozzle {
    MESSAGE_TYPE_12_7(MessageType.NOZZLE_INDEX_12_7, NozzleType.NOZZLE_TYPE_12_7, 1, 1),         //12.7 单头（12.7），one segment，
    MESSAGE_TYPE_25_4(MessageType.NOZZLE_INDEX_25_4, NozzleType.NOZZLE_TYPE_25_4, 2, 2),         //25.4 double Nozzle（12.7X2）,  double RFID，double segment
    MESSAGE_TYPE_38_1(MessageType.NOZZLE_INDEX_38_1, NozzleType.NOZZLE_TYPE_38_1, 3, 3),         //38.1 triple RFID（12.7X3），triple segment
    MESSAGE_TYPE_50_8(MessageType.NOZZLE_INDEX_50_8, NozzleType.NOZZLE_TYPE_50_8, 4, 4),         //50.8 fourfold RFID（12.7X4），fourfold segment
    MESSAGE_TYPE_16_DOT(MessageType.NOZZLE_INDEX_16_DOT, NozzleType.NOZZLE_TYPE_16_DOT, 1, 1),       //big data; single Nozzle, single RFID, 4 segments
    MESSAGE_TYPE_32_DOT(MessageType.NOZZLE_INDEX_32_DOT, NozzleType.NOZZLE_TYPE_32_DOT, 1, 1),       // big data; single Nozzle, single RFID, 4 segments

    // H.M.Wang 追加下列一行
    MESSAGE_TYPE_64_DOT(MessageType.NOZZLE_INDEX_64_DOT, NozzleType.NOZZLE_TYPE_64_DOT, 1, 1),       // big data; single Nozzle, single RFID, 4 segments

    MESSAGE_TYPE_1_INCH(MessageType.NOZZLE_INDEX_1_INCH, NozzleType.NOZZLE_TYPE_1_INCH, 1, 1),       // 1寸（25.4），单头，one segment
    MESSAGE_TYPE_1_INCH_DUAL(MessageType.NOZZLE_INDEX_1_INCH_DUAL, NozzleType.NOZZLE_TYPE_1_INCH_DUAL, 2, 2),// 1寸双头（25.4X2）； 2 RFID，2 segments
    MESSAGE_TYPE_1_INCH_TRIPLE(MessageType.NOZZLE_INDEX_1_INCH_TRIPLE, NozzleType.NOZZLE_TYPE_1_INCH_TRIPLE, 3, 3),         //25.4 double Nozzle（12.7X2）,  double RFID，double segment
    MESSAGE_TYPE_1_INCH_FOUR(MessageType.NOZZLE_INDEX_1_INCH_FOUR, NozzleType.NOZZLE_TYPE_1_INCH_FOUR, 4, 4),

// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
    MESSAGE_TYPE_127X5(MessageType.NOZZLE_INDEX_127x5, NozzleType.NOZZLE_TYPE_127x5, 5, 5),         //12.7 x 5
    MESSAGE_TYPE_127X6(MessageType.NOZZLE_INDEX_127x6, NozzleType.NOZZLE_TYPE_127x6, 6, 6),         //12.7 x 6
    MESSAGE_TYPE_127X7(MessageType.NOZZLE_INDEX_127x7, NozzleType.NOZZLE_TYPE_127x7, 7, 7),         //12.7 x 7
    MESSAGE_TYPE_127X8(MessageType.NOZZLE_INDEX_127x8, NozzleType.NOZZLE_TYPE_127x8, 8, 8),         //12.7 x 8
    MESSAGE_TYPE_1INCHX5(MessageType.NOZZLE_INDEX_1_INCHx5, NozzleType.NOZZLE_TYPE_1_INCHx5, 5, 5),         //25.4 x 5
    MESSAGE_TYPE_1INCHX6(MessageType.NOZZLE_INDEX_1_INCHx6, NozzleType.NOZZLE_TYPE_1_INCHx6, 6, 6),         //25.4 x 6
    MESSAGE_TYPE_1INCHX7(MessageType.NOZZLE_INDEX_1_INCHx7, NozzleType.NOZZLE_TYPE_1_INCHx7, 7, 7),         //25.4 x 7
    MESSAGE_TYPE_1INCHX8(MessageType.NOZZLE_INDEX_1_INCHx8, NozzleType.NOZZLE_TYPE_1_INCHx8, 8, 8),         //25.4 x 8
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头

    // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//    MESSAGE_TYPE_12_7_R5(MessageType.NOZZLE_INDEX_12_7_R5, NozzleType.NOZZLE_TYPE_12_7_R5, 1, 1);
    MESSAGE_TYPE_R6X48(MessageType.NOZZLE_INDEX_R6X48, NozzleType.NOZZLE_TYPE_R6X48, 1, 1),
    MESSAGE_TYPE_R6X50(MessageType.NOZZLE_INDEX_R6X50, NozzleType.NOZZLE_TYPE_R6X50, 1, 1),
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
    // End of H.M.Wang 2020-4-17 追加12.7R5头类型

// H.M.Wang 2020-7-23 追加32DN打印头
    MESSAGE_TYPE_32DN(MessageType.NOZZLE_INDEX_32DN, NozzleType.NOZZLE_TYPE_32DN, 1, 1),
// End of H.M.Wang 2020-7-23 追加32DN打印头

// H.M.Wang 2020-8-14 追加32SN打印头
    MESSAGE_TYPE_32SN(MessageType.NOZZLE_INDEX_32SN, NozzleType.NOZZLE_TYPE_32SN, 1, 1),
// End of H.M.Wang 2020-8-14 追加32SN打印头

// H.M.Wang 2020-8-26 追加64SN打印头
    MESSAGE_TYPE_64SN(MessageType.NOZZLE_INDEX_64SN, NozzleType.NOZZLE_TYPE_64SN, 1, 1),
// End of H.M.Wang 2020-8-26 追加64SN打印头

// H.M.Wang 2021-1-19 追加9mm打印头
    MESSAGE_TYPE_9MM(MessageType.NOZZLE_INDEX_9MM, NozzleType.NOZZLE_TYPE_9MM, 1, 1),
// End of H.M.Wang 2021-1-19 追加9mm打印头

// H.M.Wang 2021-3-6 追加E6X48,E6X50头
    MESSAGE_TYPE_E6X48(MessageType.NOZZLE_INDEX_E6X48, NozzleType.NOZZLE_TYPE_E6X48, 1, 1),
    MESSAGE_TYPE_E6X50(MessageType.NOZZLE_INDEX_E6X50, NozzleType.NOZZLE_TYPE_E6X50, 1, 1),
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
    MESSAGE_TYPE_E6X1(MessageType.NOZZLE_INDEX_E6X1, NozzleType.NOZZLE_TYPE_E6X1, 1, 1),
// H.M.Wang 2021-8-16 追加96DN头
    MESSAGE_TYPE_96DN(MessageType.NOZZLE_INDEX_96DN, NozzleType.NOZZLE_TYPE_96DN, 1, 1),
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
    MESSAGE_TYPE_E5X48(MessageType.NOZZLE_INDEX_E5X48, NozzleType.NOZZLE_TYPE_E5X48, 1, 1),
    MESSAGE_TYPE_E5X50(MessageType.NOZZLE_INDEX_E5X50, NozzleType.NOZZLE_TYPE_E5X50, 1, 1),
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2022-4-29 追加25.4x10头类型
    MESSAGE_TYPE_254X10(MessageType.NOZZLE_INDEX_254X10, NozzleType.NOZZLE_TYPE_254X10, 1, 1),//10,10
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2022-5-27 追加32x2头类型
//  新增32x2喷头类型， 实际就是64点双列的，只不过打印buffer 要转换
//  奇数bit,进上32, 偶数bit进下32 bit, 然后 上32bit , 后移4 列。大概原理如此， 后面要实验调整
    MESSAGE_TYPE_32X2(MessageType.NOZZLE_INDEX_32X2, NozzleType.NOZZLE_TYPE_32X2, 1, 1),
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
    MESSAGE_TYPE_32X3(MessageType.NOZZLE_INDEX_32X3, NozzleType.NOZZLE_TYPE_32X3, 3, 3),
    MESSAGE_TYPE_32X4(MessageType.NOZZLE_INDEX_32X4, NozzleType.NOZZLE_TYPE_32X4, 4, 4),
    MESSAGE_TYPE_32X5(MessageType.NOZZLE_INDEX_32X5, NozzleType.NOZZLE_TYPE_32X5, 5, 5),
    MESSAGE_TYPE_32X6(MessageType.NOZZLE_INDEX_32X6, NozzleType.NOZZLE_TYPE_32X6, 6, 6),
    MESSAGE_TYPE_32X7(MessageType.NOZZLE_INDEX_32X7, NozzleType.NOZZLE_TYPE_32X7, 7, 7),
    // End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
// H.M.Wang 2022-10-19 追加64SLANT头。
// 此类型暂时理解为两个32 点喷头，1-32点和33-64点。
// 原有 喷头一  镜像/倒置/偏移，  控制1 头。    二头的控制二头。
// 增加 Slant2 参数。 用于控制第二喷头倾斜。（原有SLANT  用于控制第一个32 点喷头倾斜）
// 增加 “调整2”“/”ADJ2”参数，  用于调整喷头2的宽度，规则：默认值是0， 设为n, 则展宽为 32+n
    MESSAGE_TYPE_64SLANT(MessageType.NOZZLE_INDEX_64SLANT, NozzleType.NOZZLE_TYPE_64SLANT, 1, 1),
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2023-7-29 追加48点头
    MESSAGE_TYPE_48_DOT(MessageType.NOZZLE_INDEX_48_DOT, NozzleType.NOZZLE_TYPE_48_DOT, 1, 1),
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
    MESSAGE_TYPE_22MM(MessageType.NOZZLE_INDEX_22MM, NozzleType.NOZZLE_TYPE_22MM, 1, 1),
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
    MESSAGE_TYPE_22MMX2(MessageType.NOZZLE_INDEX_22MMX2, NozzleType.NOZZLE_TYPE_22MMX2, 2, 1),
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型，特点是不允许平移，不允许镜像，不允许反转，只允许旋转，旋转按着标准方法整体旋转
    MESSAGE_TYPE_64DOTONE(MessageType.NOZZLE_INDEX_64DOTONE, NozzleType.NOZZLE_TYPE_64DOTONE, 1, 1),
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型，特点是不允许平移，不允许镜像，不允许反转，只允许旋转，旋转按着标准方法整体旋转
// H.M.Wang 2024-9-10 增加一个头类型，所有动作与64DOTONE相同，仅在SLANT的时候，纵列每行相对上行右移一点，但第17，33，49行回调至与第1行位置相同。
    MESSAGE_TYPE_16DOTX4(MessageType.NOZZLE_INDEX_16DOTX4, NozzleType.NOZZLE_TYPE_16DOTX4, 1, 1);
// End of H.M.Wang 2024-9-10 增加一个头类型，所有动作与64DOTONE相同，仅在SLANT的时候，纵列每行相对上行右移一点，但第17，33，49行回调至与第1行位置相同。

    public final int mIndex;
    public final int mType;
    public final int mHeads;
    public final int mSegments;         // 好像没有被用到 - H.M Wang
    public final boolean editZoomable; // 编辑状态下是否支持缩放

    private int mHeight;
    private float scaleW;
    private float scaleH;
    // 生成变量buffer bin时，宽度缩放因子, 只有16点和32点不需要缩放，其他喷头减半
    private int factorScale;

    public final boolean shiftEnable;   //是否支持位移
    public final boolean mirrorEnable;  // 是否支持镜像
    public final boolean reverseEnable; // 是否支持反转
    public final boolean rotateAble;
    public final boolean buffer8Enable; // buffer 8 times extension Switch
    /**
     *
     * @param type      喷头类型
     * @param heads     喷头数
     * @param segment   打印分段
     */
    private PrinterNozzle(int index, int type, int heads, int segment) {

        this.mIndex = index;
        this.mType = type;
        this.mHeads = heads;
        this.mSegments = segment;

        switch (mType) {
            case NozzleType.NOZZLE_TYPE_16_DOT:
            case NozzleType.NOZZLE_TYPE_32_DOT://大字机
// H.M.Wang 2020-3-2 修改64点头，不支持反转和镜像
                reverseEnable = true;
                shiftEnable = true;
                mirrorEnable = true;
                rotateAble = true;
                break;
// H.M.Wang 2020-7-23 追加32DN打印头
            case NozzleType.NOZZLE_TYPE_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
            case NozzleType.NOZZLE_TYPE_32SN:
// End of H.M.Wang 2020-8-18 追加32SN打印头
// H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
            case NozzleType.NOZZLE_TYPE_32X4:
            case NozzleType.NOZZLE_TYPE_32X5:
            case NozzleType.NOZZLE_TYPE_32X6:
            case NozzleType.NOZZLE_TYPE_32X7:
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            // H.M.Wang 追加下列一行
            case NozzleType.NOZZLE_TYPE_64_DOT:
// H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-9-1 取消原有的64SN的设置
//            case NozzleType.NOZZLE_TYPE_64SN:
// End of H.M.Wang 2022-9-1 取消原有的64SN的设置
// End of H.M.Wang 2020-8-26 追加64SN打印头
                reverseEnable = false;
//                reverseEnable = true;
                shiftEnable = true;
                mirrorEnable = false;
//                mirrorEnable = true;
                rotateAble = true;
                break;
// End of H.M.Wang 2020-3-2 修改64点头，不支持反转和镜像
// H.M.Wang 2022-9-1 因为64SN的变形处理是假借有4个头来在正式处理流程里面做的，因此，  mirrorEnable和   reverseEnable也都必须有效，否则不会被处理
            case NozzleType.NOZZLE_TYPE_64SN:
// H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2025-9-15 64DOTONE头允许reverse和shift
            case NozzleType.NOZZLE_TYPE_64DOTONE:
// End of H.M.Wang 2025-9-15 64DOTONE头允许reverse和shift
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
                reverseEnable = true;
                shiftEnable = true;
                mirrorEnable = true;
                rotateAble = true;
                break;
// End of H.M.Wang 2022-9-1 因为64SN的变形处理是假借有4个头来在正式处理流程里面做的，因此，  mirrorEnable和   reverseEnable也都必须有效，否则不会被处理
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case NozzleType.NOZZLE_TYPE_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                reverseEnable = false;
                shiftEnable = false;
                mirrorEnable = true;
                rotateAble = true;
                break;
// H.M.Wang 2023-7-29 追加48点头
            case NozzleType.NOZZLE_TYPE_48_DOT:
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2022-8-19 修改96DN头的操作。mirror打开，否则打印方向调整会不起作用，shift关闭，否则会导致标准的便宜处理和96DN的特殊处理叠加，出现错位的问题
            case NozzleType.NOZZLE_TYPE_96DN:
                shiftEnable = false;
                mirrorEnable = true;
                reverseEnable = false;
                rotateAble = false;
                break;
// End of H.M.Wang 2022-8-19 修改96DN头的操作。mirror打开，否则打印方向调整会不起作用，shift关闭，否则会导致标准的便宜处理和96DN的特殊处理叠加，出现错位的问题
// End of H.M.Wang 2021-8-16 追加96DN头
            /**
             * for 'Nova' header, shift & mirror is forbiden;
             */
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case NozzleType.NOZZLE_TYPE_22MM:
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
            case NozzleType.NOZZLE_TYPE_22MMX2:
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
                shiftEnable = false;
// H.M.Wang 2025-2-17 22MM打印头系列允许镜像和倒置
//                mirrorEnable = false;
//                reverseEnable = false;
                mirrorEnable = true;
                reverseEnable = true;
// End of H.M.Wang 2025-2-17 22MM打印头系列允许镜像和倒置
                rotateAble = false;
                break;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case 14:
                shiftEnable = false;
                mirrorEnable = false;
                reverseEnable = false;
                rotateAble = false;
                break;
            default:
                shiftEnable = true;
                mirrorEnable = true;
                reverseEnable = false;
                rotateAble = false;
                break;
        }

        // some features during editing
        switch (mType) {
            case NozzleType.NOZZLE_TYPE_16_DOT:
            case NozzleType.NOZZLE_TYPE_32_DOT:

// H.M.Wang 2020-7-23 追加32DN打印头
            case NozzleType.NOZZLE_TYPE_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
            case NozzleType.NOZZLE_TYPE_32X4:
            case NozzleType.NOZZLE_TYPE_32X5:
            case NozzleType.NOZZLE_TYPE_32X6:
            case NozzleType.NOZZLE_TYPE_32X7:
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
// H.M.Wang 2020-8-26 追加64SN打印头
            case NozzleType.NOZZLE_TYPE_64SN:
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2021-8-16 追加96DN头
            case NozzleType.NOZZLE_TYPE_96DN:
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case NozzleType.NOZZLE_TYPE_64DOTONE:
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case NozzleType.NOZZLE_TYPE_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                editZoomable = false;
                buffer8Enable = true;
                factorScale = 1;
                break;
// H.M.Wang 2023-1-6 取消64DOT头的旋转
            // H.M.Wang 追加下列一行
            case NozzleType.NOZZLE_TYPE_64_DOT:
// H.M.Wang 2023-7-18 32SN禁止倾斜
// H.M.Wang 2020-8-18 追加32SN打印头
            case NozzleType.NOZZLE_TYPE_32SN:
// End of H.M.Wang 2020-8-18 追加32SN打印头
// End of H.M.Wang 2023-7-18 32SN禁止倾斜
// H.M.Wang 2023-7-29 追加48点头
            case NozzleType.NOZZLE_TYPE_48_DOT:
// End of H.M.Wang 2023-7-29 追加48点头
                editZoomable = false;
                buffer8Enable = false;
                factorScale = 1;
                break;
// End of H.M.Wang 2023-1-6 取消64DOT头的旋转
            default:
                factorScale = 2;
                editZoomable = true;
                buffer8Enable = false;
                break;
        }

        initScale();
        initHeight();
    }

    /*
        这里的高度是实际打印时的打印缓冲区的高度。但是12.7xN头，由于历史原因，在下发之前，在BinInfo里面会做19字节到20字节的补位
     */
    private void initHeight() {
        switch (mType) {
            case NozzleType.NOZZLE_TYPE_12_7:
            // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//            case NozzleType.NOZZLE_TYPE_12_7_R5:
            case NozzleType.NOZZLE_TYPE_R6X48:
            case NozzleType.NOZZLE_TYPE_R6X50:
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
            // End of H.M.Wang 2020-4-17 追加12.7R5头类型
                mHeight = 152;
                break;
            case NozzleType.NOZZLE_TYPE_25_4:
                mHeight = 152 * 2;
                break;
            case NozzleType.NOZZLE_TYPE_38_1:
                mHeight = 152 * 3;
                break;
            case NozzleType.NOZZLE_TYPE_50_8:
                mHeight = 152 * 4;
                break;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_127x5:
                mHeight = 152 * 5;
                break;
            case NozzleType.NOZZLE_TYPE_127x6:
                mHeight = 152 * 6;
                break;
            case NozzleType.NOZZLE_TYPE_127x7:
                mHeight = 152 * 7;
                break;
            case NozzleType.NOZZLE_TYPE_127x8:
                mHeight = 152 * 8;
                break;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_16_DOT:
            case NozzleType.NOZZLE_TYPE_32_DOT:
// H.M.Wang 2020-7-23 追加32DN打印头
            case NozzleType.NOZZLE_TYPE_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
            case NozzleType.NOZZLE_TYPE_32SN:
// End of H.M.Wang 2020-8-18 追加32SN打印头
                mHeight = 32;
                break;

// H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
            // H.M.Wang 追加下列三行
            case NozzleType.NOZZLE_TYPE_64_DOT:
// H.M.Wang 2020-8-26 追加64SN打印头
            case NozzleType.NOZZLE_TYPE_64SN:
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case NozzleType.NOZZLE_TYPE_64DOTONE:
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case NozzleType.NOZZLE_TYPE_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                mHeight = 64;
                break;
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
                mHeight = 32*3;
                break;
            case NozzleType.NOZZLE_TYPE_32X4:
                mHeight = 32*4;
                break;
            case NozzleType.NOZZLE_TYPE_32X5:
                mHeight = 32*5;
                break;
            case NozzleType.NOZZLE_TYPE_32X6:
                mHeight = 32*6;
                break;
            case NozzleType.NOZZLE_TYPE_32X7:
                mHeight = 32*7;
                break;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_1_INCH:
                mHeight = 320;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_DUAL:
                mHeight = 640;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_TRIPLE:
                mHeight = 320 * 3;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_FOUR:
                mHeight = 320 * 4;
                break;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_1_INCHx5:
                mHeight = 320 * 5;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx6:
                mHeight = 320 * 6;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx7:
                mHeight = 320 * 7;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx8:
                mHeight = 320 * 8;
                break;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_9MM:
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X48:
            case NozzleType.NOZZLE_TYPE_E6X50:
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X1:
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
            case NozzleType.NOZZLE_TYPE_E5X48:
            case NozzleType.NOZZLE_TYPE_E5X50:
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
                mHeight = 112;
                break;
// H.M.Wang 2021-8-16 追加96DN头
            case NozzleType.NOZZLE_TYPE_96DN:
                mHeight = 96;
                break;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2022-4-29 追加25.4x10头类型
            case NozzleType.NOZZLE_TYPE_254X10:
                mHeight = 3200;
                break;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2023-7-29 追加48点头
            case NozzleType.NOZZLE_TYPE_48_DOT:
                mHeight = 48;
                break;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case NozzleType.NOZZLE_TYPE_22MM:
                if(Hp22mm.HEAD_TYPE == Hp22mm.HEAD_TYPE_1056) {
                    mHeight = 1056;
                } else {
                    mHeight = 528;
                }
                break;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
            case NozzleType.NOZZLE_TYPE_22MMX2:
                if(Hp22mm.HEAD_TYPE == Hp22mm.HEAD_TYPE_1056) {
                    mHeight = 2112;
                } else {
                    mHeight = 1056;
                }
                break;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
        }
    }

    public int getHeight() {
        return mHeight;
    }

    /*
        该函数取得的scaleX值是为了在标准内部坐标（152）到打印的绘制区域之间的转换。具体的如下：
        NOZZLE_TYPE_12_7
        NOZZLE_TYPE_R6X48
        NOZZLE_TYPE_R6X50
            基础打印头为12.7打印头
            ratioX = 1.0f。换算成pixels为152，即高度按152点，19个字节，在下发的时候，补齐一个字节，凑足20个字节（因为FPGA下发是两个字节一下发，所以每列必须有偶数个字节）
            生成bin的时候，高度为152个点，19个字节
        NOZZLE_TYPE_25_4
            基础打印头为12.7x2打印头
            ratioX = 2.0f。换算成pixels为304，即高度按304点，每个头19个字节，在下发的时候，补齐一个字节，凑足20个字节（因为FPGA下发是两个字节一下发，所以每列必须有偶数个字节）
            生成bin的时候，高度为304个点，19x2个字节
        NOZZLE_TYPE_38_1
            基础打印头为12.7x3打印头
            ratioX = 3.0f。换算成pixels为456，即高度按456点，每个头19个字节，在下发的时候，补齐一个字节，凑足20个字节（因为FPGA下发是两个字节一下发，所以每列必须有偶数个字节）
            生成bin的时候，高度为456个点，19x3个字节
        NOZZLE_TYPE_50_8
            基础打印头为12.7x4打印头
            ratioX = 4.0f。换算成pixels为608，即高度按608点，每个头19个字节，在下发的时候，补齐一个字节，凑足20个字节（因为FPGA下发是两个字节一下发，所以每列必须有偶数个字节）
            生成bin的时候，高度为608个点，19x4个字节

        以上由于历史原因，每个单头补齐的一个字节是在BinInfo类里面动态实现的。其实，在绘图的时候按着152xN进行绘制，然后在每个头之间插入一个字节的空挡，保存bin的时候直接保存每个单头的数据为20字节的话就更加方便了，
        后面的25.4xN头就是这么实现的

        NOZZLE_TYPE_1_INCH
            基础打印头为25.4x1打印头
            ratioX = 1.0f * 308 / 152。换算成pixels为308，即在绘制的时候的高度是308，但是下发之前会在后面插入空行，调整为320个pixels的高度
            生成bin的时候，高度为320个点，即40个字节，其中只有38.5个字节是实际数据，后面的1.5个字节是补空
        NOZZLE_TYPE_1_INCH_DUAL:
        NOZZLE_TYPE_1_INCH_TRIPLE:
        NOZZLE_TYPE_1_INCH_FOUR:
            同理，参照NOZZLE_TYPE_1_INCH。纵向放大相应倍数
        NOZZLE_TYPE_16_DOT
            基础打印头为16DOT打印头（大字机）
            ratioX = 1.0f * 16 / 152。就是16个点高，绘制在这16点内，但是生成bin时会生成32点高，下部16点为空，下发时也是下发每列32点（4个字节）
        NOZZLE_TYPE_32_DOT
        NOZZLE_TYPE_32DN
        NOZZLE_TYPE_32SN
            基础打印头为32DOT打印头（大字机）
            ratioX = 1.0f * 32 / 152。就是32个点高，绘制在这32点内,并且生成bin时会生成32点高，下发时也是下发每列32点（4个字节）
        NOZZLE_TYPE_64_DOT
        NOZZLE_TYPE_64SN
            基础打印头为64DOT打印头（大字机）
            ratioX = 1.0f * 64 / 152。就是64个点高，绘制在这64点内,并且生成bin时会生成64点高，下发时也是下发每列64点（8个字节）
        NOZZLE_TYPE_96DN
            基础打印头为96DOT打印头（大字机）
            ratioX = 1.0f * 96 / 152。就是96个点高，绘制在这96点内,并且生成bin时会生成96点高，下发时也是下发每列64点（12个字节）
            case NozzleType.NOZZLE_TYPE_9MM:
        NOZZLE_TYPE_9MM
        NOZZLE_TYPE_E6X48
        NOZZLE_TYPE_E6X50
        NOZZLE_TYPE_E6X1
        NOZZLE_TYPE_E5X48
        NOZZLE_TYPE_E5X50
            基础打印头为9MM打印头
            ratioX = 1.0f * 104 / 152。就是104个点高，绘制在这104点内,生成bin时会生成112点高，下发时也是下发每列112点
     */
    private void initScale() {
        switch (mType) {
            case NozzleType.NOZZLE_TYPE_12_7:
            // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//            case NozzleType.NOZZLE_TYPE_12_7_R5:
            case NozzleType.NOZZLE_TYPE_R6X48:
            case NozzleType.NOZZLE_TYPE_R6X50:
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
            // End of H.M.Wang 2020-4-17 追加12.7R5头类型
                scaleW = 1f;
                scaleH = 1f;
                break;
            case NozzleType.NOZZLE_TYPE_25_4:
                scaleW = 2f;
                scaleH = 2f;
                break;
            case NozzleType.NOZZLE_TYPE_38_1:
                scaleW = 3f;
                scaleH = 3f;
                break;
            case NozzleType.NOZZLE_TYPE_50_8:
                scaleW = 4f;
                scaleH = 4f;
                break;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_127x5:
                scaleW = 5f;
                scaleH = 5f;
                break;
            case NozzleType.NOZZLE_TYPE_127x6:
                scaleW = 6f;
                scaleH = 6f;
                break;
            case NozzleType.NOZZLE_TYPE_127x7:
                scaleW = 7f;
                scaleH = 7f;
                break;
            case NozzleType.NOZZLE_TYPE_127x8:
                scaleW = 8f;
                scaleH = 8f;
                break;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            // H.M.Wang 修改下列16行。调整25.4xn头的放大比例
            case NozzleType.NOZZLE_TYPE_1_INCH:
                scaleW = 1.0f * 308 / 152;
                scaleH = 1.0f * 308 / 152;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_DUAL:
                scaleW = 2.0f * 308 / 152;
                scaleH = 2.0f * 308 / 152;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_TRIPLE:
                scaleW = 3.0f * 308 / 152;
                scaleH = 3.0f * 308 / 152;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_FOUR:
                scaleW = 4.0f * 308 / 152;
                scaleH = 4.0f * 308 / 152;
                break;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_1_INCHx5:
                scaleW = 5.0f * 308 / 152;
                scaleH = 5.0f * 308 / 152;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx6:
                scaleW = 6.0f * 308 / 152;
                scaleH = 6.0f * 308 / 152;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx7:
                scaleW = 7.0f * 308 / 152;
                scaleH = 7.0f * 308 / 152;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx8:
                scaleW = 8.0f * 308 / 152;
                scaleH = 8.0f * 308 / 152;
                break;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_16_DOT:
                scaleW = 16f/152;
                scaleH = 16f/152;
                break;
            case NozzleType.NOZZLE_TYPE_32_DOT:
// H.M.Wang 2020-7-23 追加32DN打印头
            case NozzleType.NOZZLE_TYPE_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
            case NozzleType.NOZZLE_TYPE_32SN:
// End of H.M.Wang 2020-8-18 追加32SN打印头
                scaleW = 32f/152;
                scaleH = 32f/152;
                break;

// H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
            // H.M.Wang 追加下列四行
            case NozzleType.NOZZLE_TYPE_64_DOT:
// H.M.Wang 2020-8-26 追加64SN打印头
            case NozzleType.NOZZLE_TYPE_64SN:
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case NozzleType.NOZZLE_TYPE_64DOTONE:
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case NozzleType.NOZZLE_TYPE_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                scaleW = 64f/152;
                scaleH = 64f/152;
                break;
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
                scaleW = 32f*3/152;
                scaleH = 32f*3/152;
                break;
            case NozzleType.NOZZLE_TYPE_32X4:
                scaleW = 32f*4/152;
                scaleH = 32f*4/152;
                break;
            case NozzleType.NOZZLE_TYPE_32X5:
                scaleW = 32f*5/152;
                scaleH = 32f*5/152;
                break;
            case NozzleType.NOZZLE_TYPE_32X6:
                scaleW = 32f*6/152;
                scaleH = 32f*6/152;
                break;
            case NozzleType.NOZZLE_TYPE_32X7:
                scaleW = 32f*7/152;
                scaleH = 32f*7/152;
                break;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_9MM:
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X48:
            case NozzleType.NOZZLE_TYPE_E6X50:
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
// H.M.Wang 2021-3-30 还应该是104
// H.M.Wang 2021-3-22 原来的104没有考虑双字节，导致与高度设置的112不一致，生成的图片纵向变形
            case NozzleType.NOZZLE_TYPE_E6X1:
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
            case NozzleType.NOZZLE_TYPE_E5X48:
            case NozzleType.NOZZLE_TYPE_E5X50:
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
                scaleW = 104f/152;
                scaleH = 104f/152;
// End of H.M.Wang 2021-3-22 原来的104没有考虑双字节，导致与高度设置的112不一致，生成的图片纵向变形
// End of H.M.Wang 2021-3-30 还应该是104
                break;
// H.M.Wang 2021-8-16 追加96DN头
            case NozzleType.NOZZLE_TYPE_96DN:
                scaleW = 96f/152;
                scaleH = 96f/152;
                break;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2022-4-29 追加25.4x10头类型
            case NozzleType.NOZZLE_TYPE_254X10:
                scaleW = 10.0f * 308 / 152;
                scaleH = 10.0f * 308 / 152;
                break;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2023-7-29 追加48点头
            case NozzleType.NOZZLE_TYPE_48_DOT:
                scaleW = 48f/152;
                scaleH = 48f/152;
                break;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case NozzleType.NOZZLE_TYPE_22MM:
//                scaleW = 1056f/152;
                scaleW = 264f/152;
                if(Hp22mm.HEAD_TYPE == Hp22mm.HEAD_TYPE_1056) {
                    scaleH = 1056f/152;
                } else {
                    scaleH = 528f/152;
                }
                break;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
            case NozzleType.NOZZLE_TYPE_22MMX2:
//                scaleW = 1056f/152;
                scaleW = 528f/152;
                if(Hp22mm.HEAD_TYPE == Hp22mm.HEAD_TYPE_1056) {
                    scaleH = 2112f/152;
                } else {
                    scaleH = 1056f/152;
                }
                break;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
            default:
                scaleW = 1f;
                scaleH = 1f;
                break;
        }
    }
// H.M.Wang 2025-12-9 将散在各处的直接判断大字机等的功能统一到本类中
    public boolean isBigdotType() {
        switch (mIndex) {
            case MessageType.NOZZLE_INDEX_16_DOT:
            case MessageType.NOZZLE_INDEX_32_DOT:
// H.M.Wang 2020-7-23 追加32DN打印头
            case MessageType.NOZZLE_INDEX_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-17 追加32SN打印头
            case MessageType.NOZZLE_INDEX_32SN:
// End of H.M.Wang 2020-8-17 追加32SN打印头
// H.M.Wang 2022-5-27 追加32x2头类型
            case MessageType.NOZZLE_INDEX_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
            case NozzleType.NOZZLE_TYPE_32X4:
            case NozzleType.NOZZLE_TYPE_32X5:
            case NozzleType.NOZZLE_TYPE_32X6:
            case NozzleType.NOZZLE_TYPE_32X7:
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case MessageType.NOZZLE_INDEX_64_DOT:
// H.M.Wang 2020-8-26 追加64SN打印头
            case MessageType.NOZZLE_INDEX_64SN:
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case MessageType.NOZZLE_INDEX_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case MessageType.NOZZLE_INDEX_64DOTONE:
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case MessageType.NOZZLE_INDEX_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2023-7-29 追加48点头
            case MessageType.NOZZLE_INDEX_48_DOT:
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2021-8-16 追加96DN头
            case MessageType.NOZZLE_INDEX_96DN:
// End of H.M.Wang 2021-8-16 追加96DN头
                return true;
            default:
                return false;
        }
    }
// End of H.M.Wang 2025-12-9 将散在各处的直接判断大字机等的功能统一到本类中

    // H.M.Wang 追加这个函数以便将pixel和mm中间进行转化
    public float getPhisicalRatio() {
        float ratio = 0.0f;

        switch (mType) {
            case NozzleType.NOZZLE_TYPE_12_7:
            // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//            case NozzleType.NOZZLE_TYPE_12_7_R5:
            case NozzleType.NOZZLE_TYPE_R6X48:
            case NozzleType.NOZZLE_TYPE_R6X50:
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
            // End of H.M.Wang 2020-4-17 追加12.7R5头类型
                ratio = 1.0f * 12.7f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_25_4:
                ratio = 2.0f * 12.7f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_38_1:
                ratio = 3.0f * 12.7f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_50_8:
                ratio = 4.0f * 12.7f / 304;
                break;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_127x5:
                ratio = 5.0f * 12.7f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_127x6:
                ratio = 6.0f * 12.7f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_127x7:
                ratio = 7.0f * 12.7f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_127x8:
                ratio = 8.0f * 12.7f / 304;
                break;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_1_INCH:
                ratio = 1.0f * 25.4f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_DUAL:
                ratio = 2.0f * 25.4f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_TRIPLE:
                ratio = 3.0f * 25.4f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCH_FOUR:
                ratio = 4.0f * 25.4f / 304;
                break;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_1_INCHx5:
                ratio = 5.0f * 25.4f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx6:
                ratio = 6.0f * 25.4f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx7:
                ratio = 7.0f * 25.4f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_1_INCHx8:
                ratio = 8.0f * 25.4f / 304;
                break;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_16_DOT:
                ratio = 16f / 304;
                break;
            case NozzleType.NOZZLE_TYPE_32_DOT:
// H.M.Wang 2020-7-23 追加32DN打印头
            case NozzleType.NOZZLE_TYPE_32DN:
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-18 追加32SN打印头
            case NozzleType.NOZZLE_TYPE_32SN:
// End of H.M.Wang 2020-8-18 追加32SN打印头
                ratio = 32f / 304;
                break;
// H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_32X2:
// End of H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_64_DOT:
// H.M.Wang 2020-8-26 追加64SN打印头
            case NozzleType.NOZZLE_TYPE_64SN:
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_64SLANT:
// End of H.M.Wang 2022-10-19 追加64SLANT头。
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case NozzleType.NOZZLE_TYPE_64DOTONE:
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case NozzleType.NOZZLE_TYPE_16DOTX4:
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
                ratio = 64f / 304;
                break;
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
                ratio = 32f * 3 / 304;
                break;
            case NozzleType.NOZZLE_TYPE_32X4:
                ratio = 32f * 4 / 304;
                break;
            case NozzleType.NOZZLE_TYPE_32X5:
                ratio = 32f * 5 / 304;
                break;
            case NozzleType.NOZZLE_TYPE_32X6:
                ratio = 32f * 6 / 304;
                break;
            case NozzleType.NOZZLE_TYPE_32X7:
                ratio = 32f * 7 / 304;
                break;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_9MM:
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X48:
            case NozzleType.NOZZLE_TYPE_E6X50:
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X1:
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
            case NozzleType.NOZZLE_TYPE_E5X48:
            case NozzleType.NOZZLE_TYPE_E5X50:
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2021-3-22 根据其他头类型的算式，这里应该是208，而不是104
                ratio = 1.0f * 9.0f / 208;
// End of H.M.Wang 2021-3-22 根据其他头类型的算式，这里应该是208，而不是104
                break;
// H.M.Wang 2021-8-16 追加96DN头
            case NozzleType.NOZZLE_TYPE_96DN:
                ratio = 96f / 304;
                break;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2022-4-29 追加25.4x10头类型
            case NozzleType.NOZZLE_TYPE_254X10:
                ratio = 10.0f * 25.4f / 304;
                break;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2023-7-29 追加48点头
            case NozzleType.NOZZLE_TYPE_48_DOT:
                ratio = 48f / 304;
                break;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case NozzleType.NOZZLE_TYPE_22MM:
                ratio = 1.0f * 22.0f / 304;
                break;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
            case NozzleType.NOZZLE_TYPE_22MMX2:
                ratio = 1.0f * 44.0f / 304;
                break;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
            default:
                break;
        }
        return ratio;
    }

    public  float getScaleW() {
        return scaleW;
    }

    public  float getScaleH() {
        return scaleH;
    }

    public int getFactorScale() {
        if (factorScale != 0) {
            return factorScale;
        } else {
            return 2;
        }
    }
    /**
     * Nozzle height multiple of 152
     */
    public int factor() {
        if(this == MESSAGE_TYPE_9MM) return 1;
        if(this == MESSAGE_TYPE_22MM) return 1;
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
        if(this == MESSAGE_TYPE_22MMX2) return 1;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
        return mHeight / 152;
    }

// H.M.Wang 2020-4-17 追加12.7R5头类型
    // 返回对于R5头，横向复制的份数
    public static final int     R6_PRINT_COPY_NUM = 6;
    public static final int     R6_HEAD_NUM = 6;
    public static final int     R6X48_MAX_COL_NUM_EACH_UNIT = 48 * 6;
    public static final int     R6X50_MAX_COL_NUM_EACH_UNIT = 50 * 6;

// H.M.Wang 2021-3-6 追加E6X48,E6X50头
// H.M.Wang 2021-3-18 取消奇数行(第一行为0)的向后位移一个单位的操作)
    public static final int     E6_PRINT_COPY_NUM = 5;      // 没有单数行向后位移一个单位的操作，因此不像RX头一样，只复制5次
// End of H.M.Wang 2021-3-18 取消奇数行(第一行为0)的向后位移一个单位的操作)
    public static final int     E6_HEAD_NUM = 6;
    public static final int     E6X48_MAX_COL_NUM_EACH_UNIT = 48 * 6;
    public static final int     E6X50_MAX_COL_NUM_EACH_UNIT = 50 * 6;
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头

// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2024-4-18 在2024-4-17的修改中，复制方法按E6处理
//    public static final int     E5_PRINT_COPY_NUM = 6;
//    public static final int     E5_HEAD_NUM = 5;        // 减锁等等操作按着5个头来计算，但是生成打印缓冲区的时候生成相当于6个头的数据
    public static final int     E5_PRINT_COPY_NUM = E6_PRINT_COPY_NUM;
    public static final int     E5_HEAD_NUM = E6_HEAD_NUM;
// End of H.M.Wang 2024-4-18 在2024-4-17的修改中，复制方法按E6处理
    public static final int     E5X48_MAX_COL_NUM_EACH_UNIT = 48 * 6;
    public static final int     E5X50_MAX_COL_NUM_EACH_UNIT = 50 * 6;
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
/*    public int getRTimes() {
        if(mIndex == MessageType.NOZZLE_INDEX_12_7_R5) {
            return 6;
        } else {
            return 0;
        }
    }

    // 返回对于R5头，纵向复制的份数（对应的头数）
    public int getRHeads() {
        if(mIndex == MessageType.NOZZLE_INDEX_12_7_R5) {
            return 6;
        } else {
            return 0;
        }
    }

//    public int getRMaxCols(int ratio) {
    public int getRMaxCols() {
        if(mIndex == MessageType.NOZZLE_INDEX_12_7_R5) {
//            return 48 * 6 * ratio;
            return 48 * 6;
        } else {
            return 0;
        }
    }
*/
    // End of H.M.Wang 2020-4-17 追加12.7R5头类型

    public static PrinterNozzle getInstance(int index) {
// H.M.Wang 2020-7-27 修改打印头的获取方式，不依据信息中的打印头信息获取，而是依据系统参数设置获取
        SystemConfigFile configFile = SystemConfigFile.getInstance();
        if(null != configFile) {
            return configFile.getPNozzle();
        }
// H.M.Wang 2020-7-27 修改打印头的获取方式，不依据信息中的打印头信息获取，而是依据系统参数设置获取

        switch (index) {
            case MessageType.NOZZLE_INDEX_12_7:
                return MESSAGE_TYPE_12_7;
            case MessageType.NOZZLE_INDEX_25_4:
                return MESSAGE_TYPE_25_4;
            case MessageType.NOZZLE_INDEX_38_1:
                return MESSAGE_TYPE_38_1;
            case MessageType.NOZZLE_INDEX_50_8:
                return MESSAGE_TYPE_50_8;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case MessageType.NOZZLE_INDEX_127x5:
                return MESSAGE_TYPE_127X5;
            case MessageType.NOZZLE_INDEX_127x6:
                return MESSAGE_TYPE_127X6;
            case MessageType.NOZZLE_INDEX_127x7:
                return MESSAGE_TYPE_127X7;
            case MessageType.NOZZLE_INDEX_127x8:
                return MESSAGE_TYPE_127X8;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case MessageType.NOZZLE_INDEX_16_DOT:
                return MESSAGE_TYPE_16_DOT;
            case MessageType.NOZZLE_INDEX_32_DOT:
                return MESSAGE_TYPE_32_DOT;
// H.M.Wang 2020-7-23 追加32DN打印头
            case MessageType.NOZZLE_INDEX_32DN:
                return MESSAGE_TYPE_32DN;
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-17 追加32SN打印头
            case MessageType.NOZZLE_INDEX_32SN:
                return MESSAGE_TYPE_32SN;
// End of H.M.Wang 2020-8-17 追加32SN打印头

// H.M.Wang 2022-5-27 追加32x2头类型
            case MessageType.NOZZLE_INDEX_32X2:
                return MESSAGE_TYPE_32X2;
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case MessageType.NOZZLE_INDEX_32X3:
                return MESSAGE_TYPE_32X3;
            case MessageType.NOZZLE_INDEX_32X4:
                return MESSAGE_TYPE_32X4;
            case MessageType.NOZZLE_INDEX_32X5:
                return MESSAGE_TYPE_32X5;
            case MessageType.NOZZLE_INDEX_32X6:
                return MESSAGE_TYPE_32X6;
            case MessageType.NOZZLE_INDEX_32X7:
                return MESSAGE_TYPE_32X7;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            // H.M.Wang 追加下列两行
            case MessageType.NOZZLE_INDEX_64_DOT:
                return MESSAGE_TYPE_64_DOT;
// H.M.Wang 2020-8-26 追加64SN打印头
            case MessageType.NOZZLE_INDEX_64SN:
                return MESSAGE_TYPE_64SN;
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case MessageType.NOZZLE_INDEX_64SLANT:
                return MESSAGE_TYPE_64SLANT;
// End of H.M.Wang 2022-10-19 追加64SLANT头。
            case MessageType.NOZZLE_INDEX_1_INCH:
                return MESSAGE_TYPE_1_INCH;
            case MessageType.NOZZLE_INDEX_1_INCH_DUAL:
                return MESSAGE_TYPE_1_INCH_DUAL;
            case MessageType.NOZZLE_INDEX_1_INCH_TRIPLE:
                return MESSAGE_TYPE_1_INCH_TRIPLE;
            case MessageType.NOZZLE_INDEX_1_INCH_FOUR:
                return MESSAGE_TYPE_1_INCH_FOUR;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case MessageType.NOZZLE_INDEX_1_INCHx5:
                return MESSAGE_TYPE_1INCHX5;
            case MessageType.NOZZLE_INDEX_1_INCHx6:
                return MESSAGE_TYPE_1INCHX6;
            case MessageType.NOZZLE_INDEX_1_INCHx7:
                return MESSAGE_TYPE_1INCHX7;
            case MessageType.NOZZLE_INDEX_1_INCHx8:
                return MESSAGE_TYPE_1INCHX8;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头

            // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//            case MessageType.NOZZLE_INDEX_12_7_R5:
//                return MESSAGE_TYPE_12_7_R5;
            case MessageType.NOZZLE_INDEX_R6X48:
                return MESSAGE_TYPE_R6X48;
            case MessageType.NOZZLE_INDEX_R6X50:
                return MESSAGE_TYPE_R6X50;
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
// End of H.M.Wang 2020-4-17 追加12.7R5头类型
            case MessageType.NOZZLE_INDEX_9MM:
                return MESSAGE_TYPE_9MM;
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case MessageType.NOZZLE_INDEX_E6X48:
                return MESSAGE_TYPE_E6X48;
            case MessageType.NOZZLE_INDEX_E6X50:
                return MESSAGE_TYPE_E6X50;
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case MessageType.NOZZLE_INDEX_E6X1:
                return MESSAGE_TYPE_E6X1;
// H.M.Wang 2021-8-16 追加96DN头
            case MessageType.NOZZLE_INDEX_96DN:
                return MESSAGE_TYPE_96DN;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
            case MessageType.NOZZLE_INDEX_E5X48:
                return MESSAGE_TYPE_E5X48;
            case MessageType.NOZZLE_INDEX_E5X50:
                return MESSAGE_TYPE_E5X50;
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2022-4-29 追加25.4x10头类型
            case MessageType.NOZZLE_INDEX_254X10:
                return MESSAGE_TYPE_254X10;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2023-7-29 追加48点头
            case MessageType.NOZZLE_INDEX_48_DOT:
                return MESSAGE_TYPE_48_DOT;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case MessageType.NOZZLE_INDEX_22MM:
                return MESSAGE_TYPE_22MM;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
            case MessageType.NOZZLE_INDEX_22MMX2:
                return MESSAGE_TYPE_22MMX2;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case MessageType.NOZZLE_INDEX_64DOTONE:
                return MESSAGE_TYPE_64DOTONE;
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case MessageType.NOZZLE_INDEX_16DOTX4:
                return MESSAGE_TYPE_16DOTX4;
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            default:
                return MESSAGE_TYPE_12_7;

        }
    }

    public static PrinterNozzle getByType(int type) {
        switch (type) {
            case NozzleType.NOZZLE_TYPE_12_7:
                return MESSAGE_TYPE_12_7;
            case NozzleType.NOZZLE_TYPE_25_4:
                return MESSAGE_TYPE_25_4;
            case NozzleType.NOZZLE_TYPE_38_1:
                return MESSAGE_TYPE_38_1;
            case NozzleType.NOZZLE_TYPE_50_8:
                return MESSAGE_TYPE_50_8;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_127x5:
                return MESSAGE_TYPE_127X5;
            case NozzleType.NOZZLE_TYPE_127x6:
                return MESSAGE_TYPE_127X6;
            case NozzleType.NOZZLE_TYPE_127x7:
                return MESSAGE_TYPE_127X7;
            case NozzleType.NOZZLE_TYPE_127x8:
                return MESSAGE_TYPE_127X8;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_16_DOT:
                return MESSAGE_TYPE_16_DOT;
            case NozzleType.NOZZLE_TYPE_32_DOT:
                return MESSAGE_TYPE_32_DOT;
// H.M.Wang 2020-7-23 追加32DN打印头
            case NozzleType.NOZZLE_TYPE_32DN:
                return MESSAGE_TYPE_32DN;
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-17 追加32SN打印头
            case NozzleType.NOZZLE_TYPE_32SN:
                return MESSAGE_TYPE_32SN;
// End of H.M.Wang 2020-8-17 追加32SN打印头
// H.M.Wang 2022-5-27 追加32x2头类型
            case NozzleType.NOZZLE_TYPE_32X2:
                return MESSAGE_TYPE_32X2;
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            case NozzleType.NOZZLE_TYPE_32X3:
                return MESSAGE_TYPE_32X3;
            case NozzleType.NOZZLE_TYPE_32X4:
                return MESSAGE_TYPE_32X4;
            case NozzleType.NOZZLE_TYPE_32X5:
                return MESSAGE_TYPE_32X5;
            case NozzleType.NOZZLE_TYPE_32X6:
                return MESSAGE_TYPE_32X6;
            case NozzleType.NOZZLE_TYPE_32X7:
                return MESSAGE_TYPE_32X7;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
            // H.M.Wang 追加下列两行
            case NozzleType.NOZZLE_TYPE_64_DOT:
                return MESSAGE_TYPE_64_DOT;
// H.M.Wang 2020-8-26 追加64SN打印头
            case NozzleType.NOZZLE_TYPE_64SN:
                return MESSAGE_TYPE_64SN;
// End of H.M.Wang 2020-8-26 追加64SN打印头
// H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_64SLANT:
                return MESSAGE_TYPE_64SLANT;
// End of H.M.Wang 2022-10-19 追加64SLANT头。
            case NozzleType.NOZZLE_TYPE_1_INCH:
                return MESSAGE_TYPE_1_INCH;
            case NozzleType.NOZZLE_TYPE_1_INCH_DUAL:
                return MESSAGE_TYPE_1_INCH_DUAL;
            case NozzleType.NOZZLE_TYPE_1_INCH_TRIPLE:
                return MESSAGE_TYPE_1_INCH_TRIPLE;
            case NozzleType.NOZZLE_TYPE_1_INCH_FOUR:
                return MESSAGE_TYPE_1_INCH_FOUR;
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
            case NozzleType.NOZZLE_TYPE_1_INCHx5:
                return MESSAGE_TYPE_1INCHX5;
            case NozzleType.NOZZLE_TYPE_1_INCHx6:
                return MESSAGE_TYPE_1INCHX6;
            case NozzleType.NOZZLE_TYPE_1_INCHx7:
                return MESSAGE_TYPE_1INCHX7;
            case NozzleType.NOZZLE_TYPE_1_INCHx8:
                return MESSAGE_TYPE_1INCHX8;
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头

            // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//            case NozzleType.NOZZLE_TYPE_12_7_R5:
//                return MESSAGE_TYPE_12_7_R5;
            case NozzleType.NOZZLE_TYPE_R6X48:
                return MESSAGE_TYPE_R6X48;
            case NozzleType.NOZZLE_TYPE_R6X50:
                return MESSAGE_TYPE_R6X50;
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
// End of H.M.Wang 2020-4-17 追加12.7R5头类型
            case NozzleType.NOZZLE_TYPE_9MM:
                return MESSAGE_TYPE_9MM;
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X48:
                return MESSAGE_TYPE_E6X48;
            case NozzleType.NOZZLE_TYPE_E6X50:
                return MESSAGE_TYPE_E6X50;
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
            case NozzleType.NOZZLE_TYPE_E6X1:
                return MESSAGE_TYPE_E6X1;
// H.M.Wang 2021-8-16 追加96DN头
            case NozzleType.NOZZLE_TYPE_96DN:
                return MESSAGE_TYPE_96DN;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
            case NozzleType.NOZZLE_TYPE_E5X48:
                return MESSAGE_TYPE_E5X48;
            case NozzleType.NOZZLE_TYPE_E5X50:
                return MESSAGE_TYPE_E5X50;
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2022-4-29 追加25.4x10头类型
            case NozzleType.NOZZLE_TYPE_254X10:
                return MESSAGE_TYPE_254X10;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2023-7-29 追加48点头
            case NozzleType.NOZZLE_TYPE_48_DOT:
                return MESSAGE_TYPE_48_DOT;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
            case NozzleType.NOZZLE_TYPE_22MM:
                return MESSAGE_TYPE_22MM;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
            case NozzleType.NOZZLE_TYPE_22MMX2:
                return MESSAGE_TYPE_22MMX2;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
            case NozzleType.NOZZLE_TYPE_64DOTONE:
                return MESSAGE_TYPE_64DOTONE;
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            case NozzleType.NOZZLE_TYPE_16DOTX4:
                return MESSAGE_TYPE_16DOTX4;
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
            default:
                return MESSAGE_TYPE_12_7;
        }
    }

    /**
     * Nozzle index; this index must match with definition in array resource <string-array name="strPrinterArray"></string-array>
     */
    public static class MessageType {
        public static final int NOZZLE_INDEX_12_7 	= 0;    // 12.7
        public static final int NOZZLE_INDEX_25_4 	= 1;    // 12.7X2
        public static final int NOZZLE_INDEX_38_1  	= 2;    // 12.7x3
        public static final int NOZZLE_INDEX_50_8  	= 3;    // 12.7x4
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头
        public static final int NOZZLE_INDEX_127x5 	= 4;    // 12.7x5
        public static final int NOZZLE_INDEX_127x6 	= 5;    // 12.7x6
        public static final int NOZZLE_INDEX_127x7 	= 6;    // 12.7x7
        public static final int NOZZLE_INDEX_127x8 	= 7;    // 12.7x8
// End of H.M.Wang 2025-10-29 追加12.7x5，6，7，8头
        public static final int NOZZLE_INDEX_1_INCH = 8;    // 1 inch
        public static final int NOZZLE_INDEX_1_INCH_DUAL = 9; // 1inch X2
        public static final int NOZZLE_INDEX_1_INCH_TRIPLE = 10; // 1inch X2
        public static final int NOZZLE_INDEX_1_INCH_FOUR = 11; // 1inch X2
// H.M.Wang 2025-10-29 追加25.4x5，6，7，8头
        public static final int NOZZLE_INDEX_1_INCHx5 	= 12;    // 1 inch x5
        public static final int NOZZLE_INDEX_1_INCHx6 	= 13;    // 1 inch x6
        public static final int NOZZLE_INDEX_1_INCHx7 	= 14;    // 1 inch x7
        public static final int NOZZLE_INDEX_1_INCHx8 	= 15;    // 1 inch x8
// End of H.M.Wang 2025-10-29 追加25.4x5，6，7，8头
        public static final int NOZZLE_INDEX_16_DOT  = 16;   // 16dot
        public static final int NOZZLE_INDEX_32_DOT  = 17;   // 32 dot

        // H.M.Wang 追加下列一行
        public static final int NOZZLE_INDEX_64_DOT  = 18;   // 64 dot

        // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//        public static final int NOZZLE_INDEX_12_7_R5 = 11;  // 12.7R5
        public static final int NOZZLE_INDEX_R6X48 = 19;        // R6X48
        public static final int NOZZLE_INDEX_R6X50 = 20;        // R6X50
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
        // End of H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-7-23 追加32DN打印头
        public static final int NOZZLE_INDEX_32DN = 21;
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-14 追加32SN打印头
        public static final int NOZZLE_INDEX_32SN = 22;
// End of H.M.Wang 2020-8-14 追加32SN打印头

// H.M.Wang 2020-8-26 追加64SN打印头
        public static final int NOZZLE_INDEX_64SN = 23;
// End of H.M.Wang 2020-8-26 追加64SN打印头

        public static final int NOZZLE_INDEX_9MM = 24;
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
        public static final int NOZZLE_INDEX_E6X48 = 25;        // E6X48
        public static final int NOZZLE_INDEX_E6X50 = 26;        // E6X50
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
        public static final int NOZZLE_INDEX_E6X1 = 27;
// H.M.Wang 2021-8-16 追加96DN头
        public static final int NOZZLE_INDEX_96DN = 28;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
        public static final int NOZZLE_INDEX_E5X48 = 29;
        public static final int NOZZLE_INDEX_E5X50 = 30;
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2022-4-29 追加25.4x10头类型
        public static final int NOZZLE_INDEX_254X10 = 31;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2022-5-27 追加32x2头类型
        public static final int NOZZLE_INDEX_32X2 = 32;
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2022-10-19 追加64SLANT头
        public static final int NOZZLE_INDEX_64SLANT = 33;
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2023-7-29 追加48点头
        public static final int NOZZLE_INDEX_48_DOT = 34;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
        public static final int NOZZLE_INDEX_22MM = 35;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
        public static final int NOZZLE_INDEX_22MMX2 = 36;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
        public static final int NOZZLE_INDEX_64DOTONE = 37;
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
        public static final int NOZZLE_INDEX_16DOTX4 = 38;
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
        public static final int NOZZLE_INDEX_32X3 = 39;
        public static final int NOZZLE_INDEX_32X4 = 40;
        public static final int NOZZLE_INDEX_32X5 = 41;
        public static final int NOZZLE_INDEX_32X6 = 42;
        public static final int NOZZLE_INDEX_32X7 = 43;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
    }

    public static class NozzleType {
        public static final int NOZZLE_TYPE_12_7 = 0;
        public static final int NOZZLE_TYPE_25_4 = 2;
        public static final int NOZZLE_TYPE_38_1 = 5;
        public static final int NOZZLE_TYPE_50_8 = 6;
        public static final int NOZZLE_TYPE_32_DOT = 7;
        public static final int NOZZLE_TYPE_16_DOT = 8;
        public static final int NOZZLE_TYPE_1_INCH = 10;
        public static final int NOZZLE_TYPE_1_INCH_DUAL = 12;
        public static final int NOZZLE_TYPE_1_INCH_TRIPLE = 18;
        public static final int NOZZLE_TYPE_1_INCH_FOUR = 19;
        // H.M.Wang 追加下列一行
        public static final int NOZZLE_TYPE_64_DOT = 20;
        // H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
//        public static final int NOZZLE_TYPE_12_7_R5 = 30;
        public static final int NOZZLE_TYPE_R6X48 = 30;
        public static final int NOZZLE_TYPE_R6X50 = 31;
// End of H.M.Wang 2020-5-21 12.7R5头改为RX48，追加RX50头
        // End of H.M.Wang 2020-4-17 追加12.7R5头类型
// H.M.Wang 2020-7-23 追加32DN打印头
        public static final int NOZZLE_TYPE_32DN = 32;
// End of H.M.Wang 2020-7-23 追加32DN打印头
// H.M.Wang 2020-8-14 追加32SN打印头
        public static final int NOZZLE_TYPE_32SN = 33;
// End of H.M.Wang 2020-8-14 追加32SN打印头

// H.M.Wang 2020-8-26 追加64SN打印头
        public static final int NOZZLE_TYPE_64SN = 34;
// End of H.M.Wang 2020-8-26 追加64SN打印头
        public static final int NOZZLE_TYPE_9MM = 35;
// H.M.Wang 2021-3-6 追加E6X48,E6X50头
        public static final int NOZZLE_TYPE_E6X48 = 36;
        public static final int NOZZLE_TYPE_E6X50 = 37;
// End of H.M.Wang 2021-3-6 追加E6X48,E6X50头
        public static final int NOZZLE_TYPE_E6X1 = 38;
// H.M.Wang 2021-8-16 追加96DN头
        public static final int NOZZLE_TYPE_96DN = 39;
// End of H.M.Wang 2021-8-16 追加96DN头
// H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
        public static final int NOZZLE_TYPE_E5X48 = 40;
        public static final int NOZZLE_TYPE_E5X50 = 41;
// End of H.M.Wang 2021-8-25 追加E5X48和E5X50头类型
// H.M.Wang 2022-4-29 追加25.4x10头类型
        public static final int NOZZLE_TYPE_254X10 = 42;
// End of H.M.Wang 2022-4-29 追加25.4x10头类型
// H.M.Wang 2022-5-27 追加32x2头类型
        public static final int NOZZLE_TYPE_32X2 = 43;
// End of H.M.Wang 2022-5-27 追加32x2头类型
// H.M.Wang 2022-10-19 追加64SLANT头
        public static final int NOZZLE_TYPE_64SLANT = 44;
// End of H.M.Wang 2022-10-19 追加64SLANT头
// H.M.Wang 2023-7-29 追加48点头
        public static final int NOZZLE_TYPE_48_DOT = 45;
// End of H.M.Wang 2023-7-29 追加48点头
// H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
        public static final int NOZZLE_TYPE_22MM = 46;
// End of H.M.Wang 2024-3-11 追加hp22mm打印头，以生成1056点高的打印image
// H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
        public static final int NOZZLE_TYPE_64DOTONE = 47;
// End of H.M.Wang 2024-4-29 追加64_DOT_ONE喷头类型
// H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
        public static final int NOZZLE_TYPE_16DOTX4 = 48;
// End of H.M.Wang 2024-9-10 增加一个16DOTX4头类型，
// H.M.Wang 2025-1-19 增加22mmx2打印头类型
        public static final int NOZZLE_TYPE_22MMX2 = 49;
// End of H.M.Wang 2025-1-19 增加22mmx2打印头类型
// H.M.Wang 2025-10-29 追加12.7x5，6，7，8头及25.4x5，6，7，8头
        public static final int NOZZLE_TYPE_127x5 	= 50;    // 12.7x5
        public static final int NOZZLE_TYPE_127x6 	= 51;    // 12.7x6
        public static final int NOZZLE_TYPE_127x7 	= 52;    // 12.7x7
        public static final int NOZZLE_TYPE_127x8 	= 53;    // 12.7x8
        public static final int NOZZLE_TYPE_1_INCHx5 	= 54;    // 1 inch x5
        public static final int NOZZLE_TYPE_1_INCHx6 	= 55;    // 1 inch x6
        public static final int NOZZLE_TYPE_1_INCHx7 	= 56;    // 1 inch x7
        public static final int NOZZLE_TYPE_1_INCHx8 	= 57;    // 1 inch x8
// End of H.M.Wang 2025-10-29 追追加12.7x5，6，7，8头及25.4x5，6，7，8头
// H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
        public static final int NOZZLE_TYPE_32X3 = 58;
        public static final int NOZZLE_TYPE_32X4 = 59;
        public static final int NOZZLE_TYPE_32X5 = 60;
        public static final int NOZZLE_TYPE_32X6 = 61;
        public static final int NOZZLE_TYPE_32X7 = 62;
// End of H.M.Wang 2025-12-9 增加32X3 - 32X7打印头类型
    }
}