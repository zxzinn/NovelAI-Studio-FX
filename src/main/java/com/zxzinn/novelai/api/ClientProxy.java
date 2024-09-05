package com.zxzinn.novelai.api;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Log4j2
@Getter
public enum ClientProxy {
    NONE(null, 0),
    CLASH("127.0.0.1", 7890),
    SYSTEM_PROXY(System.getProperty("http.proxyHost"), Integer.getInteger("http.proxyPort", 0));

    private final String host;
    private final int port;

    ClientProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isAvailable() {
        if (this == NONE) return true;
        if (host == null || port == 0) return false;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Optional<ClientProxy> detectProxy() {
        return Arrays.stream(ClientProxy.values())
                .filter(proxy -> proxy != NONE)
                .filter(ClientProxy::isAvailable)
                .findFirst();
    }
}