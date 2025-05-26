package tools;

import java.util.*;
import java.util.logging.*;
import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class QLearner extends Artifact {
    private int prevZ1Level = 0; // Default to 0 or another appropriate initial value
    private int prevZ2Level = 0; // Default to 0 or another appropriate initial value
  private Lab lab; // the lab environment that will be learnt 
  private int stateCount; // the number of possible states in the lab environment
  private int actionCount; // the number of possible actions in the lab environment
  private HashMap<Integer, double[][]> qTables; // a map for storing the qTables computed for different goals

  private static final Logger LOGGER = Logger.getLogger(QLearner.class.getName());

  public void init(String environmentURL) {

    // the URL of the W3C Thing Description of the lab Thing
    this.lab = new Lab(environmentURL);

    this.stateCount = this.lab.getStateCount();
    LOGGER.info("Initialized with a state space of n="+ stateCount);

    this.actionCount = this.lab.getActionCount();
    LOGGER.info("Initialized with an action space of m="+ actionCount);

    qTables = new HashMap<>();
  }

/**
* Computes a Q matrix for the state space and action space of the lab, and against
* a goal description. For example, the goal description can be of the form [z1level, z2Level],
* where z1Level is the desired value of the light level in Zone 1 of the lab,
* and z2Level is the desired value of the light level in Zone 2 of the lab.
* For exercise 11, the possible goal descriptions are:
* [0,0], [0,1], [0,2], [0,3], 
* [1,0], [1,1], [1,2], [1,3], 
* [2,0], [2,1], [2,2], [2,3], 
* [3,0], [3,1], [3,2], [3,3].
*
*<p>
* HINT: Use the methods of {@link LearningEnvironment} (implemented in {@link Lab})
* to interact with the learning environment (here, the lab), e.g., to retrieve the
* applicable actions, perform an action at the lab during learning etc.
*</p>
* @param  goalDescription  the desired goal against the which the Q matrix is calculated (e.g., [2,3])
* @param  episodesObj the number of episodes used for calculating the Q matrix
* @param  alphaObj the learning rate with range [0,1].
* @param  gammaObj the discount factor [0,1]
* @param epsilonObj the exploration probability [0,1]
* @param rewardObj the reward assigned when reaching the goal state
**/
@OPERATION
public void calculateQ(Object[] goalDescription, Object episodesObj, Object alphaObj, Object gammaObj, Object epsilonObj, Object rewardObj) {

    // ensure that the right datatypes are used
    Integer episodes = Integer.valueOf(episodesObj.toString());
    Double alpha = Double.valueOf(alphaObj.toString());
    Double gamma = Double.valueOf(gammaObj.toString());
    Double epsilon = Double.valueOf(epsilonObj.toString());
    Integer reward = Integer.valueOf(rewardObj.toString());

    double[][] qTable = initializeQTable();

    Integer goalKey = Arrays.hashCode(goalDescription);

    // Run Q-learning algorithm for the specified number of episodes
    for (int episode = 1; episode <= episodes; episode++) {
        randomizeState(goalDescription);

        int currentState = lab.readCurrentState();

        int maxSteps = 10000;
        for (int step = 0; step < maxSteps; step++) {

            List<Integer> applicableActions = lab.getApplicableActions(currentState);

            if (applicableActions.isEmpty()) {
                break;
            }

            int action = chooseAction(qTable, currentState, applicableActions, epsilon);

            lab.performAction(action);

            int newState = lab.readCurrentState();

            double calculatedReward = calculateReward(goalDescription, reward);
            double maxQNext = getMaxQ(qTable, newState, lab.getApplicableActions(newState));

            // Update Q-value using the Q-learning formula
            qTable[currentState][action] = qTable[currentState][action] +
                    alpha * (calculatedReward + gamma * maxQNext - qTable[currentState][action]);

            currentState = newState;

            // Check if we've reached a goal state
            List<Integer> currentStateComponents = lab.currentState; // Get the actual state components

            int z1Level = currentStateComponents.get(0);
            int z2Level = currentStateComponents.get(1);

            // Get goal values with proper casting
            int goalZ1 = ((Number)goalDescription[0]).intValue();
            int goalZ2 = ((Number)goalDescription[1]).intValue();


            // Log what we're comparing
            // LOGGER.info("Comparing current state [" + z1Level + "," + z2Level + "] with goal state [" + goalZ1 + "," + goalZ2 + "]");

            boolean matchesGoal = (z1Level == ((Number)goalDescription[0]).intValue() &&
                    z2Level == ((Number)goalDescription[1]).intValue());

            if (matchesGoal) {
                LOGGER.info("Goal state reached in episode " + episode + " after " + step + " steps.");
                LOGGER.info("Goal state found at currentState " + currentState + ".");
                break;
            }

        }

        if (episodes <= 10 || episode % 100 == 0) {
            LOGGER.info("Completed episode " + episode + " of " + episodes);
        }
    }

    // Save the Q-table for this goal description
    qTables.put(goalKey, qTable);

    printQTable(qTable);

    LOGGER.info("Q-learning completed for goal " + Arrays.toString(goalDescription));
}


    /**
     * Randomizes the state of the lab environment by focusing on actions
     * that directly control the environmental factors affecting illumination
     *
     * @param goalDescription An Object array containing the goal state values to avoid
     */
    private void randomizeState(Object[] goalDescription) {
        LOGGER.info("Starting targeted illumination randomization...");
        LOGGER.info("Goal state to avoid: [" + goalDescription[0] + "," + goalDescription[1] + "]");
        Random random = new Random();

        // Get the initial state
        int initialStateId = lab.readCurrentState();
        List<Integer> initialComponents = new ArrayList<>(lab.currentState);
        LOGGER.info("Initial state ID: " + initialStateId + ", components: " + initialComponents);

        // Store the initial values of the first two components (illumination values)
        int initialZ1 = initialComponents.get(0);
        int initialZ2 = initialComponents.get(1);

        // Convert goal state components to integers for comparison
        int goalZ1 = ((Number)goalDescription[0]).intValue();
        int goalZ2 = ((Number)goalDescription[1]).intValue();

        // Check if we're already at a non-goal state
        boolean atNonGoalState = (initialZ1 != goalZ1 || initialZ2 != goalZ2);
        if (atNonGoalState) {
            LOGGER.info("Already at a non-goal state. No randomization needed.");
            return;
        }

        // Number of randomization attempts
        int maxAttempts = 10000;

        // Perform multiple random actions to try to change the illumination state
        for (int i = 0; i < maxAttempts; i++) {
            int currentState = lab.readCurrentState();
            List<Integer> applicableActions = lab.getApplicableActions(currentState);
            // LOGGER.info("Attempt " + (i+1) + ": Current state: " + currentState + ", components: " + lab.currentState);

            if (!applicableActions.isEmpty()) {
                int randomActionIndex = random.nextInt(applicableActions.size());
                int randomAction = applicableActions.get(randomActionIndex);
                // LOGGER.info("Performing random action: " + randomAction);

                lab.performAction(randomAction);

                // Log current illumination values
                lab.readCurrentState();
                List<Integer> currentComponents = new ArrayList<>(lab.currentState);
                // LOGGER.info("Initial state ID: " + initialStateId + ", components: " + initialComponents);

                // Store the initial values of the first two components (illumination values)
                int currentZ1 = currentComponents.get(0);
                int currentZ2 = currentComponents.get(1);

                // LOGGER.info("After action: Illumination values: [" + currentZ1 + "," + currentZ2 + "]");

                // Check if we've reached a non-goal state
                if (currentZ1 != goalZ1 || currentZ2 != goalZ2) {
                    LOGGER.info("Successfully reached a non-goal state: [" + currentZ1 + "," +
                            currentZ2 + "] (different from goal [" + goalZ1 + "," + goalZ2 + "])");
                    return; // Exit early once we've reached a non-goal state
                }
            } else {
                LOGGER.warning("No applicable actions available for state " + currentState);
            }
        }

        // Log final state after randomization
        int finalStateId = lab.readCurrentState();
        List<Integer> finalComponents = new ArrayList<>(lab.currentState);
        LOGGER.info("Randomization complete.");
        LOGGER.info("Initial illumination: [" + initialZ1 + "," + initialZ2 + "] → " +
                "Final illumination: [" + finalComponents.get(0) + "," + finalComponents.get(1) + "]");

        // Check if we're still at the goal state
        if (finalComponents.get(0) == goalZ1 && finalComponents.get(1) == goalZ2) {
            LOGGER.warning("WARNING: After " + maxAttempts + " attempts, could not move away from the goal state! " +
                    "This may indicate a problem with the environment dynamics or the goal state is too stable.");

            // Last resort: try a more aggressive approach - perform many more random actions
            LOGGER.info("Attempting aggressive randomization as a last resort...");
            for (int i = 0; i < 30; i++) {
                List<Integer> moreActions = lab.getApplicableActions(lab.readCurrentState());
                if (!moreActions.isEmpty()) {
                    int action = moreActions.get(random.nextInt(moreActions.size()));
                    lab.performAction(action);

                    if (lab.currentState.get(0) != goalZ1 || lab.currentState.get(1) != goalZ2) {
                        LOGGER.info("Aggressive randomization succeeded! New state: " + lab.currentState);
                        return;
                    }
                }
            }

            LOGGER.severe("CRITICAL: Could not move away from goal state even with aggressive randomization!");
        }
    }


    /**
     * Chooses an action using epsilon-greedy policy
     * @param qTable The Q-table
     * @param state The current state
     * @param applicableActions List of applicable actions
     * @param epsilon Exploration probability
     * @return The chosen action
     */
    private int chooseAction(double[][] qTable, int state, List<Integer> applicableActions, double epsilon) {
        Random random = new Random();

        // With probability epsilon, choose a random action (exploration)
        if (random.nextDouble() < epsilon) {
            int randomIndex = random.nextInt(applicableActions.size());
            return applicableActions.get(randomIndex);
        }

        // Otherwise, choose the action with the highest Q-value (exploitation)
        return getBestAction(qTable, state, applicableActions);
    }

    /**
     * Returns the action with the highest Q-value for the given state
     * @param qTable The Q-table
     * @param state The current state
     * @param applicableActions List of applicable actions
     * @return The action with the highest Q-value
     */
    private int getBestAction(double[][] qTable, int state, List<Integer> applicableActions) {
        int bestAction = applicableActions.get(0); // Default to first applicable action
        double bestValue = qTable[state][bestAction];

        for (int action : applicableActions) {
            if (qTable[state][action] > bestValue) {
                bestValue = qTable[state][action];
                bestAction = action;
            }
        }

        return bestAction;
    }

    /**
     * Returns the maximum Q-value for the next state
     * @param qTable The Q-table
     * @param state The state
     * @param applicableActions List of applicable actions
     * @return The maximum Q-value
     */
    private double getMaxQ(double[][] qTable, int state, List<Integer> applicableActions) {
        if (applicableActions.isEmpty()) {
            return 0.0; // No applicable actions
        }

        double maxValue = Double.NEGATIVE_INFINITY;
        for (int action : applicableActions) {
            maxValue = Math.max(maxValue, qTable[state][action]);
        }

        return maxValue;
    }

    /**
     * Calculates the reward for transitioning to a new state
     * @param goalDescription The goal description
     * @param goalReward The reward value to use when goal state is reached
     * @return The calculated reward
     */
    private double calculateReward(Object[] goalDescription, Integer goalReward) {
        // Extract the desired light levels from the goal description
        int goalZ1Level = Integer.parseInt(goalDescription[0].toString());
        int goalZ2Level = Integer.parseInt(goalDescription[1].toString());

        lab.readCurrentState();
        List<Integer> currentState = lab.currentState;

        // Extract components from the current state
        int z1Level = currentState.get(0);
        int z2Level = currentState.get(1);
        boolean z1Light = currentState.get(2) == 1;
        boolean z2Light = currentState.get(3) == 1;
        boolean z1Blinds = currentState.get(4) == 1;
        boolean z2Blinds = currentState.get(5) == 1;
        int sunshine = currentState.get(6);

        int prevZ1Level = this.prevZ1Level;
        int prevZ2Level = this.prevZ2Level;

        // Update the previous state for the next iteration
        this.prevZ1Level = z1Level;
        this.prevZ2Level = z2Level;

        // Check if we've reached the goal state
        boolean atGoalState = (z1Level == goalZ1Level && z2Level == goalZ2Level);

        double reward = -1.0; // Base step cost

        // Add large reward if goal state is reached
        if (atGoalState) {
            reward += goalReward;

        }

        // Apply energy consumption penalty for lights (50 units each)
        if (z1Light) {
            reward -= 5.0; // Scaled from 50 units
        }
        if (z2Light) {
            reward -= 5.0; // Scaled from 50 units
        }

        // Apply energy cost for blinds (1 unit each)
        if (z1Blinds) {
            reward -= 0.1; // Scaled from 1 unit
        }
        if (z2Blinds) {
            reward -= 0.1; // Scaled from 1 unit
        }

        // Apply penalty for rapid changes in light levels
        int z1LevelChange = Math.abs(z1Level - prevZ1Level);
        int z2LevelChange = Math.abs(z2Level - prevZ2Level);
        reward -= 0.1 * z1LevelChange + 0.1 * z2LevelChange;

        // Add contextual intelligence for energy efficiency
        // Penalize unnecessary light usage when natural light is available
        if (sunshine >= 2) { // Medium or high sunshine
            if (z1Blinds && z1Light) {
                reward -= 1.0; // Additional penalty for using artificial light when natural light is available
            }
            if (z2Blinds && z2Light) {
                reward -= 1.0;
            }
        }

        return reward;
    }


    /**
     * Checks if the current state is a goal state
     * @param state The current state index
     * @param goalDescription The goal description
     * @return True if the state is a goal state, false otherwise
     */
    private boolean isGoalState(int state, Object[] goalDescription) {
        // Extract the desired light levels from the goal description
        int goalZ1Level = Integer.parseInt(goalDescription[0].toString());
        int goalZ2Level = Integer.parseInt(goalDescription[1].toString());


        List<Object> goalStateDesc = Arrays.asList(
                goalZ1Level,
                goalZ2Level,
                null,
                null,
                null,
                null,
                null
        );

        // Get all states that match our goal description
        List<Integer> goalStates = lab.getCompatibleStates(goalStateDesc);

        // Check if our current state is one of the goal states
        return goalStates.contains(state);
    }

/**
* Returns information about the next best action based on a provided state and the QTable for
* a goal description. The returned information can be used by agents to invoke an action 
* using a ThingArtifact.
*
* @param  goalDescription  the desired goal against the which the Q matrix is calculated (e.g., [2,3])
* @param  currentStateDescription the current state e.g. [2,2,true,false,true,true,2]
* @param  nextBestActionTag the (returned) semantic annotation of the next best action, e.g. "http://example.org/was#SetZ1Light"
* @param  nextBestActionPayloadTags the (returned) semantic annotations of the payload of the next best action, e.g. [Z1Light]
* @param nextBestActionPayload the (returned) payload of the next best action, e.g. true
**/
  @OPERATION
  public void getActionFromState(Object[] goalDescription, Object[] currentStateDescription,
      OpFeedbackParam<String> nextBestActionTag, OpFeedbackParam<Object[]> nextBestActionPayloadTags,
      OpFeedbackParam<Object[]> nextBestActionPayload) {
         
        // remove the following upon implementing Task 2.3!

        // sets the semantic annotation of the next best action to be returned 
        nextBestActionTag.set("http://example.org/was#SetZ1Light");

        // sets the semantic annotation of the payload of the next best action to be returned 
        Object payloadTags[] = { "Z1Light" };
        nextBestActionPayloadTags.set(payloadTags);

        // sets the payload of the next best action to be returned 
        Object payload[] = { true };
        nextBestActionPayload.set(payload);
      }

    /**
    * Print the Q matrix
    *
    * @param qTable the Q matrix
    */
  void printQTable(double[][] qTable) {
    System.out.println("Q matrix");
    for (int i = 0; i < qTable.length; i++) {
      System.out.print("From state " + i + ":  ");
     for (int j = 0; j < qTable[i].length; j++) {
      System.out.printf("%6.2f ", (qTable[i][j]));
      }
      System.out.println();
    }
  }

  /**
  * Initialize a Q matrix
  *
  * @return the Q matrix
  */
 private double[][] initializeQTable() {
    double[][] qTable = new double[this.stateCount][this.actionCount];
    for (int i = 0; i < stateCount; i++){
      for(int j = 0; j < actionCount; j++){
        qTable[i][j] = 0.0;
      }
    }
    return qTable;
  }
}
