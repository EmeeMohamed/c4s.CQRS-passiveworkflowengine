package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.ArrayList;
import java.util.List;

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
    private String endpointName;

    public Trace(String name, String endpointName) {
        this.name = name;
        this.endpointName = endpointName;
    }
    public String getName() { return name; }
    public String getEndpointName() { return endpointName; }

    public void setName(String name) { this.name = name; }
    public void setEndpointName(String endpointName) { this.endpointName = endpointName; }

}
