package org.buildingsmart;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.buildingsmart.vo.EntityVO;
import org.buildingsmart.vo.TypeVO;

import fi.ni.gui.fx.FxInterface;

/*
 * IFCtoRDFConverterStreann is the final interface for this code. Through this class, one is able to submit an IFC file and the EXPRESS schema it follows so that
 * a corresponding IFC/RDF graph can be built.
 * 
 * The usage:
 * IFCtoRDFConverter converter = new IFCtoRDFConverter(file name);
 *   
 * @author Jyrki Oraskari
 * @author of modifications Pieter Pauwels (pipauwel.pauwels@ugent.be / pipauwel@gmail.com)
 */

/*
 * The GNU Affero General Public License
 * 
 * Copyright (c) 2014, 2015 Jyrki Oraskari (original)
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

public class IfcReaderStream {

	private String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss")
			.format(Calendar.getInstance().getTime());
	//private final String DEFAULT_PATH = "http://linkedbuildingdata.net/ifc/instances"
	//		+ timeLog + "#";
	
	public final String DEFAULT_PATH = "http://linkedbuildingdata.net/ifc/resources"
			+ timeLog + "/";
	
	//public Logger logger;
	public boolean logToFile = false;
	public BufferedWriter bw;

	/**
	 * @param args
	 *            inputFilePath outputFilePath
	 */
	public static void main(String[] args) throws IOException {
		if(args[0].equalsIgnoreCase("LOG") && args[1].equalsIgnoreCase("DIR") && args.length == 3){
			//do not give too many files to the machine!!!!					
			List<String> files = showFiles(args[2]);			
			for(String f : files){
				if(f.endsWith(".ifc")){
					IfcReaderStream r = new IfcReaderStream();
					r.logToFile = true;	
					r.setupLogger(f);
					System.out.println("Converting file : " + f + "\r\n");
					if(r.logToFile) r.bw.write("Converting file : " + f + "\r\n");
					String path = f.substring(0,f.length()-4);		
					r.convert(path+".ifc", path+".ttl", r.DEFAULT_PATH);
					r.bw.flush();
					r.bw.close();
				}
			}
		}
		else if(args[0].equalsIgnoreCase("DIR") && args.length == 2){
			//do not give too many files to the machine!!!!					
			List<String> files = showFiles(args[1]);			
			for(String f : files){
				if(f.endsWith(".ifc")){
					IfcReaderStream r = new IfcReaderStream();					
					System.out.println("Converting file : " + f + "\r\n");
					String path = f.substring(0,f.length()-4);		
					r.convert(path+".ifc", path+".ttl", r.DEFAULT_PATH);
				}
			}
		}
		else if(args[0].equalsIgnoreCase("LOG") && args.length == 3){
			IfcReaderStream r = new IfcReaderStream();
			r.logToFile = true;
			r.setupLogger(args[2]);
			r.convert(args[1], args[2], r.DEFAULT_PATH);
			r.bw.flush();
			r.bw.close();
		}
		else if (args.length != 2) {
			System.out
					.println("Usage: java IfcReader ifc_filename output_filename \nExample: java IfcReaderStream C:\\sample.ifc c:\\output.ttl (we only convert to TTL)");
			for (int i = 0; i < args.length; i++) {
				System.out.println("arg[" + i + "] : " + args[i]);
			}
		} else {
			if (args.length == 2 && !args[0].startsWith("-json")) {
				IfcReaderStream r = new IfcReaderStream();
				r.convert(args[0], args[1], r.DEFAULT_PATH);
			} else {
				if (args[0].equals("-json")) {
					try {
						IfcReaderStream r = new IfcReaderStream();
						FileInputStream fis = new FileInputStream(args[1]);
						String jsonString = slurp(fis);
						fis.close();
						r.convert(jsonString);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (args[0].equals("-jsonString")) {
					IfcReaderStream r = new IfcReaderStream();
					r.convert(args[1]);
				}
			}
		}
	}
	
	public static List<String> showFiles(String dir) {
		List<String> goodFiles = new ArrayList<String>();
		
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		
		for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile())
		    	  goodFiles.add(listOfFiles[i].getAbsolutePath());
		      else if (listOfFiles[i].isDirectory())
		    	  goodFiles.addAll(showFiles(listOfFiles[i].getAbsolutePath()));
		    }
	    return goodFiles;
	}
	
	public void setupLogger(String path){	
		String outputFile = path.substring(0,path.length()-4) + ".log";

		try {			
			File file = new File(outputFile);
			if (!file.exists()) 
				file.createNewFile();
					
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void convert(String jsonConfig) throws IOException {
		JSONObject obj = JSONObject.fromObject(jsonConfig);

		String ifc_file = obj.getString("ifc_file");
		String output_file = obj.getString("output_file");

		convert(ifc_file, output_file, DEFAULT_PATH);
	}

	private static String getExpressSchema(String ifc_file) {
		try {
			FileInputStream fstream = new FileInputStream(ifc_file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			try {
				String strLine;
				while ((strLine = br.readLine()) != null) {
					if (strLine.length() > 0) {
						if (strLine.startsWith("FILE_SCHEMA")) {
							if (strLine.indexOf("IFC2X3") != -1)
								return "IFC2X3_TC1";
							if (strLine.indexOf("IFC4") != -1)
								return "IFC4_ADD1";
							else
								return "";
						}
					}
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String slurp(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}

	public void convert(String ifc_file, String output_file,
			String baseURI) throws IOException {
		long t0 = System.currentTimeMillis();

		if (!ifc_file.endsWith(".ifc")) {
			ifc_file += ".ifc";
		}

		String exp = getExpressSchema(ifc_file);

		// check if we are able to convert this: only four schemas are supported
		if (!exp.equalsIgnoreCase("IFC2X3_Final")
				&& !exp.equalsIgnoreCase("IFC2X3_TC1")
				&& !exp.equalsIgnoreCase("IFC4_ADD1")
				&& !exp.equalsIgnoreCase("IFC4")){
			if(logToFile) bw.write("ERROR: Unrecognised EXPRESS schema: " + exp + ". File should be in IFC4 or IFC2X3 schema. Stopping conversion." + "\r\n");
			return;
		}

		// CONVERSION
		OntModel om = null;
		
		InputStream in = null;
		try {			
			om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			in = IfcReader.class.getResourceAsStream("/" + exp + ".ttl");
			om.read(in, null, "TTL");

			String expresTtl = "/express.ttl";
			InputStream expresTtlStream = IfcConvertor.class.getResourceAsStream(expresTtl);
			OntModel expressModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			expressModel.read(expresTtlStream, null, "TTL");
			
			String rdfList = "/list.ttl";
			InputStream rdfListStream = IfcConvertor.class.getResourceAsStream(rdfList);
			OntModel listModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			listModel.read(rdfListStream, null, "TTL");

			InputStream fis = IfcConvertor.class.getResourceAsStream("/ent"+exp+".ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			Map<String, EntityVO> ent = null;
			try {
				ent = (Map<String,EntityVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}

			fis = IfcConvertor.class.getResourceAsStream("/typ"+exp+".ser");
			ois = new ObjectInputStream(fis);
			Map<String, TypeVO> typ = null;
			try {
				typ = (Map<String,TypeVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}
			
			String ontURI = "http://ifcowl.openbimstandards.org/" + exp;
			
			IfcConvertorStream conv = new IfcConvertorStream(om, expressModel, listModel, new FileInputStream(ifc_file), baseURI, ent, typ, ontURI);
			conv.setIfcReader(this);
			FileOutputStream out=new FileOutputStream(output_file);
			System.out.println("started parsing stream");
			conv.parseModel2Stream(out);		
			System.out.println("finished!!");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
//		if(logToFile){
//			Model RDFModel = org.apache.jena.util.FileManager.get().loadModel(inputfile, inputtype);
//			InfModel infmodel = ModelFactory.createRDFSModel(RDFModel);
//			if (om != null && model != null) {
//				boolean valid = validateGeneratedModel(om, model);
//				if (valid == true) {
//					writeTTLRDFFiles(model, output_file);
//				} else {
//					System.err.println("The generated RDF model is invalid");
//					System.exit(1);
//				}
//				long t1 = System.currentTimeMillis();
//				System.out.println("done in " + ((t1 - t0) / 1000.0) + " seconds.");
//				if(logToFile) bw.write("done in " + ((t1 - t0) / 1000.0) + " seconds." + "\r\n");
//			} else {
//				System.out
//						.println("No ontologyModel or instanceModel found -> no files generated.");
//				if(logToFile) bw.write("No ontologyModel or instanceModel found -> no files generated." + "\r\n");
//			}
//		}
	}
		
	
	/*  These methods are created to handle the FX interface and drag&drop features */	
	public void convert(String ifc_file, String output_file, String baseURI,FxInterface fx_gui) throws IOException {

		if (!ifc_file.endsWith(".ifc")) {
			ifc_file += ".ifc";
		}

		String exp = getExpressSchema(ifc_file);

		// check if we are able to convert this: only four schemas are supported
		if (!exp.equalsIgnoreCase("IFC2X3_Final")
				&& !exp.equalsIgnoreCase("IFC2X3_TC1")
				&& !exp.equalsIgnoreCase("IFC4_ADD1")
				&& !exp.equalsIgnoreCase("IFC4")){
			fx_gui.handle_notification("ERROR: Unrecognised EXPRESS schema: " + exp + ".");
			fx_gui.handle_notification("File should be in IFC4 or IFC2X3 schema. Stopping conversion.");
			return;
		}

		// CONVERSION
		OntModel om = null;
		
		InputStream in = null;
		InputStream expin = null;
		FileOutputStream out=null;
		try {			
			om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			in = IfcReader.class.getResourceAsStream("/" + exp + ".ttl");
			om.read(in, null, "TTL");

			expin = IfcConvertor.class.getResourceAsStream("/" + exp + ".exp");

			String expresTtl = "/express.ttl";
			InputStream expresTtlStream = IfcConvertor.class.getResourceAsStream(expresTtl);
			OntModel expressModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			expressModel.read(expresTtlStream, null, "TTL");
			
			String rdfList = "/list.ttl";
			InputStream rdfListStream = IfcConvertor.class.getResourceAsStream(rdfList);
			OntModel listModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			listModel.read(rdfListStream, null, "TTL");
			
			InputStream fis = IfcConvertor.class.getResourceAsStream("/ent"+exp+".ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			Map<String, EntityVO> ent = null;
			try {
				ent = (Map<String,EntityVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}

			fis = IfcConvertor.class.getResourceAsStream("/typ"+exp+".ser");
			ois = new ObjectInputStream(fis);
			Map<String, TypeVO> typ = null;
			try {
				typ = (Map<String,TypeVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}
			
			String ontURI = "http://ifcowl.openbimstandards.org/" + exp;
			
			IfcConvertorStream conv = new IfcConvertorStream(om, expressModel, listModel, new FileInputStream(ifc_file), baseURI, ent, typ, ontURI);
			conv.setIfcReader(this);
			out=new FileOutputStream(output_file);
			conv.parseModel2Stream(out);		
			fx_gui.handle_notification(ifc_file+ " converted to RDF.");
		} catch (FileNotFoundException e1) {
			fx_gui.handle_notification(e1.getMessage());
		} finally {
			try {
				out.close();
				in.close();
			} catch (Exception e1) {
				fx_gui.handle_notification(e1.getMessage());
			}
			try {
				expin.close();
			} catch (Exception e1) {
				fx_gui.handle_notification(e1.getMessage());
			}
		}
	}

	public void convert(String ifc_file, File output_file, String baseURI,FxInterface fx_gui) throws IOException {

		if (!ifc_file.endsWith(".ifc")) {
			ifc_file += ".ifc";
		}

		String exp = getExpressSchema(ifc_file);

		// check if we are able to convert this: only four schemas are supported
		if (!exp.equalsIgnoreCase("IFC2X3_Final")
				&& !exp.equalsIgnoreCase("IFC2X3_TC1")
				&& !exp.equalsIgnoreCase("IFC4_ADD1")
				&& !exp.equalsIgnoreCase("IFC4")){
			fx_gui.handle_notification("ERROR: Unrecognised EXPRESS schema: " + exp + ".");
			fx_gui.handle_notification("File should be in IFC4 or IFC2X3 schema. Stopping conversion.");
			return;
		}

		// CONVERSION
		OntModel om = null;
		
		InputStream in = null;
		FileOutputStream out=null;
		try {			
			om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			in = IfcReader.class.getResourceAsStream("/" + exp + ".ttl");
			om.read(in, null, "TTL");

			String expresTtl = "/express.ttl";
			InputStream expresTtlStream = IfcConvertor.class.getResourceAsStream(expresTtl);
			OntModel expressModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			expressModel.read(expresTtlStream, null, "TTL");
			
			String rdfList = "/list.rdf";
			InputStream rdfListStream = IfcConvertor.class.getResourceAsStream(rdfList);
			OntModel listModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			listModel.read(rdfListStream, null, "RDF/XML");

			InputStream fis = IfcConvertor.class.getResourceAsStream("/ent"+exp+".ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			Map<String, EntityVO> ent = null;
			try {
				ent = (Map<String,EntityVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}

			fis = IfcConvertor.class.getResourceAsStream("/typ"+exp+".ser");
			ois = new ObjectInputStream(fis);
			Map<String, TypeVO> typ = null;
			try {
				typ = (Map<String,TypeVO>) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				ois.close();
			}

			String ontURI = "http://ifcowl.openbimstandards.org/" + exp;
			IfcConvertorStream conv = new IfcConvertorStream(om, expressModel, listModel, new FileInputStream(ifc_file), baseURI, ent, typ, ontURI);
			conv.setIfcReader(this);
			out=new FileOutputStream(output_file);
			conv.parseModel2Stream(out);
			fx_gui.handle_notification(ifc_file+ " converted to RDF.");
		} catch (FileNotFoundException e1) {
			fx_gui.handle_notification(e1.getMessage());
		} finally {
			try {
				out.close();
				in.close();
			} catch (Exception e1) {
				fx_gui.handle_notification(e1.getMessage());
			}
			try {
			} catch (Exception e1) {
				fx_gui.handle_notification(e1.getMessage());
			}
		}
	}
}