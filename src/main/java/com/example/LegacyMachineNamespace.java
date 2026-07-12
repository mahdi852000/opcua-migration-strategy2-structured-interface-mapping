package com.example;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.ArrayList;
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
    private final LegacyMachineSimulator simulator2;
    private final List<UaNode> customNodes = new ArrayList<>();

    public LegacyMachineNamespace(OpcUaServer server, LegacyMachineSimulator simulator) {
        super(server, NAMESPACE_URI);
        this.simulator = simulator;
        this.simulator2 = new LegacyMachineSimulator("FeederController-Prototype-02");
        getLifecycleManager().addStartupTask(this::createNodes);
    }

    private void createNodes() {
        createMachineInstance("LegacyPLC_StructuredMapping", simulator,
                false);
        createMachineInstance("LegacyPLC_StructuredMapping_2", simulator2,
                true);
    }

    private void createMachineInstance(String rootName, LegacyMachineSimulator simulatorInstance, boolean withMaintenanceAlarm) {
        System.out.println("Creating Structured Mapping namespace nodes for " + rootName + "...");

        // ── Root device object (BaseObjectType — no custom type defined) ──────────
        UaObjectNode machineNode = UaObjectNode.builder(getNodeContext())
                .setNodeId(newNodeId(rootName))
                .setBrowseName(newQualifiedName(rootName))
                .setDisplayName(LocalizedText.english(rootName))
                .setTypeDefinition(NodeIds.BaseObjectType)
                .build();

        getNodeManager().addNode(machineNode);

        customNodes.add(machineNode);

        machineNode.addReference(
                new org.eclipse.milo.opcua.sdk.core.Reference(
                        machineNode.getNodeId(),
                        NodeIds.Organizes,
                        NodeIds.ObjectsFolder.expanded(),
                        false
                )
        );

        // ── Functional group objects ──────────────────────────────────────────────
        UaObjectNode commandsGroup      = createGroup(machineNode, rootName, "Commands");
        UaObjectNode statusGroup        = createGroup(machineNode, rootName, "Status");
        UaObjectNode configurationGroup = createGroup(machineNode, rootName, "Configuration");
        UaObjectNode diagnosticsGroup   = createGroup(machineNode, rootName, "Diagnostics");
        UaObjectNode identityGroup      = createGroup(machineNode, rootName, "Identity");

        // ── Status group variables (local, per-instance) ──────────────────────────
        final UaVariableNode currentStateNode = addVariable(
                statusGroup, rootName + "/Status", "STS_CURRENT_STATE",
                NodeIds.String, simulatorInstance.getCurrentState().name());

        final UaVariableNode isRunningNode = addVariable(
                statusGroup, rootName + "/Status", "STS_IS_RUNNING",
                NodeIds.Boolean, simulatorInstance.isRunning());

        final UaVariableNode isIdleNode = addVariable(
                statusGroup, rootName + "/Status", "STS_IS_IDLE",
                NodeIds.Boolean, simulatorInstance.isIdle());

        final UaVariableNode hasFaultNode = addVariable(
                statusGroup, rootName + "/Status", "STS_HAS_FAULT",
                NodeIds.Boolean, simulatorInstance.hasFault());

        final UaVariableNode cycleActiveNode = addVariable(
                statusGroup, rootName + "/Status", "STS_CYCLE_ACTIVE",
                NodeIds.Boolean, simulatorInstance.isCycleActive());

        final UaVariableNode operationModeNode = addVariable(
                statusGroup, rootName + "/Status", "STS_OPERATION_MODE",
                NodeIds.String, simulatorInstance.getOperationMode());

        final UaVariableNode temperatureNode = addVariable(
                statusGroup, rootName + "/Status", "STS_TEMPERATURE",
                NodeIds.Double, simulatorInstance.getTemperature());

        final UaVariableNode connectionHealthNode = addVariable(
                statusGroup, rootName + "/Status", "STS_CONNECTION_HEALTH",
                NodeIds.String, simulatorInstance.getConnectionHealth());
        if (withMaintenanceAlarm) {
            addVariable(statusGroup, rootName + "/Status",
                    "STS_MAINTENANCE_ALARM_ACTIVE", NodeIds.Boolean, false);
        }

        // ── Configuration group variables ─────────────────────────────────────────
        addVariable(configurationGroup, rootName + "/Configuration",
                "CFG_TARGET_SPEED",        NodeIds.Double, simulatorInstance.getTargetSpeed());
        addVariable(configurationGroup, rootName + "/Configuration",
                "CFG_ACCELERATION_LIMIT",  NodeIds.Double, simulatorInstance.getAccelerationLimit());
        addVariable(configurationGroup, rootName + "/Configuration",
                "CFG_TIMEOUT",             NodeIds.Int32,  simulatorInstance.getTimeout());
        addVariable(configurationGroup, rootName + "/Configuration",
                "CFG_RETRY_COUNT",         NodeIds.Int32,  simulatorInstance.getRetryCount());
        addVariable(configurationGroup, rootName + "/Configuration",
                "CFG_THRESHOLD",           NodeIds.Double, simulatorInstance.getThreshold());
        if (withMaintenanceAlarm) {
            addVariable(configurationGroup, rootName + "/Configuration",
                    "CFG_MAINTENANCE_ALARM_THRESHOLD", NodeIds.Double, 80.0);
        }

        // ── Diagnostics group variables ───────────────────────────────────────────
        addVariable(diagnosticsGroup, rootName + "/Diagnostics",
                "DIAG_ERROR_CODE",          NodeIds.Int32, simulatorInstance.getErrorCode());
        addVariable(diagnosticsGroup, rootName + "/Diagnostics",
                "DIAG_WARNING_CODE",        NodeIds.Int32, simulatorInstance.getWarningCode());
        addVariable(diagnosticsGroup, rootName + "/Diagnostics",
                "DIAG_COMM_RETRY_COUNTER",  NodeIds.Int32, simulatorInstance.getCommunicationRetryCounter());
        addVariable(diagnosticsGroup, rootName + "/Diagnostics",
                "DIAG_UPTIME_SECONDS",      NodeIds.Int64, simulatorInstance.getUptimeSeconds());



        // ── Identification group variable ─────────────────────────────────────────
        addVariable(identityGroup, rootName + "/Identity",
                "ID_DEVICE_IDENTITY", NodeIds.String, simulatorInstance.getDeviceIdentity());

        // ── Per-instance status refresh closure ───────────────────────────────────
        final Runnable updateStatus = () -> {
            currentStateNode.setValue(new DataValue(new Variant(simulatorInstance.getCurrentState().name())));
            isRunningNode.setValue(new DataValue(new Variant(simulatorInstance.isRunning())));
            isIdleNode.setValue(new DataValue(new Variant(simulatorInstance.isIdle())));
            hasFaultNode.setValue(new DataValue(new Variant(simulatorInstance.hasFault())));
            cycleActiveNode.setValue(new DataValue(new Variant(simulatorInstance.isCycleActive())));
            operationModeNode.setValue(new DataValue(new Variant(simulatorInstance.getOperationMode())));
            temperatureNode.setValue(new DataValue(new Variant(simulatorInstance.getTemperature())));
            connectionHealthNode.setValue(new DataValue(new Variant(simulatorInstance.getConnectionHealth())));
        };

        // ── Commands group methods ────────────────────────────────────────────────
        addMethod(commandsGroup, rootName + "/Commands", "CMD_START",
                () -> { simulatorInstance.start();  updateStatus.run(); });
        addMethod(commandsGroup, rootName + "/Commands", "CMD_STOP",
                () -> { simulatorInstance.stop();   updateStatus.run(); });
        addMethod(commandsGroup, rootName + "/Commands", "CMD_RESET",
                () -> { simulatorInstance.reset();  updateStatus.run(); });
        addMethod(commandsGroup, rootName + "/Commands", "CMD_PAUSE",
                () -> { simulatorInstance.pause();  updateStatus.run(); });
        addMethod(commandsGroup, rootName + "/Commands", "CMD_RESUME",
                () -> { simulatorInstance.resume(); updateStatus.run(); });
        addMethod(commandsGroup, rootName + "/Commands", "CMD_HOME",
                () -> { simulatorInstance.home();   updateStatus.run(); });
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
        customNodes.add(groupNode);
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
        customNodes.add(node);
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
        customNodes.add(method);
        parent.addComponent(method);
    }
    public List<UaNode> getCustomNodes() {
        return customNodes;
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