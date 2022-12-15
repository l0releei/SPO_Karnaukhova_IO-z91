package IO;

import Global.GlobalLog;
import Low_Layer.Descriptor;
import Low_Layer.FSDescriptorsTable;
import Low_Layer.FSDriver;
import Low_Layer.FileType;
import High_Layer.HardLink;
import Physical.DeviceDriver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by LENOVO on 28.11.2018.
 */
public class ConsoleTests {

    public static void testPhysical(DeviceDriver driver){
        Scanner sc = new Scanner(System.in);
        int c = 1;
        while (true){
            System.out.println("COMMANDS:");
            System.out.println("0 - exit, 1 - write to sector(s), 2 - read sector(s), 3 - check state of sector(s),\n " +
                    "4 - create new file, 5 - print log");
            c = sc.nextInt();
            if (c==0) return;
            int[] sects = null;
            if ((c!=4) && (c!=5)){
                System.out.println("Put count of sectors:");
                int cnt  = sc.nextInt();
                System.out.println("Put sectors numbers:");
                sects = new int[cnt];
                for (int i=0; i<cnt; i++){
                    sects[i] = sc.nextInt();
                }
            }
            switch (c){
                case 1: {
                    System.out.println("Put string:");
                    String x = sc.next();
                    driver.rewrite(sects, new StringBuilder(x));
                    break;
                }
                case 2:{
                    String x = driver.read(sects).toString();
                    System.out.println(x); break;
                }
                case 3:{
                    StringBuilder s = new StringBuilder();
                    for (int i=0; i<sects.length;i++){
                        s.append(sects[i]).append(" : ").append(driver.getState(sects[i])).append("\n");
                    }
                    System.out.println(s); break;
                }
                case 4:{
                    System.out.println("Put string:");
                    String x = sc.next();
                    sects = driver.create(new StringBuilder(x));
                    StringBuilder s = new StringBuilder();
                    for (int i=0; i<sects.length;i++) s.append(sects[i]).append(" ");
                    System.out.println(s); break;
                }
                case 5:{
                    System.out.println(GlobalLog.PhysicalLog.toString());
                }
            }
        }
    }

    public static void testHardLink_fromString(){
        HardLink h = new HardLink(1, 1, "labwork2");
        System.out.println("h: tid="+h.getTargetID()+" , name="+h.getName());
        String[] s = {"@25_seregalaba#", "@64_sdfs#", "@1_nothing"};
        for (String c:s){
            h = HardLink.fromString(c);
            System.out.println("h: tid="+h.getTargetID()+" , name="+h.getName());
        }
    }

    public static void testDescriptors_String(){
        Descriptor d1 = new Descriptor(0, FileType._regular);
        Descriptor d2 = new Descriptor(0, FileType._descriptorTable);

        final String[] strings1 = {
                "T_10_12_14_15", "T_0_1_5_3", "H_4_2_8", "R_12_88_3_34_35_36_2"
        };
        final String[] strings2 = {
                "D_10_12_14_15", "D_0_1_5_3", "H_4_2_34", "D_0_32_3_34_35_36_2_7_9_10"
        };
        System.out.println("first state:");
        System.out.println("d1 = "+d1);
        System.out.println("d2 = "+d2);
        for (int i=0; i<strings1.length; i++){
            d1 = Descriptor.fromString(strings1[i]);
            d2 = Descriptor.fromString(strings2[i]);
            System.out.println("Step "+i+":");
            System.out.println("d1 = "+d1);
            System.out.println("\ttype = "+d1.getType()+", \n\tid = "+d1.getId()+", " +
                    "\n\tsize = "+d1.getSize()+", \n\trefcount = "+d1.getRefcount()+", \n\tsectors = "+ Arrays.toString(d1.getSectors()));
            System.out.println("d2 = "+d2);
            System.out.println("\ttype = "+d2.getType()+", \n\tid = "+d2.getId()+", " +
                    "\n\tsize = "+d2.getSize()+", \n\trefcount = "+d2.getRefcount()+", \n\tsectors = "+ Arrays.toString(d2.getSectors()));
        }
    }

    public static void testDescTable_Prim_String(){
        Scanner sc = new Scanner(System.in);
        FSDescriptorsTable tb = new FSDescriptorsTable();
        while (true){
            System.out.println("Put string:");
            String x = sc.next();
           // tb.primaryFromString(x);
           // System.out.println(Arrays.toString(tb.getPrimaryPtr()));
           // System.out.println(tb.primaryToString());
        }
    }

    public static void testLowAndPhysical(){
        Scanner sc = new Scanner(System.in);
        DeviceDriver dd = new DeviceDriver();
        FSDriver driver = new FSDriver(dd);
        while (true){
            System.out.println("Put command:");
            /*
                exit -      exit
                create -    create new file
                write -     rewrite file
                read -      read file
                trunc -     truncate file
                reduce -    reduce table's IDs
                link -      link file (ctr++)
                unlink -    unlink file (ctr-- if > 0)
                save -      save table to device
                load -      load table from device
                rdsect -    read sector
                wrsec -     write sector
                getstate -  sector state
                setbad -    set state of sector (0 - good, 1 - bad)
                table -     print table
                log -       print log
             */
            String cmd = sc.next();
            switch (cmd) {
                case "exit":{
                    GlobalLog.writeEvent("exit:");
                    return;
                }
                case "create": {
                    GlobalLog.writeEvent("create:");
                    System.out.println("file descriptor id: " + driver.createFile(FileType._regular));
                    break;
                }
                case "write": {
                    System.out.println("Put id");
                    int id = sc.nextInt();
                    System.out.println("Put data");
                    String str = sc.next();
                    GlobalLog.writeEvent("write "+id);
                    GlobalLog.Llayer_writeStep("Main", "data="+str);
                    System.out.println("Result: " +
                            driver.writeFile(id, str.length(), new StringBuilder(str)));
                    break;
                }
                case "read": {
                    System.out.println("Put id");
                    int id = sc.nextInt();
                    System.out.println("Put offset");
                    int off = sc.nextInt();
                    System.out.println("Put length");
                    int len = sc.nextInt();
                    GlobalLog.writeEvent("read "+id+" "+off+" "+len);
                    System.out.println("Result: \n" +
                            driver.readFile(id, off, len));
                    break;
                }
                case "trunc": {
                    System.out.println("Put id");
                    int id = sc.nextInt();
                    System.out.println("Put new size");
                    int len = sc.nextInt();
                    GlobalLog.writeEvent("trunc "+id+" "+len);
                    System.out.println("Result: " +
                            driver.resizeFile(id, len));
                    break;
                }
                case "reduce": {
                    GlobalLog.writeEvent("reduce");
                    HashMap<Integer, Integer> hm = driver.reduceTable();
                    StringBuilder sb = new StringBuilder("ID equations map:\n");
                    for (int i=-1; i<hm.size()-1; i++){
                        sb.append(hm.get(i)).append(" => ").append(i).append('\n');
                    }
                    System.out.println(sb);
                    break;
                }
                case "link": {
                    System.out.println("Put id");
                    int id = sc.nextInt();
                    GlobalLog.writeEvent("link "+id);
                    driver.link(id);
                    System.out.println("Refcount: " + driver.getRefcount(id));
                    break;
                }
                case "unlink": {
                    System.out.println("Put id");
                    int id = sc.nextInt();
                    GlobalLog.writeEvent("unlink "+id);
                    driver.unlink(id);
                    System.out.println("Refcount: " + driver.getRefcount(id));
                    break;
                }
                case "save": {
                    GlobalLog.writeEvent("save");
                    driver.save();
                    break;
                }
                case "load": {
                    GlobalLog.writeEvent("load");
                    driver.load();
                    break;
                }
                case "rdsec": {
                    System.out.println("Put number");
                    int num = sc.nextInt();
                    GlobalLog.writeEvent("rdsec "+num);
                    System.out.println(dd.read(num));
                    break;
                }
                case "wrsec":{
                    System.out.println("Put number");
                    int num = sc.nextInt();
                    System.out.println("Put data");
                    String str = sc.next();
                    GlobalLog.writeEvent("wrsec "+num);
                    GlobalLog.Llayer_writeStep("Main", "data="+str);
                    dd.forcedRewrite(num, new StringBuilder(str), 0);
                    break;
                }
                case "getstate": {
                    System.out.println("Put number");
                    int num = sc.nextInt();
                    GlobalLog.writeEvent("getstate "+num);
                    System.out.println("State: "+dd.getState(num));
                    break;
                }
                case "setbad":{
                    System.out.println("Put number");
                    int num = sc.nextInt();
                    System.out.println("Put state (0 - good, others - bad)");
                    int stt = sc.nextInt();
                    GlobalLog.writeEvent("setbad "+num+" "+stt);
                    dd.setBad(num, stt);
                    break;
                }
                case "table":{
                    GlobalLog.writeEvent("table");
                    System.out.println(driver.toPrint());
                    break;
                }
                case "log": {
                    System.out.println(GlobalLog.PhysicalLog.toString());
                    break;
                }
                default:{
                    System.out.println("Unknown operation: " + cmd);
                }
            }
        }
    }
}
