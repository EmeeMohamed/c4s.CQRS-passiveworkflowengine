package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TIMWorkItem {
    private String name;
    private List<Trace> traces;

    public TIMWorkItem(String name) {
        this.name = name;
        traces = new ArrayList<Trace>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setTrace(Trace trace) { this.traces.add(trace); }
    public List<Trace> getTraces() { return traces; }


}

class Trace {
    private String name;
    private TIMWorkItem endpointName;

    public Trace() {}

    public Trace(String name, TIMWorkItem endpointName) {
        this.name = name;
        this.endpointName = endpointName;
    }
    public String getName() { return name; }
    public TIMWorkItem getEndpointName() { return endpointName; }

    public void setName(String name) { this.name = name; }
    public void setEndpointName(TIMWorkItem endpointName) { this.endpointName = endpointName; }

}
