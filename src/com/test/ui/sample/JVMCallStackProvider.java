package com.test.ui.sample;



import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxPidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.internal.lttng2.ust.core.callstack.LttngUstCallStackProvider;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.layout.LttngUst20EventLayout;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;

import com.google.common.collect.ImmutableSet;


public class JVMCallStackProvider extends LttngUstCallStackProvider {

	 /**
     * Version number of this state provider. Please bump this if you modify
     * the contents of the generated state history in some way.
     */
    private static final int VERSION = 4;

    /** Event names indicating function entry */
    private final Set<String> funcEntryEvents;

    /** Event names indicating function exit */
    private final Set<String> funcExitEvents;

    private final ILttngUstEventLayout fLayout;
    
    public JVMCallStackProvider(ITmfTrace trace) {
        super(trace);
        if (trace instanceof JVMLttngTrace) {
            fLayout = ((JVMLttngTrace) trace).getEventLayout();
        } else {
            /* For impostor trace types, assume they use the LTTng 2.0 layout */
            fLayout = JVMLttngEventLayout.getInstance();
        }

        funcEntryEvents = ImmutableSet.of(
                fLayout.eventCygProfileFuncEntry(),
                fLayout.eventCygProfileFastFuncEntry());

        funcExitEvents = ImmutableSet.of(
                fLayout.eventCygProfileFuncExit(),
                fLayout.eventCygProfileFastFuncExit());
    }

    // ------------------------------------------------------------------------
    // Methods from AbstractTmfStateProvider
    // ------------------------------------------------------------------------

    @Override
    public LttngUstCallStackProvider getNewInstance() {
        return new LttngUstCallStackProvider(getTrace());
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    // ------------------------------------------------------------------------
    // Methods from CallStackStateProvider
    // ------------------------------------------------------------------------

    /**
     * Check that this event contains the required information we need to be
     * used in the call stack view. We need at least the "procname" and "vtid"
     * contexts.
     *
     * The "vpid" is useful too, but optional.
     */
    @Override
    protected boolean considerEvent(ITmfEvent event) {
    	if (!(event instanceof CtfTmfEvent)) {
            return false;
        } else {
        	return true;
        }
//        Object tid = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
//        return (tid instanceof Integer);
    }

    @Override
    public @Nullable ITmfStateValue functionEntry(ITmfEvent event) {
        String eventName = event.getName();
        if (!funcEntryEvents.contains(eventName)) {
            return null;
        }
        String address = (String) event.getContent().getField("method_name").getValue();
        return TmfStateValue.newValueString(address);
    }

    @Override
    public @Nullable ITmfStateValue functionExit(ITmfEvent event) {
        String eventName = event.getName();
        if (!funcExitEvents.contains(eventName)) {
            return null;
        }
        /*
         * The 'addr' field may or may not be present in func_exit events,
         * depending on if cyg-profile.so or cyg-profile-fast.so was used.
         */
//        ITmfEventField field = event.getContent().getField(fLayout.fieldAddr());
//        if (field == null) {
//            return TmfStateValue.nullValue();
//        }
//        Long address = (Long) field.getValue();
//        return TmfStateValue.newValueLong(address);
        String address = (String) event.getContent().getField("method_name").getValue();
        return TmfStateValue.newValueString(address);
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        /* We checked earlier that the "vtid" context is present */

        Integer pid = (Integer) event.getContent().getField("_context.vpid").getValue();
//        Integer pid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxPidAspect.class, event);
        if (pid == null) {
            return UNKNOWN_PID;
        }
        return pid;
    }

    @Override
    protected long getThreadId(ITmfEvent event) {
        /* We checked earlier that the "vtid" context is present */

        Integer tid = (Integer) event.getContent().getField("_context.tpid").getValue();
//        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            return UNKNOWN_PID;
        }
        return tid.longValue();
    }

    @Override
    public @Nullable String getThreadName(ITmfEvent event) {
        /* We checked earlier that the "procname" context is present */
        ITmfEventField content = event.getContent();
        ITmfEventField field = content.getField(fLayout.contextProcname());
        String procName = field == null ? StringUtils.EMPTY : (String.valueOf(field.getValue()) + '-');
        long vtid = getThreadId(event);
        return (procName + Long.toString(vtid));
    }
	
}
