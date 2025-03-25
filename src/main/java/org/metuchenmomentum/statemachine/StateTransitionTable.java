package org.metuchenmomentum.statemachine;

import java.util.EnumMap;
import java.util.Optional;

/**
 * A table that maps {@code (State, Event) -> State}.
 *
 * <p>The {@code StateTransitionTable} supports modified Moore
 * state machine by mapping {@code (State, Event)} pairs
 * to the machine's next {@code State}. Conceptually,
 * the table is a two-dimensional array of {@link Optional<S>}
 * indexed by {@code S} and {@code E} where
 * {@code table[s, 3]} is empty if the event should be
 * ignored, or the state to enter otherwise.
 *
 * <p>The basic state machine logic is:
 *
 * <ol>
 *   <li>Construct and populate the governing {@code StateTransitionTable},
 *       specifying the machine's initial state.
 *    <li>Take events. When an event is received.
 *    <ol>
 *       <li>Query the state transition table.
 *       <li>If the event should be ignored, do nothing.
 *       <li>Otherwise, the state machine should assume
 *           the returned state and perform the action
 *           that is bound to the state.
 *    </ol>
 * </ol>
 *
 * <p>A {@code switch} statement on the returned state is the
 * simplest way to bind actions to states. This yields
 * clear and compact code. See this class's unit test
 * for an example.
 *
 * <p>Note that this class encapsulates the bulk of a
 * state machine's logic. This class is immutable.
 *
 * @param <S> State enumeration. Must be an {@code enum}
 * @param <E> Event enumeration  Must be an {@code enum}
 */
public final class StateTransitionTable<S extends Enum<S>, E extends Enum<E>> {
  /**
   * The state transition table implemented with {@link java.util.Map Maps}. A
   * {@link java.util.Map Map<K, V>}, where both {@code K} and {@code V} <em>are
   * classes</em>, is a container that associates each
   * instance {@code K} with zero or one instance {@code V}. The
   * resulting map resembles a mathematical function, but
   * differs in two important aspects:
   * <ol>
   *   <li>Maps are <em>mutable</em>. Software can add and
   *       remove entries z needed. Mathematical functions,
   *       on the other hand, never change. The sine
   *       of pi/4 is always 0.</li>
   *   <li>Maps are <em>partial</em> in that values
   *       in its domain might not have corresponding
   *       values in the domain. For example, whose
   *       key is an {@link Integer} team number to
   *       team details is partial because there are
   *       {@link Integer} values that have yet to be
   *       assigned to teams.</li>
   * </ol>
   *
   * <p>The implementation associates current state values with
   * a set of responses to events. In other words, for each
   * instance of type {@code S} (i.e. the current state),
   * there is a map of values  of type {@code E} (i.e.
   * incoming event) to a new state. If the entry
   * keyed by event {@code e} has no associated state,
   * the event should be ignored.
   *
   * <p>We use an {@link java.util.EnumMap EnumMap} because it provides
   * extremely fast lookup.
   * <blockquote>
   *   Enum maps are represented internally as arrays. This representation
   *   is extremely compact and efficient.
   * </blockquote>
   * Consequently,
   * <ul>
   *   <li>Lookup takes place in constant time that is independent
   *   of the number of enumerations. What's more, array requires
   *   only three native operations
   *     <ol>
   *       <li>Fetch the index</li>
   *       <li>Multiply the index by the element size, if needed.</li>
   *       <li>Use the resulting offset to fetch the desired value.</li>
   *     </ol>
   *   </li>
   *   <li>{@link java.util.EnumMap EnumMaps} never need reindexing.
   *       Unlike their {@link java.util.HashMap HashMap} cousins,
   *       {@link java.util.EnumMap EnumMaps} do not have to rebuilt
   *       as elements are added. </li>
   * </ul>
   *
   * <p>When we think of maps as functions, the transition table
   * exhibits interesting mathematics. It's most straightforward
   * representation is a mapping of the current state and received
   * event to the new state, or, in math speak,
   * {@code (S, E) -> S}.
   * By contrast, storing the table in an {@code EnumMap<S, EnumMap<E, S>>}
   * represents it as a function
   * mapping values of {@code S} to functions values of {@code E} to values
   * of {@code S}. In math speak, this becomes{@code S -> (E -> S}.
   * We choose the latter for development and runtime speed. Had we
   * chosen to represent a single, two argument function (i.e. a
   * map keyed by an {@code S, E} pair, it would have worked
   * equally well because the two functions are mathematically
   * equivalent. However, we would have given up the
   * {@link EnumMap}'s runtime speed and need to write a custom
   * key class. Good software developers are lazy, which can
   * require a lot of work to achieve.
   *
   * <p>The transition table implementation illustrates
   * <a href='https://en.wikipedia.org/wiki/Currying'>currying</a>
   * the technique of translating a function taking
   * multiple arguments into a sequence of families of functions,
   * each taking a single argument.</p>
   */
  private final EnumMap<S, EnumMap<E, S>> transitionTable;
  /**
   * Creates a {@code #Builder} for {@link StateTransitionTable} for
   * state type {@code S} and event type {@code E}.
   *
   * @param stateClass {@code S.class}
   * @param eventClass {@code E.class}
   * @return an empty {@link Builder Builder<S, E>}
   * @param <S> the state enumeration
   * @param <E> the event enumeration
   */
  public static <S extends Enum<S>, E extends Enum<E>> Builder<S, E> builder(
      Class<S> stateClass, Class<E> eventClass) {
    return new Builder<>(stateClass, eventClass);
  }

  /**
   * Constructor, for use only by the Builder class.
   *
   * @param transitionTable fully populated transition table. The table will never
   *                        change.
   */
  private StateTransitionTable(EnumMap<S, EnumMap<E, S>> transitionTable) {
    this.transitionTable = transitionTable;
  }

  /**
   * Return the state transition, if any, that should occur when a state machine
   * in {@code currentState} receives the {@code event} event. We say "if any"
   * because the machine may ignore the event in its current state.
   *
   * @param currentState the machine's current state.
   * @param event the incoming event
   * @return An {@link Optional<S>} that contains the new state if a transition should
   *         occur. The returned {@link Optional} will be empty if the event should be
   *         ignored.
   */
  Optional<S> maybeTransition(S currentState, E event) {
      if (transitionTable.containsKey(currentState)
          && transitionTable.get(currentState).containsKey(event)) {
              return Optional.of(transitionTable.get(currentState).get(event));
          }
      return Optional.empty();
    }

  /**
   * Builder for {@link MooreTypeStateMachine} instances. Users add entries
   * to a {@link Builder}, then invoke its {@link #build()} method to create
   * the table.
   *
   * @param <S> state {@code enum} type
   * @param <E> event {@code enum} type
   */
  public static class Builder<S extends Enum<S>, E extends Enum<E>> {

    private final EnumMap<S, EnumMap<E, S>> transitionTable;
    private final Class<E> eventClass;

    /**
     * Create an empty table containing states of type {@code S}
     * and events of type {@code E}.
     *
     * @param stateClass state enumeration class {@code S.class}
     * @param eventClass event enumeration class {@code E.class}
     */
    private Builder(Class<S> stateClass, Class<E> eventClass) {
        this.eventClass = eventClass;
        transitionTable = new EnumMap<>(stateClass);
    }

    /**
     * Adds a state transition from state {@code from} to state {@code to}
     * in response to a {@code stimulus} event.
     *
     * <p>The effect of this method is to bind an {@code stimulus} ->
     *   {@code from} association to {@code from} that replaces any
     *   pre-existing binding.
     *
     * @param from current state
     * @param stimulus incoming event
     * @param to resulting state. Note that {@code from} -> {@code from}
     *           is a valid state transition.
     * @return {@code this} to support chaining.
     */
    public Builder<S, E> addTransition(S from, E stimulus, S to) {
        if (!transitionTable.containsKey(from)) {
            transitionTable.put(from, new EnumMap<>(eventClass));
        }
        transitionTable.get(from).put(stimulus, to);
        return this;
    }

    /**
     * Create a {@link StateTransitionTable StateTransitionTable<S, E}
     * from the transitions in this {@link Builder}.
     *
     * <p>This must be the <em>last</em> invocation of a {@link Builder}
     * method. Discard the {@link Builder} immediately after invoking
     * this method.
     *
     * @return the newly created {@link StateTransitionTable} as
     *         described above.
     */
    public StateTransitionTable<S, E> build() {
        return new StateTransitionTable<>(transitionTable);
    }
  }
}
