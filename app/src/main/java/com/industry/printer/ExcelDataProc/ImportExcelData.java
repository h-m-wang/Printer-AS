package com.industry.printer.ExcelDataProc;

import android.util.Xml;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import jxl.Sheet;
import jxl.Workbook;

public class ImportExcelData {
    public static final String TAG = ImportExcelData.class.getSimpleName();

    String[] colValues = new String[11];

    private List<String> pickupStaticValues(ZipFile xlsxFile) {
        try {
            List<String> valueSet = new ArrayList<String>();        // 保存在xl/sharedStrings.xml的实际值
            ZipEntry sharedStringXml = xlsxFile.getEntry("xl/sharedStrings.xml");
//            Debug.d(TAG, "sharedStringXml = " + sharedStringXml);
            InputStream inputStream = xlsxFile.getInputStream(sharedStringXml);
//            Debug.d(TAG, "inputStream1 = " + inputStream1);
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream, "utf-8");
            int evtType = xmlParser.getEventType();
//            Debug.d(TAG, "xmlParser = " + xmlParser.toString());
            while(evtType != XmlPullParser.END_DOCUMENT) {
                switch(evtType) {
                    case XmlPullParser.START_TAG:
                        String tag = xmlParser.getName();
                        if(tag.equalsIgnoreCase("t")) {     // 保存值的tag
                            valueSet.add(xmlParser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                evtType = xmlParser.next();
            }
            inputStream.close();
            return valueSet;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String[]> importXlsx(String filePath) {
        try {
            boolean flat = false;       // cell的值类型是共享字符串索引(true)还是直接值(false)
            int colIndex = -1;           // 列索引值，对应关系为(0：金元素厂标；1：南钢厂标；2：船级社图标；3：产品号（设定为搜索号）；4：标识标准；5：探伤；6：钢种；7：复层厚度；8：基层厚度；9：宽度；10：长度；11：重量（不使用）
            boolean todoRow = false;    // 是否为须处理的行，本例中，从第4行开始为处理行，1-3行不处理
            boolean rowHandled = false;    // 是否对改行做了处理

            ZipFile xlsxFile = new ZipFile(new File(filePath));

            // 将所有预存的值都提取出来
            List<String> staticValues = pickupStaticValues(xlsxFile);
            if(staticValues == null) {
                xlsxFile.close();
                return null;
            }

            List<String[]> retRows = new ArrayList<String[]>();

            // 从Sheet1中读取每个cell的信息
            ZipEntry sheetXML = xlsxFile.getEntry("xl/worksheets/sheet1.xml");
            InputStream inputStream = xlsxFile.getInputStream(sheetXML);
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream, "utf-8");
            int evtType = xmlParser.getEventType();
            while(evtType != XmlPullParser.END_DOCUMENT) {
                switch(evtType) {
                    case XmlPullParser.START_TAG:
                        String tag = xmlParser.getName();
                        if(tag.equalsIgnoreCase("row")) {
                            String r = xmlParser.getAttributeValue(null, "r");      // 获取当前行的行标，在本例中，1-3行不处理，从第4行开始处理
                            int rIndex = Integer.parseInt(r);
                            if(rIndex >= 4) {
                                todoRow = true;
                                rowHandled = false;
                                for(int i=0; i<colValues.length; i++) {
                                    colValues[i] = "";
                                }
                            } else {
                                todoRow = false;
                            }
                        } else if(tag.equalsIgnoreCase("c")) {              // 每个登记的cell
                            if(!todoRow) break;         // 跳过非处理行
                            colIndex = -1;
                            String t = xmlParser.getAttributeValue(null, "t");      // 当属性t有值时，认为其包含的v字段内为静态共享字符串表中的索引值，如果t没有值，则v字段中的值为直接值
                            if(t != null) {
                                flat = true;
                            } else {
                                flat = false;
                            }
                            String r = xmlParser.getAttributeValue(null, "r");      // 标识cell的位置，如AA1，标识第1行的第AA列，需要将列的标识名换算成对应的索引值(A=0，AA=26，，，，)
                            if(r != null) {
                                String colCode = r.replaceAll("\\d", "");       // 提取如AA这样的列的标识名
                                colIndex = (colCode.length() - 1) * 26 + (colCode.charAt(colCode.length() - 1) - 'A');      // 换算成索引值
                            }
                        } else if(tag.equalsIgnoreCase("v")) {
                            if(!todoRow) break;         // 跳过非处理行
                            if(colIndex == -1) break;   // 跳过未获取到列索引值的列中的值
                            if(colIndex > 10) break;    // 跳过非处理列的值
                            rowHandled = true;
                            String v = xmlParser.nextText();
                            if(v != null) {
                                if(flat) {
                                    colValues[colIndex] = staticValues.get(Integer.parseInt(v)).trim();
                                } else {
                                    colValues[colIndex] = v.trim();
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if(xmlParser.getName().equalsIgnoreCase("row") && todoRow && rowHandled) {
                            String[] funcCols = new String[5];
                            funcCols[0] = colValues[2].isEmpty() ? (colValues[1].isEmpty() ? (colValues[0].isEmpty() ? "" : colValues[0]) : colValues[1]) : colValues[2];   // 厂家logo
                            funcCols[1] = colValues[3];   // 产品号，搜索号
                            funcCols[2] = colValues[4];   // 标识标准
                            funcCols[3] = colValues[6] + (colValues[5].isEmpty() ? "" : (" " + colValues[5]));   // 钢种 + 探伤（如果有探伤，中间加空格）
                            funcCols[4] = colValues[7] + "+" + colValues[8] + "X" + colValues[9] + "X" + colValues[10];   // 复层+基层X宽度X长度
//                            Debug.d(TAG, funcCols[0] + "; " + funcCols[1] + "; " + funcCols[2] + "; " + funcCols[3] + "; " + funcCols[4]);
                            retRows.add(funcCols);
                        }
                        break;
                }
                evtType = xmlParser.next();
            }
            inputStream.close();
            xlsxFile.close();
            return retRows;
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String[]> importXls(String filePath) {
        try {
			InputStream inputStream = new FileInputStream(new File(filePath));
			Workbook book = Workbook.getWorkbook(inputStream);
			int sheets = book.getNumberOfSheets();
			Sheet sheet = book.getSheet(0);
			int rows = sheet.getRows();

            List<String[]> retRows = new ArrayList<String[]>();

			for(int i=3; i<rows; i++) {
			    if(sheet.getCell(3, i).getContents().trim().isEmpty()) continue;
                String[] funcCols = new String[5];
                funcCols[0] = sheet.getCell(2, i).getContents().isEmpty() ?
                        (sheet.getCell(1, i).getContents().isEmpty() ?
                                (sheet.getCell(0, i).getContents().isEmpty() ? "" : sheet.getCell(0, i).getContents()) :
                                sheet.getCell(1, i).getContents()) :
                        sheet.getCell(2, i).getContents();   // 厂家logo
                funcCols[1] = sheet.getCell(3, i).getContents();   // 产品号，搜索号
                funcCols[2] = sheet.getCell(4, i).getContents();   // 标识标准
                funcCols[3] = sheet.getCell(6, i).getContents() + (sheet.getCell(5, i).getContents().isEmpty() ? "" : (" " + sheet.getCell(5, i).getContents()));   // 钢种 + 探伤（如果有探伤，中间加空格）
                funcCols[4] = sheet.getCell(7, i).getContents() + "+" + sheet.getCell(8, i).getContents() + "X" + sheet.getCell(9, i).getContents() + "X" + sheet.getCell(10, i).getContents();   // 复层+基层X宽度X长度
//Debug.d(TAG, funcCols[0] + "; " + funcCols[1] + "; " + funcCols[2] + "; " + funcCols[3] + "; " + funcCols[4]);
                retRows.add(funcCols);
			}
            inputStream.close();
			return retRows;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String[]> importExcel(String filePath) {
        Debug.d(TAG, filePath);
        if(filePath.endsWith(".xls") || filePath.endsWith(".XLS")) {
            return importXls(filePath);
        } else if(filePath.endsWith(".xlsx") || filePath.endsWith(".XLSX")) {
            return importXlsx(filePath);
        }
        return null;
    }
}
