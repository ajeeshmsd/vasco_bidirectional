/**
 * Copyright (C) 2013 Rohan Padhye
 * 
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package vasco.soot.examples;

import java.util.Map;

import soot.Local;
import soot.PackManager;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.Constant;
import vasco.DataFlowSolution;

/**
 * A Soot {@link SceneTransformer} for performing {@link CopyConstantAnalysis}.
 * 
 * @author Rohan Padhye
 */
public class CopyConstantPropagation extends SceneTransformer {
	
	private CopyConstantAnalysis analysis;

	
	@Override
	protected void internalTransform(String arg0, @SuppressWarnings("rawtypes") Map arg1) {
		analysis = new CopyConstantAnalysis();
		analysis.doAnalysis();
		DataFlowSolution<Unit,Map<Local,Constant>> solution = analysis.getMeetOverValidPathsSolution();
		System.out.println("----------------------------------------------------------------");
		for (SootMethod sootMethod : analysis.getMethods()) {
			System.out.println(sootMethod);
			for (Unit unit : sootMethod.getActiveBody().getUnits()) {
				System.out.println("----------------------------------------------------------------");
				System.out.println(unit);
				System.out.println("IN:  " + formatConstants(solution.getValueBefore(unit)));
				System.out.println("OUT: " + formatConstants(solution.getValueAfter(unit)));
			}
			System.out.println("----------------------------------------------------------------");
		}		
	}
	
	private String formatConstants(Map<Local, Constant> value) {
		if (value == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<Local,Constant> entry : value.entrySet()) {
			Local local = entry.getKey();
			Constant constant = entry.getValue();
			if (constant != null) {
				sb.append("(").append(local).append("=").append(constant).append(") ");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Returns a reference to the {@link CopyConstantAnalysis} object. 
	 * @return a reference to the {@link CopyConstantAnalysis} object
	 */
	public CopyConstantAnalysis getAnalysis() {
		return analysis;
	}

	public static void main(String args[]) {
		String classPath = "/home/rohan/Dropbox/HeapAnalysis/test";
		String mainClass = "ConstantTest";
		String[] sootArgs = {
				"-cp", classPath, "-pp", 
				"-w", "-app", 
				"-keep-line-number",
				"-keep-bytecode-offset",
				"-p", "jb", "use-original-names",
				"-p", "cg", "implicit-entry:false",
				"-p", "cg.spark", "enabled",
				"-p", "cg.spark", "simulate-natives",
				"-p", "cg", "safe-forname",
				"-p", "cg", "safe-newinstance",
				"-main-class", mainClass,
				"-f", "none", mainClass 
		};
		CopyConstantPropagation cgt = new CopyConstantPropagation();
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.ccp", cgt));
		soot.Main.main(sootArgs);
	}
	
}