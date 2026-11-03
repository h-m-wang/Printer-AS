# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.industry.printer.hardware.RFIDDevice
-keep class com.industry.printer.hardware.RTCDevice
-keep class com.industry.printer.hardware.RTCDevice {
    *;
}
-keep class com.industry.printer.hardware.FpgaGpioOperation
-keep class com.industry.printer.hardware.HardwareJni
-keep class com.industry.printer.hardware.SmartCard
-keep class com.industry.printer.hardware.SmartCard {
    static public int writeCheckSum(int, int);
    static public int checkSum(int, int);
    static public int readManufactureID(int);
    static public int readDeviceID(int);
}

-keep class com.industry.printer.hardware.Hp22mm
-keep class com.industry.printer.hardware.Hp22mm {
    static public int getConsumedVol();
    static public int getUsableVol();
}
-keep class com.industry.printer.Utils.PlatformInfo
-keep class com.industry.printer.Utils.PlatformInfo {
    public static boolean isSmfyProduct();
}
