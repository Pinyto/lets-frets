package de.tudarmstadt.tk.smartguitarcontrol.utility;

public class utils {


    public static int[] generateRange(int start, int end){
        int[] result = new int[end-start];

        for(int i=0;i<end-start;i++) {
            result[i] = start + i;
        }
        return result;
    }
}
