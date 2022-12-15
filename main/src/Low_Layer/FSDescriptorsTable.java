package Low_Layer;

import Global.Additional;
import Global.Consts;
import Global.GlobalLog;
import com.sun.deploy.util.ArrayUtil;
import com.sun.istack.internal.Nullable;
import sun.security.krb5.internal.crypto.Des;

import java.util.*;

/**
 * Created by LENOVO on 29.11.2018.
 */
public class FSDescriptorsTable {
    final static String CLASSNAME = "DescriptorsTable";

    HashMap<Integer, Descriptor> table = new HashMap<>(2);
    
    private int idctr = 1;

    public FSDescriptorsTable() {
        Descriptor hlinklist = new Descriptor(-1, FileType._hardlinks, 0, -1, null);
        Descriptor rootdir = new Descriptor(0, FileType._directory, 0, 1, null);
        table.put(-1, hlinklist);
        table.put(0,rootdir);
    }

    public void setTable(Descriptor[] descs){
        HashMap<Integer, Descriptor> table = new HashMap<>(descs.length);
        for (Descriptor d:descs) table.put(d.getId(), d);
        this.table = table;
    }


//----------- creating files ---------------------------------
    /*
    not create files in device, only create descriptor to this.
    can create empty files (without sectors) and non-empty files (with non-zero size and with sectors)
     */

    /**
     * creates empty standard file with size=0, refcount = 1
     * returns id or -1 (error - non-standard type)
     * ATTENTION! This method not create file in device, it only creates new descriptor!
     * this method can be used without actions because system don't write empty files to device
     */
    public int createStdFile(FileType ft){
        final String METHNAME = CLASSNAME+".createStdFile";
        if (ft.isStdType()){
            int id = -2;
            for (int i=0; i<idctr; i++){    //search unused id
                if (!table.containsKey(i)){
                    id = i; break;
                }
            }
            if (id == -2) id = idctr++;
            Descriptor d = new Descriptor(ft, id, 0, 1, null);
            table.put(id, d);
            return id;
        }else{
            GlobalLog.Llayer_writeErr(METHNAME, "invalid std file type: "+ft.toString());
            return -1;
        }
    }

    /**
     * creates non-empty standard file with inputted length and sectors
     * returns id or -1 (error - non-standard type)
     * ATTENTION! This method not create file in device, it only creates new descriptor!
     * this method must be used after really actions
     */
    public int createStdFile(FileType ft, int size, int[] sectors){
        if (ft.isStdType()){
            if (sectors==null) return createStdFile(ft);
            Descriptor d = new Descriptor(ft, idctr++, size, 1, sectors);
            table.put(d.getId(), d);
            return d.getId();
        }else return -1;
    }

    /**
     * rewrite or truncate file
     * returns true if successfully of false if there are no file or file is not standard
     * ATTENTION! This method not write file to device, not reallocate sectors to file, it only changes descriptor!
     * this method must be used after really actions
     */
    public boolean rewriteStdFile(int id, int size, int[] sectors){
        final String METHNAME = CLASSNAME+".rewriteStdFile";
        if (id<0){return false;}
        Descriptor d = table.get(id);
        if (d==null){
            GlobalLog.Llayer_writeErr(METHNAME, "there are no descriptor in system with id="+id);
            return false;
        }
        d.rewrite(size, sectors);
        table.replace(id, d);
        return true;
    }

    //return sectors, where file takes, or null if file not exist / is empty
    public int[] getLocation(int id){
        Descriptor d = table.get(id);
        if (d==null) return null;
        return table.get(id).getSectors();
    }

    public FileType getType(int id){
        Descriptor d = table.get(id);
        if (d==null) return null;
        return table.get(id).getType();
    }

    public boolean exist(int id){return table.containsKey(id);}

    public boolean isValid(int id){
        return id>=0;
    }

    Descriptor getDescriptor(int id){
        return table.get(id);
    }

    /**
     * delete descriptor
     * can't delete file with id = 0
     * ATTENTION! This method not delete file from device, not free sectors that file used, it only delete descriptor!
     * this method must be used after really actions
     */
    public void deleteStdFile(int id){
        final String METHNAME = CLASSNAME+".delete";
        if (id<=0){
            GlobalLog.Llayer_writeErr(METHNAME, "invalid descriptor's id to delete, id="+id);
            return;
        }
        table.remove(id);
        reduceIDCTR();
    }

    void reduceIDCTR(){
        int max = 0;
        for (int i=1; i<idctr; i++){
            if (table.containsKey(i)) max = i;
        }
        idctr = max+1;
    }
    
    /**
     * this method is used by system to rewrite list of links
     * ATTENTION! This method not write file to device, not reallocate sectors to file, it only changes descriptor!
     * ATTENTION! Don't use this method from high layer, it dangerously
     */
    public void rewriteHardLinksList(int[] sectors){
        Descriptor d = table.get(-1);
        d.rewrite(0, sectors);
        table.replace(-1, d);
    }

    public void link(int id){
        if(!isValid(id)) return;
        table.get(id).link();
    }

    public boolean unlink(int id) {
        if (!isValid(id)) return false;
        boolean b = table.get(id).unlink();
        return (id > 0) && b;
    }

    /**
     *
     * @return
     */
    @Nullable
    public int[] deleteAnonymous(){
        final String METHNAME = CLASSNAME+".deleteAnonymous";
        Integer[] keys = (Integer[])table.keySet().toArray();
        int[] sectors = null;
        for (Integer key : keys) {
            if (key == -1) continue;
            if (key == 0) continue;
            Descriptor d = table.get(key);
            if (!d.getType().isStdType()) continue;
            if (d.getRefcount() == 0){
                GlobalLog.Llayer_writeStep(METHNAME, "delete file with id="+d.getId());
                int[] s = d.getSectors();
                if (sectors == null) sectors = s;
                else{
                    sectors = Additional.concat(sectors, s);
                }
                table.remove(key);
            }
        }
        return sectors;
    }

    /**
     * reduce IDs, that was been allocated to tables descriptors
     * @return map of equations between old id and new id
     */
    public HashMap<Integer, Integer> reduceIDs(){
        final String METHNAME = CLASSNAME+".reduceIDs";
        Descriptor hlinks = table.get(-1), root = table.get(0);
        table.remove(-1); table.remove(0);
        Object[] descs = table.values().toArray();
        HashMap<Integer, Descriptor> newTable = new HashMap<>(descs.length);
        HashMap<Integer, Integer> IDmap = new HashMap<>(descs.length+1);
        newTable.put(-1, hlinks); newTable.put(0, root);
        IDmap.put(0, 0);
        for (int i=1; i<=descs.length; i++){
            Descriptor d = (Descriptor) descs[i-1];
            GlobalLog.Llayer_writeStep(METHNAME, "file descriptor has new id: old="+d.getId()+", new="+i);
            IDmap.put(d.getId(), i);
            d.setId(i);
            newTable.put(i, d);
        }
        idctr = descs.length+1;
        table = newTable;
        return IDmap;
    }

    public StringBuilder toStr(){
        StringBuilder sb = new StringBuilder();
        Object[] descs =  table.values().toArray();
        for (Object d:descs) sb.append(((Descriptor)d).toStr());
        return sb;
    }

    public static Descriptor[] fromString(String str){
        String[] sp = str.replaceAll(Consts.SECT_EMPTYSTR, "").split(Consts.END_STR);
        Descriptor[] desc = new Descriptor[sp.length];
        for (int i=0; i<sp.length; i++){
            desc[i] = Descriptor.fromString(sp[i]);
        }
        return desc;
    }

    public StringBuilder toPrint(){
        StringBuilder sb = new StringBuilder();
        Object[] keys = table.keySet().toArray();
        for (Object key:keys) sb.append(key).append(": ").append(table.get(key).toPrint()).append('\n');
        return sb;
    }
}
