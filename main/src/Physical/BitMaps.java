package Physical;

import Global.Additional;
import Global.Consts;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class BitMaps {
    final int size = Consts.MEM_SEC_COUNT;
    boolean[] bad = new boolean[size];
    boolean[] busy = new boolean[size];

    public boolean isBad(int index){return bad[index];}
    public boolean isBusy(int index){return busy[index];}
    public boolean isFreeAndGood(int index){return !(busy[index] || bad [index]);}

    public void setBad(int index, boolean badVal) {
        this.bad[index] = badVal;
    }

    public void setBusy(int index, boolean busyVal) {
        this.busy[index] = busyVal;
    }

    /*
    search good and free sector from all. If not exist - returns -1
     */
    public int searchSector(){
        for (int i=0; i<size; i++){
            if (isFreeAndGood(i)) return i;
        }
        return -1;
    }

    public void clear(){
        for (int i=0; i<size; i++){
            bad[i] = false; busy[i] = false;
        }
    }

    public String getState(int index){
        StringBuilder sb = new StringBuilder();
        if (bad[index]) sb.append("bad, ");
        else sb.append("good, ");
        if (busy[index]) sb.append("busy");
        else sb.append("free");
        return sb.toString();
    }

    public String toString(){
        StringBuilder sbbad = new StringBuilder();
        StringBuilder sbbusy = new StringBuilder();
        //String sbad = Additional.toHex(bad), sbusy = Additional.toHex(busy);
        //return sbad+'\n'+sbusy;

        for (int i=0; i<size; i++){
            if (bad[i]) sbbad.append('1');
            else sbbad.append('0');
            if (busy[i]) sbbusy.append('1');
            else sbbusy.append('0');
        }
        return (sbbad.append("\n").append(sbbusy)).toString();
    }

    public void fromString(String str){
        String[] split = str.split("\n");

        int len = (split[0].length()<split[1].length()) ? split[0].length() : split[1].length();
        for (int i=0; i<len; i++){
            bad[i] = (split[0].charAt(i) == '1');
            busy[i] = (split[1].charAt(i) == '1');
        }
        for (int i=len; i<size; i++){
            bad[i] = false; busy[i] = false;
        }
    }
}
