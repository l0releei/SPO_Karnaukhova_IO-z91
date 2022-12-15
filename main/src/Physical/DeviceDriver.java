package Physical;

import Global.Consts;
import Global.GlobalLog;
import IO.MyIO;

import java.io.File;
import java.util.Arrays;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class DeviceDriver {
    final static String CLASSNAME = "DeviceDriver";
    //consts
    final int devsize = Consts.MEM_SEC_COUNT;
    final int secsize = Consts.MEM_SEC_SIZE;
    //device
    Sector[] device = new Sector[devsize];
    BitMaps deviceState = new BitMaps();
    //data
    //int reservedBlock = Consts.DRIVER_PRIM_TABLE_SIZE; //size of reserved to descriptors table block (0..rB-1)

    //---------- constructors -----------------------------

    public DeviceDriver() {init();}

    public DeviceDriver(int reservedBlock) {
        //this.reservedBlock = reservedBlock;
        init();
    }

    //take first blocks to descriptors table
    void init() {
        for (int i=0; i<devsize; i++) device[i] = new Sector();
        //for (int i=0; i<reservedBlock; i++) deviceState.setBusy(i, true);

    }

   //--------- public methods -----------------------------------


    /**
     *  creates new file in device
        search free good sectors and write data
     * @param data: set of symbols, that method must write to device
     * @return sectors: list of sectors, that has been allocated to file storing
     * Possible errors:
     *      * there are no free good sector to write: return null
     */
    public int[] create(StringBuilder data){
        final String METHNAME = CLASSNAME+".create";
        int size = data.length();
        int secCount = 1 + size/secsize;
        GlobalLog.PhysicalLog.writeStep(METHNAME, "write file to new "+secCount+" blocks");
        int[] sectors = searchBlock(secCount);
        if (sectors == null){
            GlobalLog.PhysicalLog.writeErr(METHNAME,
                    "there are no free good blocks, can't create file, break operation and return null");
        }else{
            forcedRewrite(sectors, data);
        }
        return sectors;
    }

//------------ READ -----------------------------------------------------

    public StringBuilder read(int sector){
        final String METHNAME = CLASSNAME+".read";
        if (isCorrectNum(sector)) return new StringBuilder().append(device[sector].read());
        else{
            //buf.append(" ///LOST_SECTOR/// ");
            GlobalLog.PhysicalLog.writeErr(METHNAME, "sector's num isn't correct, num="+sector);
            GlobalLog.PhysicalLog.writeStep(METHNAME, "ignore this sector, return empty string");
            return new StringBuilder();
        }
    }

    /**
     * reads data from sectors
     * @param sectors: int[], list of sectors, that method must read
     * @return data, readed from sector
     */
    public StringBuilder read(int[] sectors){
        final String METHNAME = CLASSNAME+".read";
        StringBuilder buf = new StringBuilder();
        GlobalLog.PhysicalLog.writeStep(METHNAME, "try to read file");
        for (int sector : sectors) buf.append(read(sector));
        return buf;
    }

//--------------- FORCED REWRITE --------------------------------------------
    /**
     * rewrite sector, ignore sector's state. If number is correct - rewrite, else - return;
     * @param sector: sector to rewrite
     * @param data: data, that must be write
     * @param offset: first symbol of substring from "data", that must be written
     * @return true if block has been rewrite
     */
    public boolean forcedRewrite(int sector, StringBuilder data, int offset){
        final String METHNAME = CLASSNAME+".forcedRewrite";
        //GlobalLog.PhysicalLog.writeStep(METHNAME, "rewrites sector "+sector);
        return rwrSector(sector, data, offset);
    }

    public boolean forcedRewrite(int[] sectors, StringBuilder data){
        final String METHNAME = CLASSNAME+".forcedRewrite";
        int offset = 0; boolean b = true;
        for (int sector:sectors){
            b &= forcedRewrite(sector, data, offset);
            offset += secsize;
        }
        return b;
    }

//------------- RESIZE WITHOUT IMMEDIATELY CHANGES ------------------------

    /**
     * allocate new block to file if new size is bigger or delete unused blocks if new size is smaller
     * @param sectors: old file block
     * @param newSize: new size of file
     * @return block of resized file
     */
    public int[] resize(int[] sectors, int newSize){
        final String METHNAME = CLASSNAME+".resize";
        int[] resized = new int[newSize];
        int size = sectors.length;
        int len = newSize - size;
        if (len > 0){    //new block is bigger then old - search new sectors
            int[] block = searchBlock(len);
            if (block==null){
                GlobalLog.PhysicalLog.writeErr(METHNAME,
                        "there are no free good sectors to file, delete file from device");
                delete(sectors, 0, len);
                return null;       //operation cant be finished: there are no free sectors, return
            }else{
                System.arraycopy(sectors, 0, resized, 0, size);
                System.arraycopy(block, 0, resized, size, len);
            }
        }else{                      //new block is smaller then old - delete "tail"
            System.arraycopy(sectors, 0, resized, 0, newSize);
            delete(sectors, newSize, -len);
        }
        return resized;
    }

//---------- DELETE BADS -----------------------------------------------------

    /**
     * substitute bad blocks to good.
     * @param sectors: old block of file
     * @param length: size of new file
     * @return sector's array with same length with changed blocks in [0..length]. Other blocks will be copied
     */
    public int[] substituteBads(int[] sectors, int length){
        final String METHNAME = CLASSNAME+".substituteBads";
        int size = sectors.length;
        int bound = (size<length)? size : length;
        if (test(sectors, 0, bound)){     //if all sectors is correct - copy data to new array
            return sectors;
        }else{      //if there are bad blocks - check all and substitute bad by new
            int[] block = new int[size];
            for (int i=0; i<bound; i++){
                int sector = sectors[i];
                if (test(sector)){  //sector is good and correct
                    block[i] = sectors[i];
                }else{              //bad or incorrect sector
                    sector = searchSector();    //search free good sector
                    if (sector != -1){          //if there is sector - add new to block and remove old
                        block[i] = sector;
                        take(sector);           //set sector as busy
                        delete(sectors[i]);
                    } else{
                        GlobalLog.PhysicalLog.writeErr(METHNAME,
                                "there are no free good sectors to substitution bad sectors, delete file from device");
                        delete(sectors, 0, size);
                        return null;       //operation cant be finished: there are no free sectors, return
                    }
                }
            }
            if (bound<size) System.arraycopy(sectors, bound, block, bound, (size-bound));
            return block;
        }
    }

//---------- REWRITE ----------------------------------------------------------

    /**
     * try to rewrite one sector.
     * If it possible - rewrite, else - search new, if there are no free good blocks - return;
     * @param sector: sector to rewrite
     * @param data: data, that must be write
     * @param offset: first symbol of substring from "data", that must be written
     * @return -1 if there are no free sectors else return number of rewritten sector
     */
    public int rewrite(int sector, StringBuilder data, int offset){
        final String METHNAME = CLASSNAME+".rewrite";
        GlobalLog.PhysicalLog.writeStep(METHNAME, "try to rewrite sector "+sector);
        if (!rwrSector(sector, data, offset)){
            int num = searchSector();
            GlobalLog.PhysicalLog.writeStep(METHNAME, "search new sector. Result = "+num);
            if (num != -1) rwrSector(sector, data, offset);
            return num;
        }
        return sector;
    }

    /**
     * try to rewrite block.
     * Substitute bad blocks by good and change size of file if it necessary
     * If it possible - return new block, else return null
     * At first method creates list of blocks
     * @param oldsectors: old block
     * @param data: data to rewrite
     * @return int[] - new block's sectors
     */
    public int[] rewrite(int[] oldsectors, StringBuilder data){
        final String METHNAME = CLASSNAME+".rewrite";
        int size = data.length();
        int secCount = 1 + size/secsize;
        int oseccount = 1 + oldsectors.length/secsize;
        //get minimal length
        //int len = (secCount<oseccount) ? secCount: oseccount;

        //STEP 1: create a block, remove bad sectors, allocate new or delete excess sectors if it necessary

        int[] sectors = resize(oldsectors, secCount);   // resize block: allocate new sectors or delete unused
        sectors = substituteBads(sectors, oseccount);   // substitute bad sectors in block of old file


        /*int[] sectors = new int[len]; //create block with minimal size
        if (test(oldsectors, 0, len)){     //if all sectors is correct - copy data to new array
            System.arraycopy(oldsectors, 0, sectors, 0, len);
        }else{      //if there are bad blocks - check all and substitute bad by new
            for (int i=0; i<len; i++){
                int sector = oldsectors[i];
                if (test(sector)){  //sector is good and correct
                    sectors[i] = oldsectors[i];
                }else{              //bad or incorrect sector
                    sector = searchSector();    //search free good sector
                    if (sector != -1){          //if there is sector - add new to block and remove old
                        sectors[i] = sector;
                        take(sector);           //set sector as busy
                        delete(oldsectors[i]);
                    } else{
                        GlobalLog.PhysicalLog.writeErr(METHNAME,
                                "there are no free good sectors to substitution bad sectors, delete file from device");
                        delete(sectors, 0, i);
                        return null;       //operation cant be finished: there are no free sectors, return
                    }
                }
            }
        }*/
        //check sizes of old and new blocks
        //sectors = resize(sectors, )
        /*
        if (secCount>oseccount){    //new block is bigger then old - search new sectors
            int[] block = searchBlock(secCount-len);
            if (block==null){
                GlobalLog.PhysicalLog.writeErr(METHNAME,
                        "there are no free good sectors to file, delete file from device");
                delete(sectors, 0, len);
                return null;       //operation cant be finished: there are no free sectors, return
            }else{
                int ctr=0;
                for (int sec:block) sectors[ctr++] = sec;
            }
        }else{                      //old block is smaller then new - delete "tail"
            delete(oldsectors, len, (oseccount-len));
        }*/

        //STEP 2: write information to block
        forcedRewrite(sectors, data);
        return sectors;
    }

//-------- TAKE ------------------------------------------

    public void take(int sector){
        final String METHNAME = CLASSNAME+".take";
        if (!isCorrectNum(sector)){
            GlobalLog.PhysicalLog.writeErr(METHNAME, "there are no sector with num " + sector);
        }
        if (!deviceState.isBusy(sector)){
            GlobalLog.PhysicalLog.writeStep(METHNAME, "set sector as busy "+sector);
            deviceState.setBusy(sector, true);
        }
    }

//--------- DELETE ----------------------------------------

    public void delete(int[] sectors){
        if (sectors == null) return;
        delete(sectors, 0, sectors.length);
    }

    public boolean delete(int[] sectors, int first, int count){
        if (sectors==null) return true;
        if (count<=0) return false;
        if (first<0) return false;
        if (first+count>devsize) return false;

        for (int i=first; i<first+count; i++){
            delete(sectors[i]);
        }
        return true;
    }

    public void delete(int sector){
        final String METHNAME = CLASSNAME+".delete";
        if (!isCorrectNum(sector)){
            GlobalLog.PhysicalLog.writeErr(METHNAME, "there are no sector with num " + sector);
        }
        deviceState.setBusy(sector, false);
    }

//-------------- ERASE -----------------------------------------------------

    /**
     * erase data in blocks. If count is very big erase to the end of array
     * @param sectors: array of sectors
     * @param offset: first erased element
     * @param count: count of elements, that must be erased
     */
    public void erase(int[] sectors, int offset, int count){
        if ((offset<0)||(offset>=sectors.length)) return;
        int bound = (offset+count<sectors.length) ? offset+count : sectors.length;
        for (int i=offset; i<bound; i++) device[i].clear();
    }

//-------------- TESTS BY GOOD ---------------------------------------------
    public boolean test(int sector){
        boolean b = isCorrectNum(sector);
        return b && (!deviceState.isBad(sector));
    }

    public boolean test(int[] sectors, int first, int count){
        boolean b = true;
        for (int i=first; i<count; i++) b &= isCorrectNum(sectors[i]) && (!deviceState.isBad(sectors[i]));
        return b;
    }

    public boolean test(int[] sectors){
        return test(sectors, 0, sectors.length);
    }



    public boolean testToWrite(int sector){
        final String METHNAME = CLASSNAME+".testToWrite";
        if (!isCorrectNum(sector)) {
            GlobalLog.PhysicalLog.writeErr(METHNAME, "sector's num isn't correct, num=" + sector);
            return false;
        }else{
            return deviceState.isFreeAndGood(sector);
        }
    }

//------ check numbers of sectors --------------------------------------

    /**
     * check one sector
     * @param sector: number
     * @return true if there is sector in device with this number
     */
    public boolean isCorrectNum(int sector){
        return ((sector>=0)&&(sector<devsize));
    }

    /**
     * check some sectors from array
     * diapason - [first..(first+count-1)]
     * @param sectors: array
     * @param first: first element of checked diapason
     * @param count: count of checked elements
     * @return true if all sectors is correct
     * ATTENTION! "FIRST" AND "COUNT" MUST BE CORRECT!
     */
    public boolean isCorrectNum(int[] sectors, int first, int count){
        boolean b = true;
        for (int i=first; i<first+count; i++){
            b &= isCorrectNum(sectors[i]);
        }
        return b;
    }

    /**
     * check all sectors from array
     * @param sectors: array
     * @return true if all sectors is correct
     */
    public boolean isCorrectNum(int[] sectors){
        return isCorrectNum(sectors, 0, sectors.length);
    }

//-------- functions to printing results--------------------

    public String getState(int index){
        return deviceState.getState(index);
    }
    public String getBitMaps(){
        return deviceState.toString();
    }


//------- additional unpublic methods ---------------------------------

    /**
     * search next free sector
     * @return
     */
    int searchSector(){
        return deviceState.searchSector();
    }

    /**
     * search and allocate block to file
     * @return block
     */
    int[] searchBlock(int count){
        final String METHNAME = CLASSNAME+".searchBlock";
        int[] sectors = new int[count];
        for (int i=0; i<sectors.length; i++) {
            int sector = searchSector();     //search free good sector
            GlobalLog.PhysicalLog.writeStep(METHNAME, "search new sector. Result = "+sector);
            if (sector != -1){               //if there is sector - add new to block and remove old
                sectors[i] = sector;
                take(sector);               //set sector as busy
            } else{
                GlobalLog.PhysicalLog.writeStep(METHNAME, "there are no free good sectors, delete allocated and return null");
                delete(sectors, 0, i);
                return null;       //operation cant be finished: there are no free sectors, return
            }
        }
        return sectors;
    }


    /**
     * rewrite this sector or write data to new. If there are no sectors to rewrite return -1
     * @param oldsector: sector to rewrite
     * @param data: data, that must be write
     * @param offset: first symbol of substring from "data", that must be written
     * @return number of sector, that really been rewrite or -1
     */
    int rewriteSector(int oldsector, StringBuilder data, int offset){
        final String METHNAME = CLASSNAME+".rewriteSector";
        boolean getnew = isCorrectNum(oldsector);
        //check exceptions
        //by correct num
        if (!getnew) {
            GlobalLog.PhysicalLog.writeErr(METHNAME, "sector's num isn't correct, num=" + oldsector);
        }
        //by good sector
        if (getnew |= (deviceState.isBad(oldsector)))
            GlobalLog.PhysicalLog.writeStep(METHNAME, "sector " + oldsector+" is bad");
        // search new block or use old
        int num = -1;
        if (getnew){
            GlobalLog.PhysicalLog.writeStep(METHNAME, "write data to new sector");
            num = deviceState.searchSector();
        }
        else{
            num = oldsector;
        }
        // do operation or return error
        if (rwrSector(num, data, offset)) return num;
        else return -1;
    }

    /*

     */
    boolean rwrSector(int num, StringBuilder data, int offset){
        final String METHNAME = CLASSNAME+".rwrSector";
        if (isCorrectNum(num)) {
            GlobalLog.PhysicalLog.writeStep(METHNAME, "write to sector "+num);
            int end = offset+secsize;
            if (end>=data.length()) end = data.length();
            String s = data.substring(offset, end);
            device[num].write(s.toCharArray());
            deviceState.setBusy(num, true);
            return true;
        }else{
            GlobalLog.PhysicalLog.writeErr(METHNAME, "uncorrect sector num "+num);
            GlobalLog.PhysicalLog.writeStep(METHNAME, "break operation and return false");
            return false;
        }
    }

//----------- To/from string ---------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(deviceState.toString()); sb.append('\n');
        for (int i=0; i<devsize; i++){
            sb.append(device[i].read()).append('\n');
        }
        return sb.toString();
    }

    public void fromString(String str) {
        String[] split = str.split("\n");
        if (split.length != devsize+2){
            GlobalLog.writeStep(CLASSNAME+".fromString", "Loaded file data has incorrect format, break operstion");
            return;
        }
        deviceState.fromString(split[0]+'\n'+split[1]);
        int len = ((split.length-2)<devsize) ? (split.length-2) : devsize;
        for (int i=0; i<len; i++){
            device[i].write(split[i].toCharArray());
        }
        for (int i = len; i<devsize; i++){
            device[i].clear();
        }
    }


    public void setBad(int sector, int state){
        deviceState.setBad(sector, (state==0));
    }

//---------- save/load from file  --------------------------------------

    public boolean saveToFile(File file){
        String data = toString();
        //System.out.println(data);
        return MyIO.writeToFile(file, data) == MyIO.SUCCESS;
    }

    public boolean loadFromFile(File file){
        String data = MyIO.readFromFile(file);
        if (data == null) return false;
        //System.out.println(data);
        fromString(data);
        return true;
    }
}
