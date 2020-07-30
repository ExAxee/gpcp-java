package org.gpcp;

import com.grack.nanojson.JsonArray;

import org.gpcp.utils.BaseHandler;
import org.gpcp.utils.Command;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ServerTest {

    static class Handler extends BaseHandler {
        @Override
        public Object unknownCommand(String commandTrigger, JsonArray arguments) {
            return "Unknown command: " + commandTrigger;
        }

        @Override
        public String handleData(final String data) {
            System.out.println("Received data: " + data);
            return super.handleData(data);
        }

        @Command
        public Double pi() {
            return 3.14159;
        }

        @Command
        public Integer massimo(Integer a, Integer b) {
            return Math.max(a, b);
        }
    }

    @Test(timeout = 5100)
    public void testStartStopDelay() throws IOException, InterruptedException {
        final Server server = new Server(
                new BaseHandler.Factory<>(Handler.class, Handler::new), true);
        server.startServer("localhost", 8000);
        System.out.println("Server started...");

        for (int i = 0; i < 5; ++i) {
            System.out.println((5-i) + " seconds remaining before stopping");
            TimeUnit.SECONDS.sleep(1);
        }

        System.out.println("Stopping");
        server.stopServer();
    }

    @Test(timeout = 100)
    public void testStartStopFast() throws IOException {
        final Server server = new Server(
                new BaseHandler.Factory<>(Handler.class, Handler::new), true);
        server.startServer("localhost", 8000);
        server.stopServer();
    }

    public static void main(String[] args) throws IOException {
        final Server server = new Server(
                new BaseHandler.Factory<>(Handler.class, Handler::new), true);
        server.startServer("localhost", 8000);

        System.out.println("Server started... (write 'q' to quit, or write an IP to remove)");

        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            final String line = bufferedReader.readLine();
            if (line.contains("q")) {
                break;
            } else {
                server.closeConnection(new String(new byte[] {127, 0, 0, 1}));
            }
        }
        System.out.println("Stopping server");
        server.stopServer();
        System.exit(0);
    }
}
