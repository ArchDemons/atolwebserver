package ru.archdemon.atol.webserver;

public class Main {

    public static void main(String[] args) throws Exception {
        Application app = new Application();

        if (args.length == 0) {
            app.start(args);
            app.stop(args);
        } else if ("start".equals(args[0])) {
            app.start(args);
        } else if ("stop".equals(args[0])) {
            app.stop(args);
        }
    }

}
