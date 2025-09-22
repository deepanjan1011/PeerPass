package p2p.utils;

import java.util.Random;
public class UploadUtils {

    public static int generateCode(){
        int DYNAMIC_STARTING_PORT = 49152;
        int DYNAMIC_ENDING_PORT = 65535; // highest valid port

        Random random = new Random();
        int range = (DYNAMIC_ENDING_PORT - DYNAMIC_STARTING_PORT) + 1;
        return DYNAMIC_STARTING_PORT + random.nextInt(range);
    }
}
