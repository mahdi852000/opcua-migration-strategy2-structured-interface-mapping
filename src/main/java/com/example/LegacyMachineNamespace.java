package com.example;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import java.util.List;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.AccessContext;
import org.eclipse.milo.opcua.sdk.server.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;


public class LegacyMachineNamespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:com:example:legacy-machine";

    private final LegacyMachineSimulator simulator;

    // Status variable nodes tracked for update
    private UaVariableNode currentStateNode;
    private UaVariableNode isRunningNode;
    private UaVariableNode isIdleNode;
    private UaVariableNode hasFaultNode;
    private UaVariableNode cycleActiveNode;
    private UaVariableNode operationModeNode;
    private UaVariableNode temperatureNode;
    private UaVariableNode connectionHealthNode;

    public LegacyMachineNamespace(OpcUaServer server, LegacyMachineSimulator simulator) {
        super(server, NAMESPACE_URI);
        this.simulator = simulator;
        getLifecycleManager().addStartupTask(this::createNodes);
    }

    private void createNodes() {
        final String ROOT = "LegacyPLC_StructuredMapping";
        System.out.println("Creating Structured Mapping namespace nodes...");

        // ── Root device object (BaseObjectType — no custom type defined) ──────────
        UaObjectNode machineNode = UaObjectNode.builder(getNodeContext())
                .setNodeId(newNodeId(ROOT))
                .setBrowseName(newQualifiedName(ROOT))
                .setDisplayName(LocalizedText.english(ROOT))
                .setTypeDefinition(NodeIds.BaseObjectType)
                .build();

        getNodeManager().addNode(machineNode);

        machineNode.addReference(
                new org.eclipse.milo.opcua.sdk.core.Reference(
                        machineNode.getNodeId(),
                        NodeIds.Organizes,
                        NodeIds.ObjectsFolder.expanded(),
                        false
                )
        );

        // ── Functional group objects ──────────────────────────────────────────────
        // Using BaseObjectType for groups reflects that Strategy 2 introduces
        // structural organization without defining reusable semantic type definitions.
        UaObjectNode commandsGroup      = createGroup(machineNode, ROOT, "Commands");
        UaObjectNode statusGroup        = createGroup(machineNode, ROOT, "Status");
        UaObjectNode configurationGroup = createGroup(machineNode, ROOT, "Configuration");
        UaObjectNode diagnosticsGroup   = createGroup(machineNode, ROOT, "Diagnostics");
        UaObjectNode identityGroup = createGroup(machineNode,ROOT,"Identity");

        // ── Status group variables ────────────────────────────────────────────────
        currentStateNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_CURRENT_STATE",
                NodeIds.String, simulator.getCurrentState().name());

        isRunningNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_IS_RUNNING",
                NodeIds.Boolean, simulator.isRunning());

        isIdleNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_IS_IDLE",
                NodeIds.Boolean, simulator.isIdle());

        hasFaultNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_HAS_FAULT",
                NodeIds.Boolean, simulator.hasFault());

        cycleActiveNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_CYCLE_ACTIVE",
                NodeIds.Boolean, simulator.isCycleActive());

        operationModeNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_OPERATION_MODE",
                NodeIds.String, simulator.getOperationMode());

        temperatureNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_TEMPERATURE",
                NodeIds.Double, simulator.getTemperature());

        connectionHealthNode = addVariable(
                statusGroup, ROOT + "/Status", "STS_CONNECTION_HEALTH",
                NodeIds.String, simulator.getConnectionHealth());

        // ── Configuration group variables ─────────────────────────────────────────
        addVariable(configurationGroup, ROOT + "/Configuration",
                "CFG_TARGET_SPEED",        NodeIds.Double, simulator.getTargetSpeed());
        addVariable(configurationGroup, ROOT + "/Configuration",
                "CFG_ACCELERATION_LIMIT",  NodeIds.Double, simulator.getAccelerationLimit());
        addVariable(configurationGroup, ROOT + "/Configuration",
                "CFG_TIMEOUT",             NodeIds.Int32,  simulator.getTimeout());
        addVariable(configurationGroup, ROOT + "/Configuration",
                "CFG_RETRY_COUNT",         NodeIds.Int32,  simulator.getRetryCount());
        addVariable(configurationGroup, ROOT + "/Configuration",
                "CFG_THRESHOLD",           NodeIds.Double, simulator.getThreshold());

        // ── Diagnostics group variables ───────────────────────────────────────────
        addVariable(diagnosticsGroup, ROOT + "/Diagnostics",
                "DIAG_ERROR_CODE",          NodeIds.Int32, simulator.getErrorCode());
        addVariable(diagnosticsGroup, ROOT + "/Diagnostics",
                "DIAG_WARNING_CODE",        NodeIds.Int32, simulator.getWarningCode());
        addVariable(diagnosticsGroup, ROOT + "/Diagnostics",
                "DIAG_COMM_RETRY_COUNTER",  NodeIds.Int32, simulator.getCommunicationRetryCounter());
        addVariable(diagnosticsGroup, ROOT + "/Diagnostics",
                "DIAG_UPTIME_SECONDS",      NodeIds.Int64, simulator.getUptimeSeconds());

        // ── Identification group variable ─────────────────────────────────────────
        addVariable(identityGroup, ROOT + "/Identity",
                "ID_DEVICE_IDENTITY", NodeIds.String, simulator.getDeviceIdentity());

        // ── Commands group methods ────────────────────────────────────────────────
        addMethod(commandsGroup, ROOT + "/Commands", "CMD_START",
                () -> { simulator.start();  updateStatusNodes(); });
        addMethod(commandsGroup, ROOT + "/Commands", "CMD_STOP",
                () -> { simulator.stop();   updateStatusNodes(); });
        addMethod(commandsGroup, ROOT + "/Commands", "CMD_RESET",
                () -> { simulator.reset();  updateStatusNodes(); });
        addMethod(commandsGroup, ROOT + "/Commands", "CMD_PAUSE",
                () -> { simulator.pause();  updateStatusNodes(); });
        addMethod(commandsGroup, ROOT + "/Commands", "CMD_RESUME",
                () -> { simulator.resume(); updateStatusNodes(); });
        addMethod(commandsGroup, ROOT + "/Commands", "CMD_HOME",
                () -> { simulator.home();   updateStatusNodes(); });
    }

    // ── Helper: create a child Object node (BaseObjectType) ──────────────────────
    private UaObjectNode createGroup(UaObjectNode parent, String parentPath, String name) {
        UaObjectNode groupNode = UaObjectNode.builder(getNodeContext())
                .setNodeId(newNodeId(parentPath + "/" + name))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setTypeDefinition(NodeIds.BaseObjectType)
                .build();
        getNodeManager().addNode(groupNode);
        parent.addComponent(groupNode);
        return groupNode;
    }

    // ── Helper: create a Variable node and attach it to a parent ─────────────────
    private UaVariableNode addVariable(
            UaObjectNode parent, String parentPath, String name,
            org.eclipse.milo.opcua.stack.core.types.builtin.NodeId dataType, Object value) {

        UaVariableNode node = UaVariableNode.build(
                getNodeContext(),
                builder -> builder
                        .setNodeId(newNodeId(parentPath + "/" + name))
                        .setAccessLevel(AccessLevel.READ_WRITE)
                        .setUserAccessLevel(AccessLevel.READ_WRITE)
                        .setBrowseName(newQualifiedName(name))
                        .setDisplayName(LocalizedText.english(name))
                        .setDataType(dataType)
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .build()
        );
        node.setValue(new DataValue(new Variant(value)));
        getNodeManager().addNode(node);
        parent.addComponent(node);
        return node;
    }

    // ── Helper: create a Method node and attach it to a parent ───────────────────
    private void addMethod(UaObjectNode parent, String parentPath, String name, Runnable action) {
        UaMethodNode method = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(parentPath + "/" + name))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        method.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext ctx, CallMethodRequest request) {
                System.out.println(name + " called from OPC UA client.");
                action.run();
                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(method);
        parent.addComponent(method);
    }

    // ── Refresh all status variable values after a command ────────────────────────
    private void updateStatusNodes() {
        currentStateNode.setValue(new DataValue(new Variant(simulator.getCurrentState().name())));
        isRunningNode.setValue(new DataValue(new Variant(simulator.isRunning())));
        isIdleNode.setValue(new DataValue(new Variant(simulator.isIdle())));
        hasFaultNode.setValue(new DataValue(new Variant(simulator.hasFault())));
        cycleActiveNode.setValue(new DataValue(new Variant(simulator.isCycleActive())));
        operationModeNode.setValue(new DataValue(new Variant(simulator.getOperationMode())));
        temperatureNode.setValue(new DataValue(new Variant(simulator.getTemperature())));
        connectionHealthNode.setValue(new DataValue(new Variant(simulator.getConnectionHealth())));
    }

    @Override
    public void onDataItemsCreated(List<DataItem> list) {}

    @Override
    public void onDataItemsModified(List<DataItem> list) {}

    @Override
    public void onDataItemsDeleted(List<DataItem> list) {}

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> list) {}
}
