package vasco;

import javafx.util.Pair;

import java.util.List;
import java.util.Set;


public abstract class BidirectionalAnalysis<M,N,F,B> extends InterProceduralAnalysis<M,N,Pair<F,B>> {
    /**
     * Constructs a new inter-procedural analysis.
     */
    Direction currentAnalysisDirection;
    public BidirectionalAnalysis(Direction firstDirection) {
        super(Direction.BI_DIRECTION);
        currentAnalysisDirection = firstDirection;
    }

    public abstract F forwardBoundaryValue(M entryPoint);
    public abstract B backwardBoundaryValue(M entryPoint);

    @Override
    public Pair<F, B> boundaryValue(M entryPoint) {
        return new Pair<>(forwardBoundaryValue(entryPoint), backwardBoundaryValue(entryPoint));
    }

    public abstract F forwardCopy(F src);
    public abstract B backwardCopy(B src);

    @Override
    public Pair<F, B> copy(Pair<F, B> src) {
        return new Pair<>(forwardCopy(src.getKey()), backwardCopy(src.getValue()));
    }

    @Override
    public Context<M, N, Pair<F, B>> getContext(M method, Pair<F, B> value) {
        // If this method does not have any contexts, then we'll have to return nothing.
        if (!contexts.containsKey(method)) {
            return null;
        }
        // Otherwise, look for a context in this method's list with the given value.
        // Forward flow, so check for ENTRY FLOWS
        for (Context<M,N,Pair<F,B>> context : contexts.get(method)) {
            if (value.getKey().equals(context.getEntryValue().getKey()) && value.getValue().equals(context.getExitValue().getValue())) {
                return context;
            }
        }
        return null;
    }

    public abstract F forwardMeet(F op1, F op2);
    public abstract B backwardMeet(B op1, B op2);

    @Override
    public Pair<F, B> meet(Pair<F, B> op1, Pair<F, B> op2) {
        return new Pair<>(forwardMeet(op1.getKey(), op2.getKey()), backwardMeet(op1.getValue(), op2.getValue()));
    }

    public abstract F forwardTopValue();
    public abstract B backwardTopValue();

    @Override
    public Pair<F, B> topValue() {
        return new Pair<>(forwardTopValue(), backwardTopValue());
    }

    @Override
    public void doAnalysis() {
        // Initial contexts
        for (M method : programRepresentation().getEntryPoints()) {
            initContext(method, boundaryValue(method));
        }
        // Perform work-list based analysis
        while(!forwardWorkList.isEmpty() || !backwardWorkList.isEmpty()) {
            while (!forwardWorkList.isEmpty()) {
                if(currentAnalysisDirection == Direction.BACKWARD)
                    break;
                if(forwardWorkList.isEmpty())
                {
                    currentAnalysisDirection = Direction.BACKWARD;
                    break;
                }
                // Get the newest context on the work-list
                Context<M, N, Pair<F, B>> currentContext = forwardWorkList.last();

                // If this context has no more nodes to analyze, then take it out of the work-list
                if (currentContext.getForwardWorkList().isEmpty()) {
                    currentContext.markAnalysed();
                    if(!currentContext.getBackwardWorkList().isEmpty())
                        backwardWorkList.add(currentContext);
                    forwardWorkList.remove(currentContext);
                    continue;
                }

                // Remove the next node to process from the context's work-list
                N node = currentContext.getForwardWorkList().pollFirst();

                if (node != null) {
                    // Compute the IN data flow value (only for non-entry units).
                    List<N> predecessors = currentContext.getControlFlowGraph().getPredsOf(node);
                    if (predecessors.size() != 0) {
                        // Initialise to the TOP value
                        F forwardIn = forwardTopValue();
                        // Merge OUT values of all predecessors
                        for (N predecessor : predecessors) {
                            F predecessorForwardOut = currentContext.getValueAfter(predecessor).getKey();
                            forwardIn = forwardMeet(forwardIn, predecessorForwardOut);
                        }
                        // Set the IN value at the node to the result
                        B backwardIn = currentContext.getValueBefore(node).getValue();
                        currentContext.setValueBefore(node, new Pair<F, B>(forwardIn,backwardIn));
                    }

                    // Store the value of OUT before the flow function is processed.
                    Pair<F,B> prevOut = currentContext.getValueAfter(node);

                    // Get the value of IN
                    F forwardIn = currentContext.getValueBefore(node).getKey();
                    B backwardIn = currentContext.getValueBefore(node).getValue();

                    if (verbose) {
                        System.out.println("FORWARD_IN = " + forwardIn);
                        System.err.println(node);
                    }

                    // Now to compute the OUT value
                    F forwardOut;

                    // Handle flow functions depending on whether this is a call statement or not
                    if (programRepresentation().isCall(node)) {

                        B backwardOut = currentContext.getValueAfter(node).getValue();
                        forwardOut = forwardTopValue();
                        boolean hit = false;
                        if (!programRepresentation().resolveTargets(currentContext.getMethod(), node).isEmpty()) {
                            for (M targetMethod : programRepresentation().resolveTargets(currentContext.getMethod(), node)) {
                                F entryValue = forwardCallEntryFlowFunction(currentContext, targetMethod, node, forwardIn, backwardIn);

                                CallSite<M, N, Pair<F,B>> callSite = new CallSite<M, N, Pair<F,B>>(currentContext, node);

                                // Check if the called method has a context associated with this entry flow:
                                Context<M, N, Pair<F,B>> targetContext = getContext(targetMethod, entryValue, );
                                // If not, then set 'targetContext' to a new context with the given entry flow.
                                if (targetContext == null) {
                                    targetContext = initContext(targetMethod, new Pair<F, B>(entryValue, backwardOut));
                                    if (verbose) {
                                        System.out.println("[NEW] X" + currentContext + " -> X" + targetContext + " " + targetMethod + " ");
                                        System.out.println("ENTRY(X" + targetContext + ") = " + entryValue);
                                    }

                                }

                                // Store the transition from the calling context and site to the called context.
                                contextTransitions.addTransition(callSite, targetContext);

                                // Check if the target context has been analysed (surely not if it is just newly made):
                                if (targetContext.isAnalysed()) {
                                    hit = true;
                                    Pair<F,B> exitValue = targetContext.getExitValue();
                                    if (verbose) {
                                        System.out.println("[HIT] X" + currentContext + " -> X" + targetContext + " " + targetMethod + " ");
                                        System.out.println("EXIT(X" + targetContext + ") = " + exitValue);
                                    }
                                    F returnedValue = forwardCallExitFlowFunction(currentContext, targetMethod, node, exitValue.getKey(), exitValue.getValue());
                                    forwardOut = forwardMeet(forwardOut, returnedValue);
                                }
                            }

                            // If there was at least one hit, continue propagation
                            if (hit) {
                                F localValue = forwardCallLocalFlowFunction(currentContext, node, forwardIn,backwardOut);
                                forwardOut = meet(forwardOut, localValue);
                            } else {
                                forwardOut = callLocalFlowFunction(currentContext, node, forwardIn);
                            }
                        } else {
                            // handle phantom method
                            forwardOut = callLocalFlowFunction(currentContext, node, forwardIn);
                        }
                    } else {
                        forwardOut = normalFlowFunction(currentContext, node, forwardIn);
                    }
                    if (verbose) {
                        System.out.println("OUT = " + forwardOut);
                        System.out.println("---------------------------------------");
                    }


                    // Merge with previous OUT to force monotonicity (harmless if flow functions are monotinic)
                    forwardOut = meet(forwardOut, prevOut);

                    // Set the OUT value
                    currentContext.setValueAfter(node, forwardOut);

                    // If OUT has changed...
                    if (forwardOut.equals(prevOut) == false) {
                        // Then add successors to the work-list.
                        for (N successor : currentContext.getControlFlowGraph().getSuccsOf(node)) {
                            currentContext.getWorkList().add(successor);
                        }
                    }
                    // If the unit is forwardIn TAILS, then we have at least one
                    // path to the end of the method, so add the NULL unit
                    if (currentContext.getControlFlowGraph().getTails().contains(node)) {
                        currentContext.getWorkList().add(null);
                    }
                } else {
                    // NULL unit, which means the end of the method.
                    assert (currentContext.getWorkList().isEmpty());

                    // Exit value is the merge of the OUTs of the tail nodes.
                    A exitValue = topValue();
                    for (N tailNode : currentContext.getControlFlowGraph().getTails()) {
                        A tailOut = currentContext.getValueAfter(tailNode);
                        exitValue = meet(exitValue, tailOut);
                    }

                    // Set the exit value of the context.
                    currentContext.setExitValue(exitValue);

                    // Mark this context as analysed at least once.
                    currentContext.markAnalysed();

                    // Add callers to work-list, if any
                    Set<CallSite<M, N, A>> callers = contextTransitions.getCallers(currentContext);
                    if (callers != null) {
                        for (CallSite<M, N, A> callSite : callers) {
                            // Extract the calling context and node from the caller site.
                            Context<M, N, A> callingContext = callSite.getCallingContext();
                            N callNode = callSite.getCallNode();
                            // Add the calling unit to the calling context's node work-list.
                            callingContext.getWorkList().add(callNode);
                            // Ensure that the calling context is on the context work-list.
                            worklist.add(callingContext);
                        }
                    }

                    // Free memory on-the-fly if not needed
                    if (freeResultsOnTheFly) {
                        Set<Context<M, N, A>> reachableContexts = contextTransitions.reachableSet(currentContext, true);
                        // If any reachable contexts exist on the work-list, then we cannot free memory
                        boolean canFree = true;
                        for (Context<M, N, A> reachableContext : reachableContexts) {
                            if (worklist.contains(reachableContext)) {
                                canFree = false;
                                break;
                            }
                        }
                        // If no reachable contexts on the stack, then free memory associated
                        // with this context
                        if (canFree) {
                            for (Context<M, N, A> reachableContext : reachableContexts) {
                                reachableContext.freeMemory();
                            }
                        }
                    }
                }


            }
        }
        // Sanity check
        for (List<Context<M,N,A>> contextList : contexts.values()) {
            for (Context<M,N,A> context : contextList) {
                if (context.isAnalysed() == false) {
                    System.err.println("*** ATTENTION ***: Only partial analysis of X" + context +
                            " " + context.getMethod());
                }
            }
        }
    }

    private Context<M,N,Pair<F,B>> initContext(M method, Pair<F, B> boundaryValue) {

    }


    /**
     * Processes the intra-procedural forward flow function of a statement that does
     * not contain a method call.
     *
     * @param context           the value context at the call-site
     * @param node              the statement whose flow function to process
     * @param forwardInValue    the forward data flow value before the statement
     * @param backwardInValue   the backward data flow value before the statement
     * @return                  the data flow value after the statement
     */
    public abstract F forwardFlowFunction(Context<M, N, Pair<F,B>> context, N node, F forwardInValue, B backwardInValue);

    /**
     * Processes the intra-procedural forward flow function of a statement that does
     * not contain a method call.
     *
     * @param context           the value context at the call-site
     * @param node              the statement whose flow function to process
     * @param forwardOutValue   the forward data flow value after the statement
     * @param backwardOutValue  the backward data flow value after the statement
     * @return                  the data flow value after the statement
     */
    public abstract B backwardFlowFunction(Context<M, N, Pair<F,B>> context, N node, B backwardOutValue, F forwardOutValue);

    /**
     * Processes the inter-procedural flow function for a method call at
     * the start of the call, to handle parameters.
     *
     * @param context           the value context at the call-site
     * @param targetMethod      the target (or one of the targets) of this call site
     * @param node              the statement containing the method call
     * @param forwardInValue    the forward data flow value before the call
     * @param backwardInValue   the forward data flow value before the call
     * @return                  the forward data flow value at the entry to the called procedure
     */
    public abstract F forwardCallEntryFlowFunction(Context<M,N,Pair<F,B>> context, M targetMethod, N node, F forwardInValue, B backwardInValue);

    /**
     * Processes the inter-procedural flow function for a method call at
     * the start of the call, to handle parameters.
     *
     * @param context           the value context at the call-site
     * @param targetMethod      the target (or one of the targets) of this call site
     * @param node              the statement containing the method call
     * @param forwardOutValue   the forward data flow value after the call
     * @param backwardOutValue  the backward data flow value after the call
     * @return                  the backward data flow value at the entry to the called procedure
     */
    public abstract B backwardCallEntryFlowFunction(Context<M,N,Pair<F,B>> context, M targetMethod, N node, B backwardOutValue, F forwardOutValue);

    /**
     * Processes the inter-procedural flow function for a method call at the
     * end of the call, to handle return values.
     *
     * @param context           the value context at the call-site
     * @param targetMethod      the target (or one of the targets) of this call site
     * @param node              the statement containing the method call
     * @param forwardExitValue  the forward data flow value at the exit of the called procedure
     * @param backwardExitValue the backward data flow value at the exit of the called procedure
     * @return                  the data flow value after the call (returned component)
     */
    public abstract F forwardCallExitFlowFunction(Context<M,N,Pair<F,B>> context, M targetMethod, N node, F forwardExitValue, B backwardExitValue);

    /**
     * Processes the inter-procedural flow function for a method call at the
     * end of the call, to handle return values.
     *
     * @param context        the value context at the call-site
     * @param targetMethod      the target (or one of the targets) of this call site
     * @param node              the statement containing the method call
     * @param forwardExitValue  the forward data flow value at the exit of the called procedure
     * @param backwardExitValue the backward data flow value at the exit of the called procedure
     * @return                  the data flow value after the call (returned component)
     */
    public abstract B backwardCallExitFlowFunction(Context<M,N,Pair<F,B>> context, M targetMethod, N node, B backwardExitValue,F forwardExitValue);

    /**
     *
     * Processes the intra-procedural flow function for a method call at the
     * call-site itself, to handle propagation of local values that are not
     * involved in the call.
     *
     * @param context           the value context at the call-site
     * @param node              the statement containing the method call
     * @param forwardOutValue   the data flow value after the call
     * @param backwardOutValue  the data flow value after the call
     * @return                  the data flow value before the call (local component)
     */
    public abstract F forwardCallLocalFlowFunction(Context<M,N,Pair<F,B>> context, N node, F forwardOutValue, B backwardOutValue);

    /**
     *
     * Processes the intra-procedural flow function for a method call at the
     * call-site itself, to handle propagation of local values that are not
     * involved in the call.
     *
     * @param context           the value context at the call-site
     * @param node              the statement containing the method call
     * @param forwardOutValue   the data flow value after the call
     * @param backwardOutValue  the data flow value after the call
     * @return                  the data flow value before the call (local component)
     */
    public abstract B backwardCallLocalFlowFunction(Context<M,N,Pair<F,B>> context, N node, B backwardOutValue, F forwardOutValue);
}
