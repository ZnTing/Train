package fr.sncf.osrd.infra.trackgraph;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.infra.railscript.value.RSMatchable;
import fr.sncf.osrd.simulation.*;

public class Switch extends TrackNode {
    public final int switchIndex;

    Switch(TrackGraph graph, int index, String id, int switchIndex) {
        super(index, id);
        this.switchIndex = switchIndex;
        graph.registerNode(this);
    }

    public Switch.State newState() {
        return new Switch.State(this);
    }

    public static final class SwitchEntityID implements EntityID<Switch.State> {
        private final int switchIndex;

        public SwitchEntityID(int switchIndex) {
            this.switchIndex = switchIndex;
        }

        @Override
        public State getEntity(Simulation sim) {
            return sim.infraState.getSwitchState(switchIndex);
        }
    }

    /** The state of the route is the actual entity which interacts with the rest of the infrastructure */
    @SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static final class State extends AbstractEntity<Switch.State> implements RSMatchable {
        public final Switch switchRef;
        private SwitchPosition position;

        State(Switch switchRef) {
            super(new SwitchEntityID(switchRef.switchIndex));
            this.switchRef = switchRef;
            // TODO: configurable default position
            this.position = SwitchPosition.LEFT;
        }

        public SwitchPosition getPosition() {
            return position;
        }

        /** Change position of the switch */
        public void setPosition(SwitchPosition position, Simulation sim) throws SimulationError {
            if (this.position != position) {
                this.position = position;
                sim.createEvent(this, 0, new Switch.SwitchUpdateEvent());
            }
        }

        @Override
        public void onTimelineEventUpdate(
                Simulation sim, TimelineEvent<?> event, TimelineEvent.State state
        ) throws SimulationError {
        }

        @Override
        public int getEnumValue() {
            return position.ordinal();
        }
    }

    public static class SwitchUpdateEvent implements TimelineEventValue { }
}
