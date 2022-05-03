package uk.ac.abdn.safeHaven2Prov;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import uk.ac.abdn.knowledgebase.CsvParser;
import uk.ac.abdn.knowledgebase.EmbeddedModel;



	@org.springframework.stereotype.Controller
	public class Controller {
		
		
		@GetMapping("/researcherReport")
		public String researcherReport( Model model) {
			
			String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX owl: <http://www.w3.org/2002/07/owl#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX dash:<https://w3id.org/shp#> PREFIX prov:<http://www.w3.org/ns/prov#>"; 
				
			
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource queriesFile = resourceLoader.getResource("/queries/queries.csv");
			
			EmbeddedModel semModel = new EmbeddedModel ();  
			String example = Controller.class.getResource("/data/DaSH_2db-example_turtle.ttl").getFile();
			Reader reader;
			
			semModel.loadData(example);     
		        System.out.println(semModel.getModel().size());
		     
			
			try {
				reader = new InputStreamReader(queriesFile.getInputStream());
				 try (CSVReader csvReader = new CSVReader(reader)) {
			            List<String[]> r = csvReader.readAll();
			            
			            for (int i=0;i<r.size();i++) {
			            	 System.out.println(r.get(i)[1]);
			            	 Query query = QueryFactory.create(prefixes + " "+ r.get(i)[1].replaceAll("\uFEFF", ""));
			            	 System.out.println(query);
			      		     QueryExecution qExe = QueryExecutionFactory.create( query, semModel.getModel());
			      	         ResultSet results = qExe.execSelect();
			      	         System.out.println(r.get(i)[0]);
			      	         
			      	         ArrayList <HashMap <String,String>> list = new ArrayList <HashMap <String,String>> ();
			      	         
			      	         while (results.hasNext()) {
			      	        	QuerySolution sol = results.next();
			      	        	Iterator<String> it = sol.varNames();
			      	        	HashMap <String, String> map = new HashMap <String, String> ();
			      	        	while (it.hasNext()) {
			      	        		String varName = it.next();
			      	        		map.put(varName,parsePrefix( sol.get(varName).toString()));
			      	        	}
			      	        	
			      	        	list.add(map);
			      	         }
			      	        
			      	        
			      	         model.addAttribute(r.get(i)[0], list);
			      	         System.out.println(list);
			            }

			        } catch (IOException | CsvException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	       
			
			//validate GRAPH
			

			//validate if all dataresources defined in the linkage plan were also used in data selection activities
			ArrayList <HashMap <String,String>> entitiesLinked = (ArrayList<HashMap<String, String>>) model.getAttribute("Q8");
			System.out.println("--------------------------------");
			System.out.println(entitiesLinked);
			
			
			ArrayList <String> msg8 = new ArrayList <String> ();
			for (int i =0 ; i< entitiesLinked.size();i++) {
				HashMap<String, String> map = entitiesLinked.get(i);
				
				int linked = Integer.parseInt(map.get("cohortMembersLinked"));
						
				int notLinked = Integer.parseInt(map.get("cohortMembersNotLinked"));
				
				int total = Integer.parseInt(map.get("cohortTotal"));
				
				if (linked+notLinked != total ) {
				
				msg8.add("Numbers of linked and not linked patients in dataset "+map.get("dataset")+ " don't match the total cohort size  "+ map.get("cohortTotal"));
			
				}
			}
			
			model.addAttribute("Q8report",msg8);
			
			
			//validate if all nhs datasets on the derivation chain have the same number of records 
			ArrayList <HashMap <String,String>> values = (ArrayList<HashMap<String, String>>) model.getAttribute("Q10-numberRecordsCheck");
			HashMap <String,HashSet<?>[]>  roots = new HashMap <String,HashSet<?>[]> ();
			HashSet <String> violationRoots = new HashSet <String> ();
			for (int i = 0; i<values.size();i++ ) {
				if (!roots.containsKey(values.get(i).get("root"))) {
					HashSet<String> records = new HashSet <String> ();
					HashSet<String> datasets = new HashSet <String> ();
					records.add(values.get(i).get("records"));
					datasets.add(values.get(i).get("dataset"));
					HashSet <String> [] set=  new HashSet   [2];
					set[0] = records;
					set[1] = datasets;
					
					roots.put(values.get(i).get("root"),set); 
					
				}
				else {
					HashSet <String> [] set = (HashSet<String>[]) roots.get(values.get(i).get("root")); 
					set[0].add(values.get(i).get("records"));
					set[1].add(values.get(i).get("dataset"));
					
					if (set[0].size()>1) {
						violationRoots.add(values.get(i).get("root"));
					}
				}
			}
			
			//check which derivation paths have violations and generate message
			ArrayList <String> msg = new ArrayList <String> ();
			Iterator<String> it = violationRoots.iterator();
			while (it.hasNext()) {
			  String rootKey = it.next();
			  HashSet <String> [] set = (HashSet<String>[]) roots.get(rootKey);
			  msg.add("The number of records in the following NHS datasets should be the same: "+ set[1]);
			}	
			model.addAttribute("Q10report",msg);
	       
			
			//validate if all dataresources defined in the linkage plan were also used in data selection activities
			ArrayList <HashMap <String,String>> entitiesNotUsed = (ArrayList<HashMap<String, String>>) model.getAttribute("Q14-usageCheck");
			System.out.println("--------------------------------");
			System.out.println(entitiesNotUsed);
			
			ArrayList <String> msg14 = new ArrayList <String> ();
			for (int i =0 ; i< entitiesNotUsed.size();i++) {
				HashMap<String, String> map = entitiesNotUsed.get(i);
				msg14.add("The dataResource "+map.get("planDataSourceId")+ " defined in the linkage plan  "+ map.get("linkagePlan")+" was never used in any data selection activity");
			}
			
			model.addAttribute("Q14report",msg14);
			
			
			
			
			
			System.out.println(msg);
			
			return "researcherReport";
		}
	
		
		@GetMapping("/")
		public String index( Model model) {
			
			String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX owl: <http://www.w3.org/2002/07/owl#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX dash:<https://w3id.org/shp#> PREFIX prov:<http://www.w3.org/ns/prov#>"; 
				
			
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource queriesFile = resourceLoader.getResource("/queries/queries.csv");
			
			EmbeddedModel semModel = new EmbeddedModel ();  
			String example = Controller.class.getResource("/data/DaSH_2db-example_turtle.ttl").getFile();
			Reader reader;
			
			semModel.loadData(example);     
		        System.out.println(semModel.getModel().size());
		     
			
			try {
				reader = new InputStreamReader(queriesFile.getInputStream());
				 try (CSVReader csvReader = new CSVReader(reader)) {
			            List<String[]> r = csvReader.readAll();
			            
			            for (int i=0;i<r.size();i++) {
			            	 System.out.println(r.get(i)[1]);
			            	 Query query = QueryFactory.create(prefixes + " "+ r.get(i)[1].replaceAll("\uFEFF", ""));
			            	 System.out.println(query);
			      		     QueryExecution qExe = QueryExecutionFactory.create( query, semModel.getModel());
			      	         ResultSet results = qExe.execSelect();
			      	         System.out.println(r.get(i)[0]);
			      	         
			      	         ArrayList <HashMap <String,String>> list = new ArrayList <HashMap <String,String>> ();
			      	         
			      	         while (results.hasNext()) {
			      	        	QuerySolution sol = results.next();
			      	        	Iterator<String> it = sol.varNames();
			      	        	HashMap <String, String> map = new HashMap <String, String> ();
			      	        	while (it.hasNext()) {
			      	        		String varName = it.next();
			      	        		map.put(varName,parsePrefix( sol.get(varName).toString()));
			      	        	}
			      	        	
			      	        	list.add(map);
			      	         }
			      	        
			      	        
			      	         model.addAttribute(r.get(i)[0], list);
			      	         System.out.println(list);
			            }

			        } catch (IOException | CsvException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	       
			
			//validate GRAPH
			

			//validate if all dataresources defined in the linkage plan were also used in data selection activities
			ArrayList <HashMap <String,String>> entitiesLinked = (ArrayList<HashMap<String, String>>) model.getAttribute("Q8");
			System.out.println("--------------------------------");
			System.out.println(entitiesLinked);
			
			
			ArrayList <String> msg8 = new ArrayList <String> ();
			for (int i =0 ; i< entitiesLinked.size();i++) {
				HashMap<String, String> map = entitiesLinked.get(i);
				
				int linked = Integer.parseInt(map.get("cohortMembersLinked"));
						
				int notLinked = Integer.parseInt(map.get("cohortMembersNotLinked"));
				
				int total = Integer.parseInt(map.get("cohortTotal"));
				
				if (linked+notLinked != total ) {
				
				msg8.add("Numbers of linked and not linked patients in dataset "+map.get("dataset")+ " don't match the total cohort size  "+ map.get("cohortTotal"));
			
				}
			}
			
			model.addAttribute("Q8report",msg8);
			
			
			//validate if all nhs datasets on the derivation chain have the same number of records 
			ArrayList <HashMap <String,String>> values = (ArrayList<HashMap<String, String>>) model.getAttribute("Q10-numberRecordsCheck");
			HashMap <String,HashSet<?>[]>  roots = new HashMap <String,HashSet<?>[]> ();
			HashSet <String> violationRoots = new HashSet <String> ();
			for (int i = 0; i<values.size();i++ ) {
				if (!roots.containsKey(values.get(i).get("root"))) {
					HashSet<String> records = new HashSet <String> ();
					HashSet<String> datasets = new HashSet <String> ();
					records.add(values.get(i).get("records"));
					datasets.add(values.get(i).get("dataset"));
					HashSet <String> [] set=  new HashSet   [2];
					set[0] = records;
					set[1] = datasets;
					
					roots.put(values.get(i).get("root"),set); 
					
				}
				else {
					HashSet <String> [] set = (HashSet<String>[]) roots.get(values.get(i).get("root")); 
					set[0].add(values.get(i).get("records"));
					set[1].add(values.get(i).get("dataset"));
					
					if (set[0].size()>1) {
						violationRoots.add(values.get(i).get("root"));
					}
				}
			}
			
			//check which derivation paths have violations and generate message
			ArrayList <String> msg = new ArrayList <String> ();
			Iterator<String> it = violationRoots.iterator();
			while (it.hasNext()) {
			  String rootKey = it.next();
			  HashSet <String> [] set = (HashSet<String>[]) roots.get(rootKey);
			  msg.add("The number of records in the following NHS datasets should be the same: "+ set[1]);
			}	
			model.addAttribute("Q10report",msg);
	       
			
			//validate if all dataresources defined in the linkage plan were also used in data selection activities
			ArrayList <HashMap <String,String>> entitiesNotUsed = (ArrayList<HashMap<String, String>>) model.getAttribute("Q14-usageCheck");
			System.out.println("--------------------------------");
			System.out.println(entitiesNotUsed);
			
			ArrayList <String> msg14 = new ArrayList <String> ();
			for (int i =0 ; i< entitiesNotUsed.size();i++) {
				HashMap<String, String> map = entitiesNotUsed.get(i);
				msg14.add("The dataResource "+map.get("planDataSourceId")+ " defined in the linkage plan  "+ map.get("linkagePlan")+" was never used in any data selection activity");
			}
			
			model.addAttribute("Q14report",msg14);
			
			
			
			
			
			System.out.println(msg);
			
			return "index";
		}
		
		
		private String parsePrefix (String input) {
			String fixPrefix = input.replace("https://w3id.org/shp#", "dash:");
			if (fixPrefix.indexOf("^") > -1 ) {
			 return fixPrefix.substring(0,fixPrefix.indexOf("^"));
			}
			else {
				return fixPrefix;
			}
		}
		
	}

