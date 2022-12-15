package Global;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class GlobalLog {
    public static Log PhysicalLog = new Log();

    public static void writeEvent(String data){PhysicalLog.writeEvent(data);}
    public static void writeStep(String caller, String data){PhysicalLog.writeStep(caller, data);}
    public static void writeErr(String caller, String data){PhysicalLog.writeErr(caller, data);}


    public static void Llayer_writeStep(String caller, String data){PhysicalLog.writeStep(caller, data);}
    public static void Llayer_writeErr(String caller, String data){PhysicalLog.writeErr(caller, data);}
}
