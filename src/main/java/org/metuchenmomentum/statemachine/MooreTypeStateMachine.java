package org.metuchenmomentum.statemachine;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A
 * <a href='https://en.wikipedia.org/wiki/Moore_machine'>Moore type</a>
 * state machine whose actions run when the machine transitions
 * <em>into</em>. This includes self transitions, i.e. transitions
 * from a state {@code s} into the same state {@code s}. Unlike
 * the classic Moore design, the {@code MooreTypeStateMachine}
 * performs arbitrary actions on state entry instead of merely
 * emitting symbols.
 *
 * @param <S> state enumeration
 * @param <E> event enumeration
 */
public final class MooreTypeStateMachine<S extends Enum<S>, E extends Enum<E>>
    implements Consumer<E> {

  private final StateTransitionTable<S, E> transitionTable;
  private final Consumer<S> stateEntryAction;
  private S currentState;

  /**
   * Constructs a new {@code MooreTypeStateMachine} from the provided
   * values. Note that the values <em>MUST NOT</em> be {@code null}.
   *
   * @param transitionTable the machine's transition table
   * @param initialState the machine's initial state
   * @param stateEntryAction a {@link Consumer<S>} that is
   *                         invoked at state transition,
   *                         even upon transitions from a
   *                         state to itself.
   */
  public MooreTypeStateMachine(
      StateTransitionTable<S, E> transitionTable,
      S initialState,
      Consumer<S> stateEntryAction) {
    this.transitionTable = Preconditions.checkNotNull(
        transitionTable, "Transition table cannot be null.");
    this.stateEntryAction = Preconditions.checkNotNull(
        stateEntryAction, "State entry action must not be null.");
    currentState = Preconditions.checkNotNull(
        initialState, "Initial state cannot be null.");
  }

  /**
   * Accept the specified event. If the event forces a state transition,
   * even a transition into the current state, change to the resulting
   * state3 and perform the action associated with it.
   *
   * @param event incoming event.
   */
  @Override
  public void accept(E event) {
    Optional<S> maybeNewState = transitionTable.maybeTransition(
        currentState,
        Preconditions.checkNotNull(event, "Event cannot be null."));
    if (maybeNewState.isPresent()) {
      currentState = maybeNewState.get();
      stateEntryAction.accept(currentState);
    }
  }
}
