package User;

import Global.GlobalLog;
import High_Layer.FileSystem;
import IO.MyIO;
import Low_Layer.FSDriver;
import Physical.DeviceDriver;

import java.io.File;
import java.util.Scanner;

/**
 * Created by LENOVO on 09.12.2018.
 */
public class ConsoleInterface {
    public static final String SPLITCMD = " ";
    Scanner scanner = new Scanner(System.in);

    public File currentFS = null;
    public DeviceDriver device;
    public FSDriver driver;
    public FileSystem fs;

    public ConsoleInterface() {
        process();
    }

    public void process(){
        boolean b;
        do {
            System.out.println("Put next command");
            b = exec(scanner.next());
        }while (!b);
    }

    public boolean exec(String command){
        boolean r = false;
        if (currentFS == null){
            switch (command){
                case "exit":{ r = true; break;}
                case "new":{
                    writeEventToLog(command);
                    currentFS = MyIO.selectFile(MyIO.getPath());
                    this.device = new DeviceDriver();
                    driver = new FSDriver(device);
                    fs = new FileSystem(driver);
                    saveAll(); break;
                }
                case "mount":{
                    writeEventToLog(command);
                    currentFS = MyIO.selectFile(MyIO.getPath());
                    this.device = new DeviceDriver();
                    driver = new FSDriver(device);
                    fs = new FileSystem(driver);
                    loadAll();
                    break;
                }
                default:
                    System.out.println("Icrorrect command. Use commands 'new' or 'mount' to creating new fs");
            }
            return r;
        }
       // loadAll();
        fs.updSysDirs();
        switch (command){
            case "exit":{ r = true; break;}
            case "unmount":{
                writeEventToLog(command);
                saveAll();
                driver = null; device = null; fs = null; currentFS = null;
                break;
            }
            case "create":{
                String way, name;
                way = askStr("Put location");
                name = askStr("Put name");
                writeEventToLog(command, way, name);
                System.out.println("Result: "+fs.create(way, name));
                break;
            }
            case "open":{
                String way;
                way = askStr("Put way with file's name at last");
                writeEventToLog(command, way);
                System.out.println("FD = "+fs.open(way));
                break;
            }
            case "write":{
                int fd = askInt("Put opened file descriptor");
                String str = askStr("Print data to write");
                writeEventToLog(command, String.valueOf(fd), str);
                System.out.println("Result: "+fs.write(fd, new StringBuilder(str)));
                break;
            }
            case "read":{
                int fd, off, len;
                fd = askInt("Put opened file descriptor");
                off = askInt("Put offset (0 - read from beginning)");
                len = askInt("Put length (-1 to read all)");
                if (len == -1) len = Integer.MAX_VALUE;
                writeEventToLog(command, String.valueOf(fd), String.valueOf(off), String.valueOf(len));
                System.out.println(fs.read(fd, off, len));
                break;
            }
            case "trunc":{
                int fd = askInt("Put opened file descriptor");
                int nsize = askInt("Put new size");
                writeEventToLog(command, String.valueOf(fd), String.valueOf(nsize));
                System.out.println("Result : "+ fs.truncate(fd, nsize));
                break;
            }
            case "close":{
                int fd = askInt("Put opened file descriptor");
                writeEventToLog(command, String.valueOf(fd));
                fs.close(fd);
                break;
            }
            case "link":{
                String fullWay = askStr("Put way with file's name at last"),
                        newWay = askStr("Put way to new file's name"),
                        newName = askStr("Put new name of file");
                writeEventToLog(command, fullWay, newWay, newName);
                System.out.println("Result = " + fs.link(fullWay, newWay, newName));
                break;
            }
            case "linkfd":{
                int fd = askInt("Put opened file descriptor");
                String newWay = askStr("Put way to new file's name"),
                        newName = askStr("Put new name of file");
                writeEventToLog(command, String.valueOf(fd), newWay, newName);
                System.out.println("Result = " + fs.link(fd, newWay, newName));
                break;
            }
            case "unlink":{
                String way = askStr("Put way to file"),
                        name = askStr("Put name of file");
                writeEventToLog(command, way, name);
                System.out.println("Result = " + fs.unlink(way, name));
                break;
            }
            case "rename":{
                String way = askStr("Put way to file"),
                        name = askStr("Put name of file"),
                        newName = askStr("Put new name of file");
                writeEventToLog(command, way, name, newName);
                System.out.println(fs.rename(way, name, newName));
                break;
            }
            case "move":{
                String way = askStr("Put way to file"),
                        name = askStr("Put name of file"),
                        newWay = askStr("Put new way to file"),
                        newName = askStr("Put new name of file");
                writeEventToLog(command, way, name, newWay, newName);
                System.out.println("Result : "+fs.moveAs(way, name, newWay, newName));
                break;
            }
            case "mkdir":{
                String way = askStr("Put way to new dir"),
                        name = askStr("Put name of new dir");
                writeEventToLog(command, way, name);
                System.out.println("Result: "+fs.createDir(way, name));
                break;
            }
            case "cd":{
                String way = askStr("Put way to dir"),
                        name = askStr("Put name of dir");
                writeEventToLog(command, way, name);
                System.out.println("Result: "+fs.setCurrentDir(way, name));
                break;
            }
            case "mksym":{
                String way = askStr("Put way to new symlink"),
                        name = askStr("Put name of symlink"),
                        cont = askStr("Put way to some file");
                writeEventToLog(command, way, name, cont);
                System.out.println("Result: "+fs.createSymlink(way, name, new StringBuilder(cont)));
                break;
            }
            case "list":{
                writeEventToLog(command);
                System.out.println(fs.toPrintHL());
                break;
            }
            case "desclist":{
                writeEventToLog(command);
                System.out.println(fs.getFDList());
                break;
            }
            case "table":{
                writeEventToLog(command);
                System.out.println(driver.toPrint());
                break;
            }
            case "blockstat":{
                int num = askInt("Put block's number");
                writeEventToLog(command, String.valueOf(num));
                System.out.println(num +" : "+ device.getState(num));
                break;
            }
            case "rdblock":{
                int num = askInt("Put block's number");
                writeEventToLog(command, String.valueOf(num));
                System.out.println("Result: "+device.read(num));
                break;
            }
            case "wrblock":{
                int num = askInt("Put block's number");
                String str = askStr("put data to write");
                writeEventToLog(command, String.valueOf(num), str);
                System.out.println("Result: "+device.forcedRewrite(num, new StringBuilder(str), 0));
                break;
            }
            case "log":{
                System.out.println(GlobalLog.PhysicalLog.toString());
                break;
            }
            default: System.out.println("Unknown command: "+command);
        }
        //saveAll();
        return r;
    }

    String askStr(String message){
        System.out.println(message);
        return scanner.next();
    }

    int askInt(String message){
        System.out.println(message);
        return scanner.nextInt();
    }

    void writeEventToLog(String command, String ... param){
        StringBuilder cmd = new StringBuilder(command);
        for (String p:param) cmd.append(SPLITCMD).append(p);
        GlobalLog.writeEvent(cmd.toString());
    }

    void saveAll(){
        fs.saveAll(currentFS);
    }

    void loadAll(){
        fs.loadAll(currentFS);
    }


}
