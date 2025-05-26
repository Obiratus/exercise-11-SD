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
episodes(2).    // number of episodes for learning
alpha(0.1).        // learning rate
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
  makeArtifact("qlearner", "tools.QLearner", [Url], QLArtId);

  // creates a ThingArtifact artifact for reading and acting on the state of the lab Thing
  makeArtifact("lab", "org.hyperagents.jacamo.artifacts.wot.ThingArtifact", [Url], LabArtId);

  // Use the calculateQ operation to learn Q tables for the desired environment state
  .print("Starting Q-learning with ", E, " episodes...");
  calculateQ([Z1Level, Z2Level], E, A, G, Eps, R);
  .print("Q-learning completed for goal [", Z1Level, ",", Z2Level, "]");

  // example use of the getActionFromState operation of the QLearner artifact
  // relevant for Task 2.3
  getActionFromState([1,1], [0, 0, false, false, false, false, 3], ActionTag, PayloadTags, Payload);


  // example use of the invokeAction operation of the ThingArtifact
  //invokeAction(ActionTag, PayloadTags, Payload)
  .