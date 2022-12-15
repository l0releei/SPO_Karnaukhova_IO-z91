package IO;

import Global.GlobalLog;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class MyIO {
    public final static int ERROR = -1, SUCCESS = 0, EX_IO = 1, EX_NULL = 2, EX_NOTEXIST = 3, EX_ISNOTFILE = 4;

    public static String PATH = new File(".").getAbsolutePath();

    public static String getPath(){
        return PATH;
    }

    public static void setPath(String newPath){ PATH = newPath;}

    public static File selectFile(String firstpath){
        JFileChooser chooser;
        if (firstpath==null) chooser = new JFileChooser();
        else chooser = new JFileChooser(firstpath);
        int ret = chooser.showDialog(null, "Выбрать файл");
        if (ret == JFileChooser.APPROVE_OPTION){
             return chooser.getSelectedFile();
        } else return null;
    }

    public static int checks(File file){
        if (file == null) return EX_NULL;
        if (file.exists()){
            //check type
            if (!file.isFile()) return EX_ISNOTFILE;
        }else{
            try{//create new file
                boolean ret = file.createNewFile();
                if (!ret) return ERROR;
            }catch (IOException e){return EX_IO;}
        }
        return SUCCESS;
    }

    public static int writeToFile(File file, String data){
        int errcode = checks(file);
        if (errcode!=SUCCESS) return errcode;
        try{ // write to file
            FileWriter fw = new FileWriter(file);
            String[] lines = data.split("\n");
            for (String line:lines) fw.write(line+"\n");
            fw.close();
            return SUCCESS;
        }catch (IOException e){
            return EX_IO;
        }
    }

    public static int appendToFile(File file, String data){
        int errcode = checks(file);
        if (errcode!=SUCCESS) return errcode;
        try{ // write to file
            FileWriter fw = new FileWriter(file);
            fw.append(data);
            fw.close();
            return SUCCESS;
        }catch (IOException e){
            return EX_IO;
        }
    }

    public static String readFromFile(File file){
        int errcode = checks(file);
        if (errcode!=SUCCESS) return null;
        try{ // write to file
            FileReader fr = new FileReader(file);
            Scanner sc = new Scanner(fr);
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()){
                sb.append(sc.nextLine());
            }
            return sb.toString();
        }catch (IOException e){
            return null;
        }
    }

}
