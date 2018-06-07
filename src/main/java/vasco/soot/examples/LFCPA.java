//package vasco.soot.examples;
//
//import javafx.util.Pair;
//import soot.*;
//import soot.jimple.IntConstant;
//import soot.jimple.NewArrayExpr;
//import soot.jimple.internal.JNewArrayExpr;
//import vasco.BidirectionalAnalysis;
//import vasco.Context;
//import vasco.InterProceduralAnalysis;
//import vasco.ProgramRepresentation;
//import vasco.callgraph.PointsToGraph;
//import vasco.soot.DefaultJimpleRepresentation;
//
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//public class LFCPA extends BidirectionalAnalysis<SootMethod, Unit, PointsToGraph, Set<String>> {
//    public LFCPA(InterProceduralAnalysis.Direction firstDirection) {
//        super(firstDirection);
//    }
//
//    @Override
//    public ProgramRepresentation<SootMethod, Unit> programRepresentation() {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardBoundaryValue(SootMethod entryPoint) {
//        // For now we only support entry to the main method
//        assert(entryPoint == Scene.v().getMainMethod());
//
//        // Ok, start setting up entry value
//        PointsToGraph entryValue = new PointsToGraph();
//
//        // Locals of main... (only reference types)
//        SootMethod mainMethod = Scene.v().getMainMethod();
//        for (Local local : mainMethod.getActiveBody().getLocals()) {
//            if (local.getType() instanceof RefLikeType) {
//                entryValue.assign(local, null);
//            }
//        }
//
//        // Command-line arguments to main...
//        Local argsLocal = mainMethod.getActiveBody().getParameterLocal(0);
//        NewArrayExpr argsExpr = new JNewArrayExpr(Scene.v().getRefType("java.lang.String"), IntConstant.v(0));
//        entryValue.assignNew(argsLocal, argsExpr);
//        entryValue.setFieldConstant(argsLocal, PointsToGraph.ARRAY_FIELD, PointsToGraph.STRING_CONST);
//
//
//        return entryValue;
//    }
//
//    @Override
//    public Set<String> backwardBoundaryValue(SootMethod entryPoint) {
//        return new HashSet<String>() {
//        };
//    }
//
//    @Override
//    public PointsToGraph forwardCopy(PointsToGraph src) {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardCopy(Set<String> src) {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardMeet(PointsToGraph op1, PointsToGraph op2) {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardMeet(Set<String> op1, Set<String> op2) {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardTopValue() {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardTopValue() {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, Unit node, PointsToGraph forwardInValue, Set<String> backwardInValue) {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, Unit node, Set<String> backwardOutValue, PointsToGraph forwardOutValue) {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardCallEntryFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, SootMethod targetMethod, Unit node, PointsToGraph forwardInValue, Set<String> backwardInValue) {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardCallEntryFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, SootMethod targetMethod, Unit node, Set<String> backwardOutValue, PointsToGraph forwardOutValue) {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardCallExitFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, SootMethod targetMethod, Unit node, PointsToGraph forwardExitValue, Set<String> backwardExitValue) {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardCallExitFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, SootMethod targetMethod, Unit node, Set<String> backwardExitValue, PointsToGraph forwardExitValue) {
//        return null;
//    }
//
//    @Override
//    public PointsToGraph forwardCallLocalFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, Unit node, PointsToGraph forwardOutValue, Set<String> backwardOutValue) {
//        return null;
//    }
//
//    @Override
//    public Set<String> backwardCallLocalFlowFunction(Context<SootMethod, Unit, Pair<PointsToGraph, Set<String>>> context, Unit node, Set<String> backwardOutValue, PointsToGraph forwardOutValue) {
//        return null;
//    }
//}
