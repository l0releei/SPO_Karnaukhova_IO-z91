package Global;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class Log {
    int eventCtr = 1;
    int stepctr = 1;
    int errorctr = 1;
    StringBuilder data = new StringBuilder();



    public void writeEvent(String evdata){
        //int time = System.
        data.append("EVENT ").append(eventCtr).append(" : ").append(evdata).append("\n");
        stepctr = 1;
        eventCtr++;
    }

    public void writeStep(String caller, String str){
        data.append("\t").append(stepctr).append(": ").append(caller).append(" :").append(str).append("\n");
        stepctr++;
    }

    public void writeErr(String caller, String errdata){
        data.append("ERROR ").append(errorctr).append(" in method ").
                append(caller).append(" : ").append(errdata).append("\n");
        errorctr++;
    }

    public String toString(){
        return data.toString();
    }

}
