package de.tudarmstadt.tk.smartguitarcontrol;

public interface Constants {
    int DB_VERSION = 3;

    int MESSAGE_TOAST = 0;
    int CONNECTED = 3;
    int DISCONNECTED = 5;
    int SUCCESS = 6;
    int LED_DONE = 7;
    int MESSAGE_DATA = 1;
    String TOAST = "toast";

    int GET_GRIP_ID = 100;
    int GET_GRIP_ID_TSG_FULL = 102;
    int GET_GRIP_ID_TSG_HALF = 103;
    int GET_GRIP_FOR_DELETE = 110;

    int FILE_SELECT_CODE = 130;
    int FILE_STORAGE_SELECT = 140;
    int PERM_FILE_STORAGE = 142;

    int COTEXT_MENU_DELETE_GRIP = 150;

    String CUSTOM_EXTRA_GRIP = "gripID";
    String CUSTOM_EXTRA_TSG_RESPONSE = "tsg_waitForResponse";
    String CUSTOM_EXTRA_TSG_MODE = "tsg_mode";
    String CUSTOM_MULTIPLE_GRIPS = "multiGripIDs";

    String CUSTOM_EXPORT_ALL = "flag_for_export";
}
