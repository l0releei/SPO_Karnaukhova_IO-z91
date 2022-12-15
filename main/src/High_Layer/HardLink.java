package High_Layer;

import Global.Consts;
import Global.GlobalLog;

/**
 * Created by LENOVO on 28.11.2018.
 */
public class HardLink {
    final static String CLASSNAME = "HardLink";

    final static String SPLIT = Consts.SPLIT_STR;
    final static String END = Consts.END_STR;

    int id = 0;             //identifier of this link
    int targetID = 0;       //identifier of target FS descriptor
    String name = "NewFile";       //assigned name of file

    //not used
    public HardLink(int targetID, String name) {
        //this.id = id;
        this.targetID = targetID;
        this.name = name;
    }

    /**
     * put new file to root directory
     * @param id
     * @param targetID
     * @param name
     */
    public HardLink(int id, int targetID, String name) {
        this.id = id;
        this.targetID = targetID;
        this.name = name;
    }

    /**
     * put new file to directory with id "parantID"

     */
    /*public HardLink(int id, int parentid, int targetID, String name) {
        this.id = id;
        this.parentid = parentid;
        this.targetID = targetID;
        this.name = name;
    }*/


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

   /* public int getParentid() {
        return parentid;
    }

    public void setParentid(int parentid) {
        this.parentid = parentid;
    }*/

    public int getTargetID() {
        return targetID;
    }

    public void setTargetID(int targetID) {
        this.targetID = targetID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StringBuilder toStr() {
        return new StringBuilder(id + SPLIT + targetID + SPLIT + name + END);
    }

    public static HardLink fromString(String data){
        final String METHNAME = CLASSNAME+".fromString";
        String[] str = data.split(SPLIT, 3);
        int i = 0;
        int id = 0;
        //int pid = 0;
        int tid = 0;
        try {
            id = Integer.valueOf(str[i++]);
            //pid = Integer.valueOf(str[i++]);
            tid = Integer.valueOf(str[i++]);
        }catch (NumberFormatException e){
            StringBuilder sb = new StringBuilder("ID is not correct number. Parsed strings = ");
            for (String s:str) sb.append(s).append(' ');
            GlobalLog.Llayer_writeErr(METHNAME, sb.toString());
            return null;
        }
        String name = str[i++];
        return new HardLink(id, tid, name);
    }

    public StringBuilder toPrint(){
        StringBuilder sb = new StringBuilder();
        sb.append(", id = ").append(id);
        sb.append(", FS descriptor ID =").append(targetID);
        sb.append(", assigned name = ").append(name);
        return sb;
    }
}
