package Physical;

import Global.Consts;
import Global.GlobalLog;

import java.util.Arrays;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class Sector {
    final int size = Consts.MEM_SEC_SIZE;
    final char emptysym = Consts.SECT_EMPTYSYM;
    char[] content = new char[size];

    public void write(char[] data){
        int dsize = data.length;
        if (dsize>size){
            GlobalLog.PhysicalLog.writeErr("Sector.write", "data is longer than sector size, len="+dsize);
            GlobalLog.PhysicalLog.writeStep("Sector.write", "ignores symbols after "+size+"th");
            dsize = size;
        }
        for (int i=0; i<dsize; i++){
            content[i] = data[i];
        }
        for (int i=dsize; i<size; i++){
            content[i] = emptysym;
        }
        GlobalLog.PhysicalLog.writeStep("Sector.write", "data writed successfully");
    }

    public char[] read(){
        GlobalLog.PhysicalLog.writeStep("Sector.read", "returns data");
        return content;
    }

    public void clear(){
        for (int i=0; i<size; i++){
            content[i] = emptysym;
        }
    }
}
