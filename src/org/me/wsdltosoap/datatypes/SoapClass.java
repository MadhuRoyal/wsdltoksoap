/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.me.wsdltosoap.datatypes;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author newky
 */
public class SoapClass
{
    public enum ClassType { Parameter, Response, ComplexType, Unknown};

    public String Name;
    public ClassType Type;
    public boolean isArray;
    public String ElementType;
    public String SuperClassType = "BaseObject";


    public List<SoapClassProperty> Properties;
	public String PackageName;

	@Deprecated
    public SoapClass(String Name)
    {
        this.Name = Name;

        this.Properties = new ArrayList<SoapClassProperty>();
    }

	public SoapClass(String packageName, String className) {
		this.Name = className;
		this.PackageName=packageName;
        this.Properties = new ArrayList<SoapClassProperty>();
	}
}
