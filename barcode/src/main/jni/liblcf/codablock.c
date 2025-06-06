/* codablock.c - Handles Codablock-F and Codablock-E */

/*
    libzint - the open source barcode library
    Copyright (C) 2016 - 2020 Harald Oehlmann

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
#include <math.h>
#include <string.h>
#ifdef _MSC_VER
#include <malloc.h>
#endif
#include <assert.h>
#include "common.h"

INTERNAL int code_128(struct zint_symbol *symbol, const unsigned char source[], const size_t length);

#define uchar unsigned char

/* FTab C128 flags - may be added */
#define CodeA 1
#define CodeB 2
#define CodeC 4
#define CEnd 8
#define CShift 16
#define CFill 32
#define CodeFNC1 64
#define CodeFNC4 128
#define ZTNum (CodeA+CodeB+CodeC)
#define ZTFNC1 (CodeA+CodeB+CodeC+CodeFNC1)

/* ASCII-Extension for Codablock-F */
#define aFNC1 (uchar)(128)
#define aFNC2 (uchar)(129)
#define aFNC3 (uchar)(130)
#define aFNC4 (uchar)(131)
#define aCodeA (uchar)(132)
#define aCodeB (uchar)(133)
#define aCodeC (uchar)(134)
#define aShift (uchar)(135)

static const char *C128Table[107] = {
    /* Code 128 character encodation - Table 1 */
    "212222", "222122", "222221", "121223", "121322", "131222", "122213",
    "122312", "132212", "221213", "221312", "231212", "112232", "122132", "122231", "113222",
    "123122", "123221", "223211", "221132", "221231", "213212", "223112", "312131", "311222",
    "321122", "321221", "312212", "322112", "322211", "212123", "212321", "232121", "111323",
    "131123", "131321", "112313", "132113", "132311", "211313", "231113", "231311", "112133",
    "112331", "132131", "113123", "113321", "133121", "313121", "211331", "231131", "213113",
    "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111", "314111",
    "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
    "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112",
    "134111", "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112",
    "421211", "212141", "214121", "412121", "111143", "111341", "131141", "114113", "114311",
    "411113", "411311", "113141", "114131", "311141", "411131", "211412", "211214", "211232",
    "2331112"
};

/* Code F Analysing-Chart */
typedef struct sCharacterSetTable
{
    int CharacterSet;       /* Still possible character sets for actual*/
    int AFollowing;     /* Still following Characters in Charset A */
    int BFollowing;     /* Still following Characters in Charset B */
    int CFollowing;     /* Still following Characters in Charset C */
} CharacterSetTable;

/* Find the possible Code-128 Character sets for a character
 * The result is an or of CodeA, CodeB, CodeC, CodeFNC1, CodeFNC4 depending on the
 * possible Code 128 character sets.
 */
static int GetPossibleCharacterSet(unsigned char C)
{
    if (C<='\x1f')      /* Control chars */
        return CodeA;
    if (C>='0' && C<='9')
        return ZTNum;   /* ZTNum=CodeA+CodeB+CodeC */
    if (C==aFNC1)
        return ZTFNC1;  /* ZTFNC1=CodeA+CodeB+CodeC+CodeFNC1 */
    if (C==aFNC4)
        return (CodeA | CodeB | CodeFNC4);
    if (C>='\x60' && C<='\x7f')      /* 60 to 127 */
        return CodeB;
    return CodeA+CodeB;
}

/* Create a Table with the following information for each Data character:
 *  int CharacterSet    is an or of CodeA, CodeB, CodeC, CodeFNC1, CodeFNC4,
 *          depending on which character set is applicable.
 *          (Result of GetPossibleCharacterSet)
 *  int AFollowing,BFollowing   The number of source characters you still may encode
 *          in this character set.
 *  int CFollowing  The number of characters encodable in CodeC if we
 *          start here.
 */
static void CreateCharacterSetTable(CharacterSetTable T[], unsigned char *data, const int dataLength)
{
    int charCur;
    int runChar;

    /* Treat the Data backwards */
    charCur=dataLength-1;
    T[charCur].CharacterSet=GetPossibleCharacterSet(data[charCur]);
    T[charCur].AFollowing=((T[charCur].CharacterSet & CodeA)==0)?0:1;
    T[charCur].BFollowing=((T[charCur].CharacterSet & CodeB)==0)?0:1;
    T[charCur].CFollowing=0;

    for (charCur--;charCur>=0;charCur--)
    {
        T[charCur].CharacterSet=GetPossibleCharacterSet(data[charCur]);
        T[charCur].AFollowing=
            ((T[charCur].CharacterSet & CodeA)==0)?0:T[charCur+1].AFollowing+1;
        T[charCur].BFollowing=
            ((T[charCur].CharacterSet & CodeB)==0)?0:T[charCur+1].BFollowing+1;
        T[charCur].CFollowing=0;

    }
    /* Find the CodeC-chains */
    for (charCur=0;charCur<dataLength;charCur++)
    {
        T[charCur].CFollowing=0;
        if ((T[charCur].CharacterSet & CodeC)!=0)
        {
            /* CodeC possible */
            runChar=charCur;
            do{
                /* Whether this is FNC1, whether next is */
                /* numeric */
                if (T[runChar].CharacterSet==ZTFNC1)
                    /* FNC1 */
                    ++(T[charCur].CFollowing);
                else
                {
                    ++runChar;
                    if (runChar>=dataLength)
                        break;
                    /* Only a Number may follow */
                    if (T[runChar].CharacterSet==ZTNum)
                        T[charCur].CFollowing+=2;
                    else
                        break;
                }
                ++runChar;
            } while (runChar<dataLength);
        }
    }
}

/* Find the amount of numerical characters in pairs which will fit in
 * one bundle into the line (up to here). This is calculated online because
 * it depends on the space in the line.
 */
static int RemainingDigits(CharacterSetTable *T, int charCur,int emptyColumns)
{
    int digitCount;     /* Numerical digits fitting in the line */
    int runChar;
    runChar=charCur;
    digitCount=0;
    while(emptyColumns>0 && runChar<charCur+T[charCur].CFollowing)
    {
        if (T[runChar].CharacterSet!=ZTFNC1)
        {
            /* NOT FNC1 */
            digitCount+=2;
            runChar++;
        }
        runChar++;
        emptyColumns--;
    }
    return digitCount;
}

/* Find the Character distribution at a given column count.
 * If too many rows (>44) are requested the columns are extended.
 * Parameters :
 *  T       Pointer on the Characters which fit in the row
 *          If a different count is calculated it is corrected
 *          in the callers workspace.
 *  pFillings   Output of filling characters
 *  pSet        Output of the character sets used, allocated by me.
 *  Return value    Resulting row count
 */

static int Columns2Rows(struct zint_symbol *symbol, CharacterSetTable *T, const int dataLength,
        int * pRows, int * pUseColumns, int * pSet, int * pFillings)
{
    int useColumns;     /* Usable Characters per line */
    int fillings;       /* Number of filling characters */
    int rowsCur;
    int runChar;
    int emptyColumns;   /* Number of codes still empty in line. */
    int emptyColumns2;  /* Alternative emptyColumns to compare */
    int CPaires;        /* Number of digit pairs which may fit in the line */
    int characterSetCur;        /* Current Character Set */
    int isFNC4; /* Set if current character FNC4 */

    useColumns=*pUseColumns;

    /* >>> Loop until rowsCur <= 44 */
    do {
        int charCur=0;
        memset(pSet,0,dataLength*sizeof(int));
        rowsCur=0;

        /* >>> Line Loop */
        do{
            /* >> Start Character */
            emptyColumns=useColumns;    /* Remained place in Line */

            /* >>Choose in Set A or B */
            /* (C is changed as an option later on) */

            pSet[charCur]=characterSetCur=
                (T[charCur].AFollowing > T[charCur].BFollowing)
                ? CodeA : CodeB;

            /* >> Test on Numeric Mode C */
            CPaires=RemainingDigits(T,charCur, emptyColumns);
            if (CPaires>=4)
            {
                /* 4 Digits in Numeric compression ->OK */
                /* > May an odd start find more ? */
                /* Skip leading <FNC1>'s */
                /* Typical structure : <FNC1><FNC1>12... */
                /* Test if numeric after one isn't better.*/
                runChar=charCur;
                emptyColumns2=emptyColumns;
                while (T[runChar].CharacterSet==ZTFNC1)
                {
                    ++runChar;
                    --emptyColumns2;
                }
                if (CPaires>=RemainingDigits(T,runChar+1,emptyColumns2-1))
                {
                    /* Start odd is not better */
                    /* We start in C */
                    pSet[charCur]=characterSetCur=CodeC;
                    /* Increment charCur */
                    if (T[charCur].CharacterSet!=ZTFNC1)
                        ++charCur;      /* 2 Num.Digits */
                }
            }
            ++charCur;
            --emptyColumns;

            /* >> Following characters */
            while(emptyColumns>0 && charCur<dataLength)
            {
                isFNC4 = (T[charCur].CharacterSet & CodeFNC4);
                switch(characterSetCur){
                case CodeA:
                case CodeB:
                    /* >> Check switching to CodeC */
                    /* Switch if :
                     *  - Character not FNC1
                     *  - 4 real Digits will fit in line
                     *  - an odd Start will not be better
                     */
                    if (T[charCur].CharacterSet==ZTNum
                        && (CPaires=RemainingDigits(T,charCur, emptyColumns-1))>=4
                        && CPaires > RemainingDigits(T,charCur+1,emptyColumns-2))
                    {
                        /* > Change to C */
                        pSet[charCur]=characterSetCur=CodeC;
                        charCur+=2; /* 2 Digit */
                        emptyColumns-=2; /* <SwitchC>12 */
                    } else if (characterSetCur==CodeA)
                    {
                        if (T[charCur].AFollowing == 0 || (isFNC4 && T[charCur].AFollowing == 1))
                        {
                            /* Must change to B */
                            if (emptyColumns == 1 || (isFNC4 && emptyColumns == 2))
                            {
                                /* Can't switch: */
                                pSet[charCur-1]|=CEnd+CFill;
                                emptyColumns=0;
                            }else{
                                /* <Shift> or <switchB>? */
                                if (T[charCur].BFollowing == 1 || (isFNC4 && T[charCur].BFollowing == 2))
                                {
                                    /* Note using order "FNC4 shift char" (same as CODE128) not "shift FNC4 char" as given in Table B.1 and Table B.2 */
                                    if (isFNC4) { /* So skip FNC4 and shift value instead */
                                        --emptyColumns;
                                        ++charCur;
                                    }
                                    pSet[charCur]|=CShift;
                                } else {
                                    pSet[charCur]|=CodeB;
                                    characterSetCur = CodeB;
                                }
                                emptyColumns-=2;
                                ++charCur;
                            }
                        } else if (isFNC4 && emptyColumns == 1) {
                            /* Can't fit extended ASCII on same line */
                            pSet[charCur-1]|=CEnd+CFill;
                            emptyColumns=0;
                        }else{
                            --emptyColumns;
                            ++charCur;
                        }
                    } else { /* Last possibility : CodeB */
                        if (T[charCur].BFollowing == 0 || (isFNC4 && T[charCur].BFollowing == 1))
                        {
                            /* Must change to A */
                            if (emptyColumns == 1 || (isFNC4 && emptyColumns == 2))
                            {
                                /* Can't switch: */
                                pSet[charCur-1]|=CEnd+CFill;
                                emptyColumns=0;
                            } else {
                                /* <Shift> or <switchA>? */
                                if (T[charCur].AFollowing == 1 || (isFNC4 && T[charCur].AFollowing == 2))
                                {
                                    /* Note using order "FNC4 shift char" (same as CODE128) not "shift FNC4 char" as given in Table B.1 and Table B.2 */
                                    if (isFNC4) { /* So skip FNC4 and shift value instead */
                                        --emptyColumns;
                                        ++charCur;
                                    }
                                    pSet[charCur]|=CShift;
                                } else {
                                    pSet[charCur]|=CodeA;
                                    characterSetCur = CodeA;
                                }
                                emptyColumns-=2;
                                ++charCur;
                            }
                        } else if (isFNC4 && emptyColumns == 1) {
                            /* Can't fit extended ASCII on same line */
                            pSet[charCur-1]|=CEnd+CFill;
                            emptyColumns=0;
                        }else{
                            --emptyColumns;
                            ++charCur;
                        }
                    }
                    break;
                case CodeC:
                    if(T[charCur].CFollowing>0)
                    {
                        charCur+=(T[charCur].CharacterSet==ZTFNC1)?1:2;
                        emptyColumns--;
                    }else{
                        /* Must change to A or B */
                        if (emptyColumns==1)
                        {
                            /* Can't switch: */
                            pSet[charCur-1]|=CEnd+CFill;
                            emptyColumns=0;
                        }else{
                            /*<SwitchA> or <switchA>?*/
                            characterSetCur=pSet[charCur]=
                                (T[charCur].AFollowing > T[charCur].BFollowing)
                                ?CodeA:CodeB;
                            emptyColumns-=2;
                            ++charCur;
                        }
                    }
                    break;
                } /* switch */
            } /* while */

            /* > End of Codeline */
            pSet[charCur-1]|=CEnd;
            ++rowsCur;
        } while (charCur<dataLength); /* <= Data.Len-1 */

        /* Allow for check characters K1, K2 */
        switch (emptyColumns) {
        case 1:
            pSet[charCur-1]|=CFill;
            /* fall through */
        case 0:
            ++rowsCur;
            fillings=useColumns-2+emptyColumns;
            break;
        case 2:
            fillings=0;
            break;
        default:
            pSet[charCur-1]|=CFill;
            fillings=emptyColumns-2;
        }

        if (rowsCur>44) {
            ++useColumns;
            if (useColumns > 62) {
                return ZINT_ERROR_TOO_LONG;
            }
        } else if (rowsCur == 1) {
            rowsCur = 2;
            fillings += useColumns;
        }
    } while(rowsCur>44);
    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("  -> out: rowsCur <%i>, useColumns <%i>, fillings <%i>\n",rowsCur,useColumns,fillings);
    }
    *pUseColumns=useColumns;
    *pRows=rowsCur;
    *pFillings=fillings;
    return 0;
}

/* Find columns if row count is given.
 */
static int Rows2Columns(struct zint_symbol *symbol, CharacterSetTable *T, const int dataLength,
        int * pRows, int * pUseColumns, int * pSet, int * pFillings)
{
    int rowsCur;
    int rowsRequested;  /* Number of requested rows */
    int columnsRequested; /* Number of requested columns (if any) */
    int backupRows = 0;
    int fillings;
    int backupFillings = 0;
    int useColumns;
    int testColumns;    /* To enter into Width2Rows */
    int backupColumns = 0;
    int fBackupOk = 0;      /* The memorised set is o.k. */
    int testListSize = 0;
    int pTestList[62];
#ifndef _MSC_VER
    int *pBackupSet[dataLength];
#else
    int *pBackupSet = (int *)alloca(dataLength*sizeof(int));
#endif

    rowsRequested=*pRows;
    columnsRequested = *pUseColumns >= 4 ? *pUseColumns : 0;

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        printf("Optimizer : Searching <%i> rows\n", rowsRequested);
    }

    if (columnsRequested) {
        testColumns = columnsRequested;
    } else {
        /* First guess */
        testColumns=dataLength/rowsRequested;
        if (testColumns > 62)
            testColumns = 62;
        else if (testColumns < 4)
            testColumns = 4;
    }

    for (;;) {
        int errorCur;
        pTestList[testListSize] = testColumns;
        testListSize++;
        useColumns=testColumns; /* Make a copy because it may be modified */
        errorCur = Columns2Rows(symbol, T, dataLength, &rowsCur, &useColumns, pSet, &fillings);
        if (errorCur != 0)
            return errorCur;
        if (rowsCur<=rowsRequested) {
            /* Less or exactly line number found */
            /* check if column count below already tested or at smallest/requested */
            int fInTestList = (rowsCur == 2 || testColumns == 4 || testColumns == columnsRequested);
            int posCur;
            for (posCur = 0; posCur < testListSize && ! fInTestList; posCur++) {
                if ( pTestList[posCur] == testColumns-1 )
                    fInTestList = 1;
            }
            if (fInTestList) {
                /* >> Smaller Width already tested
                 */
                if (rowsCur < rowsRequested) {
                    fillings += useColumns * (rowsRequested - rowsCur);
                    rowsCur = rowsRequested;
                }
                /* Exit with actual */
                *pFillings=fillings;
                *pRows=rowsCur;
                *pUseColumns = useColumns;
                return 0;
            }
            /* > Test more rows (shorter CDB) */
            fBackupOk=(rowsCur==rowsRequested);
            memcpy(pBackupSet,pSet,dataLength*sizeof(int));
            backupFillings=fillings;
            backupColumns=useColumns;
            backupRows=rowsCur;
            --testColumns;
        } else {
            /* > Too many rows */
            int fInTestList = fBackupOk;
            int posCur;
            for (posCur = 0; posCur < testListSize && ! fInTestList; posCur++) {
                if ( pTestList[posCur] == testColumns+1 )
                    fInTestList = 1;
            }
            if (fInTestList) {
                /* The next less-rows (larger) code was
                 * already tested. So give the larger
                 * back.
                 */
                memcpy(pSet,pBackupSet,dataLength*sizeof(int));
                *pFillings=backupFillings;
                *pRows=backupRows;
                *pUseColumns=backupColumns;
                return 0;
            }
            /* > Test less rows (longer code) */
            backupRows=rowsCur;
            memcpy(pBackupSet,pSet,dataLength*sizeof(int));
            backupFillings=fillings;
            backupColumns=useColumns;
            fBackupOk=0;
            ++testColumns;
        }
    }
}

/* Print a character in character set A
 */
static void A2C128_A(uchar **ppOutPos,uchar c)
{
    uchar * pOutPos = *ppOutPos;
    switch(c){
    case aCodeB: *pOutPos=100; break;
    case aFNC4: *pOutPos=101; break;
    case aFNC1: *pOutPos=102; break;
    case aFNC2: *pOutPos=97; break;
    case aFNC3: *pOutPos=96; break;
    case aCodeC: *pOutPos=99; break;
    case aShift: *pOutPos=98; break;
    default:
        /* +++ HaO 13.11.98 c>' ' && c < '\x1F' corrected */
        if(c>=' ' && c<='_')
            *pOutPos=(uchar)(c-' ');
        else
            *pOutPos=(uchar)(c+64);
        break;
    }
    (*ppOutPos)++;
}

/* Output c in Set B
 */
static void A2C128_B(uchar **ppOutPos,uchar c)
{
    uchar * pOutPos = *ppOutPos;
    switch(c){
    case aFNC1: *pOutPos=102; break;
    case aFNC2: *pOutPos=97; break;
    case aFNC3: *pOutPos=96; break;
    case aFNC4: *pOutPos=100; break;
    case aCodeA: *pOutPos=101; break;
    case aCodeC: *pOutPos=99; break;
    case aShift: *pOutPos=98; break;
    default: *pOutPos=(uchar)(c-' '); break;
    }
    ++(*ppOutPos);
}

/* Output c1, c2 in Set C
 */
static void A2C128_C(uchar **ppOutPos,uchar c1,uchar c2)
{
    uchar * pOutPos = *ppOutPos;
    switch(c1){
    case aFNC1: *pOutPos=102; break;
    case aCodeB: *pOutPos=100; break;
    case aCodeA: *pOutPos=101; break;
    default: *pOutPos=(char)(10 * (c1- '0') + (c2 - '0'));break;
    }
    (*ppOutPos)++;
}

/* Output a character in Characterset
 */
static void ASCIIZ128(uchar **ppOutPos, int CharacterSet,uchar c1, uchar c2)
{
    if (CharacterSet==CodeA)
        A2C128_A(ppOutPos,c1);
    else if(CharacterSet==CodeB)
        A2C128_B(ppOutPos,c1);
    else
        A2C128_C(ppOutPos,c1,c2);
}

/* XLate Tables D.2, D.3 and F.1 of Codablock-F Specification and call output
 */
static void SumASCII(uchar **ppOutPos, int Sum, int CharacterSet)
{
    switch (CharacterSet){
    case CodeA: /* Row # Indicators and Data Check Characters K1/K2 for CodeA and CodeB are the same */
    case CodeB:
        if (Sum<=31)
            A2C128_B(ppOutPos, (uchar)(Sum+96));
        else if(Sum<=47)
            A2C128_B(ppOutPos, (uchar)Sum);
        else
            A2C128_B(ppOutPos, (uchar)(Sum+10));
        break;
    case CodeC:
        A2C128_C(ppOutPos
            ,(char)(Sum/10+'0') ,(uchar)(Sum%10+'0'));
        break;
    }
}

/* Main function called by zint framework
 */
INTERNAL int codablock(struct zint_symbol *symbol,const unsigned char source[], const size_t length) {
    int charCur, dataLength;
    int Error;
    int rows, columns, useColumns;
    int fillings;
    int Sum1,Sum2;
    uchar * pOutPos;
    int rowCur;
    int characterSetCur;
    int emptyColumns;
    char dest[1000];
    int r, c;
#ifdef _MSC_VER
    CharacterSetTable *T;
    unsigned char *data;
    int *pSet;
    uchar * pOutput;
#endif
    /* Suppresses clang-analyzer-core.VLASize warning */
    assert(length > 0);

    /* Parameter check */
    /* option1: rows <= 0: automatic, 1..44 */
    rows = symbol->option_1;
    if (rows == 1) {
        Error = code_128(symbol, source, length);
        if (Error < 5) {
            symbol->output_options |= BARCODE_BIND;
            if (symbol->border_width == 0) { /* Allow override if non-zero */
                symbol->border_width = 1; /* AIM ISS-X-24 Section 4.6.1 b) (note change from previous default 2) */
            }
        }
        return Error;
    }
    if (rows > 44) {
        strcpy(symbol->errtxt, "410: Rows parameter not in 0..44");
        return ZINT_ERROR_INVALID_OPTION;
    }
    /* option_2: (usable data) columns: <= 0: automatic, 9..67 (min 9 == 4 data, max 67 == 62 data) */
    columns = symbol->option_2;
    if ( ! (columns <= 0 || (columns >= 9 && columns <= 67)) ) {
        strcpy(symbol->errtxt, "411: Columns parameter not in 0, 9..67");
        return ZINT_ERROR_INVALID_OPTION;
    }

#ifndef _MSC_VER
    unsigned char data[length*2+1];
#else
    data = (unsigned char *) alloca(length * 2+1);
#endif

    dataLength = 0;
    if (symbol->output_options & READER_INIT) {
        data[dataLength] = aFNC3;
        dataLength++;
    }
    /* Replace all Codes>127 with <fnc4>Code-128 */
    for (charCur = 0; charCur < (int) length; charCur++) {
        if (source[charCur]>127)
        {
            data[dataLength] = aFNC4;
            dataLength++;
            data[dataLength] = (unsigned char)(source[charCur]&127);
        } else
            data[dataLength] = source[charCur];
        dataLength++;
    }

    /* Build character set table */
#ifndef _MSC_VER
    CharacterSetTable T[dataLength];
    int pSet[dataLength];
#else
    T=(CharacterSetTable *)alloca(dataLength*sizeof(CharacterSetTable));
    pSet = (int *)alloca(dataLength*sizeof(int));
#endif
    CreateCharacterSetTable(T,data,dataLength);

    /* Find final row and column count */
    /* nor row nor column count given */
    if (rows <= 0 && columns <= 0) {
        /* use 1/1 aspect/ratio Codablock */
        columns = floor(sqrt(dataLength)) + 5;
        if (columns > 67) {
            columns = 67;
        } else if (columns < 9) {
            columns = 9;
        }
        if (symbol->debug & ZINT_DEBUG_PRINT) {
            printf("Auto column count for %d characters:%d\n", dataLength, columns);
        }
    }
    /* There are 5 Codewords for Organisation Start(2),row(1),CheckSum,Stop */
    useColumns = columns - 5;
    if ( rows > 0 ) {
        /* row count given */
        Error = Rows2Columns(symbol, T, dataLength, &rows, &useColumns, pSet, &fillings);
    } else {
        /* column count given */
        Error = Columns2Rows(symbol, T, dataLength, &rows, &useColumns, pSet, &fillings);
    }
    if (Error != 0) {
        strcpy(symbol->errtxt, "413: Data string too long");
        return Error;
    }
    /* Suppresses clang-analyzer-core.VLASize warning */
    assert(rows >= 2 && useColumns >= 4);

    /* Data Check Characters K1 and K2, Annex F */
    Sum1 = Sum2 = 0;
    for (charCur = 0; charCur < (int) length; charCur++) {
        Sum1 = (Sum1 + (charCur + 1) * source[charCur]) % 86; /* Mod as we go along to avoid overflow */
        Sum2 = (Sum2 + charCur * source[charCur]) % 86;
    }

    if (symbol->debug & ZINT_DEBUG_PRINT) { /* start a new level of local variables */
        int DPos;
        printf("\nData:");
        for (DPos=0 ; DPos< dataLength ; DPos++)
            fputc(data[DPos],stdout);
        printf("\n Set:");
        for (DPos=0 ; DPos< dataLength ; DPos++) {
            switch (pSet[DPos]&(CodeA+CodeB+CodeC)) {
            case CodeA: fputc('A',stdout); break;
            case CodeB: fputc('B',stdout); break;
            case CodeC: fputc('C',stdout); break;
            default: fputc('.',stdout); break;
            }
        }
        printf("\nFNC1:");
        for (DPos=0 ; DPos< dataLength ; DPos++)
            fputc((pSet[DPos]&CodeFNC1)==0?'.':'X',stdout);
        printf("\n END:");
        for (DPos=0 ; DPos< dataLength ; DPos++)
            fputc((pSet[DPos]&CEnd)==0?'.':'X',stdout);
        printf("\nShif:");
        for (DPos=0 ; DPos< dataLength ; DPos++)
            fputc((pSet[DPos]&CShift)==0?'.':'X',stdout);
        printf("\nFILL:");
        for (DPos=0 ; DPos< dataLength ; DPos++)
            fputc((pSet[DPos]&CFill)==0?'.':'X',stdout);
        fputc('\n',stdout);
        printf("K1 %d, K2 %d\n", Sum1, Sum2);
    }

    columns = useColumns + 5;

    /* >>> Build C128 code numbers */
    /* The C128 column count contains Start (2CW), Row ID, Checksum, Stop */
#ifndef _MSC_VER
    uchar pOutput[columns * rows];
#else
    pOutput = (unsigned char *)alloca(columns * rows * sizeof(char));
#endif
    pOutPos = pOutput;
    charCur=0;
    /* >> Loop over rows */
    for (rowCur=0 ; rowCur<rows ; rowCur++) {
        if (charCur>=dataLength)
        {
            /* >> Empty line with StartA, aCodeB, row #, and then filler aCodeC aCodeB etc */
            *pOutPos='\x67';
            pOutPos++;
            *pOutPos = 100; /* aCodeB */
            pOutPos++;
            characterSetCur = CodeB;
            SumASCII(&pOutPos, rowCur + 42, characterSetCur); /* Row # */
            emptyColumns = useColumns;
            if (rowCur == rows - 1) {
                emptyColumns -= 2;
            }
            while (emptyColumns>0)
            {
                if(characterSetCur==CodeC)
                {
                    A2C128_C(&pOutPos,aCodeB,'\0');
                    characterSetCur=CodeB;
                }else{
                    A2C128_B(&pOutPos,aCodeC);
                    characterSetCur=CodeC;
                }
                --emptyColumns;
            }
        }else{
            /* >> Normal Line */
            /* > Startcode */
            switch (pSet[charCur] & (CodeA+CodeB+CodeC)){
            case CodeA:
                *pOutPos = '\x67';
                pOutPos++;
                *pOutPos = '\x62';
                pOutPos++;
                characterSetCur=CodeA;
                break;
            case CodeB:
                *pOutPos = '\x67';
                pOutPos++;
                *pOutPos = '\x64';
                pOutPos++;
                characterSetCur=CodeB;
                break;
            case CodeC:
            default:
                *pOutPos = '\x67';
                pOutPos++;
                *pOutPos = '\x63';
                pOutPos++;
                characterSetCur=CodeC;
                break;
            }
            /* > Set F1 */
            /* In first line : # of rows */
            SumASCII(&pOutPos, rowCur == 0 ? rows - 2 : rowCur + 42, characterSetCur);
            /* >>> Data */
            emptyColumns=useColumns;
            /* >> Character loop */
            while (emptyColumns > 0 && charCur < dataLength)
            {
                /* ? Change character set */
                if (emptyColumns < useColumns)
                {
                    if ((pSet[charCur]&CodeA)!=0)
                    {
                        /* Change to A */
                        ASCIIZ128(&pOutPos,characterSetCur,aCodeA,'\0');
                        --emptyColumns;
                        characterSetCur=CodeA;
                    } else if ((pSet[charCur]&CodeB)!=0)
                    {
                        /* Change to B */
                        ASCIIZ128(&pOutPos,characterSetCur,aCodeB,'\0');
                        --emptyColumns;
                        characterSetCur=CodeB;
                    } else if ((pSet[charCur]&CodeC)!=0)
                    {
                        /* Change to C */
                        ASCIIZ128(&pOutPos,characterSetCur,aCodeC,'\0');
                        --emptyColumns;
                        characterSetCur=CodeC;
                    }
                }
                if ((pSet[charCur]&CShift)!=0)
                {
                    /* >> Shift it and put out the shifted character */
                    ASCIIZ128(&pOutPos,characterSetCur,aShift,'\0');
                    emptyColumns-=2;
                    characterSetCur=(characterSetCur==CodeB)?CodeA:CodeB;
                    ASCIIZ128(&pOutPos,characterSetCur,data[charCur],'\0');
                    characterSetCur=(characterSetCur==CodeB)?CodeA:CodeB;
                }else{
                    /* Normal Character */
                    if (characterSetCur==CodeC)
                    {
                        if (data[charCur]==aFNC1)
                            A2C128_C(&pOutPos,aFNC1,'\0');
                        else
                        {
                            A2C128_C(&pOutPos, data[charCur], charCur + 1 < dataLength ? data[charCur + 1] : 0);
                            ++charCur;
                            /* We need this here to get the good index */
                            /* for the termination flags in Set. */
                        }
                    }else
                        ASCIIZ128(&pOutPos,characterSetCur,data[charCur],'\0');
                    --emptyColumns;
                }
                /* >> End Criteria */
                if ((pSet[charCur] & CFill) || (pSet[charCur] & CEnd))
                {
                    /* Fill Line but leave space for checks in last line */
                    if (rowCur == rows - 1) {
                        emptyColumns -= 2;
                    }
                    while(emptyColumns>0)
                    {
                        switch(characterSetCur){
                        case CodeC:
                            A2C128_C(&pOutPos,aCodeB,'\0');
                            characterSetCur=CodeB;
                            break;
                        case CodeB:
                            A2C128_B(&pOutPos,aCodeC);
                            characterSetCur=CodeC;
                            break;
                        case CodeA:
                            A2C128_A(&pOutPos,aCodeC);
                            characterSetCur=CodeC;
                            break;
                        }
                        --emptyColumns;
                    }
                }
                ++charCur;
            } /* Loop over characters */
        } /* if filling-Line / normal */

        /* Add checksum in last line */
        if (rowCur == rows - 1)
        {
            SumASCII(&pOutPos,Sum1,characterSetCur);
            SumASCII(&pOutPos,Sum2,characterSetCur);
        }
        /* Add Code 128 checksum */
        {
            int Sum = pOutput[columns * rowCur] % 103;
            int Pos = 1;
            for ( ; Pos < useColumns+3 ; Pos++)
            {
                Sum = (Sum + pOutput[columns * rowCur + Pos] * Pos) % 103;
            }
            *pOutPos=(uchar)Sum;
            pOutPos++;
        }
        /* Add end character */
        *pOutPos=106;
        pOutPos++;
    } /* End Lineloop */

    if (symbol->debug & ZINT_DEBUG_PRINT) {
        /* Dump the output to the screen
         */
        printf("\nCode 128 Code Numbers:\n");
        {   /* start a new level of local variables */
            int DPos, DPos2;
            for (DPos=0 ; DPos< rows ; DPos++)
            {
                for (DPos2=0 ; DPos2 < columns ; DPos2++)
                {
                    printf("%3d ",(int)(pOutput[DPos*columns+DPos2]));
                }
                printf("\n");
            }
        }
        printf("rows=%i columns=%i fillings=%i\n", rows, columns, fillings);
    }
#ifdef ZINT_TEST
    if (symbol->debug & ZINT_DEBUG_TEST) {
        debug_test_codeword_dump(symbol, pOutput, rows * columns);
    }
#endif

    /* Paint the C128 patterns */
    for (r = 0; r < rows; r++) {
        strcpy(dest, "");
        for(c = 0; c < columns; c++) {
            strcat(dest, C128Table[pOutput[r * columns + c]]);
        }
        expand(symbol, dest);
        symbol->row_height[r] = 10;
    }

    symbol->output_options |= BARCODE_BIND;

    if (symbol->border_width == 0) { /* Allow override if non-zero */
        symbol->border_width = 1; /* AIM ISS-X-24 Section 4.6.1 b) (note change from previous default 2) */
    }

    return 0;
}
