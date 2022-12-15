import Physical.DeviceDriver;
import User.ConsoleInterface;

import java.util.Scanner;

/**
 * Created by LENOVO on 26.11.2018.
 */
public class Main {

    static DeviceDriver driver = new DeviceDriver();

    public static void main(String[] args) {
        //ConsoleTests.testPhysical(driver);
        //ConsoleTests.testDescriptors_String();
        //ConsoleTests.testDescTable_Prim_String();
        //ConsoleTests.testLowAndPhysical();
        ConsoleInterface ci = new ConsoleInterface();
    }
}
