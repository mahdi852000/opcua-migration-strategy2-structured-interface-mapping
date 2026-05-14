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
        final String WRAPPER_OBJECT = "LegacyPLC_Wrapper";
        System.out.println("Creating LegacyMachine namespace nodes...");

        UaObjectNode machineNode = UaObjectNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT))
                .setBrowseName(newQualifiedName(WRAPPER_OBJECT))
                .setDisplayName(LocalizedText.english(WRAPPER_OBJECT))
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

        currentStateNode = UaVariableNode.build(
                getNodeContext(),
                builder->builder
                        .setNodeId(newNodeId(WRAPPER_OBJECT + "/STS_CURRENT_STATE"))
                        .setAccessLevel(AccessLevel.READ_WRITE)
                        .setUserAccessLevel(AccessLevel.READ_WRITE)
                        .setBrowseName(newQualifiedName( "STS_CURRENT_STATE"))
                        .setDisplayName(LocalizedText.english("STS_CURRENT_STATE"))
                        .setDataType(NodeIds.String)
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .build()
        );

        currentStateNode.setValue(
                new DataValue(
                        new Variant(simulator.getCurrentState().name())
                )
        );
        getNodeManager().addNode(currentStateNode);
        machineNode.addComponent(currentStateNode);


        isRunningNode = addVariable(
                machineNode, WRAPPER_OBJECT, "STS_IS_RUNNING", NodeIds.Boolean, simulator.isRunning());
        isIdleNode = addVariable(
                machineNode, WRAPPER_OBJECT, "STS_IS_IDLE", NodeIds.Boolean, simulator.isIdle());
        hasFaultNode = addVariable(
                machineNode, WRAPPER_OBJECT, "STS_HAS_FAULT", NodeIds.Boolean, simulator.hasFault());
        cycleActiveNode = addVariable(
                machineNode, WRAPPER_OBJECT, "STS_CYCLE_ACTIVE", NodeIds.Boolean, simulator.isCycleActive());

        operationModeNode = addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "STS_OPERATION_MODE",
                NodeIds.String,
                simulator.getOperationMode()
        );

        temperatureNode = addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "STS_TEMPERATURE",
                NodeIds.Double,
                simulator.getTemperature()
        );

        connectionHealthNode = addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "STS_CONNECTION_HEALTH",
                NodeIds.String,
                simulator.getConnectionHealth()
        );


        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "CFG_TARGET_SPEED",
                NodeIds.Double,
                simulator.getTargetSpeed()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "CFG_ACCELERATION_LIMIT",
                NodeIds.Double,
                simulator.getAccelerationLimit()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "CFG_TIMEOUT",
                NodeIds.Int32,
                simulator.getTimeout()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "CFG_RETRY_COUNT",
                NodeIds.Int32,
                simulator.getRetryCount()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "CFG_THRESHOLD",
                NodeIds.Double,
                simulator.getThreshold()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "DIAG_ERROR_CODE",
                NodeIds.Int32,
                simulator.getErrorCode()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "DIAG_WARNING_CODE",
                NodeIds.Int32,
                simulator.getWarningCode()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "DIAG_COMM_RETRY_COUNTER",
                NodeIds.Int32,
                simulator.getCommunicationRetryCounter()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "DIAG_UPTIME_SECONDS",
                NodeIds.Int64,
                simulator.getUptimeSeconds()
        );

        addVariable(
                machineNode,
                WRAPPER_OBJECT,
                "ID_DEVICE_IDENTITY",
                NodeIds.String,
                simulator.getDeviceIdentity()
        );

        UaMethodNode startMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT + "/CMD_START"))
                .setBrowseName(newQualifiedName("CMD_START"))
                .setDisplayName(LocalizedText.english("CMD_START"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        startMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                System.out.println("Start method called from OPC UA client.");
                simulator.start();
                updateStatusNodes();
                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(startMethod);
        machineNode.addComponent(startMethod);

        UaMethodNode stopMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT + "/CMD_STOP"))
                .setBrowseName(newQualifiedName("CMD_STOP"))
                .setDisplayName(LocalizedText.english("CMD_STOP"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        stopMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {

                System.out.println("Stop method called from OPC UA client.");

                simulator.stop();

                updateStatusNodes();

                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(stopMethod);
        machineNode.addComponent(stopMethod);

        UaMethodNode resetMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT + "/CMD_RESET"))
                .setBrowseName(newQualifiedName("CMD_RESET"))
                .setDisplayName(LocalizedText.english("CMD_RESET"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        resetMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {

                System.out.println("Reset method called from OPC UA client.");

                simulator.reset();

                updateStatusNodes();

                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(resetMethod);
        machineNode.addComponent(resetMethod);
        UaMethodNode pauseMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT + "/CMD_PAUSE"))
                .setBrowseName(newQualifiedName("CMD_PAUSE"))
                .setDisplayName(LocalizedText.english("CMD_PAUSE"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        pauseMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                System.out.println("Pause method called from OPC UA client.");

                simulator.pause();

                updateStatusNodes();

                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(pauseMethod);
        machineNode.addComponent(pauseMethod);

        UaMethodNode resumeMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT + "/CMD_RESUME"))
                .setBrowseName(newQualifiedName("CMD_RESUME"))
                .setDisplayName(LocalizedText.english("CMD_RESUME"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        resumeMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                System.out.println("Resume method called from OPC UA client.");

                simulator.resume();

                updateStatusNodes();

                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(resumeMethod);
        machineNode.addComponent(resumeMethod);

        UaMethodNode homeMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId(WRAPPER_OBJECT + "/CMD_HOME"))
                .setBrowseName(newQualifiedName("CMD_HOME"))
                .setDisplayName(LocalizedText.english("CMD_HOME"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        homeMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                System.out.println("Home method called from OPC UA client.");

                simulator.home();

                updateStatusNodes();

                return new CallMethodResult(
                        new StatusCode(StatusCodes.Good),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                );
            }
        });

        getNodeManager().addNode(homeMethod);
        machineNode.addComponent(homeMethod);

    }
    private void updateStatusNodes() {
        currentStateNode.setValue(new DataValue(new Variant(simulator.getCurrentState().name())));
        isRunningNode.setValue(new DataValue(new Variant(simulator.isRunning())));
        isIdleNode.setValue(new DataValue(new Variant(simulator.isIdle())));
        hasFaultNode.setValue(new DataValue(new Variant(simulator.hasFault())));
        operationModeNode.setValue(new DataValue(new Variant(simulator.getOperationMode())));
        temperatureNode.setValue(new DataValue(new Variant(simulator.getTemperature())));
        connectionHealthNode.setValue(new DataValue(new Variant(simulator.getConnectionHealth())));
        cycleActiveNode.setValue(new DataValue(new Variant(simulator.isCycleActive())));
    }
    private UaVariableNode addVariable(
            UaObjectNode parent,
            String parentPath,
            String name,
            org.eclipse.milo.opcua.stack.core.types.builtin.NodeId dataType,
            Object value
    ) {
        UaVariableNode variableNode = UaVariableNode.build(
                getNodeContext(),
                builder -> builder
                        .setNodeId(newNodeId(parentPath+ "/" + name))
                        .setAccessLevel(AccessLevel.READ_WRITE)
                        .setUserAccessLevel(AccessLevel.READ_WRITE)
                        .setBrowseName(newQualifiedName(name))
                        .setDisplayName(LocalizedText.english(name))
                        .setDataType(dataType)
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .build()
        );

        variableNode.setValue(new DataValue(new Variant(value)));

        getNodeManager().addNode(variableNode);
        parent.addComponent(variableNode);

        return variableNode;
    }


    @Override
    public void onDataItemsCreated(List<DataItem> list) {
        // Not required for PoC
    }

    @Override
    public void onDataItemsModified(List<DataItem> list) {
        // Not required for PoC
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> list) {
        // Not required for PoC
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> list) {
        // Not required for PoC
    }
}