package High_Layer;

import Global.Consts;
import Global.GlobalLog;
import Low_Layer.FSDriver;
import Low_Layer.FileType;

import java.io.*;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by LENOVO on 09.12.2018.
 */
public class FileSystem {
    final static String CLASSNAME = "FileSystem";
    final int rootId = 0;
    final String FSPLIT = Consts.FILE_SPLITSTR;

    Directory ROOT;
    Directory PATH;

    HashMap<Integer, Integer> descriptors = new HashMap<>();    // key - high file descriptor, value - real descriptor

    HardLinksList HLlist;
    FSDriver driver;

    public FileSystem(FSDriver driver) {
        HLlist = new HardLinksList();
        this.driver = driver;
        ROOT = new Directory(0, null, "");
        PATH = ROOT;
    }

    //--------- possible system calls ------------------------------


    /**
     * create new location and name of existing file
     * @param way: way to source file, dir or symlink
     * @param newWay: new location of file (existing directory)
     * @param newName: new name of file
     * @return linkId if success, else return -1
     */
    public int link(String way, String newWay, String newName){
        final String METHNAME = CLASSNAME+".link (by linkId)";

        int linkId = search(way);       // STEP 1: get existing linkID
        if (linkId<0){
            GlobalLog.writeErr(METHNAME, "can't open old file location by way "+way);
            return -1;
        }
        int destId = search(newWay);    // STEP 2: analyze new way. It must be existing directory
        if (destId<0){
            GlobalLog.writeErr(METHNAME, "Incorrect destination way "+newWay);
            return -1;
        }
        Directory dest = readDir(destId, newWay);   // STEP 3: read directory content
        if (dest == null){
            GlobalLog.writeErr(METHNAME, "can't open dest directory by hard link, link id = "+destId+", way ="+newWay);
            return -1;
        }
        // STEP 4: get set of names from dir and check matches
        int newId = HLlist.searchByNameInSet(dest.getOwnedFiles(), newName);
        if (newId != -1){
            GlobalLog.writeErr(METHNAME, "file with name "+newName+" is already exist in destination directory, break operation");
            return -1;
        }
        // STEP 5: create new link and change refcount
        newId = HLlist.link(linkId, newName);
        driver.link(HLlist.getDescId(linkId));
        dest.addFile(newId);        // STEP 6: add new ID to dest dir
        writeDir(destId, dest);     // STEP 7: save dir to device
        return newId;
    }

    /**
     * create new name to file, uses descriptor
     * @param fileDescriptor: high layer's descriptor
     * @param newWay: new location of file (existing directory)
     * @param newName: new name of file
     * @return linkId if success, else return -1
     */
    public int link(int fileDescriptor, String newWay, String newName){
        final String METHNAME = CLASSNAME+".link (by fileDesc)";

        int descId = -1;             // STEP 1: check descriptor, get real descriptor
        if (descriptors.containsKey(fileDescriptor)) descId = descriptors.get(fileDescriptor);
        else{
            GlobalLog.writeErr(METHNAME, "Descriptor not exist, file not opened, fd = "+fileDescriptor);
            return -1;
        }
        int destId = search(newWay);  // STEP 2: analyze new way. It must be existing directory
        if (destId<0){
            GlobalLog.writeErr(METHNAME, "Incorrect destination way "+newWay);
            return -1;
        }
        Directory dest = readDir(destId, newWay);   // STEP 3: read directory content
        if (dest == null){
            GlobalLog.writeErr(METHNAME, "can't open dest directory by hard link, link id = "+destId+", way ="+newWay);
            return -1;
        }
        // STEP 4: get set of names from dir and check matches
        int newId = HLlist.searchByNameInSet(dest.getOwnedFiles(), newName);
        if (newId != -1){
            GlobalLog.writeErr(METHNAME, "file with name "+newName+" is already exist in destination directory, break operation");
            return -1;
        }
        // STEP 5: create new link and change refcount
        newId = HLlist.createLink(descId, newName);
        driver.link(descId);
        dest.addFile(newId);        // STEP 6: add new ID to dest dir
        writeDir(destId, dest);     // STEP 7: save dir to device
        return newId;
    }

    /**
     * remove link to file
     * @param way: location of file (existing directory)
     * @param name: name of file
     * @return 0 if successful or -1 if error
     */
    public int unlink(String way, String name){
        final String METHNAME = CLASSNAME+".unlink";

        int dirId = search(way);                // STEP 1: get existing directory id
        if (dirId<0){
            GlobalLog.writeErr(METHNAME, "can't open file location by way "+way);
            return -1;
        }
        Directory dir = readDir(dirId, way);     // STEP 2: read directory content
        if (dir == null){
            GlobalLog.writeErr(METHNAME, "can't open dest directory by hard link, link id = "+dirId+", way ="+way);
            return -1;
        }
        //STEP 3: get set of names from dir and check matches
        int linkId = HLlist.searchByNameInSet(dir.getOwnedFiles(), name);
        if (linkId == -1){
            GlobalLog.writeErr(METHNAME, "file with name "+name+" not exist in directory, break operation");
            return -1;
        }
        //STEP 4: remove hard link and change refcount
        HLlist.unlink(linkId);
        driver.unlink(HLlist.getDescId(linkId));
        dir.removeFile(linkId);     // STEP 5: remove ID from dest dir
        writeDir(dirId, dir);       // STEP 6: save dir to device
        return 0;
    }

    /**
     * change name of file without moving and other operations
     * @param way: location of file (existing directory)
     * @param name: old name
     * @param newName: new name
     * @return 0 if success
     */
    public int rename(String way, String name, String newName){
        final String METHNAME = CLASSNAME+".rename";
        int dirId = search(way);                    // STEP 1: get existing directory id
        if (dirId<0){
            GlobalLog.writeErr(METHNAME, "can't open file location by way "+way);
            return -1;
        }
        Directory dir = readDir(dirId, way);        // STEP 2: read directory content
        if (dir == null){
            GlobalLog.writeErr(METHNAME, "can't open dest directory by hard link, link id = "+dirId+", way ="+way);
            return -1;
        }
        //STEP 3: get set of names from dir and check matches
        int linkId = HLlist.searchByNameInSet(dir.getOwnedFiles(), name);
        if (linkId != -1){
            GlobalLog.writeErr(METHNAME, "file with name "+name+" not exist in directory, break operation");
            return -1;
        }
        if (HLlist.searchByNameInSet(dir.getOwnedFiles(), newName) != -1){
            GlobalLog.writeErr(METHNAME, "file with name "+newName+" is already exist in directory, break operation");
            return -1;
        }
        //STEP 4: rename hard link
        HLlist.rename(linkId, newName);
        return 0;
    }

    /**
     * move file to new dir and rename it (if it necessary)
     * @param way: location of file (existing directory)
     * @param name: file name
     * @param newWay: new location of file (existing directory)
     * @param newName: new file name
     * @return 0 if success
     */
    public int moveAs(String way, String name, String newWay, String newName){
        final String METHNAME = CLASSNAME+".moveAs";
        // STEP 1: get existing directories ids
        int srcId = search(way), destId = search(newWay);
        if (srcId<0){
            GlobalLog.writeErr(METHNAME, "can't open directory by way "+way);
            return -1;
        }
        if (destId<0){
            GlobalLog.writeErr(METHNAME, "can't open directory by way "+newWay);
            return -1;
        }
        // STEP 2: get existing directories content
        Directory src = readDir(srcId, way), dest = readDir(destId, newWay);
        //STEP 3: get set of names from dir and check matches
        int linkId = HLlist.searchByNameInSet(src.getOwnedFiles(), name);
        if (linkId == -1){
            GlobalLog.writeErr(METHNAME, "file with name "+name+" not exist in source directory, break operation");
            return -1;
        }
        if (HLlist.searchByNameInSet(dest.getOwnedFiles(), newName) != -1){
            GlobalLog.writeErr(METHNAME, "file with name "+newName+" is already exist in destination directory, break operation");
            return -1;
        }
        // STEP 4: remove link id from src and add it to dest
        dest.addFile(linkId);
        src.removeFile(linkId);
        // STEP 5: rename file
        HLlist.rename(linkId, newName);
        // STEP 6: save directories to device
        writeDir(srcId, src);
        writeDir(destId, dest);
        return 0;
    }

    public int move(String way, String name, String newWay){
        return moveAs(way, name, newWay, name);
    }


    //----- working with regular files -------------------
    //... by way
    public boolean create(String way, String name){
        final String METHNAME = CLASSNAME+".create";
        int descId = createNew(FileType._regular, way, name, null);
        return descId != -1;
    }

    /**
     * open file, return it's descriptor (high, not real)
     * @param way: location of existing file with file name at last
     * @return fd if success else return -1
     */
    public int open(String way){
        final String METHNAME = CLASSNAME+".open";
        int linkId = search(way);
        if (linkId<0){
            GlobalLog.writeErr(METHNAME, "Can't open file by way "+ way);
            return -1;
        }
        int descId = HLlist.getDescId(linkId);
        if (descId == -1){
            GlobalLog.writeErr(METHNAME, "File with id \""+linkId+"\" not exist, break operation");
            return -1;
        }
        FileType ft = driver.getFiletype(descId);
        if ((ft == null)||(ft != FileType._regular)){
            GlobalLog.writeErr(METHNAME, "File with id \""+linkId+"\" isn't regular and can't be opened, break operation");
            return -1;
        }
        return createDesc(descId);
    }

    int getFSDescriptor(int fileDescriptor){
        if (descriptors.containsKey(fileDescriptor)) return descriptors.get(fileDescriptor);
        else return -1;
    }

    //... by file descriptor
    public boolean write(int fileDescriptor, StringBuilder data){
        int descId = getFSDescriptor(fileDescriptor);
        if (descId != -1) return driver.writeFile(descId, data.length(), data);
        else return false;
    }

    public StringBuilder read(int fileDescriptor, int offset, int length){
        int descId = getFSDescriptor(fileDescriptor);
        if (descId != -1) return driver.readFile(descId, offset, length);
        else return new StringBuilder();
    }

    public boolean append(int fileDescriptor, StringBuilder newData){
        int descId = getFSDescriptor(fileDescriptor);
        if (descId != -1){
            StringBuilder sb = driver.readFile(descId, 0, Integer.MAX_VALUE);
            sb.append(newData);
            return driver.writeFile(descId, sb.length(), sb);
        }else return false;
    }

    public boolean truncate(int fileDescriptor, int newSize){
        int descId = getFSDescriptor(fileDescriptor);
        if (descId != -1) return driver.resizeFile(descId, newSize);
        else return false;
    }

    public void close(int fileDescriptor){
        deleteDesc(fileDescriptor);
    }

    //----- working with directories -------------------

    public boolean createDir(String way, String name){
        return createNew(FileType._directory, way, name, null) != -1;
    }

    public int setCurrentDir(String way, String name){
        final String METHNAME = CLASSNAME+".setCurrentDir";

        int dirId = search(way);                // STEP 1: get parent directory id
        if (dirId<0){
            GlobalLog.writeErr(METHNAME, "can't open file location by way "+way);
            return -1;
        }
        Directory dir = readDir(dirId, way);     // STEP 2: read directory content
        if (dir == null){
            GlobalLog.writeErr(METHNAME, "can't open directory by hard link, link id = "+dirId+", way ="+way);
            return -1;
        }
        //STEP 3: get set of names from dir and check matches
        int linkId = HLlist.searchByNameInSet(dir.getOwnedFiles(), name);
        if (linkId == -1){
            GlobalLog.writeErr(METHNAME, "Directory with name "+name+" not exist in directory, break operation");
            return -1;
        }
        //STEP 4: read path file
        Directory it = readDir(linkId, way);
        if (it == null){
            GlobalLog.writeErr(METHNAME, "can't open directory by hard link, link id = "+linkId+", way ="+way+FSPLIT+name);
            return -1;
        }
        it.name = name;
        PATH = it;
        return 0;
    }

   /* public int setCurrentDir(String way){
        final String METHNAME = CLASSNAME+".setCurrentDir";

        int dirId = search(way);                // STEP 1: get parent directory id
        if (dirId<0){
            GlobalLog.writeErr(METHNAME, "can't open file location by way "+way);
            return -1;
        }
        Directory dir = readDir(dirId, way);     // STEP 2: read directory content
        if (dir == null){
            GlobalLog.writeErr(METHNAME, "can't open directory by hard link, link id = "+dirId+", way ="+way);
            return -1;
        }
        String[] spl = way.split(FSPLIT);
        StringBuilder sb = new StringBuilder(spl[0]);
        for (int i=1; i<spl.length-1; i++) sb.append(FSPLIT).append(spl[i]);

    }*/

    //----- working with symlinks ----------------------

    public boolean checkWay(String str){
        return search(str) != -1;
    }

    public int createSymlink(String way, String name, StringBuilder content){
        final String METHNAME = CLASSNAME+".createSymlink";
        createNew(FileType._symlink, way, name, content);
        return 1;
    }

    //----- working with other layers -------------------

    public void saveHardLinks(){
        StringBuilder sb = HLlist.toStr();
        driver.writeLinks(sb);
    }

    public void loadHardLinks(){
        StringBuilder sb = driver.getLinks();
        HLlist.setList(HardLinksList.fromString(sb.toString()));
    }

    public StringBuilder toPrintHL(){
        return HLlist.toPrint();
    }

    public StringBuilder getFDList(){
        StringBuilder sb = new StringBuilder();
        Object[] descs = descriptors.keySet().toArray();
        for (Object key:descs)
            sb.append("fd = ").append(key).append(", FS descriptor = ").append(descriptors.get(key)).append('\n');
        return sb;
    }

    public void saveAll(java.io.File file){
        saveHardLinks();
        driver.saveAll(file);
    }

    public void loadAll(java.io.File file){
        driver.loadAll(file);
        loadHardLinks();
        updSysDirs();
    }

    public void mountNew(){
        PATH = ROOT;
        descriptors.clear();
    }

    public void updSysDirs(){
        Directory rt = readDir(ROOT.getLinkId(), ROOT.getWay()),
                pt = readDir(PATH.getLinkId(), PATH.getWay());
        if (rt != null) ROOT = rt;
        if (pt != null) PATH = pt;
    }

    //----- non-public methods ---------------------------

    int createNew(FileType ft, String way, String name, StringBuilder content){
        final String METHNAME = CLASSNAME+".createNew";

        //STEP 1: get directory id
        int dirId = search(way);
        if (dirId<0){
            GlobalLog.writeErr(METHNAME, "can't open file location by way "+way);
            return -1;
        }
        Directory dir = readDir(dirId, way);        // STEP 2: read directory content
        if (dir == null){
            GlobalLog.writeErr(METHNAME, "can't open dest directory by hard link, link id = "+dirId+", way ="+way);
            return -1;
        }
        //STEP 3: get set of names from dir and check matches
        int linkId = HLlist.searchByNameInSet(dir.getOwnedFiles(), name);
        if (linkId != -1){
            GlobalLog.writeErr(METHNAME, "file with name "+name+" already exist in directory, break operation");
            return -1;
        }
        int descID = driver.createFile(ft);         //STEP 4: create file on device
        if (content!=null) driver.writeFile(descID, content.length(), content);
        linkId = HLlist.createLink(descID, name);   //STEP 5: create link to file
        dir.addFile(linkId);                        //STEP 6: add file on directory
        writeDir(dirId, dir);                       //STEP 7: save directory
        return descID;                              //return descriptor
    }

    /**
     * search linkId of file
     * @param way: way to file
     * @return linkId
     * Algorithm:
     *      1) split string by '/'
     *      2) analyze way before first '/'
     *          if first symbol is / or ' ' - absolutely way, begins from ROOT directory
     *          if '.' or ".." - relatively way.
     *              If '.' - begins from PATH directory
     *              If ".." - append way of PATH in beginning of searched way and call this method recursive
     *              If others - return error and -3
     *      3) in cycle:
     *          * search current substring in current opened directory
     *          * if not exist - return -1;
     *          * if exist:
     *              + if there are no next substring - return it's ID
     *              else:
     *                  - if it is directory - open, read it content, save it as current dir and continue
     *                  - if it is regular file - return error and -2
     *                  - if it is symlink file: read content, append "tail" of way way to it and call this method recursively for new way
     */
    int search(String way){
        final int ERR_NOT_EXIST = -1, ERR_REGFILE_ISNT_LAST = -2, ERR_INCORRECT_WAY = -3,
                ERR_EMPTYDIR = -4, ERR_DRIVER = -5, ERR_INCORRECT_FT = -6;
        final String METHNAME = CLASSNAME+".search";

        GlobalLog.writeStep(METHNAME, "Search file by way "+way);
        String[] substr = way.split(FSPLIT);

        Directory currentDir = ROOT;
        StringBuilder sbway = new StringBuilder();

        if (substr.length == 0) return rootId;

        switch (substr[0]){
            case "":
            case " ":{
                GlobalLog.writeStep(METHNAME, "Search by absolutely way");
                sbway.append("");
                break;
            }
            case ".":{
                GlobalLog.writeStep(METHNAME, "Search by relatively way");
                sbway.append(PATH.way).append(FSPLIT).append(PATH.name);
                currentDir = PATH;
                break;
            }
            case "..":{
                GlobalLog.writeStep(METHNAME, "Append way of PATH parent to this and call recursive");
                StringBuilder trueWay = new StringBuilder(PATH.getWay());
                for (int i=1; i<substr.length; i++){trueWay.append(FSPLIT).append(substr[i]);}
                return search(trueWay.toString());
            }
            default:{
                GlobalLog.writeErr(METHNAME, "Incorrect way: way can't begins from \""+substr[0]+"\", break operation");
                return ERR_INCORRECT_WAY;
            }
        }


        int last = substr.length - 1;
        GlobalLog.writeStep(METHNAME, "last = "+last);
        for (int i=1; i<substr.length; i++){
            GlobalLog.writeStep(METHNAME, "i = "+i);

            sbway.append(FSPLIT).append(substr[i]);

            String name = substr[i];
            GlobalLog.writeStep(METHNAME, "Analyze file "+name);

            int[] set = currentDir.getOwnedFiles();
            if (set == null){
                GlobalLog.writeErr(METHNAME, "Directory \""+currentDir.name+"\" is empty, but isn't last in way, break operation");
                return ERR_EMPTYDIR;
            }
            GlobalLog.writeStep(METHNAME, "Parent dir has files with ids: "+ Arrays.toString(set));

            int linkId = HLlist.searchByNameInSet(set, name);
            if (linkId == -1){
                GlobalLog.writeErr(METHNAME, "File \""+name+"\" not exist, break operation");
                return ERR_NOT_EXIST;
            }
            GlobalLog.writeStep(METHNAME, "Searching on dir returns link id "+linkId);

            if (i >= last) return linkId;   // return id of hard link if file successfully found

            int descId = HLlist.getDescId(linkId);
            if (descId == -1){
                GlobalLog.writeErr(METHNAME, "Dir or symlink with name \""+name+"\" not exist, break operation");
                return ERR_NOT_EXIST;
            }
            GlobalLog.writeStep(METHNAME, "From links lins takes FSDescriptor id = "+descId);

            FileType ft = driver.getFiletype(descId);
            if (ft == null){
                GlobalLog.writeErr(METHNAME, "Dir or symlink with name \""+name+"\" hasn't descriptor but has name, fatal error");
                return ERR_DRIVER;
            }
            GlobalLog.writeStep(METHNAME, "This descriptor descripe file with type: "+ft);

            switch (ft){
                case _regular:{
                    GlobalLog.writeErr(METHNAME, "Regular file \""+name+"\" isn't last in way, break operation");
                    return ERR_REGFILE_ISNT_LAST;
                }
                case _directory:{
                    GlobalLog.writeStep(METHNAME, "Analyze as directory: ");
                    StringBuilder data = driver.readFile(descId, 0, Integer.MAX_VALUE);
                    currentDir = new Directory(linkId, data, sbway.toString());
                    continue;
                }
                case _symlink:{
                    GlobalLog.writeStep(METHNAME, "Analyze as symlink: ");
                    StringBuilder data = driver.readFile(descId, 0, Integer.MAX_VALUE);
                    for (int j=i+1; j<substr.length; j++) data.append(FSPLIT).append(substr[j]);
                    return search(data.toString());
                }
                default:{
                    GlobalLog.writeErr(METHNAME, "Incorrect file type \""+ft.toString()+"\", break operation");
                    return ERR_INCORRECT_FT;
                }
            }
        }
        return currentDir.getLinkId();
    }

    //----- open/close descriptors ---------------------------
    int MAXDESC = 0;

    int createDesc(int FSDescId){
        for (int i=0; i<MAXDESC; i++){
            if (!descriptors.containsKey(i)){
                descriptors.put(i, FSDescId);
                return i;
            }
        }
        int key = MAXDESC++;
        descriptors.put(key, FSDescId);
        return key;
    }

    void deleteDesc(int FileDesc){
        descriptors.remove(FileDesc);
        reduceDescriptorIDCTR();
    }

    void reduceDescriptorIDCTR(){
        int max = -1;
        for (int i=0; i<MAXDESC; i++){
            if (descriptors.containsKey(i)) max = i;
        }
        MAXDESC = max+1;
    }

    //----- read and write directories

    /**
     * read directory content from device
     * @param linkId: used to identifying of directory
     * @param way: additional parameter, used as field to new dir
     * @return object or null if error
     */
    Directory readDir(int linkId, String way){
        final String METHNAME = CLASSNAME+".readDir";

        int descId = HLlist.getDescId(linkId);
        if (descId == -1){
            GlobalLog.writeErr(METHNAME, "Dir with id \""+linkId+"\" not exist, break operation");
            return null;
        }
        FileType ft = driver.getFiletype(descId);
        if ((ft == null)||(ft != FileType._directory)){
            GlobalLog.writeErr(METHNAME, "File with id \""+linkId+"\" isn't directory, break operation");
            return null;
        }
        StringBuilder data = driver.readFile(descId, 0, Integer.MAX_VALUE);
        return new Directory(linkId, data, way);
    }

    /**
     * write directory content to device
     * @param linkId: used to identifying of directory
     * @param dir: object, that must ne write
     * @return true if success or false if error (read log to concreting error type)
     */
    boolean writeDir(int linkId, Directory dir){
        final String METHNAME = CLASSNAME+".writeDir";

        int descId = HLlist.getDescId(linkId);
        if (descId == -1){
            GlobalLog.writeErr(METHNAME, "Dir with id \""+linkId+"\" not exist, break operation");
            return false;
        }
        FileType ft = driver.getFiletype(descId);
        if ((ft == null)||(ft != FileType._directory)){
            GlobalLog.writeErr(METHNAME, "File with id \""+linkId+"\" isn't directory, break operation");
            return false;
        }
        StringBuilder data = dir.toStr();
        return driver.writeFile(descId, data.length(), data);
    }
}
