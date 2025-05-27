//illuminance controller agent

/*
* The URL of the W3C Web of Things Thing Description (WoT TD) of a lab environment
* Simulated lab WoT TD: "https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl"
* Real lab WoT TD: Get in touch with us by email to acquire access to it!
*/

/* Initial beliefs and rules */

// the agent has a belief about the location of the W3C Web of Thing (WoT) Thing Description (TD)
// that describes a lab environment to be learnt
learning_lab_environment("https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl").

// the agent believes that the task that takes place in the 1st workstation requires an indoor illuminance
// level of Rank 2, and the task that takes place in the 2nd workstation requires an indoor illumincance
// level of Rank 3. Modify the belief so that the agent can learn to handle different goals.
task_requirements([2,3]).

// Q-learning parameters
episodes(5).    // number of episodes for learning
alpha(0.8).        // learning rate
gamma(0.9).        // discount factor
epsilon(0.1).      // exploration probability
reward(100).       // reward for reaching goal state

/* Initial goals */
!start. // the agent has the goal to start

/*
 * Plan for reacting to the addition of the goal !start
 * Triggering event: addition of goal !start
 * Context: the agent believes that there is a WoT TD of a lab environment located at Url, and that
 * the tasks taking place in the workstations require indoor illuminance levels of Rank Z1Level and Z2Level
 * respectively
 * Body: creates a QLearnerArtifact for learning and a ThingArtifact for acting on the lab environment.
*/
@start
+!start : learning_lab_environment(Url)
  & task_requirements([Z1Level, Z2Level])
  & episodes(E)
  & alpha(A)
  & gamma(G)
  & epsilon(Eps)
  & reward(R) <-

  .print("Hello world");
  .print("I want to achieve Z1Level=", Z1Level, " and Z2Level=",Z2Level);

  // creates a QLearner artifact for learning the lab Thing described by the W3C WoT TD located at URL
  .print("Creating QLearner artifact...");
  makeArtifact("qlearner", "tools.QLearner", [Url], QLArtId);
  .print("QLearner artifact created with ID: ", QLArtId);
  focus(QLArtId);

  // creates a EnvironmentHelper artifact
  .print("Creating EnvironmentHelper artifact...");
  makeArtifact("Lab", "tools.EnvironmentHelper", [Url], HelperArtId);
  .print("Lab artifact created with ID: ", HelperArtId);
  focus(HelperArtId);


  // creates a ThingArtifact artifact for reading and acting on the lab Thing
  .print("Creating ThingArtifact...");
  makeArtifact("lab", "org.hyperagents.jacamo.artifacts.wot.ThingArtifact", [Url], LabThingArtId);
  .print("ThingArtifact created with ID: ", LabThingArtId);
  focus(LabThingArtId);


  // Use the calculateQ operation to learn Q tables for the desired environment state
  .print("Starting Q-learning with ", E, " episodes...");
  calculateQ([Z1Level, Z2Level], E, A, G, Eps, R);
  .print("Q-learning completed for goal [", Z1Level, ",", Z2Level, "]");

  // After learning, try to achieve the goal
  !achieve_goal([Z1Level, Z2Level]).

/*
 * Plan for achieving the goal state
 * Context: the agent has a goal state to achieve
 */
+!achieve_goal(GoalState) <-
  .print("Trying to achieve goal state: ", GoalState);
  !monitor_and_act(GoalState).


+!find_value(Tags, Values, PropertyName, ResultValue) <-
    // Construct the full URI for the property
    .concat("http://example.org/was#", PropertyName, FullURI);

    .length(Tags, TagsLength);
    +find_value_index(0);
    -found_value;

    while (find_value_index(I) & I < TagsLength & not found_value) {
        .nth(I, Tags, CurrentTag);
        if (CurrentTag = FullURI) {
            .nth(I, Values, TempValue);
            +found_value(TempValue);
            +found_value;
        } else {
            -+find_value_index(I+1);
        }
    }

    // Clean up
    -find_value_index(_);
    if (found_value) {
        ?found_value(ResultValue);
        -found_value(ResultValue);
    } else {
        ResultValue = "not-found";
    }
    -found_value.

/*
 * Plan for monitoring the environment and taking actions
 * Context: the agent is trying to achieve a goal state
 */
+!monitor_and_act(GoalState) <-
  .print("Starting to monitor environment and take actions");

  // Read the Status property which contains Z1Level and Z2Level
  // .print("Reading Status property via TD...");
  // readProperty("Status", Tags, Values);
  // .print("Status properties: ", Tags);
  // .print("Status values: ", Values);

  // Read the current state directly from the Lab artifact
  .print("Reading Status property via EnvironmentHelper...");
  getCurrentState(CurrentState);
  .print("Current state from EnvironmentHelper artifact: ", CurrentState);

  // Extract individual values from CurrentState
  .nth(0, CurrentState, Z1L);
  .nth(1, CurrentState, Z2L);
  .nth(2, CurrentState, Z1Light);
  .nth(3, CurrentState, Z2Light);
  .nth(4, CurrentState, Z1Blinds);
  .nth(5, CurrentState, Z2Blinds);
  .nth(6, CurrentState, Sunshine);

  // Print to verify the extracted values
  .print("Parsed current state: Z1L=", Z1L, ", Z2L=", Z2L, ", Z1Light=", Z1Light, ", Z2Light=", Z2Light,
         ", Z1Blinds=", Z1Blinds, ", Z2Blinds=", Z2Blinds, ", Sunshine=", Sunshine);

  // Parse the goal state
  .nth(0, GoalState, GoalZ1Level);
  .nth(1, GoalState, GoalZ2Level);

  .print("Goal state: ", GoalState, " (Z1Level=", GoalZ1Level, ", Z2Level=", GoalZ2Level, ")");

  if (Z1L == GoalZ1Level & Z2L == GoalZ2Level) {
    .print("Goal state achieved! Z1Level=", Z1L, " and Z2Level=", Z2L);
  } else {
    .print("Goal not yet achieved. Current: Z1Level=", Z1L, ", Z2Level=", Z2L);
    .print("Getting next best action from QLearner...");

    // Get the next best action from the Q-learner
    .print("Calling getActionFromState with goal ", GoalState, " and current state ", CurrentState);
    getActionFromState(GoalState, CurrentState, ActionTag, PayloadTags, Payload);
    .print("Received action: ", ActionTag, " with payload tags: ", PayloadTags, " and payload: ", Payload);

    // Execute the action
    .print("Executing action using invokeAction...");
    invokeAction(ActionTag, PayloadTags, Payload);
    .print("Action executed successfully");

    // Wait a bit before checking the state again
    .print("Waiting for the environment to respond...");
    .wait(1000);
    .print("Wait completed, checking state again");

    // Pause for 2 seconds before the next monitoring cycle
    .print("Pausing for 2 seconds...");
    .wait(2000);

    // Recursively monitor and act until goal is achieved
    !monitor_and_act(GoalState);
  }.

// Handle failures in monitoring and acting
-!monitor_and_act(GoalState)[error(ErrorId), error_msg(Msg)] <-
  .print("Failed to monitor and act. Error ID: ", ErrorId, " Message: ", Msg);
  .wait(2000);
  !monitor_and_act(GoalState).

// Generic failure handler
-!monitor_and_act(GoalState) <-
  .print("Failed to monitor and act. Retrying...");
  .wait(2000);
  !monitor_and_act(GoalState).