package org.metuchenmomentum.statemachine;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Validates {@link StateTransitionTable}
 *
 * <p>The test uses two transition tables, one loosely based on a robotics
 * cycle, the other loosely based on the Metuchen Momentum 2025 robot.
 *
 * <p></p>Note that this test runs under
 * a <a href='https://junit.org/junit5/docs/current/user-guide/'>JUnit5</a>.
 * Please see the foregoing link for details.
 */
class StateTransitionTableTest {
  enum RoboticsState {
    SEASON_STARTING,
    PRE_SEASON,
    REVEAL,
    DESIGNING,
    BUILDING,
    COMPETING,
  }

  enum RoboticsEvent {
    SCHOOL_START,
    REVEAL,
    HAVE_DESIGN,
    ROBOT_COMPLETE,
    SEASON_OVER,
  }

  enum ElevatorState {
    BOTTOM,
    PLATFORM,
    LOW_POLE,
    MIDDLE_POLE,
    TOP_POLE
  }

  enum ElevatorMovement {
    UP,
    DOWN
  }

  /**
   * Verifies that an empty table returns an empty value for all events
   * in all states. This test is vacuously based on the robotics season.
   */
  @Test
  public void emptyTableEverythingIgnored() {
    StateTransitionTable<RoboticsState, RoboticsEvent> tableUnderTest =
        StateTransitionTable.builder(RoboticsState.class, RoboticsEvent.class)
            .build();
    for (var currentState: RoboticsState.values()) {
      for (var currentEvent : RoboticsEvent.values()) {
        assertThat(tableUnderTest.maybeTransition(currentState, currentEvent)).isEmpty();
      }
    }
  }

  /**
   * Verify behavior of a logically complete transition table. The
   * table is loosely based on the 2025 robot, which has a four position
   * elevator (if you consider being at the bottom a valid position). Unlike
   * the real elevator, the modelled elevator moves up and down one position
   * at a time, which would clearly be silly in competition.
   */
  @Test
  public void populatedTransitionTable() {
    StateTransitionTable<ElevatorState, ElevatorMovement> tableUnderTest =
        StateTransitionTable.builder(ElevatorState.class, ElevatorMovement.class)
            .addTransition(ElevatorState.BOTTOM, ElevatorMovement.UP, ElevatorState.PLATFORM)
            .addTransition(ElevatorState.PLATFORM, ElevatorMovement.UP, ElevatorState.LOW_POLE)
            .addTransition(ElevatorState.LOW_POLE, ElevatorMovement.UP, ElevatorState.MIDDLE_POLE)
            .addTransition(ElevatorState.MIDDLE_POLE, ElevatorMovement.UP, ElevatorState.TOP_POLE)

            .addTransition(ElevatorState.TOP_POLE, ElevatorMovement.DOWN, ElevatorState.MIDDLE_POLE)
            .addTransition(ElevatorState.MIDDLE_POLE, ElevatorMovement.DOWN, ElevatorState.LOW_POLE)
            .addTransition(ElevatorState.LOW_POLE, ElevatorMovement.DOWN, ElevatorState.PLATFORM)
            .addTransition(ElevatorState.PLATFORM, ElevatorMovement.DOWN, ElevatorState.BOTTOM)
            .build();

    assertThat(tableUnderTest.maybeTransition(ElevatorState.BOTTOM, ElevatorMovement.UP))
        .hasValue(ElevatorState.PLATFORM);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.PLATFORM, ElevatorMovement.UP))
        .hasValue(ElevatorState.LOW_POLE);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.LOW_POLE, ElevatorMovement.UP))
        .hasValue(ElevatorState.MIDDLE_POLE);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.MIDDLE_POLE, ElevatorMovement.UP))
        .hasValue(ElevatorState.TOP_POLE);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.TOP_POLE, ElevatorMovement.UP))
        .isEmpty();

    assertThat(tableUnderTest.maybeTransition(ElevatorState.TOP_POLE, ElevatorMovement.DOWN))
        .hasValue(ElevatorState.MIDDLE_POLE);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.MIDDLE_POLE, ElevatorMovement.DOWN))
        .hasValue(ElevatorState.LOW_POLE);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.LOW_POLE, ElevatorMovement.DOWN))
        .hasValue(ElevatorState.PLATFORM);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.PLATFORM, ElevatorMovement.DOWN))
        .hasValue(ElevatorState.BOTTOM);
    assertThat(tableUnderTest.maybeTransition(ElevatorState.BOTTOM, ElevatorMovement.DOWN))
        .isEmpty();
  }
}
