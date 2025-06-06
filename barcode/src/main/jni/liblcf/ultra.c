/*  ultra.c - Ultracode

    libzint - the open source barcode library
    Copyright (C) 2020 Robin Stuart <rstuart114@gmail.com>

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

 /* This version was developed using AIMD/TSC15032-43 v0.99c Edit 60, dated 4th Nov 2015 */

#ifdef _MSC_VER
#include <malloc.h>
#endif
#include <stdio.h>
#include "common.h"

#define EIGHTBIT_MODE       10
#define ASCII_MODE          20
#define C43_MODE            30

#define PREDICT_WINDOW      12

#define GFMUL(i, j) ((((i) == 0)||((j) == 0)) ? 0 : gfPwr[(gfLog[i] + gfLog[j])])

static const char fragment[27][14] = {"http://", "https://", "http://www.", "https://www.",
        "ftp://", "www.", ".com", ".edu", ".gov", ".int", ".mil", ".net", ".org",
        ".mobi", ".coop", ".biz", ".info", "mailto:", "tel:", ".cgi", ".asp",
        ".aspx", ".php", ".htm", ".html", ".shtml", "file:"};

static const char ultra_c43_set1[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,%";
static const char ultra_c43_set2[] = "abcdefghijklmnopqrstuvwxyz:/?#[]@=_~!.,-";
static const char ultra_c43_set3[] = "{}`()\"+'<>|$;&\\^*";
static const char ultra_digit[] = "0123456789,/";
static const char ultra_colour[] = "WCBMRYGK";

//static const int ultra_maxsize[] = {34, 78, 158, 282}; // According to Table 1
static const int ultra_maxsize[] = {34, 81, 158, 282}; // Adjusted to allow 79-81 codeword range in 3-row symbols (only 1 secondary vertical clock track, not 2, so 3 extra)

static const int ultra_mincols[] = {5, 13, 23, 30}; // # Total Tile Columns from Table 1

static const int kec[] = {0, 1, 2, 4, 6, 8}; // Value K(EC) from Table 12

static const int dccu[] = {
    051363, 051563, 051653, 053153, 053163, 053513, 053563, 053613, //  0-7
    053653, 056153, 056163, 056313, 056353, 056363, 056513, 056563, //  8-15
    051316, 051356, 051536, 051616, 053156, 053516, 053536, 053616, // 16-23
    053636, 053656, 056136, 056156, 056316, 056356, 056516, 056536  // 24-31
};

static const int dccl[] = {
    061351, 061361, 061531, 061561, 061631, 061651, 063131, 063151, //  0-7
    063161, 063531, 063561, 063631, 065131, 065161, 065351, 065631, //  8-15
    031351, 031361, 031531, 031561, 031631, 031651, 035131, 035151, // 16-23
    035161, 035361, 035631, 035651, 036131, 036151, 036351, 036531  // 24-31
};

static const int tiles[] = {
    013135, 013136, 013153, 013156, 013163, 013165, 013513, 013515, 013516, 013531, //   0-9
    013535, 013536, 013561, 013563, 013565, 013613, 013615, 013616, 013631, 013635, //  10-19
    013636, 013651, 013653, 013656, 015135, 015136, 015153, 015163, 015165, 015313, //  20-29
    015315, 015316, 015351, 015353, 015356, 015361, 015363, 015365, 015613, 015615, //  30-39
    015616, 015631, 015635, 015636, 015651, 015653, 015656, 016135, 016136, 016153, //  40-49
    016156, 016165, 016313, 016315, 016316, 016351, 016353, 016356, 016361, 016363, //  50-59
    016365, 016513, 016515, 016516, 016531, 016535, 016536, 016561, 016563, 016565, //  60-69
    031315, 031316, 031351, 031356, 031361, 031365, 031513, 031515, 031516, 031531, //  70-79
    031535, 031536, 031561, 031563, 031565, 031613, 031615, 031631, 031635, 031636, //  80-89
    031651, 031653, 031656, 035131, 035135, 035136, 035151, 035153, 035156, 035161, //  90-99
    035163, 035165, 035315, 035316, 035351, 035356, 035361, 035365, 035613, 035615, // 100-109
    035616, 035631, 035635, 035636, 035651, 035653, 035656, 036131, 036135, 036136, // 110-119
    036151, 036153, 036156, 036163, 036165, 036315, 036316, 036351, 036356, 036361, // 120-129
    036365, 036513, 036515, 036516, 036531, 036535, 036536, 036561, 036563, 036565, // 130-139
    051313, 051315, 051316, 051351, 051353, 051356, 051361, 051363, 051365, 051513, // 140-149
    051516, 051531, 051536, 051561, 051563, 051613, 051615, 051616, 051631, 051635, // 150-159
    051636, 051651, 051653, 051656, 053131, 053135, 053136, 053151, 053153, 053156, // 160-169
    053161, 053163, 053165, 053513, 053516, 053531, 053536, 053561, 053563, 053613, // 170-179
    053615, 053616, 053631, 053635, 053636, 053651, 053653, 053656, 056131, 056135, // 180-189
    056136, 056151, 056153, 056156, 056161, 056163, 056165, 056313, 056315, 056316, // 190-199
    056351, 056353, 056356, 056361, 056363, 056365, 056513, 056516, 056531, 056536, // 200-209
    056561, 056563, 061313, 061315, 061316, 061351, 061353, 061356, 061361, 061363, // 210-219
    061365, 061513, 061515, 061516, 061531, 061535, 061536, 061561, 061563, 061565, // 220-229
    061615, 061631, 061635, 061651, 061653, 063131, 063135, 063136, 063151, 063153, // 230-239
    063156, 063161, 063163, 063165, 063513, 063515, 063516, 063531, 063535, 063536, // 240-249
    063561, 063563, 063565, 063613, 063615, 063631, 063635, 063651, 063653, 065131, // 250-259
    065135, 065136, 065151, 065153, 065156, 065161, 065163, 065165, 065313, 065315, // 260-269
    065316, 065351, 065353, 065356, 065361, 065363, 065365, 065613, 065615, 065631, // 270-279
    065635, 065651, 065653, 056565, 051515                                     // 280-284
};

/* The following adapted from ECC283.C "RSEC codeword generator"
 * from Annex B of Ultracode draft
 * originally written by Ted Williams of Symbol Vision Corp.
 * Dated 2001-03-09
 * Corrected thanks to input from Terry Burton */

/* 
 * NOTE: Included here is an attempt to allow code compression within Ultracode. Unfortunately
 * the copy of the standard this was written from was an early draft which includes self
 * contradictions, so this is a "best guess" implementation. Because it is not guaranteed
 * to be correct this compression is not applied by default. To enable compression set
 * 
 * symbol->option_3 = ULTRA_COMPRESSION;
 * 
 * Code compression should be enabled by default when it has been implemented according to
 * a more reliable version of the specification.
 */

/* Generate divisor polynomial gQ(x) for GF283() given the required ECC size, 3 to 101 */
static void ultra_genPoly(short EccSize, unsigned short gPoly[], unsigned short gfPwr[], unsigned short gfLog[]) {
    int i, j;

    gPoly[0] = 1;
    for (i = 1; i < (EccSize + 1); i++) gPoly[i] = 0;

    for (i = 0; i < EccSize; i++) {
        for (j = i; j >= 0; j--)
            gPoly[j + 1] = (gPoly[j] + GFMUL(gPoly[j + 1], gfPwr[i + 1])) % 283;
        gPoly[0] = GFMUL(gPoly[0], gfPwr[i + 1]);
    }
    for (i = EccSize - 1; i >= 0; i -= 2) gPoly[i] = 283 - gPoly[i];

    /* gPoly[i] is > 0 so modulo operation not needed */
}

/* Generate the log and antilog tables for GF283() multiplication & division */
static void ultra_initLogTables(unsigned short gfPwr[], unsigned short gfLog[]) {
    int i, j;

    for (j = 0; j < 283; j++) gfLog[j] = 0;
    i = 1;
    for (j = 0; j < 282; j++) {
        /* j + 282 indicies save doing the modulo operation in GFMUL */
        gfPwr[j + 282] = gfPwr[j] = (short) i;
        gfLog[i] = (short) j;
        i = (i * 3) % 283;
    }
}

static void ultra_gf283(short DataSize, short EccSize, int Message[]) {
    /* Input is complete message codewords in array Message[282]
     * DataSize is number of message codewords
     * EccSize is number of Reed-Solomon GF(283) check codewords to generate
     *
     * Upon exit, Message[282] contains complete 282 codeword Symbol Message
     * including leading zeroes corresponding to each truncated codeword */

    unsigned short gPoly[283], gfPwr[(282 * 2)], gfLog[283];
    int i, j, n;
    unsigned short t;

    /* first build the log & antilog tables used in multiplication & division */
    ultra_initLogTables(gfPwr, gfLog);

    /* then generate the division polynomial of length EccSize */
    ultra_genPoly(EccSize, gPoly, gfPwr, gfLog);

    /* zero all EccSize codeword values */
    for (j = 281; (j > (281 - EccSize)); j--) Message[j] = 0;

    /* shift message codewords to the right, leave space for ECC checkwords */
    for (i = DataSize - 1; (i >= 0); j--, i--) Message[j] = Message[i];

    /* add zeroes to pad left end Message[] for truncated codewords */
    j++;
    for (i = 0; i < j; i++) Message[i] = 0;

    /* generate (EccSize) Reed-Solomon checkwords */
    for (n = j; n < (j + DataSize); n++) {
        t = (Message[j + DataSize] + Message[n]) % 283;
        for (i = 0; i < (EccSize - 1); i++) {
            Message[j + DataSize + i] = (Message[j + DataSize + i + 1] + 283
            - GFMUL(t, gPoly[EccSize - 1 - i])) % 283;
        }
        Message[j + DataSize + EccSize - 1] = (283 - GFMUL(t, gPoly[0])) % 283;
    }
    for (i = j + DataSize; i < (j + DataSize + EccSize); i++)
        Message[i] = (283 - Message[i]) % 283;
}

/* End of Ted Williams code */

static int ultra_find_fragment(const unsigned char source[], int source_length, int position) {
    int retval = -1;
    int j, k, latch, fraglen;

    for (j = 0; j < 27; j++) {
        latch = 0;
        fraglen = strlen(fragment[j]);
        if ((position + fraglen) <= source_length) {
            latch = 1;
            for (k = 0; k < fraglen; k++) {
                if (source[position + k] != fragment[j][k]) {
                    latch = 0;
                    break;
                }
            }
        }

        if (latch) {
            retval = j;
        }
    }

    return retval;
}

/* Encode characters in 8-bit mode */
static float look_ahead_eightbit(unsigned char source[], int in_length, int in_locn, char current_mode, int end_char, int cw[], int* cw_len, int gs1)
{
    int codeword_count = 0;
    int i;
    int letters_encoded = 0;

    if (current_mode != EIGHTBIT_MODE) {
        cw[codeword_count] = 282; // Unlatch
        codeword_count += 1;
    }

    i = in_locn;
    while ((i < in_length) && (i < end_char)) {
        if ((source[i] == '[') && gs1) {
            cw[codeword_count] = 268; // FNC1
        } else {
            cw[codeword_count] = source[i];
        }
        i++;
        codeword_count++;
    }

    letters_encoded = i - in_locn;

    *cw_len = codeword_count;

    if (codeword_count == 0) {
        return 0.0;
    } else {
        return (float)letters_encoded / (float)codeword_count;
    }
}

/* Encode character in the ASCII mode/submode (including numeric compression) */
static float look_ahead_ascii(unsigned char source[], int in_length, int in_locn, char current_mode, int symbol_mode, int end_char, int cw[], int* cw_len, int* encoded, int gs1) {
    int codeword_count = 0;
    int i;
    int first_digit, second_digit, done;
    int letters_encoded = 0;

    if (current_mode == EIGHTBIT_MODE) {
        cw[codeword_count] = 267; // Latch ASCII Submode
        codeword_count++;
    }

    if (current_mode == C43_MODE) {
        cw[codeword_count] = 282; // Unlatch
        codeword_count++;
        if (symbol_mode == EIGHTBIT_MODE) {
            cw[codeword_count] = 267; // Latch ASCII Submode
            codeword_count++;
        }
    }

    i = in_locn;
    do {
        /* Check for double digits */
        done = 0;
        if (i + 1 < in_length) {
            first_digit = posn(ultra_digit, source[i]);
            second_digit = posn(ultra_digit, source[i + 1]);
            if ((first_digit != -1) && (second_digit != -1)) {
                /* Double digit can be encoded */
                if ((first_digit >= 0) && (first_digit <= 9) && (second_digit >= 0) && (second_digit <= 9)) {
                    /* Double digit numerics */
                    cw[codeword_count] = (10 * first_digit) + second_digit + 128;
                    codeword_count++;
                    i += 2;
                    done = 1;
                } else if ((first_digit >= 0) && (first_digit <= 9) && (second_digit == 10)) {
                    /* Single digit followed by selected decimal point character */
                    cw[codeword_count] = first_digit + 228;
                    codeword_count++;
                    i += 2;
                    done = 1;
                } else if ((first_digit == 10) && (second_digit >= 0) && (second_digit <= 9)) {
                    /* Selected decimal point character followed by single digit */
                    cw[codeword_count] = second_digit + 238;
                    codeword_count++;
                    i += 2;
                    done = 1;
                } else if ((first_digit >= 0) && (first_digit <= 9) && (second_digit == 11)) {
                    /* Single digit or decimal point followed by field deliminator */
                    cw[codeword_count] = first_digit + 248;
                    codeword_count++;
                    i += 2;
                    done = 1;
                } else if ((first_digit == 11) && (second_digit >= 0) && (second_digit <= 9)) {
                    /* Field deliminator followed by single digit or decimal point */
                    cw[codeword_count] = second_digit + 259;
                    codeword_count++;
                    i += 2;
                    done = 1;
                }
            }
        }

        if (!done && source[i] < 0x80) {
            if ((source[i] == '[') && gs1) {
                cw[codeword_count] = 272; // FNC1
            } else {
                cw[codeword_count] = source[i];
            }
            codeword_count++;
            i++;
        }
    } while ((i < in_length) && (i < end_char) && (source[i] < 0x80));

    letters_encoded = i - in_locn;
    if (encoded != NULL) {
        *encoded = letters_encoded;
    }

    *cw_len = codeword_count;

    if (codeword_count == 0) {
        return 0.0;
    } else {
        return (float)letters_encoded / (float)codeword_count;
    }
}

/* Returns true if should latch to subset other than given `subset` */
static int c43_should_latch_other(const unsigned char data[], const size_t length, const unsigned int locn, int subset, int gs1) {
    unsigned int i, fraglen, predict_window;
    int cnt, alt_cnt, fragno;
    const char* set = subset == 1 ? ultra_c43_set1 : ultra_c43_set2;
    const char* alt_set = subset == 2 ? ultra_c43_set1 : ultra_c43_set2;

    if (locn + 3 > length) {
        return 0;
    }
    predict_window = locn + 3;

    for (i = locn, cnt = 0, alt_cnt = 0; i < predict_window; i++) {
        if (data[i] <= 0x1F || data[i] >= 0x7F || (gs1 && data[i] == '[')) {
            break;
        }

        fragno = ultra_find_fragment(data, length, i);
        if (fragno != -1 && fragno != 26) {
            fraglen = strlen(fragment[fragno]);
            predict_window += fraglen;
            if (predict_window > length) {
                predict_window = length;
            }
            i += fraglen - 1;
        } else {
            if (strchr(set, data[i]) != NULL) {
                cnt++;
            }
            if (strchr(alt_set, data[i]) != NULL) {
                alt_cnt++;
            }
        }
    }

    return alt_cnt > cnt;
}

static int get_subset(unsigned char source[], int in_length, int in_locn, int current_subset) {
    int fragno;
    int subset = 0;

    fragno = ultra_find_fragment(source, in_length, in_locn);
    if ((fragno != -1) && (fragno != 26)) {
        subset = 3;
    } else if (current_subset == 2) {
        if (posn(ultra_c43_set2, source[in_locn]) != -1) {
            subset = 2;
        } else if (posn(ultra_c43_set1, source[in_locn]) != -1) {
            subset = 1;
        }
    } else {
        if (posn(ultra_c43_set1, source[in_locn]) != -1) {
            subset = 1;
        } else if (posn(ultra_c43_set2, source[in_locn]) != -1) {
            subset = 2;
        }
    }

    if (subset == 0) {
        if (posn(ultra_c43_set3, source[in_locn]) != -1) {
            subset = 3;
        }
    }

    return subset;
}

/* Encode characters in the C43 compaction submode */
static float look_ahead_c43(unsigned char source[], int in_length, int in_locn, char current_mode, int end_char, int subset, int cw[], int* cw_len, int* encoded, int gs1, int debug) {
    int codeword_count = 0;
    int subcodeword_count = 0;
    int i;
    int fragno;
    int sublocn = in_locn;
    int new_subset;
    int unshift_set;
    int base43_value;
    int letters_encoded = 0;
    int pad;

#ifndef _MSC_VER
    int subcw[(in_length + 3) * 2];
#else
    int * subcw = (int *) alloca((in_length + 3) * 2 * sizeof (int));
#endif /* _MSC_VER */

    if (current_mode == EIGHTBIT_MODE) {
        /* Check for permissable URL C43 macro sequences, otherwise encode directly */
        fragno = ultra_find_fragment(source, in_length, sublocn);

        if ((fragno == 2) || (fragno == 3)) {
            // http://www. > http://
            // https://www. > https://
            fragno -= 2;
        }

        switch(fragno) {
            case 17: // mailto:
                cw[codeword_count] = 276;
                sublocn += strlen(fragment[fragno]);
                codeword_count++;
                break;
            case 18: // tel:
                cw[codeword_count] = 277;
                sublocn += strlen(fragment[fragno]);
                codeword_count++;
                break;
            case 26: // file:
                cw[codeword_count] = 278;
                sublocn += strlen(fragment[fragno]);
                codeword_count++;
                break;
            case 0: // http://
                cw[codeword_count] = 279;
                sublocn += strlen(fragment[fragno]);
                codeword_count++;
                break;
            case 1: // https://
                cw[codeword_count] = 280;
                sublocn += strlen(fragment[fragno]);
                codeword_count++;
                break;
            case 4: // ftp://
                cw[codeword_count] = 281;
                sublocn += strlen(fragment[fragno]);
                codeword_count++;
                break;
            default:
                if (subset == 1) {
                    cw[codeword_count] = 260; // C43 Compaction Submode C1
                    codeword_count++;
                }

                if ((subset == 2) || (subset == 3)) {
                    cw[codeword_count] = 266; // C43 Compaction Submode C2
                    codeword_count++;
                }
                break;
        }
    }

    if (current_mode == ASCII_MODE) {
        if (subset == 1) {
            cw[codeword_count] = 278; // C43 Compaction Submode C1
            codeword_count++;
        }

        if ((subset == 2) || (subset == 3)) {
            cw[codeword_count] = 280; // C43 Compaction Submode C2
            codeword_count++;
        }
    }
    unshift_set = subset;

    while ((sublocn < in_length) && (sublocn < end_char)) {
        /* Check for FNC1 */
        if (gs1 && source[sublocn] == '[') {
            break;
        }

        new_subset = get_subset(source, in_length, sublocn, subset);

        if (new_subset == 0) {
            break;
        }

        if ((new_subset != subset) && ((new_subset == 1) || (new_subset == 2))) {
            if (c43_should_latch_other(source, in_length, sublocn, subset, gs1)) {
                subcw[subcodeword_count] = 42; // Latch to other C43 set
                subcodeword_count++;
                unshift_set = new_subset;
            } else {
                subcw[subcodeword_count] = 40; // Shift to other C43 set for 1 char
                subcodeword_count++;
                subcw[subcodeword_count] = posn(new_subset == 1 ? ultra_c43_set1 : ultra_c43_set2, source[sublocn]);
                subcodeword_count++;
                sublocn++;
                continue;
            }
        }

        subset = new_subset;

        if (subset == 1) {
            subcw[subcodeword_count] = posn(ultra_c43_set1, source[sublocn]);
            subcodeword_count++;
            sublocn++;
        }

        if (subset == 2) {
            subcw[subcodeword_count] = posn(ultra_c43_set2, source[sublocn]);
            subcodeword_count++;
            sublocn++;
        }

        if (subset == 3) {
            subcw[subcodeword_count] = 41; // Shift to set 3
            subcodeword_count++;

            fragno = ultra_find_fragment(source, in_length, sublocn);
            if (fragno == 26) {
                fragno = -1;
            }
            if ((fragno >= 0) && (fragno <= 18)) {
                subcw[subcodeword_count] = fragno; // C43 Set 3 codewords 0 to 18
                subcodeword_count++;
                sublocn += strlen(fragment[fragno]);
            }
            if ((fragno >= 19) && (fragno <= 25)) {
                subcw[subcodeword_count] = fragno + 17; // C43 Set 3 codewords 36 to 42
                subcodeword_count++;
                sublocn += strlen(fragment[fragno]);
            }
            if (fragno == -1) {
                subcw[subcodeword_count] = posn(ultra_c43_set3, source[sublocn]) + 19; // C43 Set 3 codewords 19 to 35
                subcodeword_count++;
                sublocn++;
            }
            subset = unshift_set;
        }
    }

    pad = 3 - (subcodeword_count % 3);
    if (pad == 3) {
        pad = 0;
    }

    for (i = 0; i < pad; i++) {
        subcw[subcodeword_count] = 42; // Latch to other C43 set used as pad
        subcodeword_count++;
    }

    if (debug & ZINT_DEBUG_PRINT) {
        printf("C43 codewords %.*s: (%d)", in_length, source + in_locn, subcodeword_count);
        for (i = 0; i < subcodeword_count; i++) printf( " %d", subcw[i]);
        printf("\n");
    }

    letters_encoded = sublocn - in_locn;
    if (encoded != NULL) {
        *encoded = letters_encoded;
    }

    for (i = 0; i < subcodeword_count; i += 3) {
        base43_value = (43 * 43 * subcw[i]) + (43 * subcw[i + 1]) + subcw[i + 2];
        cw[codeword_count] = base43_value / 282;
        codeword_count++;
        cw[codeword_count] = base43_value % 282;
        codeword_count++;
    }

    *cw_len = codeword_count;

    if (codeword_count == 0) {
        return 0.0;
    } else {
        return (float)letters_encoded / (float)codeword_count;
    }
}

/* Produces a set of codewords which are "somewhat" optimised - this could be improved on */
static int ultra_generate_codewords(struct zint_symbol *symbol, const unsigned char source[], const size_t in_length, int codewords[]) {
    int i;
    int crop_length;
    int codeword_count = 0;
    int input_locn = 0;
    char symbol_mode;
    char current_mode;
    int subset;
    float eightbit_score;
    float ascii_score;
    float c43_score;
    int end_char;
    int block_length;
    int fragment_length;
    int fragno;
    int gs1 = 0;
    int ascii_encoded, c43_encoded;

#ifndef _MSC_VER
    unsigned char crop_source[in_length + 1];
    char mode[in_length + 1];
    int cw_fragment[in_length * 2 + 1];
#else
    unsigned char * crop_source = (unsigned char *) alloca((in_length + 1) * sizeof (unsigned char));
    char * mode = (char *) alloca((in_length + 1) * sizeof (char));
    int * cw_fragment = (int *) alloca((in_length * 2 + 1) * sizeof (int));
#endif /* _MSC_VER */

    if ((symbol->input_mode & 0x07) == GS1_MODE) {
        gs1 = 1;
    }

    // Decide start character codeword (from Table 5)
    symbol_mode = ASCII_MODE;
    for (i = 0; i < (int) in_length; i++) {
        if (source[i] >= 0x80) {
            symbol_mode = EIGHTBIT_MODE;
            break;
        }
    }
    
    if (symbol->option_3 != ULTRA_COMPRESSION && !gs1) {
        // Force eight-bit mode by default as other modes are poorly documented
        symbol_mode = EIGHTBIT_MODE;
    }

    if (symbol->output_options & READER_INIT) {
        /* Reader Initialisation mode */
        if (symbol_mode == ASCII_MODE) {
            codewords[0] = 272; // 7-bit ASCII mode
            codewords[1] = 271; // FNC3
        } else {
            codewords[0] = 257; // 8859-1
            codewords[1] = 269; // FNC3
        }
        codeword_count = 2;
    } else {
        /* Calculate start character codeword */
        if (symbol_mode == ASCII_MODE) {
            if (gs1) {
                codewords[0] = 273;
            } else {
                codewords[0] = 272;
            }
        } else {
            if ((symbol->eci >= 3) && (symbol->eci <= 18) && (symbol->eci != 14)) {
                // ECI indicates use of character set within ISO/IEC 8859
                codewords[0] = 257 + (symbol->eci - 3);
                if (codewords[0] > 267) {
                    // Avoids ECI 14 for non-existant ISO/IEC 8859-12
                    codewords[0]--;
                }
            } else if ((symbol->eci > 18) && (symbol->eci <= 898)) {
                // ECI indicates use of character set outside ISO/IEC 8859
                codewords[0] = 275 + (symbol->eci / 256);
                codewords[1] = symbol->eci % 256;
                codeword_count++;
            } else if (symbol->eci == 899) {
                // Non-language byte data
                codewords[0] = 280;
            } else if ((symbol->eci > 899) && (symbol->eci <= 9999)) {
                // ECI beyond 899 needs to use fixed length encodable ECI invocation (section 7.6.2)
                // Encode as 3 codewords
                codewords[0] = 257; // ISO/IEC 8859-1 used to enter 8-bit mode
                codewords[1] = 274; // Encode ECI as 3 codewords
                codewords[2] = (symbol->eci / 100) + 128;
                codewords[3] = (symbol->eci % 100) + 128;
                codeword_count += 3;
            } else if (symbol->eci >= 10000) {
                // Encode as 4 codewords
                codewords[0] = 257; // ISO/IEC 8859-1 used to enter 8-bit mode
                codewords[1] = 275; // Encode ECI as 4 codewords
                codewords[2] = (symbol->eci / 10000) + 128;
                codewords[3] = ((symbol->eci % 10000) / 100) + 128;
                codewords[4] = (symbol->eci % 100) + 128;
                codeword_count += 4;
            } else {
                codewords[0] = 257; // Default is assumed to be ISO/IEC 8859-1 (ECI 3)
            }
        }

        if ((codewords[0] == 257) || (codewords[0] == 272)) {
            fragno = ultra_find_fragment((unsigned char *)source, in_length, 0);

            // Check for http:// at start of input
            if ((fragno == 0) || (fragno == 2)) {
                codewords[0] = 281;
                input_locn = 7;
                symbol_mode = EIGHTBIT_MODE;
            }


            // Check for https:// at start of input
            if ((fragno == 1) || (fragno == 3)) {
                codewords[0] = 282;
                input_locn = 8;
                symbol_mode = EIGHTBIT_MODE;
            }
        }
    }

    codeword_count++;

    /* Check for 06 Macro Sequence and crop accordingly */
    if (in_length >= 9
            && source[0] == '[' && source[1] == ')' && source[2] == '>' && source[3] == '\x1e'
            && source[4] == '0' && source[5] == '6' && source[6] == '\x1d'
            && source[in_length - 2] == '\x1e' && source[in_length - 1] == '\x04') {

            if (symbol_mode == EIGHTBIT_MODE) {
                codewords[codeword_count] = 271; // 06 Macro
            } else {
                codewords[codeword_count] = 273; // 06 Macro
            }
            codeword_count++;

            for (i = 7; i < ((int) in_length - 2); i++) {
                crop_source[i - 7] = source[i];
            }
            crop_length = in_length - 9;
            crop_source[crop_length] = '\0';
   } else {
        /* Make a cropped version of input data - removes http:// and https:// if needed */
        for (i = input_locn; i < (int) in_length; i++) {
            crop_source[i - input_locn] = source[i];
        }
        crop_length = in_length - input_locn;
        crop_source[crop_length] = '\0';
    }

    /* Attempt encoding in all three modes to see which offers best compaction and store results */
    if (symbol->option_3 == ULTRA_COMPRESSION || gs1) {
        current_mode = symbol_mode;
        input_locn = 0;
        do {
            end_char = input_locn + PREDICT_WINDOW;
            eightbit_score = look_ahead_eightbit(crop_source, crop_length, input_locn, current_mode, end_char, cw_fragment, &fragment_length, gs1);
            ascii_score = look_ahead_ascii(crop_source, crop_length, input_locn, current_mode, symbol_mode, end_char, cw_fragment, &fragment_length, &ascii_encoded, gs1);
            subset = c43_should_latch_other(crop_source, crop_length, input_locn, 1 /*subset*/, gs1) ? 2 : 1;
            c43_score = look_ahead_c43(crop_source, crop_length, input_locn, current_mode, end_char, subset, cw_fragment, &fragment_length, &c43_encoded, gs1, 0 /*debug*/);

            mode[input_locn] = 'a';
            current_mode = ASCII_MODE;

            if ((c43_score > ascii_score) && (c43_score > eightbit_score)) {
                mode[input_locn] = 'c';
                current_mode = C43_MODE;
            }

            if ((eightbit_score > ascii_score) && (eightbit_score > c43_score)) {
                mode[input_locn] = '8';
                current_mode = EIGHTBIT_MODE;
            }
            if (mode[input_locn] == 'a') {
                for (i = 0; i < ascii_encoded; i++) {
                    mode[input_locn + i] = 'a';
                }
                input_locn += ascii_encoded;
            } else if (mode[input_locn] == 'c') {
                for (i = 0; i < c43_encoded; i++) {
                    mode[input_locn + i] = 'c';
                }
                input_locn += c43_encoded;
            } else {
                input_locn++;
            }
        } while (input_locn < crop_length);
    } else {
        // Force eight-bit mode
        for (input_locn = 0; input_locn < crop_length; input_locn++) {
            mode[input_locn] = '8';
        }
    }
    mode[crop_length] = '\0';

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("Mode: %s (%d)\n", mode, (int) strlen(mode));
    }

    /* Use results from test to perform actual mode switching */
    current_mode = symbol_mode;
    input_locn = 0;
    do {
        fragment_length = 0;
        block_length = 0;
        while (input_locn + block_length < crop_length && mode[input_locn + block_length] == mode[input_locn]) {
            block_length++;
        }

        switch(mode[input_locn]) {
            case 'a':
                look_ahead_ascii(crop_source, crop_length, input_locn, current_mode, symbol_mode, input_locn + block_length, cw_fragment, &fragment_length, NULL, gs1);
                current_mode = ASCII_MODE;
                break;
            case 'c':
                subset = c43_should_latch_other(crop_source, crop_length, input_locn, 1 /*subset*/, gs1) ? 2 : 1;
                look_ahead_c43(crop_source, crop_length, input_locn, current_mode, input_locn + block_length, subset, cw_fragment, &fragment_length, NULL, gs1, symbol->debug);

                /* Substitute temporary latch if possible */
                if ((current_mode == EIGHTBIT_MODE) && (cw_fragment[0] == 260) && (fragment_length >= 5) && (fragment_length <= 11)) {
                    /* Temporary latch to submode 1 from Table 11 */
                    cw_fragment[0] = 256 + ((fragment_length - 5) / 2);
                } else if ((current_mode == EIGHTBIT_MODE) && (cw_fragment[0] == 266) && (fragment_length >= 5) && (fragment_length <= 11)) {
                    /* Temporary latch to submode 2 from Table 11 */
                    cw_fragment[0] = 262 + ((fragment_length - 5) / 2);
                } else if ((current_mode == ASCII_MODE) && (cw_fragment[0] == 278) && (fragment_length >= 5) && (fragment_length <= 11)) {
                    /* Temporary latch to submode 1 from Table 9 */
                    cw_fragment[0] = 274 + ((fragment_length - 5) / 2);
                } else {
                    current_mode = C43_MODE;
                }
                break;
            case '8':
                look_ahead_eightbit(crop_source, crop_length, input_locn, current_mode, input_locn + block_length, cw_fragment, &fragment_length, gs1);
                current_mode = EIGHTBIT_MODE;
                break;
        }

        for (i = 0; i < fragment_length; i++) {
            codewords[codeword_count + i] = cw_fragment[i];
        }
        codeword_count += fragment_length;

        input_locn += block_length;
    } while (input_locn < crop_length);

    return codeword_count;
}

INTERNAL int ultracode(struct zint_symbol *symbol, const unsigned char source[], const size_t in_length) {
    int data_cw_count = 0;
    int acc, qcc;
    int ecc_level;
    int rows, columns;
    int total_cws;
    int pads;
    int cw_memalloc;
    int codeword[282 + 3]; // Allow for 3 pads in final 57th (60th incl. clock tracks) column of 5-row symbol (57 * 5 == 285)
    int i, j, locn;
    int total_height, total_width;
    char tilepat[6];
    int tilex, tiley;
    int dcc;
#ifdef _MSC_VER
    int* data_codewords;
    char* pattern;
#endif /* _MSC_VER */

    cw_memalloc = in_length * 2;
    if (cw_memalloc < 283) {
        cw_memalloc = 283;
    }

    if (symbol->eci > 811799) {
        strcpy(symbol->errtxt, "590: ECI value not supported by Ultracode");
        return ZINT_ERROR_INVALID_OPTION;
    }

#ifndef _MSC_VER
    int data_codewords[cw_memalloc];
#else
    data_codewords = (int *) alloca(cw_memalloc * sizeof (int));
#endif /* _MSC_VER */

    data_cw_count = ultra_generate_codewords(symbol, source, in_length, data_codewords);

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("Codewords returned = %d\n", data_cw_count);
    }
#ifdef ZINT_TEST
    if (symbol->debug & ZINT_DEBUG_TEST) {
        debug_test_codeword_dump_int(symbol, data_codewords, data_cw_count);
    }
#endif

    data_cw_count += 2; // 2 == MCC + ACC (data codeword count includes start char)

    /* Default ECC level is EC2 */
    if ((symbol->option_1 <= 0) || (symbol->option_1 > 6)) {
        ecc_level = 2;
    } else {
        ecc_level = symbol->option_1 - 1;
    }

    /* ECC calculation from section 7.7.2 */
    if (ecc_level == 0) {
        qcc = 3;
    } else {
        if ((data_cw_count % 25) == 0) {
            qcc = (kec[ecc_level] * (data_cw_count / 25)) + 3 + 2;
        } else {
            qcc = (kec[ecc_level] * ((data_cw_count / 25) + 1)) + 3 + 2;
        }

    }
    acc = qcc - 3;

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("ECC codewords: %d\n", qcc);
    }

    /* Maximum capacity is 282 codewords */
    total_cws = data_cw_count + qcc + 3; // 3 == TCC pattern + RSEC pattern + QCC pattern
    if (total_cws > 282) {
        strcpy(symbol->errtxt, "591: Data too long for selected error correction capacity");
        return ZINT_ERROR_TOO_LONG;
    }

    rows = 5;
    for (i = 2; i >= 0; i--) {
        if (total_cws - 6 <= ultra_maxsize[i]) { // Total codewords less 6 overhead (Start + MCC + ACC + 3 TCC/RSEC/QCC patterns)
            rows--;
        }
    }

    if ((total_cws % rows) == 0) {
        pads = 0;
        columns = total_cws / rows;
    } else {
        pads = rows - (total_cws % rows);
        columns = (total_cws / rows) + 1;
    }
    columns += columns / 15; // Secondary vertical clock tracks

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("Calculated size is %d rows by %d columns\n", rows, columns);
    }

    /* Insert MCC and ACC into data codewords */
    for (i = 282; i > 2; i--) {
        data_codewords[i] = data_codewords[i - 2];
    }
    data_codewords[1] = data_cw_count; // MCC
    data_codewords[2] = acc; // ACC

    locn = 0;
    /* Calculate error correction codewords (RSEC) */
    ultra_gf283((short) data_cw_count, (short) qcc, data_codewords);

    /* Rearrange to make final codeword sequence */
    codeword[locn++] = data_codewords[282 - (data_cw_count + qcc)]; // Start Character
    codeword[locn++] = data_cw_count; // MCC
    for (i = 0; i < qcc; i++) {
        codeword[locn++] = data_codewords[(282 - qcc) + i]; // RSEC Region
    }
    codeword[locn++] = data_cw_count + qcc; // TCC = C + Q - section 6.11.4
    codeword[locn++] = 283; // Separator
    codeword[locn++] = acc; // ACC
    for (i = 0; i < (data_cw_count - 3); i++) {
        codeword[locn++] = data_codewords[(282 - ((data_cw_count - 3) + qcc)) + i]; // Data Region
    }
    for (i = 0; i < pads; i++) {
        codeword[locn++] = 284; // Pad pattern
    }
    codeword[locn++] = qcc; // QCC

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("Rearranged codewords with ECC:\n");
        for (i = 0; i < locn; i++) {
            printf("%d ", codeword[i]);
        }
        printf("\n");
    }

    total_height = (rows * 6) + 1;
    total_width = columns + 6;

    /* Build symbol */
#ifndef _MSC_VER
    char pattern[total_height * total_width];
#else
    pattern = (char *) alloca(total_height * total_width * sizeof (char));
#endif /* _MSC_VER */

    for (i = 0; i < (total_height * total_width); i++) {
        pattern[i] = 'W';
    }

    /* Border */
    for (i = 0; i < total_width; i++) {
        pattern[i] = 'K'; // Top
        pattern[(total_height * total_width) - i - 1] = 'K'; // Bottom
    }
    for (i = 0; i < total_height; i++) {
        pattern[total_width * i] = 'K'; // Left
        pattern[(total_width * i) + 3] = 'K';
        pattern[(total_width * i) + (total_width - 1)] = 'K'; // Right
    }

    /* Clock tracks */
    for (i = 0; i < total_height; i += 2) {
        pattern[(total_width * i) + 1] = 'K'; // Primary vertical clock track
        if (total_width > 20) {
            pattern[(total_width * i) + 19] = 'K'; // Secondary vertical clock track
        }
        if (total_width > 36) {
            pattern[(total_width * i) + 35] = 'K'; // Secondary vertical clock track
        }
        if (total_width > 52) {
            pattern[(total_width * i) + 51] = 'K'; // Secondary vertical clock track
        }
    }
    for (i = 6; i < total_height; i += 6) {
        for (j = 5; j < total_width; j += 2) {
            pattern[(total_width * i) + j] = 'K'; // Horizontal clock track
        }
    }

    /* Place tiles */
    tilepat[5] = '\0';
    tilex = 0;
    tiley = 0;
    for (i = 0; i < locn; i++) {
        for (j = 0; j < 5; j++) {
            tilepat[4 - j] = ultra_colour[(tiles[codeword[i]] >> (3 * j)) & 0x07];
        }
        if ((tiley + 1) >= total_height) {
            tiley = 0;
            tilex++;

            if (tilex == 14) {
                tilex++;
            }
            if (tilex == 30) {
                tilex++;
            }
            if (tilex == 46) {
                tilex++;
            }
        }

        for (j = 0; j < 5; j++) {
            pattern[((tiley + j + 1) * total_width) + (tilex + 5)] = tilepat[j];
        }
        tiley += 6;
    }

    /* Add data column count */
    dcc = columns - ultra_mincols[rows - 2];
    tilex = 2;
    tiley = (total_height - 11) / 2;
    /* DCCU */
    for (j = 0; j < 5; j++) {
        tilepat[4 - j] = ultra_colour[(dccu[dcc] >> (3 * j)) & 0x07];
    }
    for (j = 0; j < 5; j++) {
        pattern[((tiley + j) * total_width) + tilex] = tilepat[j];
    }
    /* DCCL */
    tiley += 6;
    for (j = 0; j < 5; j++) {
        tilepat[4 - j] = ultra_colour[(dccl[dcc] >> (3 * j)) & 0x07];
    }
    for (j = 0; j < 5; j++) {
        pattern[((tiley + j) * total_width) + tilex] = tilepat[j];
    }

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("DCC: %d\n", dcc);

        for (i = 0; i < (total_height * total_width); i++) {
            printf("%c", pattern[i]);
            if ((i + 1) % total_width == 0) {
                printf("\n");
            }
        }
    }

    /* Put pattern into symbol */
    symbol->rows = total_height;
    symbol->width = total_width;

    for (i = 0; i < total_height; i++) {
        symbol->row_height[i] = 1;
        for(j = 0; j < total_width; j++) {
            set_module_colour(symbol, i, j, posn(ultra_colour, pattern[(i * total_width) + j]));
        }
    }

    return 0;
}
