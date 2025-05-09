package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.Map;

public class TIMWorkItem {
    private String name;
    private Trace trace;

    public TIMWorkItem() {}

    public TIMWorkItem(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setTrace(Trace trace) { this.trace = trace; }
    public Trace getTrace() { return trace; }


}

class Trace {
    private String name;
    private String endpointName;

    public Trace() {}

    public Trace(String name, String endpointName) {
        this.name = name;
        this.endpointName = endpointName;
    }
    public String getName() { return name; }
    public String getEndpointName() { return endpointName; }



}

class TraceAssigner {
    private final Map<String, String> traceMapping;

    public TraceAssigner(Map<String, String> traceMapping) {
        this.traceMapping = traceMapping;
    }

    public Trace assignTraceByEndpointName(String endpointName) {
        String traceName = traceMapping.getOrDefault(endpointName, "unknown");

        return new Trace(traceName, endpointName);
    }

}
