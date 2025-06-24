package com.industry.printer.ExcelDataProc;

import android.util.Xml;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;
import com.industry.printer.Utils.ToastUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ImportExcelData {
    public static final String TAG = ImportExcelData.class.getSimpleName();
/*
    final ArrayList<String> usbs = ConfigPath.getMountedUsb();
				if (usbs.size() <= 0) {
        ToastUtil.show(getContext(), R.string.toast_plug_usb);
        break;
    }

    File[] files = new File(paths.get(0)).listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            if(pathname.getName().endsWith(".xls") || pathname.getName().endsWith(".xlsx")) return true;
            return false;
        }
    });
*/
    public void importExcel(String filePath) {
        try {
            List<String> ls = new ArrayList<String>();
            String str = "";
            String v = null;
            boolean flat = false;

            ZipFile xlsxFile = new ZipFile(new File(filePath));
            ZipEntry sharedStringXml = xlsxFile.getEntry("xl/sharedStrings.xml");
            Debug.d(TAG, "sharedStringXml = " + sharedStringXml);
            InputStream inputStream1 = xlsxFile.getInputStream(sharedStringXml);
            Debug.d(TAG, "inputStream1 = " + inputStream1);
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream1, "utf-8");
            int evtType = xmlParser.getEventType();
            Debug.d(TAG, "xmlParser = " + xmlParser.toString());
            while(evtType != XmlPullParser.END_DOCUMENT) {
                switch(evtType) {
                    case XmlPullParser.START_TAG:
                        String tag = xmlParser.getName();
                        if(tag.equalsIgnoreCase("t")) {
                            ls.add(xmlParser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                evtType = xmlParser.next();
            }
            Debug.d(TAG, "tag = " + ls.toString());

            ZipEntry sheetXML = xlsxFile.getEntry("xl/worksheets/sheet1.xml");
            InputStream inputStreamsheet = xlsxFile.getInputStream(sheetXML);
            XmlPullParser xmlParsersheet = Xml.newPullParser();
            xmlParsersheet.setInput(inputStreamsheet, "utf-8");
            int evtTypesheet = xmlParsersheet.getEventType();
            while(evtTypesheet != XmlPullParser.END_DOCUMENT) {
                switch(evtTypesheet) {
                    case XmlPullParser.START_TAG:
                        String tag = xmlParsersheet.getName();
                        if(tag.equalsIgnoreCase("row")) {

                        } else if(tag.equalsIgnoreCase("c")) {
                            String t = xmlParsersheet.getAttributeValue(null, "t");
                            if(t != null) {
                                flat = true;
                            } else {
                                flat = false;
                            }
                            String r = xmlParsersheet.getAttributeValue(null, "r");
                            if(r != null) {
                                str += (r + "(");
                                int row = Integer.parseInt(r.replaceAll("[^0-9]", ""));
                                String cs = r.replaceAll("\\d", "");
                                int col = (cs.length() - 1) * 26 + (cs.charAt(cs.length() - 1) - 'A') + 1;
                                str += (row + "," + col + "):");
                            }
                        } else if(tag.equalsIgnoreCase("v")) {
                            v = xmlParsersheet.nextText();
                            if(v != null) {
                                if(flat) {
                                    str += ls.get(Integer.parseInt(v)) + " ";
                                } else {
                                    str += v + " ";
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if(xmlParsersheet.getName().equalsIgnoreCase("row") && v != null) {
                            str += "\n";
                        }
                        break;
                }
                evtTypesheet = xmlParsersheet.next();
            }
            Debug.d(TAG, str);

/*
			InputStream inputStream = new FileInputStream(new File(filePath));
			Workbook book = Workbook.getWorkbook(inputStream);
			int sheets = book.getNumberOfSheets();
			Sheet sheet = book.getSheet(0);
			int rows = sheet.getRows();
			for(int i=0; i<rows; i++) {
				Debug.d(TAG, sheet.getCell(0, i).getContents());
			}*/
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
