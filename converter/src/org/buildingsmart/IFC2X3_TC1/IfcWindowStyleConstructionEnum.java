package org.buildingsmart.IFC2X3_TC1;
import org.buildingsmart.*;
import java.util.*;

/*
 * IFC Java class
/ *
 * The GNU Affero General Public License
 * 
 * Copyright (c) 2014 Jyrki Oraskari (original)
 * Copyright (c) 2014 Pieter Pauwels (modifications - pipauwel.pauwels@ugent.be / pipauwel@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class IfcWindowStyleConstructionEnum extends Thing
{
	public enum IfcWindowStyleConstructionEnum_value{ALUMINIUM, HIGH_GRADE_STEEL, STEEL, WOOD, ALUMINIUM_WOOD, PLASTIC, OTHER_CONSTRUCTION, NOTDEFINED};
	private IfcWindowStyleConstructionEnum_value value;

	public IfcWindowStyleConstructionEnum_value getIfcWindowStyleConstructionEnum() {
		return value;
	}

	public void setIfcWindowStyleConstructionEnum(IfcWindowStyleConstructionEnum_value value){
		this.value=value;
	}

}