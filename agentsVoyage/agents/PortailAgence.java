package agents;

import data.Journey;
import data.JourneysList;
import gui.PortailGui;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import launch.LaunchSimu;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortailAgence extends GuiAgent {
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
    private gui.PortailGui window;
    /**
     * topic from which the alert will be received
     */
    private AID topic;

    @Override
    protected void onGuiEvent(GuiEvent guiEvent) {
        if (guiEvent.getType() == AgenceAgent.EXIT) {
            doDelete();
        }
    }

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
        window = new gui.PortailGui(this);
        window.display();

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
                    window.println("Message reçu sur le topic " + topic.getLocalName() + ". Contenu " + msg.getContent() + " émis par " + msg.getSender().getLocalName());

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
/*
        // attendre une demande de catalogue & achat
        var template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));
        addBehaviour(new ContractNetVente(this, template, catalog));
*/
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

    public PortailGui getWindow() {
        return window;
    }

    public void println(String msg) {
        window.println(msg);
    }
}
