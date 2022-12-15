package Low_Layer;

import Global.Additional;
import Global.Consts;
import Global.GlobalLog;
import Physical.DeviceDriver;
import sun.security.krb5.internal.crypto.Des;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by LENOVO on 28.11.2018.
 */
public class FSDriver {
    final static String CLASSNAME = "FSDriver";

    final int PRIM_SIZE = Consts.DESC_PRIMARY_LENGTH;
    final int PRIM_EMPTY = Consts.DESC_PRIMARY_EMPTYVAL;
    final int[] PRIM_LOCATION = {0, 1, 2, 3};

    int[] primaryPtr = new int[PRIM_SIZE];
    FSDescriptorsTable table = new FSDescriptorsTable();
    DeviceDriver device;

    public FSDriver(DeviceDriver device) {
        this.device = device;
        for (int i=0; i<PRIM_SIZE; i++) primaryPtr[i] = PRIM_EMPTY;
        for (int i:PRIM_LOCATION) device.take(i);
        save();
    }

    //---- getters/setters --------------------------------------------------

    public void setPrimaryPtr(int[] newdata){
        for (int i=0; i<PRIM_SIZE; i++){
            if (i<newdata.length) primaryPtr[i] = newdata[i];
            else primaryPtr[i] = -1;
        }
    }

    public int[] getPrimaryPtr() {
        return primaryPtr;
    }

//-------- public methods, working with files ---------------------------

    /**
     * creates empty file
     * @param type: type of created file
     * @return id of new file or -1 (ERR: invalid type)
     * creates descriptor of empty file
     */
    public int createFile(FileType type){
        return table.createStdFile(type);
    }

    public StringBuilder readFile(int id, int offset, int length){
        final String METHNAME = CLASSNAME+".readFile";
        if (!table.isValid(id)){
            GlobalLog.Llayer_writeErr(METHNAME, "ID isn't valid, break operation, id="+id);
            return null;
        }
        Descriptor dsc = table.getDescriptor(id);
        if (dsc == null){
            GlobalLog.Llayer_writeErr(METHNAME, "File not exist, id="+id);
            return null;
        }
        GlobalLog.Llayer_writeStep(METHNAME, "reads file, id="+id+", offset="+offset+", length="+length);
        int[] location = dsc.getSectors();
        if (location == null){
            GlobalLog.Llayer_writeStep(METHNAME, "empty file, returns empty string");
            return new StringBuilder("");
        }
        StringBuilder sb = device.read(location);
        if (dsc.getType() == FileType._regular){
            int size = dsc.getSize();
            if (offset>size){
                GlobalLog.Llayer_writeStep(METHNAME, "offset>size. Returns empty str. Offset="+offset+", size="+size);
                return new StringBuilder("");
            }
            int last = ((offset+length)<size) ? (offset+length) : (size);
            return new StringBuilder(sb.substring(offset, last));
        } else return sb;
    }

    public boolean writeFile(int id, int newsize, StringBuilder data){
        final String METHNAME = CLASSNAME+".writeFile";
        if (!table.isValid(id)){
            GlobalLog.Llayer_writeErr(METHNAME, "ID isn't valid, break operation");
            return false;
        }
        GlobalLog.Llayer_writeStep(METHNAME, "writes file, id="+id);
        int[] location = table.getLocation(id);
        if (location == null){
            //if file is empty / not exist - create it
            GlobalLog.Llayer_writeStep(METHNAME, "file is empty, create new file on device");
            location = device.create(data);
        } else{
            location = device.rewrite(location, data);
        }
        if (location == null){
            GlobalLog.Llayer_writeErr(METHNAME, "device memory overflow, break operation");
            return false;
        }
        if (table.rewriteStdFile(id, newsize, location)) return true;
        else GlobalLog.Llayer_writeErr(METHNAME,
                "undefined error: descriptor exist, file has been writed, but descriptor can't be update");
        return false;
    }

    public boolean resizeFile(int id, int newsize){
        final String METHNAME = CLASSNAME+".resize";
        Descriptor d = table.getDescriptor(id);
        int oseccount = d.getSectors().length;
        int rsize = newsize/Consts.MEM_SEC_SIZE + 1;
        GlobalLog.Llayer_writeStep(METHNAME, "resize file, id="+id+", oldsize="+oseccount+" sectors, new real size="+rsize+" sectors");
        int[] sectors = device.resize(d.getSectors(), rsize);
        if (sectors==null){
            GlobalLog.Llayer_writeErr(METHNAME, "device memory overflow, break operation");
            return false;
        }
        if (oseccount < rsize) device.erase(sectors, oseccount, (rsize - oseccount));
        d.rewrite(newsize, sectors);
        return true;
    }

    public void link(int id){
        table.link(id);
    }

    public void unlink(int id){
        final String METHNAME = CLASSNAME+".unlink";
        boolean b = table.unlink(id);
        if (b){
            //delete file if it unused
            GlobalLog.Llayer_writeStep(METHNAME, "file hasn't references and will be removed, id="+id);
            int[] sect = table.getLocation(id);
            device.delete(sect);
            table.deleteStdFile(id);
        }
    }

    public int getRefcount(int id){
        return table.table.containsKey(id) ? table.getDescriptor(id).getRefcount() : -1;
    }

    public FileType getFiletype(int id){return table.table.containsKey(id) ? table.getType(id) : null;}

//-------- special methods, working with table --------------------------

    public void writeLinks(StringBuilder data){
        final String METHNAME = CLASSNAME+".writeLinks";
        int[] location = table.getLocation(-1);
        GlobalLog.Llayer_writeStep(METHNAME, "rewrite hard links");
        if (location == null){
            //if file is empty / not exist - create it
            location = device.create(data);
        } else{
            location = device.rewrite(location, data);
        }
        if (location == null){
            GlobalLog.Llayer_writeErr(METHNAME, "device memory overflow, break operation");
            return;
        }
        table.rewriteHardLinksList(location);
    }

    public StringBuilder getLinks(){
        int[] location = table.getLocation(-1);
        if (location == null) return new StringBuilder();
        else return device.read(location);
    }

    public HashMap<Integer, Integer> reduceTable(){
        final String METHNAME = CLASSNAME+".reduceTable";
        GlobalLog.Llayer_writeStep(METHNAME, "reduce table, delete gaps between IDs");
        return table.reduceIDs();
    }

    //not to use
    public void deleteUnlinkedFiles(){
        int[] sectors = table.deleteAnonymous();
        device.delete(sectors);
    }

//-------- SAVE/LOAD PRIMARY ----------------------------------------------------

    public StringBuilder primaryToStr(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<PRIM_SIZE; i++){
            if (primaryPtr[i] == PRIM_EMPTY) break;
            else sb.append(primaryPtr[i]).append(Consts.REC_SPLIT);
        }
        return sb;
    }

    public boolean primaryFromString(String str){
        String[] split = str.replaceAll(String.valueOf(Consts.SECT_EMPTYSYM), "").split(String.valueOf(Consts.REC_SPLIT));
        //delete all empty symbols and split string to substring
        int L = split.length;
        if (L>PRIM_SIZE) return false;
        for (int i=0; i<L; i++){   //last string - nothing or empty symbols, ignore it
            primaryPtr[i] = Integer.valueOf(split[i]);
        }
        for (int i=L; i<PRIM_SIZE; i++) primaryPtr[i] = PRIM_EMPTY;
        return true;
    }

//------------ SAVE/LOAD  -----------------------------------------
    int[] tableblock = null;


    void savePrimary(){
        StringBuilder sb = primaryToStr();
        for (int i:PRIM_LOCATION) device.forcedRewrite(i, sb, 0);
    }

    void loadPrimary(){
        StringBuilder sb;
        for (int i:PRIM_LOCATION)
            if (device.test(i)){
                sb = device.read(i);
                primaryFromString(sb.toString());
                return;
            }
        GlobalLog.Llayer_writeErr(CLASSNAME, "cant download primary ptr: all sectors is bad, fatal error");
    }

    void save(StringBuilder s){
        int[] location = device.create(s);     // write data to device as file
        if (tableblock!=null) tableblock = Additional.concat(tableblock, location);
        else tableblock = location.clone();
        if (location == null) return;
        if (location.length <= PRIM_SIZE){                  // if primary can be rewrited
            setPrimaryPtr(location);
            savePrimary();
        }else {
            Descriptor d = new Descriptor(-1, FileType._descriptorTable);   //create additional table descriptor
            d.setSectors(location);         //put to it ptr to previous table
            save(d.toStr());
        }
    }

    public void save(){
        if (tableblock != null) device.delete(tableblock);  // delete all old data
        tableblock = null;
        StringBuilder s = table.toStr();                    // transform table to StringBuilder
        save(s);
    }

    void load(int[] sectors){
        if (tableblock!=null) tableblock = Additional.concat(tableblock, sectors);
        else tableblock = sectors.clone();
        StringBuilder sb = device.read(sectors);
        Descriptor[] tb = FSDescriptorsTable.fromString(sb.toString());
        if (tb[0].getType()==FileType._descriptorTable){
            int[] nxtsec = tb[0].getSectors();
            load(nxtsec);
        }else{
            table.setTable(tb);
        }
    }

    public void load(){
        loadPrimary();
        int x = 0;
        for (int i=0; i<PRIM_SIZE; i++){
            if (primaryPtr[i] == PRIM_EMPTY){
                x = i; break;
            }
        }
        int[] primloc = new int[x];
        System.arraycopy(primaryPtr, 0, primloc, 0, x);
        load(primloc);
    }

    public void saveAll(File file){
        save();
        device.saveToFile(file);
    }

    public void loadAll(File file){
        device.loadFromFile(file);
        load();
    }

    public StringBuilder toPrint(){
        StringBuilder sb = new StringBuilder();
        for (int i:primaryPtr) sb.append(i).append(' ');
        return sb.append('\n').append(table.toPrint());
    }

}
