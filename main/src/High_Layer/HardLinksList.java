package High_Layer;

import Low_Layer.Descriptor;
import Low_Layer.FSDescriptorsTable;
import Low_Layer.FSDriver;

import java.util.HashMap;

/**
 * Created by LENOVO on 30.11.2018.
 */
public class HardLinksList {
    final static String CLASSNAME = "HardLinksList";
    final static String ENDSTR = HardLink.END;

    public static final HardLink root = new HardLink(0, 0, "");    //primary link to root directory. Cant be deleted
    HashMap<Integer, HardLink> list = new HashMap<>();

    int IDCTR = 1;

    public HardLinksList() {list.put(0, root);}


    /**
     * create name to file that not exist on system or will be linked by descriptor
     * @param descId: descriptor to FS driver
     * @param newName: new name of file
     * @return id of created link
     */
    public int createLink(int descId, String newName){
        int id = getNewId();        // id for new link
        HardLink nhl = new HardLink(id, descId, newName);
        list.put(id, nhl);  // add link to list
        return id;
    }

    /**
     * create new name to file. Use id of existing link to identifying file
     * @param linkId: id of source link to file
     * @param newName: own name of new file (can be not unique)
     * @return id of new link or -1 if error
     * Errors:
     *      there are no link with this id
     */
    public int link(int linkId, String newName){
        HardLink h = list.get(linkId);  //get source link
        if (h==null) return -1;     // return if source link not exist
        int id = getNewId();        // id for new link
        int tid = h.getTargetID();  // descriptor's id
        HardLink nhl = new HardLink(id, tid, newName);
        list.put(id, nhl);  // add link to list
        //driver.link(tid);   // refcount ++
        return id;          // return id of new link
    }

    /**
     * removes existing link. Delete file if there are no more links to this
     * @param linkId: id of source link to file
     */
    public void unlink(int linkId){
        if (list.containsKey(linkId)){
            if (linkId == 0) return;        //root dir name "" can't be unlinked
            HardLink h = list.get(linkId);
            int tid = h.getTargetID();      // descriptor's id
            //driver.unlink(tid);             // refcount --. Delete file if refcount == 0
            reduceIDCTR();                  // reduce IDCTR if it possible
            list.remove(linkId);            // remove link from list
        }
    }

    /**
     * change local name of file without movement
     * @param linkId:  id of source link to file
     * @param newName: new name of link
     * @return true if file successfully renamed
     * ERRORS:
     *      there are no link with this id
     *      you try to rename main name of root dir
     */
    public boolean rename(int linkId, String newName){
        if (list.containsKey(linkId)){
            if (linkId == 0) return false;      //root dir name "" can't be changed
            HardLink h = list.get(linkId);
            h.setName(newName);
            list.replace(linkId, h);
            return true;
        }
        return false;
    }

    /**
     * search file by name
     * @param linkId:  id of source link to file
     * @return descriptor's id or -1 if there are no file with this name
     */
    public int getDescId(int linkId){
        if (list.containsKey(linkId)){
            return list.get(linkId).getTargetID();
        }
        return -1;
    }

    public HardLink getLink(int linkId){
        return list.get(linkId);
    }

    /**
     * search in all list
     * @param name
     * @return
     */
    public int searchByName(String name){
        for (Object key:list.keySet()){
            HardLink h = list.get(key);
            if (name.equals(h.getName())) return (Integer)key;
        }
        return -1;
    }

    /**
     * search link with name "name" in set of links. Used to searching.
     * @param set: array with id's, that must be analyzed. If =null return -1
     * @param name: name, that used to compare
     * @return id of link with that name or -1
     */
    public int searchByNameInSet(int[] set, String name){
        final String METHNAME = CLASSNAME+".searchByName";
        if (set == null){
            return -1;
        }else{
            if (name == null) return -1;
            for (int key:set){
                if (list.containsKey(key)){
                    HardLink h = list.get(key);
                    if (name.equals(h.getName())) return key;
                }
            }
        }
        return -1;
    }

    int getNewId(){
        for (int i=1; i<IDCTR; i++){
            if (!list.containsKey(i)){
                return i;
            }
        }
        return IDCTR++;
    }

    void reduceIDCTR(){
        int max = 1;
        for (int i=1; i<IDCTR; i++){
            if (list.containsKey(i)){
                max = i;
            }
        }
        IDCTR = max+1;
    }

    //----- save/load

    public StringBuilder toStr(){
        StringBuilder sb = new StringBuilder();
        Object[] links =  list.values().toArray();
        for (Object o:links) sb.append(((HardLink)o).toStr());
        return sb;
    }

    public static HardLink[] fromString(String str){
        String[] sp = str.split(ENDSTR);
        HardLink[] links = new HardLink[sp.length];
        for (int i=0; i<sp.length; i++){
            links[i] = HardLink.fromString(sp[i]);
        }
        return links;
    }

    public void setList(HardLink[] links){
        HashMap<Integer, HardLink> table = new HashMap<>(links.length);
        for (HardLink h:links) table.put(h.getId(), h);
        this.list = table;
    }

    public StringBuilder toPrint(){
        StringBuilder sb = new StringBuilder();
        Object[] keys = list.keySet().toArray();
        for (Object key:keys) sb.append(key).append(": ").append(list.get(key).toPrint()).append('\n');
        return sb;
    }
}
