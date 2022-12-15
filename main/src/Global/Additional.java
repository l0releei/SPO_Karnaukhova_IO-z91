package Global;

/**
 * Created by LENOVO on 30.11.2018.
 */
public class Additional {
    public static int[] concat(int[] a, int[] b) {
        int aLen = a.length;
        int bLen = b.length;
        int[] c= new int[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
    
    public static int hexToInt(char c){
        if (Character.isDigit(c)) return Character.getNumericValue(c);
        switch (c){
            case 'A': return 10;
            case 'B': return 11;
            case 'C': return 12;
            case 'D': return 13;
            case 'E': return 14;
            case 'F': return 15;
            default: return 0;        
        }
    }   
    
    public static char intToHex(int x){
        switch (x){
            case 0:{ return '0';}
            case 1:{ return '1';}
            case 2:{ return '2';}
            case 3:{ return '3';}
            case 4:{ return '4';}
            case 5:{ return '5';}
            case 6:{ return '6';}
            case 7:{ return '7';}
            case 8:{ return '8';}
            case 9:{ return '9';}
            case 10:{ return 'A';}
            case 11:{ return 'B';}
            case 12:{ return 'C';}
            case 13:{ return 'D';}
            case 14:{ return 'E';}
            case 15:{ return 'F';}
        }
        return '0';
    }
    
    public static String toHex(boolean[] arr){
        StringBuilder str = new StringBuilder();
        if (arr.length % 4 == 0){
            for (int off = 0; off < arr.length; off+=4){
                int x = 0;
                x = arr[off] ? x+8 : x;
                x = arr[off+1] ? x+4 : x;
                x = arr[off+2] ? x+2 : x;
                x = arr[off+3] ? x+1 : x;
                str.append(intToHex(x));
            }
        }
        return str.toString();
    }
    
    public static boolean[] fromHex(String str){
        boolean[] arr = new boolean[4*str.length()];
        int off = 0;
        for (char c:str.toCharArray()){
            int x = hexToInt(c);
            arr[off] = x>8 ? true : false;
            arr[off+1] = x>4 ? true : false;
            arr[off+2] = x>2 ? true : false;
            arr[off+3] = x>1 ? true : false;
            off+=4;
        }
        return arr;
    }
}
