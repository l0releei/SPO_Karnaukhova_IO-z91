package High_Layer;

import Global.Consts;
import Global.GlobalLog;
import Low_Layer.FileType;

/**
 * Created by LENOVO on 09.12.2018.
 */
public class Directory extends File {
    static final String CLASSNAME = "Directory";
    static final String SPLIT = Consts.SPLIT_STR;
    static FileType type = FileType._directory;
    int[] ownedFiles;

    public Directory(int linkId, StringBuilder content, String way) {
        super(linkId, content, way);
        fromStr(content);
    }

    public StringBuilder toStr(){
        StringBuilder content =  new StringBuilder("");
        if (ownedFiles != null){
            for (int i=0; i<ownedFiles.length-1; i++) content.append(ownedFiles[i]).append(SPLIT);
            content.append(ownedFiles[ownedFiles.length-1]);
        }
        return content;
    }

    public void fromStr(StringBuilder str){
        if ((str == null)||(str.length() == 0)){
            ownedFiles = null;
            return;
        }
        String[] ids = str.toString().split(SPLIT);
        int[] files = new int[ids.length];
        try{
            for (int i=0; i<ids.length; i++) files[i] = Integer.valueOf(ids[i]);
            ownedFiles = files;
        }catch (NumberFormatException e){
            GlobalLog.Llayer_writeErr(CLASSNAME+".fromStr", "Invalid id format");
        }
    }

    public int[] getOwnedFiles() {
        return ownedFiles;
    }

    public int indexOf(int linkId){
        if (ownedFiles == null) return -1;
        for (int i=0; i<ownedFiles.length; i++) if (ownedFiles[i] == linkId) return i;
        return -1;
    }

    public void addFile(int linkId){
        if (indexOf(linkId)!=-1) return;
        if (ownedFiles == null){
            ownedFiles = new int[1];
            ownedFiles[0] = linkId;
            return;
        }
        int[] files = new int[ownedFiles.length+1];
        System.arraycopy(ownedFiles, 0, files, 0, ownedFiles.length);
        files[ownedFiles.length] = linkId;
        ownedFiles = files;
    }

    public void removeFile(int linkId){
        int i = indexOf(linkId);
        if (i==-1) return;
        if (ownedFiles == null) return;
        int[] files = new int[ownedFiles.length-1];
        System.arraycopy(ownedFiles, 0, files, 0, i);
        System.arraycopy(ownedFiles, i+1, files, i, (ownedFiles.length-i-1));
        ownedFiles = files;
    }
}
