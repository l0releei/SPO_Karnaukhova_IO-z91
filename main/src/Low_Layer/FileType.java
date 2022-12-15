package Low_Layer;

import Global.Consts;

/**
 * Created by LENOVO on 27.11.2018.
 */
public enum FileType {
    _regular, _directory, _symlink, _hardlinks, _descriptorTable;

    /*
    codes to save:
    E - empty (it isn't file ot it is special file)
    R - regular file
    D - directory
    S - symlink file
    H - hardlinks list
    T - table of descriptors
     */
    final static String codes = Consts.FILETYPE_CODES;

    public static char getCode(FileType type){
        switch (type){
            case _regular: return codes.charAt(1);
            case _directory: return codes.charAt(2);
            case _symlink: return codes.charAt(3);
            case _hardlinks: return codes.charAt(4);
            case _descriptorTable: return codes.charAt(5);
            default: return codes.charAt(0);
        }
    }

    public static FileType getType(char c){
        char[] cds = codes.toCharArray();
        int i=0;
        while (i<cds.length) {
           if(Character.toUpperCase(c)==cds[i]){
               switch (i){
                   case 1: return _regular;
                   case 2: return _directory;
                   case 3: return _symlink;
                   case 4: return _hardlinks;
                   case 5: return _descriptorTable;
                   default:return null;
               }
            }
            i++;
        }
        return null;
    }

    @Override
    public String toString() {
        switch (this){
            case _regular: return "regular file";
            case _directory: return "directory";
            case _symlink: return "symbolic link";
            case _hardlinks: return "hardlinks list";
            case _descriptorTable: return "additional descriptors table";
            default: return "undefined";
        }
    }

    public boolean isStdType(){
        switch (this){
            case _regular:
            case _directory:
            case _symlink: return true;
            case _hardlinks:
            case _descriptorTable: return false;
            default:return false;
        }
    }
}
