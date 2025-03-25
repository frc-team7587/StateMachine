package org.metuchenmomentum.statemachine;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Validates {@link MooreTypeStateMachine}. The test
 * is based on a hypothetical lamp switch that lies
 * on the floor and responds to two actions. Stomping
 * on the switch turns it on and off. (Stomp switches
 * are common in the UK.) Kicking the switch changes
 * the attached lamp's color. (This feature is pure
 * fantasy.) For completeness, we include a test-only
 * action and a partial implementation.
 *
 * <p>Note that this is
 * a <a href='https://junit.org/junit5/docs/current/user-guide/'>JUnit5</a>
 * test that uses
 * <a href='https://docs.google.com/document/d/15mJ2Qrldx-J14ubTEnBj7nYN2FB8ap7xOn8GRAi24_A/edit?tab=t.0'>Mockito</a>
 * to verify the dummy state machine's behavior. Please see the foregoing
 * links for details.
 */
class MooreTypeStateMachineTest {
  /**
   * States for the stomp and kick state machine
   */
  public enum State {
    OFF,
    ON,
    CHANGING_COLOR,
  }

  /**
   * Events for the stomp akd kick state machine. Not surprisingly,
   * it limits interactions to kicking and stomping.
   */
  public enum Event {
    KICK,
    STOMP,
  }

  /**
   * Available colors.
   */
  public enum Color {
    WHITE,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    CYAN,
    BLUE,
    MAGENTA,
  }

  /**
   * State transition table for the stomp and kick switch.
   */
  private static final StateTransitionTable<State, Event> TRANSITION_TABLE =
      StateTransitionTable.builder(State.class, Event.class)
          .addTransition(State.OFF, Event.STOMP, State.ON)
          .addTransition(State.ON, Event.STOMP, State.OFF)
          .addTransition(State.CHANGING_COLOR, Event.STOMP, State.OFF)
          .addTransition(State.ON, Event.KICK, State.CHANGING_COLOR)
          .addTransition(State.CHANGING_COLOR, Event.KICK, State.CHANGING_COLOR)
          .build();

  /**
   * A dummy action that merely records the new state, if entered.
   * Fetching the state resets the new state value to null, which
   * is returned when a transition does not occur.
   */
  private static class TestAction implements Consumer<State> {
    @Nullable
    private State maybeEnteredState;

    private TestAction() {
      maybeEnteredState = null;
    }

    @Override
    public void accept(State enteredState) {
      maybeEnteredState = enteredState;
    }

    @Nullable
    private State getAndReset() {
      var result = maybeEnteredState;
      maybeEnteredState = null;
      return result;
    }
  }

  /**
   * Hardware abstraction layer that controls the
   * switch hardware.
   */
  private interface SwitchHAL {
    void turnOn();

    void turnOff();

    void setColor(Color color);
  }

  /**
   * A partial implementation of the lamp control, with hardware interaction
   * to be filled in by subclasses.
   */
  private static class SwitchAction implements Consumer<State> {

    private static final Color[] COLORS = Color.values();

    private final SwitchHAL hardwareAbstraction;
    private int colorIndex;

    private SwitchAction(SwitchHAL hardwareAbstraction) {
      this.hardwareAbstraction = hardwareAbstraction;
      colorIndex = 0;
    }

    @Override
    public void accept(State state) {
      switch (state) {
        case State.OFF:
          hardwareAbstraction.turnOff();
          break;
        case State.ON:
          hardwareAbstraction.turnOn();
          break;
        case State.CHANGING_COLOR:
          colorIndex = (++colorIndex) % COLORS.length;
          hardwareAbstraction.setColor(COLORS[colorIndex]);
      }
    }
  }

  private SwitchHAL mockHardwareAbstractionLayer;
  private MooreTypeStateMachine<State, Event> machine;

  @BeforeEach
  public void setUp() {
    mockHardwareAbstractionLayer = Mockito.mock(SwitchHAL.class);
    var action = new SwitchAction(mockHardwareAbstractionLayer);
    machine = new MooreTypeStateMachine<>(
        TRANSITION_TABLE,
        State.OFF,
        action);
  }

  @Test
  public void transitionTest() {
    TestAction action = new TestAction();
    MooreTypeStateMachine<State, Event> machine = new MooreTypeStateMachine<>(
        TRANSITION_TABLE,
        State.OFF,
        action);
    machine.accept(Event.KICK);
    assertThat(action.maybeEnteredState).isNull();
    machine.accept(Event.STOMP);
    assertThat(action.getAndReset()).isEqualTo(State.ON);
    machine.accept(Event.STOMP);
    assertThat(action.getAndReset()).isEqualTo(State.OFF);
    machine.accept(Event.STOMP);
    assertThat(action.getAndReset()).isEqualTo(State.ON);
    machine.accept(Event.KICK);
    assertThat(action.getAndReset()).isEqualTo(State.CHANGING_COLOR);
    machine.accept(Event.KICK);
    assertThat(action.getAndReset()).isEqualTo(State.CHANGING_COLOR);
  }

  @Test
  public void kickWhenOff_doesNothing() {
    machine.accept(Event.KICK);
    Mockito.verifyNoInteractions(mockHardwareAbstractionLayer);
  }

  @Test
  public void stompOnAndOff_turnsOnAndOff() {
    machine.accept(Event.STOMP);
    machine.accept(Event.KICK);
    machine.accept(Event.KICK);
    machine.accept(Event.STOMP);

    var inOrder = Mockito.inOrder(mockHardwareAbstractionLayer);
    inOrder.verify(mockHardwareAbstractionLayer).turnOn();
    inOrder.verify(mockHardwareAbstractionLayer).setColor(Color.RED);
    inOrder.verify(mockHardwareAbstractionLayer).setColor(Color.ORANGE);
    inOrder.verify(mockHardwareAbstractionLayer).turnOff();
    inOrder.verifyNoMoreInteractions();
  }
}
