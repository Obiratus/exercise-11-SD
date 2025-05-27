package tools;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Helper class to encapsulate Lab functionality
 */
public class EnvironmentHelper extends Artifact {

    private static final Logger LOGGER = Logger.getLogger(EnvironmentHelper.class.getName());
    private Lab lab; // Instance of Lab

    @OPERATION
    public void init(String environmentURL) {
        // Create and initialize the Lab instance
        try {
            lab = new Lab(environmentURL);
            LOGGER.info("EnvironmentHelper initialized with Lab at: " + environmentURL);
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize Lab: " + e.getMessage());
            failed("Failed to initialize Lab: " + e.getMessage());
        }
    }

    @OPERATION
    public void getCurrentState(OpFeedbackParam<Integer[]> currentState) {
        if (lab == null) {
            LOGGER.severe("Lab is not initialized. Please call the 'init' operation first.");
            failed("Lab has not been initialized yet.");
            return;
        }

        List<Integer> labState = lab.getCurrentState();

        if (labState == null) {
            LOGGER.warning("Lab.getCurrentState() returned null.");
            failed("Current state is null.");
            return;
        }

        try {
            Integer[] stateArray = labState.toArray(new Integer[0]);
            currentState.set(stateArray);
        } catch (Exception e) {
            LOGGER.severe("Error while returning the current state: " + e.getMessage());
            failed("Failed to return the current state. Exception: " + e.getMessage());
        }
    }




    @OPERATION
    public void readCurrentState(OpFeedbackParam<Integer> stateIndex) {
        if (lab == null) {
            failed("Lab is not initialized. Did you call init first?");
            return;
        }
        stateIndex.set(lab.readCurrentState());
    }

    @OPERATION
    public void getCompatibleStates(List<Object> stateDescription, OpFeedbackParam<List<Integer>> compatibleStates) {
        if (lab == null) {
            failed("Lab is not initialized. Did you call init first?");
            return;
        }
        compatibleStates.set(lab.getCompatibleStates(stateDescription));
    }

    @OPERATION
    public void getApplicableActions(int state, OpFeedbackParam<List<Integer>> applicableActions) {
        if (lab == null) {
            failed("Lab is not initialized. Did you call init first?");
            return;
        }
        applicableActions.set(lab.getApplicableActions(state));
    }

    @OPERATION
    public void performAction(int action) {
        if (lab == null) {
            failed("Lab is not initialized. Did you call init first?");
            return;
        }
        lab.performAction(action);
        LOGGER.info("Performed action: " + action);
    }
}