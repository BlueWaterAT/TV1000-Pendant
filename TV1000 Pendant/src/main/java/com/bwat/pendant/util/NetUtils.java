package com.bwat.pendant.util;

/**
 * @author Kareem ElFaramawi
 */
public class NetUtils {
    public static final String IP_ADDRESS = "(\\d{1,3}\\.){3}\\d{1,3}";
    public static final String PORT = "\\d+";

    public static boolean isValidIPAddress(String ip) {
        return ip.matches(IP_ADDRESS);
    }

    public static boolean isValidPort(String port) {
        return port.matches(PORT);
    }
}
