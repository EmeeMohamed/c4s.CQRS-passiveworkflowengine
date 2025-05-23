package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TIMWorkItem {
    private String name;
    private List<Trace> traceList;

    public TIMWorkItem(String name) {
        this.name = name;
        traceList = new ArrayList<Trace>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setTrace(String traceName, String endpoint) {
        traceList.add(new Trace(traceName, endpoint));
    }
    public List<Trace> getTraces() {
        return  traceList;}


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
