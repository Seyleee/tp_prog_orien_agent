
package agents;

import com.opencsv.CSVReader;
import comportements.ContractNetVente;
import data.Journey;
import data.JourneysList;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import launch.LaunchSimu;

import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Journey Seller
 *
 * @author Emmanuel ADAM
 */
@SuppressWarnings("serial")
public class AgenceAgent extends GuiAgent {
    /**
     * code shared with the gui to quit the agent
     */
    public static final int EXIT = 0;

    /**
     * catalog of the proposed journeys
     */
    private JourneysList catalog;
    /**
     * graphical user interface linked to the seller agent
     */
    private gui.AgenceGui window;
    /**
     * topic from which the alert will be received
     */
    private AID topic;

    public static final int SOLDES = 1;

    private AID topicIncrementJourney;
    public AID topicDecrementJourney;
    public AID topicJourneySold;
    public AID topicFinSoldes;
    public AID topicSoldes;

    // Initialisation de l'agent
    @Override
    protected void setup() {
            final Object[] args = getArguments(); // Recuperation des arguments
            catalog = new JourneysList();
            window = new gui.AgenceGui(this);
            window.display();

            if (args != null && args.length > 0) {
                fromCSV2Catalog((String) args[0]);
            }

            topicSoldes = AgentToolsEA.generateTopicAID(this, "SOLDES");
            topicFinSoldes = AgentToolsEA.generateTopicAID(this, "FIN DES SOLDES");
            topicJourneySold = AgentToolsEA.generateTopicAID(this, "Vayage en soldes acheté");

            AgentToolsEA.register(this, "travel agency", "seller");

            //REGLAGE ECOUTE DE LA RADIO
            topic = AgentToolsEA.generateTopicAID(this, "TRAFFIC NEWS");
            //ecoute des messages radio
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    var msg = myAgent.receive(MessageTemplate.MatchTopic(topic));
                    if (msg != null) {
                        println("Message reçu sur le topic " + topic.getLocalName() + ". Contenu " + msg.getContent() + " émis par " + msg.getSender().getLocalName());

                        Pattern patternStart = Pattern.compile("trajet (.*?) -");
                        Matcher matcherStart = patternStart.matcher(msg.getContent());
                        Pattern patternStop = Pattern.compile("-> (.*?) .");
                        Matcher matcherStop = patternStop.matcher(msg.getContent());
                        if (matcherStart.find() && matcherStop.find())
                            catalog.removeDirectJourney(matcherStart.group(1), matcherStop.group(1));
                    } else {
                        block();
                    }
                }
            });
            //FIN REGLAGE ECOUTE DE LA RADIO

            topicDecrementJourney = AgentToolsEA.generateTopicAID(this, "Decrement journey");
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    var msg = myAgent.receive(MessageTemplate.MatchTopic(topicDecrementJourney));
                    if (msg != null) {
                        try {
                            Journey journey = (Journey) msg.getContentObject();
                            catalog.decrementJourneyPlaces(journey);
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    } else {
                        block();
                    }
                }
            });

            topicIncrementJourney = AgentToolsEA.generateTopicAID(this, "Increment journey");
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    var msg = myAgent.receive(MessageTemplate.MatchTopic(topicIncrementJourney));
                    if (msg != null) {
                        try {
                            Journey journey = (Journey) msg.getContentObject();
                            catalog.incrementJourneyPlaces(journey);
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    } else {
                        block();
                    }
                }
            });

            // attendre une demande de catalogue & achat
            var template = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.CFP));
            addBehaviour(new ContractNetVente(this, template, catalog));

    }

    // Fermeture de l'agent
    @Override
    protected void takeDown() {
        // S'effacer du service pages jaunes
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            LaunchSimu.logger.log(Level.SEVERE, fe.getMessage());
        }
        LaunchSimu.logger.log(Level.INFO, "Agent Agence : " + getAID().getName() + " quitte la plateforme.");
        window.dispose();
    }

    /**
     * methode invoquee par la gui
     */
    @Override
    protected void onGuiEvent(GuiEvent guiEvent) {
        if (guiEvent.getType() == AgenceAgent.EXIT) {
            doDelete();
        }
    }

    /**
     * initialize the catalog from a cvs file<br>
     * csv line = origine, destination,means,departureTime,duration,financial
     * cost, co2, confort, nbRepetitions(optional),frequence(optional)
     *
     * @param file name of the cvs file
     */
    private void fromCSV2Catalog(final String file) {
        try (var cvsReader = new CSVReader(new FileReader(file), ',', '\'', 1)) {
            String[] nextLine;
            while ((nextLine = cvsReader.readNext()) != null) {
                String origine = nextLine[0].trim().toUpperCase();
                String destination = nextLine[1].trim().toUpperCase();
                String means = nextLine[2].trim();
                int departureDate = Integer.parseInt(nextLine[3].trim());
                int duration = Integer.parseInt(nextLine[4].trim());
                int cost = Integer.parseInt(nextLine[5].trim());
                int co2 = Integer.parseInt(nextLine[6].trim());
                int confort = Integer.parseInt(nextLine[7].trim());
                int nbRepetitions = (nextLine.length == 9) ? 0 : Integer.parseInt(nextLine[8].trim());
                int frequence = (nbRepetitions == 0) ? 0 : Integer.parseInt(nextLine[9].trim());
                Journey firstJourney = new Journey(origine, destination, means, departureDate, duration, cost,
                        co2, confort);
                firstJourney.setProposedBy(this.getLocalName());
                window.println(firstJourney.toString());
                catalog.addJourney(firstJourney);
                if (nbRepetitions > 0) {
                    repeatJourney(departureDate, nbRepetitions, frequence, firstJourney);
                }
            }
        } catch (NumberFormatException | IOException e) {
            window.println(e.getMessage());
        }
    }

    /**
     * repeat a journey on a sequence of dates into a catalog
     *
     * @param departureDate date of the first journey
     * @param nbRepetitions nb of journeys to add
     * @param frequence     frequency of the journeys in minutes
     * @param journey       the first journey to clone
     */
    private void repeatJourney(final int departureDate, final int nbRepetitions, final int frequence,
                               final Journey journey) {
        int nextDeparture = departureDate;
        for (int i = 0; i < nbRepetitions; i++) {
            final Journey cloneJ = journey.clone();
            nextDeparture = Journey.addTime(nextDeparture, frequence);
            cloneJ.setDepartureDate(nextDeparture);
            window.println(cloneJ.toString());
            catalog.addJourney(cloneJ);
        }
    }

    /**display a msg on the window*/
    public void println(String msg) {
        window.println(msg);
    }

    ///// GETTERS AND SETTERS
    public gui.AgenceGui getWindow() {
        return window;
    }


}
