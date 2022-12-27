package org.abc.perftest;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class Options {
    @Option(names = { "-b", "--basic-auth" }, paramLabel = "BASIC-AUTH", description = "The base64 basic auth string used for authentication")
    String basicAuthBase64String;

    @Option(names = { "-s", "--server" }, paramLabel = "SERVER", description = "The API server to connect to")
    String server;

    @Option(names = { "-d", "--output-dir" }, paramLabel = "DIRECTORY", description = "The output directory to output CSV files to")
    String outputDirectory;

    static Options parseArgs(String[] args) throws Exception {
        Options options = new Options();
        new CommandLine(options).parseArgs(args);

        if (options.basicAuthBase64String == null || options.basicAuthBase64String.isEmpty()) {
            throw new Exception("Must specify basic auth string");
        }

        if (options.server == null || options.server.isEmpty()) {
            throw new Exception("Must specify server");
        }

        if (options.outputDirectory == null || options.outputDirectory.isEmpty()) {
            throw new Exception("Must specify output directory");
        }

        return options;
    }
}