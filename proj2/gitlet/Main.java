package gitlet;

import java.io.IOException;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author JinYu
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Please enter a command.");
                System.exit(0);
            }
            String firstArg = args[0];
            switch (firstArg) {
                case "init":
                    Repository.init();
                    break;
                case "add":
                    if (args.length < 2) {
                        System.out.println("Please specify a file to add.");
                        System.exit(0);
                    }
                    Repository.add(args[1]);
                    break;
                case "commit":
                    if (args.length < 2 || args[1].trim().isEmpty()) {
                        System.out.println("Please enter a commit message.");
                        System.exit(0);
                    }
                    Repository.commit(args[1]);
                    break;
                case "rm":
                    Repository.rm(args[1]);
                    break;
                case "log":
                    Repository.log();
                    break;
                case "global-log":
                    Repository.globalLog();
                    break;
                case "find":
                    Repository.find(args[1]);
                    break;
                case "status":
                    Repository.status();
                    break;
                case "checkout":
                    Repository.handleCheckout(args);
                    break;
                case "branch":
                    Repository.branch(args[1]);
                    break;
                case "rm-branch":
                    Repository.removeBranch(args[1]);
                    break;
                case "reset":
                    Repository.reset(args[1]);
                default:
                    System.out.println("No command with that name exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while processing the command: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
