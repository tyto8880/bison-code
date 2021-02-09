package kafkaRouting;

import java.util.Scanner;

/*
 * =====================================================================================================================
 *  This file provides a console interface for controlling a, newly created, agent producer. Users of this interface
 *  can send data of any of the pre-defined types over the stream (with any values they wish) and terminate the
 *  producer.
 *
 *         NOTE: For this file to function properly, it must be run alongside an active instance of zookeeper,
 *               the kafka server with all subtopics created (via CreateTopics.bat), and MainDataRouting.
 * =====================================================================================================================
 */

public class AgentInterface {
    public static void main(String[] args) throws InterruptedException {
        Scanner userIn = new Scanner(System.in);    // Create scanner for user input
        AgentProducer agent = new AgentProducer();  // Create AgentProducer object for user to command
        String uSelection = "nothing";

        // Funcitonality menu
        while(!uSelection.equals("2")) {
            System.out.println("Agent Producer Options:");
            System.out.println("1. Send Temporal Event");
            System.out.println("2. Terminate");
            uSelection = userIn.nextLine();     // Read user menu selection

            switch(uSelection) {
                case "1":
                    System.out.println("Which event occurred (A/B)?");
                    String eventType = userIn.nextLine();
                    while(!eventType.equalsIgnoreCase("A") && !eventType.equalsIgnoreCase("B")) {
                        System.out.println("ERROR: Invalid event type selection.");
                        System.out.println("Which event occurred (A/B)?");
                        eventType = userIn.nextLine();
                    }
                    agent.sendTemporal(eventType);
                    break;
                case "2":
                    System.out.println("Terminating agent producer.");
                    break;
                default:
                    System.out.println("Error: Invalid Selection!");
            }
        }
    }
}
