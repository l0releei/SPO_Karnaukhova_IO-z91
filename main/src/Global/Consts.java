package Global;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class Consts {
    //physical layer

    public static final int MEM_SEC_COUNT = 256;            //count of sectors in device
    public static final int MEM_SEC_SIZE = 64;              //size of sector (in chars)
    public static final char SECT_EMPTYSYM = ' ';           //empty symbol, that used in erase procedure

    public static final String SECT_EMPTYSTR = String.valueOf(SECT_EMPTYSYM);
    //public static final int DRIVER_PRIM_TABLE_SIZE = 16;    //size of primary descriptor table

    //low layer

    /*
    codes to save:
    E - empty
    R - regular file
    D - directory
    S - symlink file
    H - hardlinks list
    T - table of descriptors
     */
    public static final String FILETYPE_CODES = "ERDSHT";
    public static final char REC_SPLIT = '_';
    public static final char REC_BEGIN = '@';
    public static final char REC_END = '#';

    public static final String SPLIT_STR = String.valueOf(REC_SPLIT);
    public static final String BEGIN_STR = String.valueOf(REC_BEGIN);
    public static final String END_STR = String.valueOf(REC_END);

    public static final int DESC_MAX_SEC_NUM_LENGTH = String.valueOf(MEM_SEC_COUNT).length();
    public static final int DESC_PRIMARY_LENGTH = MEM_SEC_SIZE/(DESC_MAX_SEC_NUM_LENGTH+1);
    public static final int DESC_PRIMARY_EMPTYVAL = -1;

    public static final char FILE_SPLITSYM = '/';
    public static final String FILE_SPLITSTR = String.valueOf(FILE_SPLITSYM);
}

