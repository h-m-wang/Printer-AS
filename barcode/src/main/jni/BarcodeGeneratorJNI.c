#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include "BarcodeGeneratorJNI.h"
#include "liblcf/zint.h"

#define VERSION_CODE "1.0.2"
#define MAX_SYMBOL_POOL_SIZE 1

struct SymbolPool {
    struct zint_symbol* symbols[MAX_SYMBOL_POOL_SIZE];
    int count;
    pthread_mutex_t mutex;
};

struct SymbolPool symbolPool;

void initializeSymbolPool() {
    pthread_mutex_init(&symbolPool.mutex, NULL);
    symbolPool.count = 0;
    for (int i = 0; i < MAX_SYMBOL_POOL_SIZE; i++) {
        symbolPool.symbols[i] = ZBarcode_Create();
        if (symbolPool.symbols[i] != NULL) {
            symbolPool.count++;
            LOGI("BarcodeGeneratorJNI_ZBarcode_Create", VERSION_CODE);
        }
    }
}

void cleanupSymbolPool() {
    pthread_mutex_lock(&symbolPool.mutex);
    for (int i = 0; i < symbolPool.count; i++) {
        if (symbolPool.symbols[i] != NULL) {
            ZBarcode_Delete(symbolPool.symbols[i]);
            LOGI("BarcodeGeneratorJNI_ZBarcode_Delete", VERSION_CODE);
        }
    }
    symbolPool.count = 0;
    pthread_mutex_unlock(&symbolPool.mutex);
    pthread_mutex_destroy(&symbolPool.mutex);
}

JNIEXPORT jobject JNICALL Java_com_industry_printer_data_BarcodeGeneratorJNI_generateBarcode(
        JNIEnv *env, jobject object, jobject obj, jstring strText, jint rotateAngle) {

//    LOGI("generateBarcode entered", VERSION_CODE);

    pthread_mutex_lock(&symbolPool.mutex);

    struct zint_symbol* symbol = NULL;
    if (symbolPool.count > 0) {
        symbol = symbolPool.symbols[--symbolPool.count];
    } else {
        symbol = ZBarcode_Create();
        if (symbol == NULL) {
            pthread_mutex_unlock(&symbolPool.mutex);
            LOGE("Failed to create ZBarcode symbol", VERSION_CODE);
            return NULL;
        }
//        LOGI("Created new ZBarcode symbol", VERSION_CODE);
    }
    pthread_mutex_unlock(&symbolPool.mutex);

    ZBarcode_Clear(symbol);

    jclass barcodeObject = (*env)->GetObjectClass(env, obj);
    if (barcodeObject == NULL) {
        ZBarcode_Delete(symbol);
        LOGE("Failed to get Java object class", VERSION_CODE);
        return NULL;
    }

#define GET_INT_FIELD(fieldName, sig) \
        (*env)->GetIntField(env, obj, (*env)->GetFieldID(env, barcodeObject, fieldName, sig))

    symbol->symbology     = GET_INT_FIELD("symbology", "I");
    symbol->input_mode    = GET_INT_FIELD("input_mode", "I");
    symbol->output_options= GET_INT_FIELD("output_options", "I");
    symbol->option_1      = GET_INT_FIELD("option_1", "I");
    symbol->option_2      = GET_INT_FIELD("option_2", "I");
    symbol->option_3      = GET_INT_FIELD("option_3", "I");
    symbol->show_hrt      = GET_INT_FIELD("show_hrt", "I");
    symbol->bReduction    = GET_INT_FIELD("bReduction", "I");
    symbol->fgColor       = GET_INT_FIELD("fgColor", "I");
    symbol->bgColor       = GET_INT_FIELD("bgColor", "I");

    const char* kText = (*env)->GetStringUTFChars(env, strText, NULL);
    if (kText == NULL) {
        ZBarcode_Delete(symbol);
        LOGE("Failed to get text string", VERSION_CODE);
        return NULL;
    }

    int error = ZBarcode_Encode(symbol, (unsigned char*)kText, strlen(kText));
    if (error != 0) {
        (*env)->ReleaseStringUTFChars(env, strText, kText);
        ZBarcode_Delete(symbol);
        LOGE("Barcode encoding failed", VERSION_CODE);
        return NULL;
    }

    error = ZBarcode_Print(symbol, rotateAngle);
    if (error != 0) {
        (*env)->ReleaseStringUTFChars(env, strText, kText);
        ZBarcode_Delete(symbol);
        LOGE("Barcode printing failed", VERSION_CODE);
        return NULL;
    }

    (*env)->ReleaseStringUTFChars(env, strText, kText);

    jclass zintSymbolClass = (*env)->FindClass(env, "com/industry/printer/data/ZIntSymbol");
    if (zintSymbolClass == NULL) {
        ZBarcode_Delete(symbol);
        return NULL;
    }

    jmethodID constructor = (*env)->GetMethodID(env, zintSymbolClass, "<init>", "()V");
    if (constructor == NULL) {
        ZBarcode_Delete(symbol);
        return NULL;
    }

    jobject result = (*env)->NewObject(env, zintSymbolClass, constructor);
    if (result == NULL) {
        ZBarcode_Delete(symbol);
        return NULL;
    }

#define SET_FIELD(fieldName, sig, type, val) \
        (*env)->Set##type##Field(env, result, \
        (*env)->GetFieldID(env, zintSymbolClass, fieldName, sig), val)

    SET_FIELD("errorNumber", "I", Int, symbol->errorNumber);
    SET_FIELD("symbology", "I", Int, symbol->symbology);
    SET_FIELD("width", "I", Int, symbol->width);
    SET_FIELD("height", "I", Int, symbol->height);
    SET_FIELD("bitmap_width", "I", Int, symbol->bitmap_width);
    SET_FIELD("bitmap_height", "I", Int, symbol->bitmap_height);
    SET_FIELD("option_1", "I", Int, symbol->option_1);
    SET_FIELD("option_2", "I", Int, symbol->option_2);
    SET_FIELD("option_3", "I", Int, symbol->option_3);
    SET_FIELD("input_mode", "I", Int, symbol->input_mode);
    SET_FIELD("scale", "F", Float, symbol->scale);
    SET_FIELD("show_hrt", "I", Int, symbol->show_hrt);
    SET_FIELD("ecc_level", "C", Char, symbol->ecc_level);
    SET_FIELD("eci", "I", Int, symbol->eci);

    jintArray pixelsArray = (*env)->NewIntArray(env, symbol->bitmap_width * symbol->bitmap_height);
    if (pixelsArray != NULL) {
        jint* pixels = (*env)->GetIntArrayElements(env, pixelsArray, NULL);
        if (pixels != NULL) {
            memcpy(pixels, symbol->pixels, symbol->bitmap_width * symbol->bitmap_height * sizeof(int));
            (*env)->ReleaseIntArrayElements(env, pixelsArray, pixels, 0);
        }
        SET_FIELD("pixels", "[I", Object, pixelsArray);
        (*env)->DeleteLocalRef(env, pixelsArray);
    }

    jstring errText = (*env)->NewStringUTF(env, symbol->errtxt);
    if (errText != NULL) {
        SET_FIELD("errtxt", "Ljava/lang/String;", Object, errText);
        (*env)->DeleteLocalRef(env, errText);
    }

    pthread_mutex_lock(&symbolPool.mutex);
    if (symbolPool.count < MAX_SYMBOL_POOL_SIZE) {
        symbolPool.symbols[symbolPool.count++] = symbol;
        LOGI("Returned symbol to pool", VERSION_CODE);
    } else {
        ZBarcode_Delete(symbol);
        LOGI("Deleted symbol (pool full)", VERSION_CODE);
    }
    pthread_mutex_unlock(&symbolPool.mutex);

    return result;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    LOGI("BarcodeGeneratorJNI loading", VERSION_CODE);
    initializeSymbolPool();
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("BarcodeGeneratorJNI unloading", VERSION_CODE);
    cleanupSymbolPool();
}