package org.me.wsdltosoap;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.me.wsdltosoap.businesslogic.ClassProcessor;
import org.me.wsdltosoap.businesslogic.Helper;
import org.me.wsdltosoap.datatypes.Function;
import org.me.wsdltosoap.datatypes.PropertyContainer;
import org.me.wsdltosoap.datatypes.SoapClass;
import org.me.wsdltosoap.datatypes.SoapClass.ClassType;
import org.me.wsdltosoap.datatypes.SoapClassProperty;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WSDLParser {
	
	private static final String JAXP_SCHEMA_LANGUAGE =
	    "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	private static final String W3C_XML_SCHEMA =
	    "http://www.w3.org/2001/XMLSchema"; 
	private static String WSDL_URI= 
		//"http://localhost:8088/mockMortgageLoanStatusPort?WSDL";
		//"http://www.webservicex.net/country.asmx?WSDL";
		"file:///C:/Users/Puspendu/Desktop/country.asmx.wsdl";
	
	private MyXmlPrettyPrinter prettyPrinter=new MyXmlPrettyPrinter();
	
	private Logger logger=Logger.getLogger(WSDLParser.class.getName());

	private HashMap<String,String> nameSpaceMap=null;
	private Document document=null;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//setProxyProperties();
		new WSDLParser().parse();

	}

	private static void setProxyProperties() {
		System.getProperties().put("http.proxyHost", "proxy.cognizant.com");
		System.getProperties().put("http.proxyPort", "6050");
		System.getProperties().put("http.proxyUser", "258265");
		System.getProperties().put("http.proxyPassword", "cts-1011");
	}

	private void parse() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(true);
		try {
			  factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			} catch (Exception e) {
				e.printStackTrace();
			}
		DocumentBuilder documentBuilder=factory.newDocumentBuilder();
		logger.info("DocumentBuilder "
				+ (documentBuilder.isNamespaceAware()? "is":"is not") 
				+" NamespaceAware");
		
		logger.info("DocumentBuilder "
				+ (documentBuilder.isXIncludeAware()? "is":"is not") 
				+" XIncludeAware");
		document=documentBuilder.parse(WSDL_URI);
		document.normalizeDocument();
		prettyPrint(document);
		PropertyContainer.Namespace = document.getDocumentElement().getAttribute("targetNamespace");
		populateNameSpaceMap();		
		Element serviceElement= getServiceNode();
		PropertyContainer.ServiceName=serviceElement.getAttribute("name");
		NodeList serviceportNodes=getServicePortNodes(serviceElement);
		Element soapServicePortElement=getSoapServicePortElement(serviceportNodes);
		PropertyContainer.SoapPortName=soapServicePortElement.getAttribute("name");
		NodeList bindingNodes=getBindingNodes();
		NodeList portTypeNodes=getPortTypeNodes();		
		NodeList messageNodes=getMessageNodes();
		NodeList typeSchemaNodes=getTypesSchemaNodes();
		prettyPrint(soapServicePortElement);
		populateFunctions(bindingNodes, portTypeNodes, messageNodes,
				typeSchemaNodes);
		
		for(int typeSchemaNodeIndex=0;typeSchemaNodeIndex<typeSchemaNodes.getLength();typeSchemaNodeIndex++){

             //now get schema element
            Node typeschemaNode = typeSchemaNodes.item(typeSchemaNodeIndex);

            //serviceElement.getAttribute("name");
            if(typeschemaNode.getNodeType() == Node.ELEMENT_NODE)
            {
                Element typeschemaElement = (Element)typeschemaNode;

                //get elements
                NodeList elementNodes = typeschemaElement.getElementsByTagNameNS(StandardNamespace.xsd,"element");

                //get complexTypes
                NodeList complexTypesNodes = typeschemaElement.getElementsByTagNameNS(StandardNamespace.xsd,"complexType");

                //process element nodes and get class information

                //create Classes container - make it big
                //PropertyContainer.Classes = new ArrayList<SoapClass>();


                //iterate through xsd:element objects
                for (int elLoop = 0;elLoop < elementNodes.getLength();elLoop++)
                {
                    Node elementNode = elementNodes.item(elLoop);

                    if(elementNode.getNodeType() == Node.ELEMENT_NODE)
                    {
                        Element elementElement = (Element)elementNode;

                        //get header elements from list - as the list contains all xsd:elements from the schema nodes



                        if (elementNode.hasChildNodes())
                        {
                            //check to see not has name

                        	String [] classNameParts={elementElement.getAttribute("name")};
                        	SoapClass newClass =null;
                        	if(classNameParts[0].contains(":")){
                        		classNameParts=classNameParts[0].split(":");
                        		newClass=new SoapClass(Helper.convertUrlToJavaPackageName(elementElement.lookupNamespaceURI(classNameParts[0])), classNameParts[1]);
                        	}else{
                        		newClass=new SoapClass(Helper.convertUrlToJavaPackageName(PropertyContainer.Namespace), classNameParts[0]);
                        	}
                             

                                //set classtype to unknown
                                newClass.Type = ClassType.Unknown;
                                //now get properties from class
                                //get elements
                                NodeList propertyNodes = elementElement.getElementsByTagNameNS(StandardNamespace.xsd,"element");

                                //iterate through properties
                                for (int propLoop = 0; propLoop < propertyNodes.getLength();propLoop++)
                                {
                                    Node propertyNode = propertyNodes.item(propLoop);

                                    if(propertyNode.getNodeType() == Node.ELEMENT_NODE)
                                    {
                                        Element propertyElement = (Element)propertyNode;

                                        //create new property class
                                        SoapClassProperty newProp = new SoapClassProperty(propertyElement.getAttribute("name"));
                                        newProp.SetPropertyClassType(propertyElement.getAttribute("type").toString());

                                        //check to see if is array of objects
                                        if (propertyElement.getAttribute("maxOccurs").equals("unbounded"))
                                        {
                                            //yes is array
                                            newProp.SetIsArray(true);
                                            
                                        }
                                        
                                        newClass.Properties.add(newProp);

                                    }

                                }
                                



                                System.out.println("Element Class: " + newClass.Name + " Properties: " + newClass.Properties.size());

                                

                                PropertyContainer.Classes.add(newClass);
                                
                               
                            
                            
                        }
                    }

                }

                //iterate through xsd:comlextypes objects
                for (int ctLoop = 0;ctLoop < complexTypesNodes.getLength();ctLoop++)
                {
                    Node ctypeNode = complexTypesNodes.item(ctLoop);

                    if(ctypeNode.getNodeType() == Node.ELEMENT_NODE)
                    {
                        Element ctypeElement = (Element)ctypeNode;

                        //get header elements from list - as the list contains all xsd:elements from the schema nodes
                        if (ctypeNode.hasChildNodes())
                        {
                            //xsd:elements show up for some reason but have no name
                            if (!ctypeElement.getAttribute("name").isEmpty())
                            {
                            	String [] classNameParts={ctypeElement.getAttribute("name")};
                            	SoapClass newClass =null;
                            	if(classNameParts[0].contains(":")){
                            		classNameParts=classNameParts[0].split(":");
                            		newClass=new SoapClass(Helper.convertUrlToJavaPackageName(ctypeElement.lookupNamespaceURI(classNameParts[0])), classNameParts[1]);
                            	}else{
                            		newClass=new SoapClass(Helper.convertUrlToJavaPackageName(PropertyContainer.Namespace), classNameParts[0]);
                            	}
                              
                                //set class type to complex type
                                newClass.Type = ClassType.ComplexType;

                                //check for base super class
                                //NodeList spBaseNode = ctypeElement.getChildNodes();
                                NodeList spBaseNode = ctypeElement.getElementsByTagNameNS(StandardNamespace.xsd,"extension");
                                //Node ctNode = spBaseNode.item(1);

                                if (spBaseNode.getLength() != 0)
                                {
                                    
                                    Node sbBaseNode = spBaseNode.item(0);

                                    if(sbBaseNode.getNodeType() == Node.ELEMENT_NODE)
                                    {
                                        Element sbBaseElement = (Element)sbBaseNode;
                                        
                                        newClass.SuperClassType = sbBaseElement.getAttribute("base").replaceAll("tns:", "");

                                    }
                                }

                                System.out.println("SuperClass: " + newClass.SuperClassType);
                                
                                 //get elements
                                NodeList propertyNodes = ctypeElement.getElementsByTagNameNS(StandardNamespace.xsd,"element");

                                //iterate through properties
                                for (int propLoop = 0; propLoop < propertyNodes.getLength();propLoop++)
                                {
                                    Node propertyNode = propertyNodes.item(propLoop);

                                    if(propertyNode.getNodeType() == Node.ELEMENT_NODE)
                                    {
                                        Element propertyElement = (Element)propertyNode;

                                        //create new property class
                                        SoapClassProperty newProp = new SoapClassProperty(propertyElement.getAttribute("name"));
                                        newProp.SetPropertyClassType(propertyElement.getAttribute("type"));

                                        //check to see if is array of objects
                                        if (propertyElement.getAttribute("maxOccurs").equals("unbounded"))
                                        {
                                            //yes is array
                                            newProp.SetIsArray(true);
                                            newClass.ElementType = newProp.getPropertyClassType();
                                            newClass.isArray = true;
                                        }

                                        newClass.Properties.add(newProp);

                                    }

                                }
                                
                                 //get Attributes
                                NodeList attributeNodes = ctypeElement.getElementsByTagNameNS(StandardNamespace.xsd,"attribute");
                                if(attributeNodes!=null && attributeNodes.getLength()>0){
                                   for(int i=0;i<attributeNodes.getLength();i++){
                                       Element attributeElement= (Element)attributeNodes.item(i);
                                       NamedNodeMap namedNodeMap= attributeElement.getAttributes();
                                       for(int j=0;j<namedNodeMap.getLength();j++){
                                           Node attributeOfattributeElement=namedNodeMap.item(j);
                                           System.out.println(attributeElement.getAttribute("name")+
                                                   "#"+attributeOfattributeElement.getNodeName()+
                                                   "-->"+attributeOfattributeElement.getNodeValue());
                                       }
                                   }
                                }

                                System.out.println("Complex Type: " + newClass.Name + " Properties: " + newClass.Properties.size());

                                PropertyContainer.ComplexTypes.add(newClass);

                            }

                        }
                    }

                }

                //all class should have been created now - trim array
                System.out.println("Class Count: " + PropertyContainer.Classes.size());
                System.out.println("Complex Types: " + PropertyContainer.ComplexTypes.size());


            }
		}


        
		//create paramter and return class
        ClassProcessor classProcessor=new ClassProcessor();
        try {
			classProcessor.createFunctionClasses();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void populateFunctions(NodeList bindingNodes,
			NodeList portTypeNodes, NodeList messageNodes,
			NodeList typeSchemaNodes) {
		for (int i = 0; i < bindingNodes.getLength(); i++) {
			Node bindingNode = bindingNodes.item(i);
			Element bindingElement = (Element)bindingNode;
			 if (bindingElement.getAttribute("name").equals(PropertyContainer.GetSoapPortName()))
             {
				 String portTypeNS = bindingElement.getAttribute("type").replaceFirst("tns:", "");
				 Element matchedPortTypeElement=null;
				 for(int ptt=0;ptt<portTypeNodes.getLength();ptt++){
              	   Element portTypeElement=(Element) portTypeNodes.item(ptt);
              	   if(portTypeElement.getAttribute("name").equals(portTypeNS)){
              		   //Found Matched PortType
              		   prettyPrint("Found Matched PortType: "+ portTypeNS,portTypeElement);
              		 matchedPortTypeElement=portTypeElement;
              		 break;
              	   }
                 }
				//get operations
                 NodeList bindingOperations = getElementsByTagName(bindingElement,StandardNamespace.wsdl,"operation",nameSpaceMap);
                 logger.info("Operations Found: "+ bindingOperations.getLength());
                 //instatiate the Function array now we know how many functions there are.
                 PropertyContainer.Functions = new Function[bindingOperations.getLength()];
                 for(int op=0; op<bindingOperations.getLength() ; op++)
                 {
                	 	Element operationElement =(Element) bindingOperations.item(op);
                	 	prettyPrint("Operation Node: "+op,operationElement);
                	 	Function newFunc = new Function();
                	 	newFunc.Name = operationElement.getAttribute("name");
                	 	Element soapOperationElement=(Element)getElementsByTagName(
                	 			operationElement, StandardNamespace.soap,
                    		   "operation", nameSpaceMap).item(0);
                       newFunc.SoapAction=soapOperationElement.getAttribute("soapAction");
                       NodeList portTypeOperations = getElementsByTagName(matchedPortTypeElement, StandardNamespace.wsdl, "operation", nameSpaceMap);
                       Element matchedPortTypeOperation=null;
                       for(int ptop=0;ptop<portTypeOperations.getLength();ptop++){
                    	   Element portTypeOperation=(Element) portTypeOperations.item(ptop);
                    	   if(portTypeOperation.getAttribute("name").equals(operationElement.getAttribute("name"))){
                    		   prettyPrint("Found Matching Operation under PortType ", portTypeOperation);
                    		   matchedPortTypeOperation=portTypeOperation;
                    		   break;
                    	   }
                       }
                       Element matchedPortTypeOperationInput=getSingleElementByTagName(matchedPortTypeOperation,StandardNamespace.wsdl,
                    		   "input",nameSpaceMap);
                       Element matchedPortTypeOperationOutput=getSingleElementByTagName(matchedPortTypeOperation,StandardNamespace.wsdl,
                    		   "output",nameSpaceMap);
                       Element matchedPortTypeOperationFault=getSingleElementByTagName(matchedPortTypeOperation,StandardNamespace.wsdl,
                    		   "fault",nameSpaceMap);
                       String inputMsg=matchedPortTypeOperationInput!=null?matchedPortTypeOperationInput.getAttribute("message"):null;
                       //TODO: Put some better code on NameSpace Handling
                       inputMsg=inputMsg!=null?inputMsg.replaceFirst("tns:", ""):"";
                       String outputMsg=matchedPortTypeOperationOutput!=null?matchedPortTypeOperationOutput.getAttribute("message"):null;
                       //TODO: Put some better code on NameSpace Handling
                       outputMsg=outputMsg!=null?outputMsg.replaceFirst("tns:", ""):"";
                       String faultMsg=matchedPortTypeOperationFault!=null?matchedPortTypeOperationFault.getAttribute("message"):null;
                       //TODO: Put some better code on NameSpace Handling
                       faultMsg=faultMsg!=null?faultMsg.replaceFirst("tns:", ""):"";
                       for(int msg=0;msg<messageNodes.getLength();msg++){
                    	   Element messageElement=(Element) messageNodes.item(msg);
                    	   String messageName=messageElement.getAttribute("name");
                    	   Element elementElement=null;
                    	   if(messageName.equals(inputMsg)){
                    		   prettyPrint("Found Matched input ", messageElement);
                    		   Element part= (Element) messageElement.getElementsByTagNameNS(StandardNamespace.wsdl, "part").item(0);
                    		   String inputName=part.getAttribute("name");
                    		   String inputType=part.getAttribute("element");
                    		   elementElement=findOrCreateElement(part,typeSchemaNodes);
                    		   newFunc.InputType=getSoapType(elementElement);
                    	   }else if(messageName.equals(outputMsg)){
                    		   prettyPrint("Found Matched output ", messageElement);
                    		   Element part= (Element) messageElement.getElementsByTagNameNS(StandardNamespace.wsdl, "part").item(0);
                    		   elementElement= findOrCreateElement(part,typeSchemaNodes);
                    		   newFunc.OutputType=getSoapType(elementElement);
                    	   }else if(messageName.equals(faultMsg)){
                    		   prettyPrint("Found Matched Fault ", messageElement);
                    		   Element part= (Element) messageElement.getElementsByTagNameNS(StandardNamespace.wsdl, "part").item(0);
                    		   elementElement= findOrCreateElement(part,typeSchemaNodes);
                    		   newFunc.FaultType=getSoapType(elementElement);
                    	   }
                    	   
                       }
                       //add newFunc to functions array
                       PropertyContainer.Functions[op] = newFunc;
                 }
             }
		}
	}

	
	/**
	 * @param elementElement
	 * @return FQDN of Serializable Object Type
	 */
	private String getSoapType(final Element elementElement) {
		String resultantType=null;
		if(elementElement.hasAttribute("type")){
			   //Referening another Type defined somewhere else
			   String typeAttr=elementElement.getAttribute("type");
			   if(typeAttr.contains(":")){//Has NameSpace prefix
				   String [] typeAttrParts=typeAttr.split(":");
				   resultantType=getPackageNameFromURI(elementElement.lookupNamespaceURI(typeAttrParts[0]))+typeAttrParts[1];
			   }else{
				   resultantType=typeAttr;
			   }
			   logger.info("Referenced Type "+resultantType);
		   }else{
			   //Type is defined locally, under this node
			   //TODO: implement this scenario
			   resultantType=Helper.convertUrlToJavaPackageName(PropertyContainer.Namespace)+"."+ elementElement.getAttribute("name");
		   }
		return resultantType;
	}

	private String getPackageNameFromURI(final String namespaceURI) {
		int protocolNameLength=namespaceURI.indexOf("://");
		String [] packageNameParts= namespaceURI.substring(
				protocolNameLength+3).split("//")[0].split("\\.");
		StringBuffer packageName=new StringBuffer();
		for (int i = packageNameParts.length; --i>=0;) {
			packageName.append(packageNameParts[i]+'.');
		}
		return packageName.toString();
	}

	private Element findOrCreateElement(Element part,NodeList typeSchemaNodes) {
		Element matchedElement=null;
		String [] inputType=part.getAttribute("element").split(":");
		if(inputType.length==1){
			
		}else if(inputType.length==2){
			String namespaceURI= part.lookupNamespaceURI(inputType[0]);
			logger.info("Found NameSpace: "+inputType[0]+ " For "+namespaceURI);
			for(int i=0;(i<typeSchemaNodes.getLength()) && (null==matchedElement);i++){
				//String mappedPrefix=typeSchemaNodes.item(i).lookupPrefix(namespaceURI);
				String targetNamespace=((Element)typeSchemaNodes.item(i)).getAttribute("targetNamespace");
				if(targetNamespace.equals(namespaceURI)){
					NodeList elementNodes=((Element)typeSchemaNodes.item(i)).getElementsByTagNameNS(StandardNamespace.xsd,"element");
					if(elementNodes!=null && elementNodes.getLength()>0){
						for (int j = 0; j < elementNodes.getLength(); j++) {
							Element elementElement=(Element)elementNodes.item(j);
							if(elementElement.getAttribute("name").equals(inputType[1])){
								matchedElement=elementElement;
	                    		prettyPrint("Found Matched Element for part @element: "+part.getAttribute("element"), matchedElement);
								break;
							}
						}
					}
				}
			}
		}else{
			throw new RuntimeException("Should not Happen");
		}
		return matchedElement;
	}

	/**
	 * Find WSDL Schemas
	 * @return {@link NodeList} of wsdl:types-->xsd:schema
	 */
	private NodeList getTypesSchemaNodes() {
		NodeList wsdlTypesSchemaNodes=null;
		   NodeList wsdlTypesNodes= document.getElementsByTagNameNS(StandardNamespace.wsdl, "types");
		   if(wsdlTypesNodes!=null && wsdlTypesNodes.getLength()>0){
			   wsdlTypesSchemaNodes=((Element)wsdlTypesNodes.item(0)).
					   getElementsByTagNameNS(StandardNamespace.xsd, "schema");
			   prettyPrint("Found Schemas: ",wsdlTypesSchemaNodes);
		   }
		   
		return wsdlTypesSchemaNodes;
	}

	private void prettyPrint(String msg, NodeList wsdlTypesSchemaNodes) {
		for(int i=0;i<wsdlTypesSchemaNodes.getLength();i++){
			prettyPrint(msg+" ["+i+"]",wsdlTypesSchemaNodes.item(i));
		}
	}

	private void prettyPrint(String msg, Node operationNode) {
		System.out.println(msg);
		prettyPrint(operationNode);
		
	}

	private Element getSoapServicePortElement(NodeList serviceportNodes) {
		Element soapServicePortElement=null;
		for(int i=0;i<serviceportNodes.getLength();i++){
			Element servicePortElement=(Element) serviceportNodes.item(i);
			NodeList addressNodes= getElementsByTagName(servicePortElement, StandardNamespace.soap,
					"address", nameSpaceMap);
			if(addressNodes!=null && addressNodes.getLength()>0){
				soapServicePortElement=servicePortElement;
				break;
			}
		}
		return soapServicePortElement;
	}

	private NodeList getMessageNodes() {
		NodeList messageNodes=null;
		messageNodes=getElementsByTagName(document, StandardNamespace.wsdl, "message", nameSpaceMap);
		logger.info("Messages Found :"+messageNodes.getLength());
		for(int i=0;i<messageNodes.getLength();i++){
			Element messageNode=(Element) messageNodes.item(i);
			logger.info("    Message ----> @name="
					+messageNode.getAttribute("name"));		
		}
		return messageNodes;
	}

	private NodeList getPortTypeNodes() {
		NodeList portTypeNodes=null;
		portTypeNodes=getElementsByTagName(document, StandardNamespace.wsdl, "portType", nameSpaceMap);
		logger.info("PortType Found :"+portTypeNodes.getLength());
		for(int i=0;i<portTypeNodes.getLength();i++){
			Element bindingNode=(Element) portTypeNodes.item(i);
			logger.info("    portType----> @name="
					+bindingNode.getAttribute("name"));		
		}
		return portTypeNodes;
	}

	private NodeList getBindingNodes(){
		NodeList bindingNodes=null;
		bindingNodes=getElementsByTagName(document, StandardNamespace.wsdl, "binding", nameSpaceMap);
		logger.info("Service Binding Found :"+bindingNodes.getLength());
		for(int i=0;i<bindingNodes.getLength();i++){
			Element bindingNode=(Element) bindingNodes.item(i);
			logger.info("    binding----> @name="
					+bindingNode.getAttribute("name")
					+"   @type=" + bindingNode.getAttribute("type"));		
		}
		return bindingNodes;
	}
	
	private NodeList getServicePortNodes(Node serviceNode){
		NodeList serviceportNodes=null;
		serviceportNodes= getElementsByTagName(serviceNode, StandardNamespace.wsdl, "port", nameSpaceMap) ;
		logger.info("Service Ports Found :"+serviceportNodes.getLength());
		for(int i=0;i<serviceportNodes.getLength();i++){
			Element servicePort=(Element) serviceportNodes.item(i);
			logger.info("    port----> @name="
					+servicePort.getAttribute("name")
					+"   @binding=" + servicePort.getAttribute("binding"));			
		}
		return serviceportNodes;
	}
	private Element getServiceNode() {
		 NodeList serviceNodes= getElementsByTagName(document,StandardNamespace.wsdl, "service",nameSpaceMap);
		 Element serviceNode=(Element) serviceNodes.item(0);
		 logger.info("Found Service Node named: "+ serviceNode.getAttribute("name"));
		 return serviceNode;
	}

	private NodeList getElementsByTagName(final Node node, final String namespaceURI,
			final String nodename, final HashMap<String,String> nameSpaceMap) {
		NodeList nodes=null;
		String namespacePrefix=nameSpaceMap.get(namespaceURI);
		String effectiveNodeName=(namespacePrefix!=null?namespacePrefix+":":"")+nodename;
		if(effectiveNodeName.startsWith("xmlns:")){
			effectiveNodeName=effectiveNodeName.substring("xmlns:".length());
		}
		if(node instanceof Element){
			nodes=((Element)node).getElementsByTagName(effectiveNodeName);
		}else if(node instanceof Document){
			nodes=((Document)node).getElementsByTagName(effectiveNodeName);
		}
		return nodes;
	}
	
	private Element getSingleElementByTagName(final Node node, final String namespaceURI,
			final String nodename, final HashMap<String,String> nameSpaceMap){
		NodeList nodes=getElementsByTagName(node, namespaceURI, nodename, nameSpaceMap);
		return (nodes!=null)&& (nodes.getLength()>0)?((Element) nodes.item(0)):null;
	}

	private void populateNameSpaceMap() {
		this.nameSpaceMap=new HashMap<String, String>();
		NamedNodeMap attributes=document.getDocumentElement().getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attributeNode =attributes.item(i);
			if(attributeNode.getNodeName().startsWith("xmlns")
					|| attributeNode.getNodeName().startsWith("targetNamespace")){
				nameSpaceMap.put(attributeNode.getNodeValue(), attributeNode.getNodeName());
				logger.info("Adding NameSpace: " 
						+ attributeNode.getNodeValue() 
						+"--->" + attributeNode.getNodeName());
			}
		}
		logger.info("NameSpace Found: " + nameSpaceMap.size());
		//return nameSpaceMap;
	}

	private void prettyPrint(final Node node){
		try {
			Document doc=null;
			if(node instanceof Document){
				doc=(Document) node;
				prettyPrinter.serialize(doc, System.out);
			}else{
				// Set up the output transformer
			      TransformerFactory transfac = TransformerFactory.newInstance();
			      Transformer trans = transfac.newTransformer();
			      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			      trans.setOutputProperty(OutputKeys.INDENT, "yes");

			      // Print the DOM node

			      StringWriter sw = new StringWriter();
			      StreamResult result = new StreamResult(sw);
			      DOMSource source = new DOMSource(node);
			      trans.transform(source, result);
			      String xmlString = sw.toString();

			      System.out.println(xmlString);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	

}
