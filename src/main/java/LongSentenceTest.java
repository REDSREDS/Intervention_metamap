
//

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import gov.nih.nlm.nls.metamap.*;


import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */

public class LongSentenceTest {

  public static HashMap<String, String> process(String terms, MetaMapApi api) throws Exception
  {
  	List<String> text = new ArrayList<>();
  	HashMap<String, String> termMap = new LinkedHashMap<String, String>();
	  try {
		  List<Result> resultList = api.processCitationsFromString(terms);
		  if (resultList != null) {
			  for (Result result: resultList) {
				  List<AcronymsAbbrevs> aaList = result.getAcronymsAbbrevs();
				  if (aaList.size() > 0) {
					  text.add("\nAcronyms and Abbreviations:");
					  for (AcronymsAbbrevs e: aaList) {
						  text.add("\nAcronym: " + e.getAcronym());
						  text.add("\nExpansion: " + e.getExpansion());
						  text.add("\nCount list: " + e.getCountList());
						  text.add("\nCUI list: " + e.getCUIList());
					  }
				  } else {
					  text.add("\n None.");
				  }
				  for (Utterance utterance: result.getUtteranceList()) {
					  text.add("\nUtterance:");
					  text.add("\n Id: " + utterance.getId());
					  text.add("\n Utterance text: " + utterance.getString());
					  text.add("\n Position: " + utterance.getPosition());
					  for (PCM pcm: utterance.getPCMList()) {
						  text.add("\nPhrase:");
						  text.add("\n text: " + pcm.getPhrase().getPhraseText());
//						  text.add("\n phrase position" + pcm.getPhrase().getPosition());

						  text.add("\nMappings:");
						  for (Mapping map: pcm.getMappingList()) {
							  text.add("\n Map Score: " + map.getScore());
							  for (Ev mapEv: map.getEvList()) {
								  text.add("\n   Score: " + mapEv.getScore());
								  text.add("\n   Concept Id: " + mapEv.getConceptId());
								  text.add("\n   Concept Name: " + mapEv.getConceptName());
								  text.add("\n   Semantic Types: " + mapEv.getSemanticTypes());
								  text.add("\n   Positional Info: " + mapEv.getPositionalInfo());
								  termMap.put(mapEv.getPositionalInfo().toString(), " (" + mapEv.getConceptId()+", " + mapEv.getConceptName() +")");
							  }
						  }
					  }
				  }
			  }
		  } else {
			  text.add("\nNo result");
		  }
	  } catch (Exception e) {
		  text.add("\nError when querying Prolog Server: " +
				  e.getMessage() + '\n');
    }
	  return termMap;
  }

  public static List<String> readInter(String inputPath) {
  	List<String> listInter = new ArrayList<>();
  	try {
		File input = new File(inputPath);
		Scanner scanner = new Scanner(input);
		while(scanner.hasNext()) {
			listInter.add(scanner.nextLine());
		}
	} catch(FileNotFoundException e) {
  		System.out.println(e.getMessage());
	}

  	return listInter;


  }

  public static List<String> readCSV(String inputPath) {
  	List<String> listInter = new ArrayList<>();
	  try {

		  // Create an object of filereader
		  // class with CSV file as a parameter.
		  FileReader filereader = new FileReader(inputPath);

		  // create csvReader object passing
		  // file reader as a parameter
		  CSVReader csvReader = new CSVReader(filereader);
		  String[] nextRecord;

		  // we are going to read data line by line
		  while ((nextRecord = csvReader.readNext()) != null) {
			  listInter.add(nextRecord[2]);
		  }
	  }
	  catch (Exception e) {
		  e.printStackTrace();
	  }
	  return listInter;
  }

  public static void saveInter(String outPath, List<String> listInter, List<String> lines) {
	  File file = new File(outPath);
	  try {
		  // create FileWriter object with file as parameter
		  FileWriter outputfile = new FileWriter(file);

		  // create CSVWriter object filewriter object as parameter
		  CSVWriter writer = new CSVWriter(outputfile);

		  // adding header to csv
		  String[] header = { "raw_text", "concepts", "pattern" };
		  writer.writeNext(header);

		  // add data to csv
		  for(int i = 0; i < listInter.size(); i++) {
		  	String[] line = new String[3];
		  	line[0] = listInter.get(i);
		  	line[1] = lines.get(i) == "" ? "none" : lines.get(i);
		  	writer.writeNext(line);
		  }

		  // closing writer connection
		  writer.close();
	  }
	  catch (IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  }
  }

  public static List<String> extractPattern(List<String> listInter, MetaMapApi api) throws Exception {
	  List<String> lines = new ArrayList<>();
	  for(String item : listInter) {
		  StringBuilder stringBuilder = new StringBuilder();
		  HashMap<String, String> termMap = process(item, api);
		  int start = 0;
		  int end = 0;
		  for (Map.Entry<String, String> entry : termMap.entrySet()) {
			  String position = entry.getKey();
			  System.out.println(item);
			  System.out.println(position);
			  Pattern p = Pattern.compile("\\d+");
			  Matcher m = p.matcher(position);
			  List<Integer> nums = new ArrayList<>();
			  while (m.find()) {
				  nums.add(Integer.valueOf(m.group()));
			  }
			  if(nums.get(0) < start) {
			  	continue;
			  }
			  end = nums.get(0);
			  if(end != 0) {
				  stringBuilder.append(item.substring(start, end));
			  }

			  stringBuilder.append("{ }");
			  start = nums.get(nums.size() - 2) + nums.get(nums.size() - 1);

		  }
		  stringBuilder.append(item.substring(start));
		  lines.add(stringBuilder.toString());
	  }
	  return lines;
  }

  public static List<String> getConcepts(List<String> listInter, MetaMapApi api) throws Exception{
  	List<String> lines = new ArrayList<>();
  	for(String item : listInter) {
  		StringBuilder stringBuilder = new StringBuilder();
  		HashMap<String, String> termMap = process(item, api);
  		for(Map.Entry<String, String> entry : termMap.entrySet()) {
  			String position = entry.getKey();
  			System.out.println(item);
  			System.out.println(position);
			Pattern p = Pattern.compile("\\d+");
			Matcher m = p.matcher(position);
			List<Integer> nums = new ArrayList<>();
			while(m.find()) {
				nums.add(Integer.valueOf(m.group()));
			}
  			int start = nums.get(0);
  			int end = nums.get(nums.size() - 2) + nums.get(nums.size() - 1);
  			System.out.println(item.length());
  			System.out.println(start);
			System.out.println(end);
			System.out.println();

			stringBuilder.append(item.substring(start, end));
			stringBuilder.append(entry.getValue() + " -> ");
		}
  		lines.add(stringBuilder.toString());

	}
  	return lines;
  }

  public static void writePattern(List<String> listPattern, String outPath) {
	  try {
		  FileWriter myWriter = new FileWriter(outPath);
		  for(String item : listPattern) {
			  myWriter.write(item + '\n');
			  myWriter.write('\n');
		  }

		  myWriter.close();
		  System.out.println("Successfully wrote to the file.");
	  } catch (IOException e) {
		  System.out.println("An error occurred.");
		  e.printStackTrace();
	  }
  }

  public static void main(String[] args) 
    throws Exception {
    MetaMapApi api = new MetaMapApiImpl();

//	api.setOptions("-J phsu,anti,clnd,topp,edac,medd,dora");
//	List<String> test = new ArrayList<>();
//    test.add("standard of care plus hydroxychloroquine");
//    test.add("intravenous dexamethasone plus standard care");
	  api.setOptions("-J aapp,antb,bacs,bodm,chem,chvf,chvs,clnd,elii,enzy,hops,horm,imft,irda,inch,nnon,orch,phsu,rcpt,strd,vita,acty,bhvr,dora,evnt,gora,inbe,mcha,ocac,socb,hlca,topp,edac,medd");
	  //extract the concept
//	  List<String> listInter = readInter("/home/dbmi/Documents/covid/intervention_ascii.txt");
//	  saveInter("/home/dbmi/Documents/covid/intervention_concepts_more.csv",listInter, getConcepts(listInter, api));

	  //extract the pattern
//	  List<String> listInter = readCSV("/home/dbmi/Documents/covid/intervention_valid_more.csv");
//	  saveInter("/home/dbmi/Documents/covid/intervention_pattern_more.csv", listInter, extractPattern(listInter, api));

	  // coordination and preposition extract
	  // write to the correct style.
	  List<String> listPattern = readCSV("/home/dbmi/Documents/covid/intervention_concept_pattern.csv");
	  writePattern(listPattern, "/home/dbmi/Documents/covid/intervention_tagging.txt");

  }
}
