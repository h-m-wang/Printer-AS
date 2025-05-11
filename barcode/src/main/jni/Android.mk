LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_CFLAGS := -std=c99 -Wall
LOCAL_CFLAGS := -D SINGLE_THREADED
LOCAL_MODULE := libBarcodeGeneratorJNI
LOCAL_MODULE_TAGS := optional
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
# LOCAL_SHARED_LIBRARIES := libutils \
        libcutils

LOCAL_SRC_FILES := 	BarcodeGeneratorJNI.c \
					liblcf/2of5.c \
					liblcf/auspost.c \
					liblcf/aztec.c \
					liblcf/bmp.c \
					liblcf/codablock.c \
					liblcf/code.c \
					liblcf/code1.c \
					liblcf/code128.c \
					liblcf/code16k.c \
					liblcf/code49.c \
					liblcf/common.c \
					liblcf/composite.c \
					liblcf/dllversion.c \
					liblcf/dmatrix.c \
					liblcf/dotcode.c \
					liblcf/eci.c \
					liblcf/emf.c \
					liblcf/gb18030.c \
					liblcf/gb2312.c \
					liblcf/general_field.c \
					liblcf/gif.c \
					liblcf/gridmtx.c \
					liblcf/gs1.c \
					liblcf/hanxin.c \
					liblcf/imail.c \
					liblcf/large.c \
					liblcf/library.c \
					liblcf/mailmark.c \
					liblcf/maxicode.c \
					liblcf/medical.c \
					liblcf/pcx.c \
					liblcf/pdf417.c \
					liblcf/plessey.c \
					liblcf/png.c \
					liblcf/postal.c \
					liblcf/ps.c \
					liblcf/qr.c \
					liblcf/raster.c \
					liblcf/reedsol.c \
					liblcf/rss.c \
					liblcf/sjis.c \
					liblcf/svg.c \
					liblcf/telepen.c \
					liblcf/tif.c \
					liblcf/ultra.c \
					liblcf/upcean.c \
					liblcf/vector.c \
					liblcf/output.c \
#					liblcf/render.c \

LOCAL_C_INCLUDES += $(LOCAL_PATH)/liblcf \

LOCAL_C_INCLUDES += system/core/include/cutils

include $(BUILD_SHARED_LIBRARY)
