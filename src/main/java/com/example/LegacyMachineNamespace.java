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

    public LegacyMachineNamespace(OpcUaServer server, LegacyMachineSimulator simulator) {
        super(server, NAMESPACE_URI);

        this.simulator = simulator;

        getLifecycleManager().addStartupTask(this::createNodes);
    }

    private void createNodes() {
        System.out.println("Creating LegacyMachine namespace nodes...");
        UaObjectNode machineNode = UaObjectNode.builder(getNodeContext())
                .setNodeId(newNodeId("LegacyMachine_1"))
                .setBrowseName(newQualifiedName("LegacyMachine_1"))
                .setDisplayName(LocalizedText.english("LegacyMachine_1"))
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
                        .setNodeId(newNodeId("LegacyMachine_1/CurrentState"))
                        .setAccessLevel(AccessLevel.READ_WRITE)
                        .setUserAccessLevel(AccessLevel.READ_WRITE)
                        .setBrowseName(newQualifiedName("CurrentState"))
                        .setDisplayName(LocalizedText.english("CurrentState"))
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
        addVariable(
                machineNode,
                "IsRunning",
                NodeIds.Boolean,
                simulator.isRunning()
        );

        addVariable(
                machineNode,
                "ErrorCode",
                NodeIds.Int32,
                simulator.getErrorCode()
        );

        addVariable(
                machineNode,
                "TargetSpeed",
                NodeIds.Double,
                simulator.getTargetSpeed()
        );
        UaMethodNode startMethod = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId("LegacyMachine_1/Start"))
                .setBrowseName(newQualifiedName("Start"))
                .setDisplayName(LocalizedText.english("Start"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        startMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                System.out.println("Start method called from OPC UA client.");
                simulator.start();
                currentStateNode.setValue(
                        new DataValue(new Variant(simulator.getCurrentState().name()))
                );
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
                .setNodeId(newNodeId("LegacyMachine_1/Stop"))
                .setBrowseName(newQualifiedName("Stop"))
                .setDisplayName(LocalizedText.english("Stop"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        stopMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {

                System.out.println("Stop method called from OPC UA client.");

                simulator.stop();

                currentStateNode.setValue(
                        new DataValue(
                                new Variant(simulator.getCurrentState().name())
                        )
                );

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
                .setNodeId(newNodeId("LegacyMachine_1/Reset"))
                .setBrowseName(newQualifiedName("Reset"))
                .setDisplayName(LocalizedText.english("Reset"))
                .setExecutable(true)
                .setUserExecutable(true)
                .build();

        resetMethod.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {

                System.out.println("Reset method called from OPC UA client.");

                simulator.reset();

                currentStateNode.setValue(
                        new DataValue(
                                new Variant(simulator.getCurrentState().name())
                        )
                );

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
    }








    private void addVariable(
            UaObjectNode parent,
            String name,
            org.eclipse.milo.opcua.stack.core.types.builtin.NodeId dataType,
            Object value
    ) {
        UaVariableNode variableNode = UaVariableNode.build(
                getNodeContext(),
                builder -> builder
                        .setNodeId(newNodeId("LegacyMachine_1/" + name))
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