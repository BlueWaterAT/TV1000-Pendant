package com.bwat.pendant;

import android.os.Environment;

public class Constants {
    //Programming table format
    public final static String PROJECTS_DIR = Environment.getExternalStorageDirectory().getPath() + "/AGV Programmer";
    public final static String EXTENSION = ".jtb";
    public final static String PROGRAM_EXTENSION = ".prg";
    public final static String COMMA = ",";
    public final static String COMMENT = ";";

    //FTP related variables
    public final static int FTP_PORT = 22;
    public final static String FTP_USER = "root";
    public final static String FTP_PASS = "bwat1234";
    public final static String FTP_REMOTE_DIR = "/hmi/prg/";

    public final static int PROGRAM_DEFAULT = 1;
}
