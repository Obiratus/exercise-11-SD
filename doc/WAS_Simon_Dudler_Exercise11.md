# Exercise 11: Reinforcement Learning Agents
The git repository can be found here: https://github.com/Obiratus/exercise-9-SD

## Task 1: Learning the Q Way
Reinforcement learning was used.
### State Space Design
Represented as a 7-dimensional vector:
``` 
[z1Level, z2Level, z1Light, z2Light, z1Blinds, z2Blinds, sunshine]
```
Where:
- `z1Level`, `z2Level`: Light levels in Zone 1 and Zone 2 (values: 0-3)
- `z1Light`, `z2Light`: Light fixture status in each zone (boolean: on/off)
- `z1Blinds`, `z2Blinds`: Blinds status in each zone (boolean: open/closed)
- `sunshine`: External sunshine level (values: 0-3)

This results in 4 × 4 × 2 × 2 × 2 × 2 × 4 = 1,024 possible states.

#### Justification
- Captures all relevant environmental variables affecting lighting conditions
- Includes controllable elements (lights, blinds) and external factors (sunshine)
- Discrete representation simplifies learning while maintaining adequate granularity

### Action Space Design
The actions available to the system are:
1. Turn Zone 1 lights on/off
2. Turn Zone 2 lights on/off
3. Open/close Zone 1 blinds
4. Open/close Zone 2 blinds

This creates a total of 8 possible actions (2^3 combinations of operations).

#### Justification
- Direct mapping to physical actuators in the environment
- Clear and interpretable actions for human operators


### Reward Function Design
Designed as follows:
``` 
R(s, a, s') = -1                           // Base step cost
             + 100 if goal state reached   // Large reward for reaching goal
             - 0.5 × (z1Light + z2Light)   // Energy consumption penalty
             - 0.1 × |Δz1Level| - 0.1 × |Δz2Level|  // Penalize rapid changes
```
#### Justification
- Encourages reaching the goal state efficiently (fewer steps)
- Penalizes unnecessary energy consumption
- Balances between user comfort (stable lighting) and energy efficiency
- Avoids rapid oscillations in lighting conditions

### Why Reinforcment Learning (RL)?
Because, this is, what this Assignment is all about ;-) Just kidding.

RL, is well-suited for the smart building light control system due to its ability to adapt to changing conditions, optimize for multiple objectives, and learn efficient strategies without explicit programming. 
The discrete state and action space design strikes a balance between complexity and learnability. 

While alternatives like rule-based systems might be simpler to implement initially, they lack the adaptability and optimization capabilities that make RL attractive for this dynamic environment.


#### Benefits of Reinforcement Learning for This Use Case
1. Adaptability to changing conditions
    - RL can adapt to changing external conditions (varying sunshine levels)
   
2. Optimization of multiple objectives
    - Balances competing goals (energy efficiency vs. comfort)
    - Can optimize for long-term rewards rather than immediate benefits

3. Learning from experience
    - Improves performance over time based on environmental interactions
    - Discovers non-obvious strategies that might elude human programmers --> Otherwise all possibilites need to be considered beforehand

4. No need for labeled data
    - Unlike supervised learning, doesn't require extensive datasets
    - Can learn directly from interaction with the environment


#### Challenges of Reinforcement Learning for This Use Case
1. Training time requirements
    - Requires extensive exploration to converge to optimal policy
    - May need thousands of episodes to learn effectively

2. Hyperparameter tuning
    - Performance heavily depends on proper setting of alpha, gamma, epsilon
    - May require extensive experimentation to tune properly

3. Stability issues
    - May oscillate between suboptimal policies


### Alternative Approaches
#### Rule-Based System
(The most obvious one)

**Pros:**
- Simple to implement and understand
- Predictable behavior
- No training required
- Easily explainable

**Cons:**
- Limited adaptability to changing conditions
- Requires manual definition of all rules
- Difficult to optimize for multiple objectives
- May miss non-obvious optimal strategies

#### Supervised Learning Approach
(even more work for me)

**Pros:**
- Could learn from human expert behaviors
- Potentially faster training with good dataset
- More predictable behavior

**Cons:**
- Requires extensive labeled dataset
- Cannot outperform the training examples
- Less exploration of alternative strategies
- Limited adaptability

### Anti Glare
For this specific anti-glare constraint, a hybrid approach would be suitable:
1. Update the state space
   - to include occupancy information
2. Add a large negative reward
   - for violating the anti-glare constraint
3. Implement a safety override
   - for deployment to guarantee constraint satisfaction

This approach provides the following benefits:
- The agent learns to avoid actions that lead to glare conditions
- The safety override ensures the constraint is never violated in practice
- The model can still optimize for energy efficiency and user comfort within these constraints

### Implementation Details
To implement this in practice:
1. Modify the reward function
   - in the `calculateReward` method to include the anti-glare penalty
2. Add a safety wrapper
   - around the `getActionFromState` operation that checks and corrects actions that would violate the constraint
3. Update the state representation
   - to track workstation occupancy
   - For example with:`z1Occupied`, `z2Occupied`: Boolean values indicating if a human is operating a workstation in Zone 1 or Zone 2 
   - Warning: This increases our state space size to 4 × 4 × 2 × 2 × 2 × 2 × 4 × 2 × 2 = 4,096 possible states.


### Learning Considerations
As a note of warning.
With this new constraint:
- The agent needs to learn more complex relationships between light levels and blinds positions
- Additional exploration may be required to discover effective policies

An advantage is, that the Q-learning algorithm itself doesn't need to change as it will naturally adapt to the new reward structure and learn to avoid the heavily penalized states.



## Task 2: 



## Task 3: 



## Declaration of aids

| Task   | Aid                   | Description                      |
|--------|-----------------------|----------------------------------|
| Task 1 | IntelliJ AI Assistant | Explain code.                    |
| Task 1 | ChatGPT 4o            | Help formatting for markup file. |

