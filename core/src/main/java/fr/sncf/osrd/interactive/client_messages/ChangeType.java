package fr.sncf.osrd.interactive.client_messages;

import fr.sncf.osrd.infra_state.routes.RouteState;
import fr.sncf.osrd.interactive.changes_adapters.SerializedChange;
import fr.sncf.osrd.interactive.changes_adapters.SerializedRouteStatus;
import fr.sncf.osrd.simulation.Change;
import java.util.HashMap;
import java.util.Map;

public enum ChangeType {
    ROUTE_STATUS(RouteState.RouteStatusChange.class, SerializedRouteStatus.class);

    private static final Map<Class<? extends Change>, ChangeType> changeTypeMap = new HashMap<>();

    static {
        for (var changeType : ChangeType.values())
            changeTypeMap.put(changeType.internalChangeType, changeType);
    }

    public final Class<? extends Change> internalChangeType;
    public final Class<? extends SerializedChange> serializedChangeType;

    ChangeType(Class<? extends Change> internalChangeType, Class<? extends SerializedChange> serializedChangeType) {
        this.internalChangeType = internalChangeType;
        this.serializedChangeType = serializedChangeType;
    }

    public static ChangeType fromChange(Change change) {
        return fromInternalType(change.getClass());
    }

    public static ChangeType fromInternalType(Class<? extends Change> internalType) {
        return changeTypeMap.get(internalType);
    }

}
