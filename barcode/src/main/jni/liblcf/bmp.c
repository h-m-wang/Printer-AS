/* bmp.c - Handles output to Windows Bitmap file */

/*
    libzint - the open source barcode library
    Copyright (C) 2009 - 2020 Robin Stuart <rstuart114@gmail.com>

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
    3. Neither the name of the project nor the names of its contributors
       may be used to endorse or promote products derived from this software
       without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
    OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
    LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
    OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.
 */
/* vim: set ts=4 sw=4 et : */

#include <stdio.h>
#include "common.h"
#include "bmp.h"        /* Bitmap header structure */

#ifdef _MSC_VER
#include <io.h>
#include <fcntl.h>
#endif

int convertToHex(char* str) {
    int intValue = strtoul(str, NULL, 16);
    return intValue;
}

// Function to allocate memory for the pixels array
int allocateMemoryBlock(int size) {
    if (globalMemoryBlock == NULL) {
        globalMemoryBlock = (int*)malloc((size_t)size * sizeof(int));
        if (globalMemoryBlock == NULL) {
            return ZINT_ERROR_MEMORY;
        }
    }
    return 0;
}

INTERNAL int bmp_pixel_plot(struct zint_symbol *symbol, char *pixelbuf) {
    // Call allocateMemoryBlock once to allocate memory
//    allocateMemoryBlock(symbol->bitmap_height * symbol->bitmap_width);
//    // Use the globalMemoryBlock for the pixels array
//    symbol->pixels = globalMemoryBlock;

//    symbol->pixels = malloc((size_t)(symbol->bitmap_height * symbol->bitmap_width) * sizeof(int));
//    if (symbol->pixels == NULL) {
//        strcpy(symbol->errtxt, "602: Out of memory");
//        return ZINT_ERROR_MEMORY;
//    }

    // Check if we need to allocate or reallocate
    if (symbol->pixels == NULL || (size_t)symbol->allocated_bitmap_size < (symbol->bitmap_height * symbol->bitmap_width)) {
        // Free old memory if exists
        if (symbol->pixels != NULL) {
            free(symbol->pixels);
        }
        symbol->pixels = malloc((symbol->bitmap_height * symbol->bitmap_width) * sizeof(int));
        if (symbol->pixels == NULL) {
            strcpy(symbol->errtxt, "602: Out of memory");
            return ZINT_ERROR_MEMORY;
        }
        symbol->allocated_bitmap_size = (int)(symbol->bitmap_height * symbol->bitmap_width);
    }

//    const int fgColor = convertToHex(symbol->fgcolour);
//    const int bgColor = convertToHex(symbol->bgcolour);
    for (int index = 0; index < symbol->bitmap_height * symbol->bitmap_width; index++) {
        if (pixelbuf[index] == '1') {
            symbol->pixels[index] = symbol->fgColor;
        } else if (pixelbuf[index] == '0') {
            symbol->pixels[index] = symbol->bgColor;
        }
    }
    return 0;
}

