package at.jku.isse.passiveprocessengine.frontend.botsupport;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import at.jku.isse.designspace.artifactconnector.core.repository.CoreTypeFactory;
import at.jku.isse.passiveprocessengine.core.BuildInType;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.CARDINALITIES;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType.PPEPropertyType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.frontend.registry.PropertyConversionUtil;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.activeobjects.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.types.SpecificProcessStepType;
import lombok.NonNull;

public class HumanReadableSchemaExtractor {

	private Map<PPEInstanceType, Set<PPEInstance>> samples;
	final SchemaRegistry schemaReg;
	final PPEInstanceType stepType;
	final PPEInstanceType procType;
	final PPEInstanceType artType;
	
	public HumanReadableSchemaExtractor(@NonNull SchemaRegistry schemaReg) {
		samples = Collections.emptyMap();
		this.schemaReg = schemaReg;
		this.stepType = schemaReg.getType(ProcessStep.class);
		this.procType = schemaReg.getType(ProcessInstance.class);
		this.artType = schemaReg.getTypeByName(CoreTypeFactory.BASE_TYPE_NAME);
	}
	
	public Map<PPEInstanceType, String> getSchemaForInstanceTypeAndOneHop(PPEInstanceType type, boolean includeSubTypes) {
		Map<PPEInstanceType, String> schemas = new HashMap<>();
		Set<PPEInstanceType> foundComplexTypes = new HashSet<>();
		schemas.put(type, getSchema(type, foundComplexTypes));
		foundComplexTypes.stream()
		.filter(cType -> !schemas.containsKey(cType))
		.forEach(cType -> schemas.put(cType,(getSchema(cType, null))));
		// subtypes of type:
		if (includeSubTypes) {
			List<AbstractMap.SimpleEntry<PPEInstanceType, String>> childTypes = schemas.keySet().stream()
				.flatMap(ctype -> ctype.getAllSubtypesRecursively().stream())
				.distinct()
				.filter(cType -> !schemas.containsKey(cType))
				.map(cType -> new AbstractMap.SimpleEntry<PPEInstanceType, String>(cType,(getSchema(cType, null))))
				.collect(Collectors.toList());
			childTypes.stream().forEach(tuple -> schemas.put(tuple.getKey(), tuple.getValue()));
		}		
		return schemas;
	}
	
	public String getSchemaForInstanceType(PPEInstanceType type) {
		return getSchema(type, null);
	}
	
	private String getSchema(PPEInstanceType type, Set<PPEInstanceType> foundComplexTypes) {
		// check if a process or step:
		StringBuffer sb = new StringBuffer();
		getSchemaStrings(type, foundComplexTypes)
		.forEach(propDesc -> sb.append(propDesc));
		return sb.toString();
	}
	
	public List<String> getSchemaStrings(PPEInstanceType type, Set<PPEInstanceType> foundComplexTypes) {
		// check if a process or step:
		if (type.isOfTypeOrAnySubtype(stepType)) {
			return getSchemaForStep(type, foundComplexTypes);
		}
		//				if (type.getParentType()==null) {
		//					sb.append(String.format("\r\nThe %s object consists of the following properties: ", type.getName()));
		//				} else {
		//					String parent = type.getParentType().getName();
		//					if ( type.getParentType().isOfTypeOrAnySubtype(artType)) {
		//						sb.append(String.format("\r\nThe %s object consists of the following properties: ", type.getName()));
		//					} else {
		//						sb.append(String.format("\r\nThe %s object is a subtype of %s and consists of the following properties: ", type.getName(), parent));
		//					}
		//				}
		return type.getPropertyNamesIncludingSuperClasses().stream()
				.map(propName -> type.getPropertyType(propName))
				.map(prop ->  { 
					if (foundComplexTypes != null && isAComplexType(prop)) {
						foundComplexTypes.add(prop.getInstanceType());
					}
					return getPropertyDescription(type, prop); } )
				.sorted()
				.toList();				
	}
	
	private List<String> getSchemaForStep(PPEInstanceType type, Set<PPEInstanceType> foundComplexTypes) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("\r\nThe process step %s consists of the following input and output properties: ", type.getName()));
		return type.getPropertyNamesIncludingSuperClasses().stream()
			.map(propName -> type.getPropertyType(propName))
			.filter(prop -> prop.getName().startsWith(SpecificProcessStepType.PREFIX_IN) || prop.getName().startsWith(SpecificProcessStepType.PREFIX_OUT))
			.map(prop ->  { 
				if (foundComplexTypes != null && isAComplexType(prop)) {
					foundComplexTypes.add(prop.getInstanceType());
				}
				return getPropertyDescription(type, prop); } )
			.toList();
		
	}
	//name, of multiple bug
	private String getPropertyDescription(PPEInstanceType type, PPEPropertyType prop) {
		if (samples.containsKey(type)) { // we have some sample values to add
			String sampleValues = samples.get(type).stream()
					.filter(sType -> sType.getInstanceType().isOfTypeOrAnySubtype(type)) // ensure no typing problem
					.map(sType -> sType.getTypedProperty(prop.getName(), Object.class) ) // now we have the values
					.filter(Objects::nonNull) // ignore null objects
					.map(propValue -> PropertyConversionUtil.valueToString(propValue).trim())
					.distinct()
					.filter(strValue -> strValue.length() > 0)
					.collect(Collectors.joining(", "));
			if (sampleValues.length() > 0) {
				return String.format("\r\n %s %s %s e.g., %s", prop.getName(), getCardinality(prop), getType(prop), condenseValue(sampleValues));
			} else 
				return String.format("\r\n %s %s %s", prop.getName(), getCardinality(prop), getType(prop));
		} else 
			return String.format("\r\n %s %s %s", prop.getName(), getCardinality(prop), getType(prop));
	}

	private String condenseValue(String valueString) {
		if (valueString.length() > 100) {
			int posLastWhiteSpace = valueString.lastIndexOf(" ");
			if (posLastWhiteSpace > 80) 
				return valueString.substring(0, posLastWhiteSpace);
			else 
				return valueString.substring(0, 100)+" ...";
		} else
			return valueString;
	}
	
	private boolean isAComplexType(PPEPropertyType prop) {
		if (BuildInType.isAtomicType(prop.getInstanceType()))
			return false;
		else
			return true;
	}
	
	private String getType(PPEPropertyType prop) {
		return prop.getInstanceType().getName();
	}

	private static String getCardinality(PPEPropertyType prop) {
		if (prop.getCardinality().equals(CARDINALITIES.SINGLE)) {
			return "of type ";
		} else {
			return "of multiple ";
		}
	}
	
	public Map<PPEInstanceType, List<PPEInstanceType>> clusterTypes(Collection<PPEInstanceType> types) {
		 return types.stream().collect(Collectors.groupingBy(PPEInstanceType::getParentType));		
	}
	
	public boolean doClustersIncludeType( Map<PPEInstanceType, List<PPEInstanceType>> clusters, PPEInstanceType type) {
		if (clusters.keySet().contains(type))
			return true;
		return clusters.values().stream().anyMatch(clusterList -> clusterList.contains(type));
	}
	
	
	public Entry<Set<String>, List<ArrayList<String>>> processSubgroup(PPEInstanceType parentType, List<PPEInstanceType> group) {
		Set<String> superProps = new HashSet<>(this.getSchemaStrings(parentType, null));
		
		List<ArrayList<String>> specificProps = group.stream().map(type -> this.getSchemaStrings(type, null))
			.map(strList -> new ArrayList<>(strList))
			.map(strList -> { strList.removeAll(superProps); return strList; })
			.toList();
		
		// all the properties not in the super type
		Set<String> allSpecificProps = specificProps.stream().flatMap(strList -> strList.stream()).distinct().collect(Collectors.toSet());
		// all the props that each object in the group has
		var commonProps = allSpecificProps.stream()
				.filter(propCandidate -> specificProps.stream()
							.allMatch(propList -> propList.contains(propCandidate)) )
				.toList();
		// lets add these commonProps to super Prop and remove them from individual prop
		superProps.addAll(commonProps);
		// lets remove from individual ones
		specificProps.stream().forEach(propList -> propList.removeAll(commonProps));
								
		return new AbstractMap.SimpleEntry<>(superProps, specificProps);
	}
	//call my TIM
	//check if the PPEInstanceType name has the same name in my TIM data:
	// If yes then connect it with it trace
	public String compileSchemaList(PPEInstanceType parentType, List<PPEInstanceType> group, Set<String> parentProps, List<ArrayList<String>> individualProps) {
		StringBuffer sb = new StringBuffer(String.format("\r\nGeneric object type %s contains following properties:", parentType.getName()));
		parentProps.stream().sorted().forEach(prop -> sb.append(prop));
				
		for (int i = 0 ; i < group.size() ; i++) {
			var type = group.get(i);
			var props  = individualProps.get(i);
			if (props.isEmpty()) continue;
			sb.append(String.format("\r\n\r\nObject type %s contains following properties:", type.getName()));
			props.stream().sorted().forEach(prop -> sb.append(prop));
		}

		//I started work here

		List<TIMWorkItem> workItems = useTIMWorkItem();
		Map<String, List<Trace>> typeWithTraces = new HashMap<>();

		System.out.println("----------------------");
		for (int i = 0 ; i < group.size() ; i++) {
			var type = group.get(i);
			workItems.stream().filter(item -> item.getName().equals(type.getName()))
					.forEach(item -> typeWithTraces.putIfAbsent(item.getName(), item.getTraces()));
			sb.append(typeWithTraces);
		}

		for (String item : typeWithTraces.keySet()){
			System.out.println("Item: " + item);
            for (Trace trace : typeWithTraces.get(item)) {
                System.out.println("Trace: " + trace.getName() + " Endpoint: " + trace.getEndpointName());
            }
        }

		System.out.println("----------------------");

		return sb.toString();
	}

	//My methods

	public List<TIMWorkItem> useTIMWorkItem() {
		List<TIMWorkItem> items = new ArrayList<>();

		items.add(new TIMWorkItem("Bug"));//0
		items.add(new TIMWorkItem("Requirement"));//1
		items.add(new TIMWorkItem("Change Request"));//2

		Trace req = new Trace("affectedByItems", items.get(0).getName());
		Trace req2 = new Trace("predecessorItems", items.get(2).getName());
		Trace bug = new Trace("affectsItems", items.get(1).getName());

		items.get(0).setTrace(bug);
		items.get(1).setTrace(req);
		items.get(1).setTrace(req2);

	/*
		System.out.println("WorkItem: " + items.get(0).getName());
		for (Trace trace : items.get(0).getTraces()) {
			System.out.println("Trace: " + trace.getName() + " -> Endpoint: " + trace.getEndpointName().getName());
		}

		System.out.println();

		System.out.println("WorkItem: " + items.get(1).getName());
		for (Trace trace : items.get(1).getTraces()) {
			System.out.println("Trace: " + trace.getName() + " -> Endpoint: " + trace.getEndpointName().getName());
		}
	 */
		return items;
	}

	//end of my methods
}
 