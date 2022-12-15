package High_Layer;

import Low_Layer.FileType;

/**
 * Created by LENOVO on 09.12.2018.
 */
public class File {
    static FileType type;

    int linkId;
    StringBuilder content;
    String way, name;

    public File(int linkId, StringBuilder content, String way) {
        this.linkId = linkId;
        this.content = content;
        this.way = way;
    }

    public FileType getType() {
        return type;
    }

    public int getLinkId() {
        return linkId;
    }

    public String getWay() {
        return way;
    }
}
