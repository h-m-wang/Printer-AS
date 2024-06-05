package com.industry.printer.ui.Test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.industry.printer.R;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.StringUtil;
import com.industry.printer.hardware.ExtGpio;
import com.industry.printer.hardware.FpgaGpioOperation;
import com.industry.printer.hardware.Hp22mm;
import com.industry.printer.hardware.IInkDevice;
import com.industry.printer.hardware.InkManagerFactory;
import com.industry.printer.hardware.SmartCardManager;

/*
  墨袋减锁实验，每点击一次DO，减锁一次
 */
public class TestHp22mm implements ITestOperation {
    public static final String TAG = TestHp22mm.class.getSimpleName();

    private Context mContext = null;
    private FrameLayout mContainer = null;
    private ListView mHp22mmTestLV = null;

    private int mSubIndex = 0;

    private int mIDSIdx = 1;
    private int mPENIdx = 0;

    private final String TITLE = "Hp22mm Test";

    // H.M.Wang 2022-10-15 增加Hp22mm库的测试
    private String[] HP22MM_TEST_ITEMS = new String[] {
            "",
// H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
            "T0 -- Bulk Writing",
// End of H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
            "T1 -- Quick Start",
            "----------------------",
            "1 -- Init IDS",
            "2 -- Init PD",
            "3 -- ids_get_supply_status",
//        "ids_get_supply_id[1]",
            "4 -- pd_get_print_head_status",
            "5 -- pd_sc_get_status",
            "6 -- pd_sc_get_info",
            "7 -- Pairing",
            "8 -- Pressurize",
            "9 -- Depressurize",
            "10 -- ids_set_platform_info",
            "11 -- pd_set_platform_info",
            "12 -- ids_set_date",
            "13 -- pd_set_date",
            "14 -- ids_set_stall_insert_count[1]",
//            "15 -- Start Print",
//            "16 -- Stop Print",
            "17 -- Dump Registers",
// H.M.Wang 2024-1-30 增加一个SPI Test项目，就是像一个寄存器写n次，读n次看完成度
//            "18 -- SPI Test",
// End of H.M.Wang 2024-1-30 增加一个SPI Test项目，就是像一个寄存器写n次，读n次看完成度
//            "19 -- Write FIFO",
//            "20 -- FIFO -> DDR",
//            "21 -- DDR -> FIFO",
//            "22 -- Read FIFO",
            "23 -- Update PD MCU\nFrom [U-Disk/PD.s19]",
            "24 -- Update FPGA FLASH\nFrom [U-Disk/FPGA.s19]",
            "25 -- Update IDS MCU\nFrom [U-Disk/IDS.s19]",
            "26 -- Toggle PI4",
            "27 -- Toggle PI5"
    };

    private String[] Registers = new String[] {
            "2-Write FIFO(RO)    ",
            "3-Read FIFO(RO)     ",
            "4-Words/Col         ",
            "5-Advance Bytes     ",
            "6-Image Columns     ",
            "7-Start P0 S0 Odd   ",
            "8-Start P0 S0 Even  ",
            "9-Start P0 S1 Odd   ",
            "10-Start P0 S1 Even ",
            "11-Start P1 S0 Odd  ",
            "12-Start P1 S0 Even ",
            "13-Start P1 S1 Odd  ",
            "14-Start P1 S1 Even ",
            "15-Encoder Freq     ",
            "16-TOF Freq         ",
            "17-External Encoder ",
            "18-Encoder Divider  ",
            "19-External TOF     ",
            "20-P0 TOF Offset    ",
            "21-P1 TOF Offset    ",
            "22-Print Direction  ",
            "23-Column Spacing   ",
            "24-Slot Spacing     ",
            "25-Enable Print     ",
            "26-Start Print Count",
            "27-End Print Count  ",
            "28-Reset(WR)        ",
            "29-Column Enable    ",
            "30-Flash Enable     ",
            "31-PDG Revision(RO) ",
            "32-Clock Freq(RO)   ",
            "33-Ready            ",
    };


    private String[] mHp22mmTestResult = new String[HP22MM_TEST_ITEMS.length];

// H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
    private final static int HP22MM_TEST_BULK_WRITE                    = 1;
// End of H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
    private final static int HP22MM_TEST_QUICK_START                    = 2;
    private final static int HP22MM_TEST_NOTHING                        = 3;
    private final static int HP22MM_TEST_INIT_IDS                       = 4;
    private final static int HP22MM_TEST_INIT_PD                        = 5;
    private final static int HP22MM_TEST_IDS_GET_SUPPLY_STATUS          = 6;
    //    private final static int HP22MM_TEST_IDS_GET_SUPPLY_INFO              = 8;
//    private final static int HP22MM_TEST_IDS_GET_SUPPLY_ID              = 8;
    private final static int HP22MM_TEST_PD_GET_PRINT_HEAD_STATUS       = 7;
    private final static int HP22MM_TEST_PD_SC_GET_STATUS               = 8;
    private final static int HP22MM_TEST_PD_SC_GET_INFO                 = 9;
    private final static int HP22MM_TEST_PAIRING                        = 10;
    private final static int HP22MM_TEST_PRESSURIZE                     = 11;
    private final static int HP22MM_TEST_DEPRESSURIZE                   = 12;
    private final static int HP22MM_TEST_IDS_SET_PF_INFO                = 13;
    private final static int HP22MM_TEST_PD_SET_PF_INFO                 = 14;
    private final static int HP22MM_TEST_IDS_SET_DATE                   = 15;
    private final static int HP22MM_TEST_PD_SET_DATE                    = 16;
    private final static int HP22MM_TEST_IDS_SET_STALL_INSERT_COUNT     = 17;
//    private final static int HP22MM_TEST_START_PRINT                    = 17;
//    private final static int HP22MM_TEST_STOP_PRINT                     = 18;
    private final static int HP22MM_TEST_DUMP_REGISTERS                 = 18;
// H.M.Wang 2024-1-30 增加一个SPI Test项目，就是像一个寄存器写n次，读n次看完成度
//    private final static int HP22MM_TEST_SPI_TEST                       = 20;
// End of H.M.Wang 2024-1-30 增加一个SPI Test项目，就是像一个寄存器写n次，读n次看完成度
//    private final static int HP22MM_TEST_MCU2FIFO                       = 21;
//    private final static int HP22MM_TEST_FIFO2DDR                       = 22;
//    private final static int HP22MM_TEST_DDR2FIFO                       = 23;
//    private final static int HP22MM_TEST_FIFO2MCU                       = 24;
    private final static int HP22MM_TEST_UPDATE_PD_MCU                  = 19;
    private final static int HP22MM_TEST_UPDATE_FPGA_FLASH              = 20;
    private final static int HP22MM_TEST_UPDATE_IDS_MCU                 = 21;
    private final static int HP22MM_TOGGLE_PI4                          = 22;
    private final static int HP22MM_TOGGLE_PI5                          = 23;

    private final int MSG_SHOW_22MM_TEST_RESULT = 109;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_22MM_TEST_RESULT:
                    View view = (View)msg.obj;
                    dispHp22mmTestItem(view);
                    view.invalidate();
                    break;
            }
        }
    };

    public TestHp22mm(Context ctx, int index) {
        mContext = ctx;
        mSubIndex = index;
    }

    @Override
    public void show(FrameLayout f) {
        mContainer = f;

        mHp22mmTestLV = (ListView)LayoutInflater.from(mContext).inflate(R.layout.test_hp22mm, null);
        mHp22mmTestLV.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return HP22MM_TEST_ITEMS.length;
            }

            @Override
            public Object getItem(int i) {
                return HP22MM_TEST_ITEMS[i];
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(null == convertView) {
                    convertView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.hp22mm_test_item, null);
                }

                convertView.setTag(position);
                dispHp22mmTestItem(convertView);

                return convertView;
            }
        });
        mHp22mmTestLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                doHp22mmTest(view, i);
            }
        });

        mContainer.addView(mHp22mmTestLV);
    }

    private void dispHp22mmTestItem(View view) {
        int position = ((Integer)view.getTag());

        LinearLayout selArea = (LinearLayout) view.findViewById(R.id.hp22mm_test_sel_btn_area);
        LinearLayout cmdBtn = (LinearLayout) view.findViewById(R.id.hp22mm_test_cmd_btn);
        if(position == 0) {
            selArea.setVisibility(View.VISIBLE);
            cmdBtn.setVisibility(View.GONE);

            final TextView ids0 = (TextView) view.findViewById(R.id.hp22mm_test_ids0_btn);
            final TextView ids1 = (TextView) view.findViewById(R.id.hp22mm_test_ids1_btn);
            final TextView pen0 = (TextView) view.findViewById(R.id.hp22mm_test_pen0_btn);
            final TextView pen1 = (TextView) view.findViewById(R.id.hp22mm_test_pen1_btn);

            if(mIDSIdx == 1) {
                ids1.setBackgroundResource(R.color.white);
                ids0.setBackgroundResource(R.color.background);
            } else {
                ids0.setBackgroundResource(R.color.white);
                ids1.setBackgroundResource(R.color.background);
            }

            if(mPENIdx == 1) {
                pen1.setBackgroundResource(R.color.white);
                pen0.setBackgroundResource(R.color.background);
            } else {
                pen0.setBackgroundResource(R.color.white);
                pen1.setBackgroundResource(R.color.background);
            }

            ids0.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mIDSIdx == 1) {
                        mIDSIdx = 0;
                        ids0.setBackgroundResource(R.color.white);
                        ids1.setBackgroundResource(R.color.background);
                    }
                }
            });
            ids1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mIDSIdx == 0) {
                        mIDSIdx = 1;
                        ids1.setBackgroundResource(R.color.white);
                        ids0.setBackgroundResource(R.color.background);
                    }
                }
            });
            pen0.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mPENIdx == 1) {
                        mPENIdx = 0;
                        pen0.setBackgroundResource(R.color.white);
                        pen1.setBackgroundResource(R.color.background);
                    }
                }
            });
            pen1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mPENIdx == 0) {
                        mPENIdx = 1;
                        pen1.setBackgroundResource(R.color.white);
                        pen0.setBackgroundResource(R.color.background);
                    }
                }
            });
        } else {
            selArea.setVisibility(View.GONE);
            cmdBtn.setVisibility(View.VISIBLE);

            TextView cmd = (TextView) view.findViewById(R.id.hp22mm_test_cmd);
            cmd.setText(HP22MM_TEST_ITEMS[position]);
            View pb = view.findViewById(R.id.hp22mm_test_progress_bar);
            pb.setVisibility(View.GONE);
            TextView result = (TextView) view.findViewById(R.id.hp22mm_test_result);
            result.setText(mHp22mmTestResult[position]);
            if(null == mHp22mmTestResult[position] || mHp22mmTestResult[position].isEmpty()) {
                result.setVisibility(View.GONE);
            } else if(mHp22mmTestResult[position].startsWith("Success")) {
                result.setTextColor(Color.BLACK);
                result.setVisibility(View.VISIBLE);
            } else {
                result.setTextColor(Color.RED);
                result.setVisibility(View.VISIBLE);
            }
        }
    }

    private void doHp22mmTest(final View view, final int index) {
        if(index == 0) return;      // skip select ids/pen line

        View pb = view.findViewById(R.id.hp22mm_test_progress_bar);
        pb.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mHp22mmTestResult[index] = "Processing ... ";
                    Message msg = mHandler.obtainMessage(MSG_SHOW_22MM_TEST_RESULT);
                    msg.obj = view;
                    mHandler.sendMessage(msg);

                    switch (index) {
// H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
                        case HP22MM_TEST_BULK_WRITE:
                            if (0 == Hp22mm.hp22mmBulkWriteTest()) {
                                mHp22mmTestResult[index] = "Success\n";
                            } else {
                                mHp22mmTestResult[index] = "Failed\n";
                            }
                            break;
// End of H.M.Wang 2024-4-19 增加一个写入大块数据的测试项目
                        case HP22MM_TEST_QUICK_START:
                            if (0 != Hp22mm.init_ids(mIDSIdx)) {
                                mHp22mmTestResult[index] = "init_ids failed\n" + Hp22mm.ids_get_sys_info();
                                break;
                            }
                            if (0 != Hp22mm.init_pd(mPENIdx)) {
                                mHp22mmTestResult[index] = "init_pd failed\n" + Hp22mm.pd_get_sys_info();
                                break;
                            }
                            if (0 != Hp22mm.ids_get_supply_status()) {
                                mHp22mmTestResult[index] = "ids_get_supply_status failed\n" + Hp22mm.ids_get_supply_status_info();
                                break;
                            }
                            if (0 != Hp22mm.pd_get_print_head_status()) {
                                mHp22mmTestResult[index] = "pd_get_print_head_status failed\n" + Hp22mm.pd_get_print_head_status_info();
                                break;
                            }
                            if (0 != Hp22mm.DeletePairing()) {
                                mHp22mmTestResult[index] = "DeletePairing failed";
                                break;
                            }
                            if (0 != Hp22mm.DoPairing()) {
                                mHp22mmTestResult[index] = "DoPairing failed";
                                break;
                            }
                            if (0 != Hp22mm.DoOverrides()) {
                                mHp22mmTestResult[index] = "DoOverrides failed";
                                break;
                            }
                            if (0 != Hp22mm.Pressurize()) {
                                mHp22mmTestResult[index] = "Pressurize failed";
                                break;
                            }
                            if (0 != Hp22mm.startPrint()) {
                                mHp22mmTestResult[index] = "PD power on failed";
                                break;
                            }

                            if (0 != Hp22mm.printTestPage()) {
                                mHp22mmTestResult[index] = "Printing test page failed";
                                break;
                            }

                            if (0 != Hp22mm.stopPrint()) {
                                mHp22mmTestResult[index] = "PD power off failed";
                                break;
                            }

                            mHp22mmTestResult[index] = "Success";
                            break;
                        case HP22MM_TEST_INIT_IDS:
                            if (0 == Hp22mm.init_ids(mIDSIdx)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.ids_get_sys_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.ids_get_sys_info();
                            }
                            break;
                        case HP22MM_TEST_INIT_PD:
                            if (0 == Hp22mm.init_pd(mPENIdx)) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_get_sys_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.pd_get_sys_info();
                            }
                            break;
                        case HP22MM_TEST_IDS_GET_SUPPLY_STATUS:
                            if (0 == Hp22mm.ids_get_supply_status()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.ids_get_supply_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.ids_get_supply_status_info();
                            }
                            break;
//                        case HP22MM_TEST_IDS_GET_SUPPLY_ID:
//                            if (0 == Hp22mm.ids_get_supply_id()) {
//                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.ids_get_supply_id_info();
//                            } else {
//                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.ids_get_supply_id_info();
//                            }
//                            break;
                        case HP22MM_TEST_PD_GET_PRINT_HEAD_STATUS:
                            if (0 == Hp22mm.pd_get_print_head_status()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_get_print_head_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed\n" + Hp22mm.pd_get_print_head_status_info();
                            }
                            break;
                        case HP22MM_TEST_PD_SC_GET_STATUS:
                            if (0 == Hp22mm.pd_sc_get_status()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_sc_get_status_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SC_GET_INFO:
                            if (0 == Hp22mm.pd_sc_get_info()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.pd_sc_get_info_info();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PAIRING:
                            if (0 != Hp22mm.DeletePairing()) {
                                mHp22mmTestResult[index] = "DeletePairing failed";
                                break;
                            }
                            if (0 != Hp22mm.DoPairing()) {
                                mHp22mmTestResult[index] = "DoPairing failed";
                                break;
                            }
                            if (0 != Hp22mm.DoOverrides()) {
                                mHp22mmTestResult[index] = "DoOverrides failed";
                                break;
                            }
                            mHp22mmTestResult[index] = "Success";
                            break;
                        case HP22MM_TEST_PRESSURIZE:
                            if (0 == Hp22mm.Pressurize()) {
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.getPressurizedValue();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DEPRESSURIZE:
                            if (0 == Hp22mm.Depressurize()) {
                                Thread.sleep(1000);
                                mHp22mmTestResult[index] = "Success\n" + Hp22mm.getPressurizedValue();
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_IDS_SET_PF_INFO:
                            if (0 == Hp22mm.ids_set_platform_info()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SET_PF_INFO:
                            if (0 == Hp22mm.pd_set_platform_info()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_IDS_SET_DATE:
                            if (0 == Hp22mm.ids_set_date()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_PD_SET_DATE:
                            if (0 == Hp22mm.pd_set_date()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_IDS_SET_STALL_INSERT_COUNT:
                            if (0 == Hp22mm.ids_set_stall_insert_count()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;/*
                        case HP22MM_TEST_START_PRINT:
// H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
                            errStr = Hp22mm.startPrint();
                            if (StringUtil.isEmpty(errStr)) {
                                mHp22mmTestResult[index] = "Printing launched";
                            } else {
                                mHp22mmTestResult[index] = errStr;
                            }
// End of H.M.Wang 2023-7-27 将startPrint函数的返回值修改为String型，返回错误的具体内容
                            break;
                        case HP22MM_TEST_STOP_PRINT:
                            if (0 == Hp22mm.stopPrint()) {
                                mHp22mmTestResult[index] = "Printing Stopped";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;*/
                        case HP22MM_TEST_DUMP_REGISTERS:
                            int[] regs = Hp22mm.dumpRegisters(Registers.length);
                            StringBuilder sb = new StringBuilder();
                            for(int i=0; i<Registers.length; i++) {
                                sb.append(Registers[i] + ": " + regs[i] + "\n");
                            }
                            mHp22mmTestResult[index] = "Success\n" + sb.toString();
                            break;
/*
// H.M.Wang 2024-1-30 增加一个SPI Test项目，就是像一个寄存器写n次，读n次看完成度
                        case HP22MM_TEST_SPI_TEST:
                            str = Hp22mm.spiTest();
                            if (null != str) {
                                mHp22mmTestResult[index] = str;
                            } else {
                                mHp22mmTestResult[index] = "Failed\nRegister write/read error";
                            }
                            break;
// End of H.M.Wang 2024-1-30 增加一个SPI Test项目，就是像一个寄存器写n次，读n次看完成度
                        case HP22MM_TEST_MCU2FIFO:
                            int ret = Hp22mm.mcu2fifo();
                            if (ret >= 0) {
                                mHp22mmTestResult[index] = "Success\n" + "R2 = " + ret;
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_FIFO2DDR:
                            ret = Hp22mm.fifo2ddr();
                            if (ret >= 0) {
                                mHp22mmTestResult[index] = "Success\n" + "R2 = " + ret;
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_DDR2FIFO:
                            ret = Hp22mm.ddr2fifo();
                            if (ret >= 0) {
                                mHp22mmTestResult[index] = "Success\n" + "R3 = " + ret;
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_FIFO2MCU:
                            ret = Hp22mm.fifo2mcu();
                            if (ret >= 0) {
                                mHp22mmTestResult[index] = "Success\n" + "R3 = " + ret;
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;*/
                        case HP22MM_TEST_UPDATE_PD_MCU:
                            if (0 == Hp22mm.UpdatePDFW()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_UPDATE_FPGA_FLASH:
                            if (0 == Hp22mm.UpdateFPGAFlash()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TEST_UPDATE_IDS_MCU:
                            if (0 == Hp22mm.UpdateIDSFW()) {
                                mHp22mmTestResult[index] = "Success";
                            } else {
                                mHp22mmTestResult[index] = "Failed";
                            }
                            break;
                        case HP22MM_TOGGLE_PI4:
                            int valPI4 = ExtGpio.readGpioTestPin('I', 4);
                            ExtGpio.writeGpioTestPin('I', 4, (valPI4 != 0 ? 0 : 1));
                            int valPI4_1 = ExtGpio.readGpioTestPin('I', 4);
                            mHp22mmTestResult[index] = "Success\n" + valPI4 + " -> " + valPI4_1;
                            break;
                        case HP22MM_TOGGLE_PI5:
                            int valPI5 = ExtGpio.readGpioTestPin('I', 5);
                            ExtGpio.writeGpioTestPin('I', 5, (valPI5 != 0 ? 0 : 1));
                            int valPI5_1 = ExtGpio.readGpioTestPin('I', 5);
                            mHp22mmTestResult[index] = "Success\n" + valPI5 + " -> " + valPI5_1;
                            break;
                    }
                    msg = mHandler.obtainMessage(MSG_SHOW_22MM_TEST_RESULT);
                    msg.obj = view;
                    mHandler.sendMessage(msg);
                } catch(UnsatisfiedLinkError e) {
                    Debug.e(TAG, e.getMessage());
                } catch(Exception e) {
                    Debug.e(TAG, e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void setTitle(TextView tv) {
        tv.setText(TITLE);
    }

    @Override
    public boolean quit() {
        mContainer.removeView(mHp22mmTestLV);
        return true;
    }
}
