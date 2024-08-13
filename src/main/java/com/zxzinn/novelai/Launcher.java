package com.zxzinn.novelai;

import static javafx.application.Application.launch;

public class Launcher {
    public static void main(String[] args) {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        Application.main(args);
    }
}