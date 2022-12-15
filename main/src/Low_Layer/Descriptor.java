package Low_Layer;

import Global.Consts;
import Global.GlobalLog;

import javax.sound.midi.MetaEventListener;

/**
 * Created by LENOVO on 27.11.2018.
 */
public class Descriptor {
    final static String CLASSNAME = "Descriptor";
    final static String BEGIN = Consts.BEGIN_STR;
    final static String SPLIT = Consts.SPLIT_STR;

    /** RULES
     * there are 5 type of files:
     * main types (use in high layer)
     *      regular
     *      directory
     *      symlink
     *additional types
     *      hardlink list - use to storing hardlinks, can has only one descriptor in system
     *      additional descriptor table - use to extending primary table
     *
     * All types has array of sectors, where file stores.
     * id - identifier to hard link (link used it to recognizing assigned files)
     * size - really size in symbols. System use this parameter to cutting empty symbols after data block on read.
     * refcount - use to delete unused files. Additional types don't use this parameter
     * there 3 parameters used only in main types. Other types not use this parameters
     */

    int id = -1;
    FileType type = FileType._regular;  //
    int size = 0;                       // in symbols
    int refcount = 1;                   // count of references (not used for descriptor table and hardlinks list)
    int[] sectors = null;               // sectors, used to

    public Descriptor(int id, FileType type) {
        this.type = type; this.id = id;
    }

    public Descriptor(FileType type, int id) {
        this.type = type; this.id = id;
    }

    public Descriptor(FileType type) {
        this.type = type;
    }

    public Descriptor(int id, FileType type, int size, int refcount, int[] sectors) {
        this.id = id;
        this.type = type;
        this.size = size;
        this.refcount = refcount;
        this.sectors = sectors;
    }

    public Descriptor(FileType type, int id, int size, int refcount, int[] sectors) {
        this.id = id;
        this.type = type;
        this.size = size;
        this.refcount = refcount;
        this.sectors = sectors;
    }

    public void rewrite(int size, int[] sectors){
        this.size = size; this.sectors = sectors;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getRefcount() {
        return refcount;
    }

    public void setRefcount(int refcount) {
        this.refcount = refcount;
    }

    public int[] getSectors() {
        return sectors;
    }

    public void setSectors(int[] sectors) {
        this.sectors = sectors;
    }

    public void link(){refcount++;}
    public boolean unlink(){return (--refcount)==0;}

    /**
    format:
     for regular, directory and symlink:
        [RDS]_id_size_refcount{_sectors[i]}#
     for hardlinks list and additional descriptor tables:
        [HT]_{_sectors[i]}#
     */
    public StringBuilder toStr() {
        StringBuilder sb = new StringBuilder();
        sb.append(FileType.getCode(type));
        switch (type){
            case _regular:
            case _directory:
            case _symlink:
            {
                sb.append(SPLIT);
                sb.append(id).append(SPLIT);
                sb.append(size).append(SPLIT);
                sb.append(refcount);
            }
        }
        if (sectors != null) for (int s:sectors) {sb.append(SPLIT).append(s);}
        sb.append(Consts.REC_END);
        return sb;
    }

    @Override
    public String toString() {
        return toStr().toString();
    }

    public static Descriptor fromString(String s){
        final String METHNAME = CLASSNAME+".fromString";
        String[] data = s.split(SPLIT);
        //parse filetype
        char c = data[0].charAt(0);
        FileType ft = FileType.getType(c);
        int i = 1;
        int id = -1;
        int size = 0;
        int refcount = -1;
        int[] sec = null;
        try {
            switch (ft) {
                case _regular:
                case _directory:
                case _symlink: {
                    id = Integer.valueOf(data[i++]);
                    size = Integer.valueOf(data[i++]);
                    refcount = Integer.valueOf(data[i++]);
                }
                default:
            }
            if (i!=data.length){
                sec = new int[data.length - i];
                for (int j = 0; j < sec.length; j++) {
                    sec[j] = Integer.valueOf(data[i++]);
                }
            }
            return new Descriptor(id, ft, size, refcount, sec);
        }catch (NullPointerException npe){
            GlobalLog.Llayer_writeErr(METHNAME, "Undefined file type identifier "+c);
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder();
            sb.append("Integer value can't be parse. Values of variable:\n\tfileid = ").append(c).
                    append("\n\tid = ").append(id).
                    append(", defvalue: -1\n\tsize = ").append(size).
                    append(", defvalue=0\n\trefcount = ").append(refcount).
                    append(", defvalue = -1\n\tsectors (defvalue=0):\n");
            if (sec == null) sb.append("\t\t NULL (wasn't create)");
            else for (int sc:sec) sb.append("\t\t").append(sc).append("\n");
            GlobalLog.Llayer_writeErr(METHNAME, sb.toString());
        }
        return null;
    }

    public StringBuilder toPrint(){
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append(", id=").append(id);
        sb.append(", size=").append(size);
        sb.append(", ref=").append(refcount).append(", sect:");
        if (sectors != null) for (int s:sectors) {sb.append(" ").append(s);}
        else sb.append(" <null> ");
        return sb;
    }

}
