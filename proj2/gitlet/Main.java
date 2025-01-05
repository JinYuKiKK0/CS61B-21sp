package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        // TODO: what if args is empty?
        String firstArg = args[0];
        if(firstArg == null){
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                Repository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                Repository.add(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                if(args[1] == null){
                    System.out.println("Please enter a commit message.");
                    break;
                }
                Repository.commit(args[1]);
                break;
            case "rm":
                Repository.rm(args[1]);
                break;
        }
    }
}
