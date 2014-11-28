package assembler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class AssemblyToJava {
	
	
	private ArrayList<String> code = new ArrayList<String>();
	private ArrayList<String> stringVar = new ArrayList<String>();
	private ArrayList<String> intVar = new ArrayList<String>();
	private ArrayList<String> javaMain = new ArrayList<String>();
	private StringBuilder java = new StringBuilder();
	String fileName = "";
	
	public AssemblyToJava(String fileName){
		this.fileName = fileName.substring(0, fileName.indexOf("."));
		loadFile(fileName);
		getVariables();
		getMain();
		analyzeCode();
		printJavaFile();
	}
	private void loadFile(String fileName){
		File file = new File(fileName);
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader(file));
			String source = reader.readLine();
			while(source!=null){
				source=source.replace("\n", "");
				source=source.replace("\t", "");
				source=source.trim();
				if(!source.equals(""))
					code.add(source);
				source = reader.readLine();
			}
			reader.close();
		}
		catch(FileNotFoundException e){
			System.out.println("File not found.");
		}
		catch(IOException e){
			System.out.println("Error");
		}
	}
	private void getVariables(){
		boolean onDataBlock = false;
		ArrayList<String> varDeclarations = new ArrayList<String>();
		for(String line:code){
			if(onDataBlock && line.contains(".")){
				break;
			}
			if(line.equals(".data")){
				onDataBlock = true;
			}
			else if(onDataBlock){
				varDeclarations.add(line);
			}
		}
		//removes extra empty line
		varDeclarations.remove("");
		
		for(String line:varDeclarations){
			if(line.contains("\"")){
				String[] splitLine;
				splitLine = line.split(" db ");
				stringVar.add(splitLine[0]);
				String temp = splitLine[1];
				temp = temp.substring(1);
				temp = "\"" + temp.substring(0, temp.indexOf("\"")+1);
				stringVar.add(temp);
			}
			else{
				String[] splitLine;
				if(line.contains(" db "))
					splitLine = line.split(" db ");
				else
					splitLine = line.split(" dw ");
				if(splitLine[1].matches("[0-9]+")){
					intVar.add(splitLine[0]);
					intVar.add(splitLine[1]);
				}
			}
		}
	}
	private void getMain(){
		boolean onMainBlock = false;;
		ArrayList<String> mainCode = new ArrayList<String>();
		for(String line:code){
			if(onMainBlock && line.equals("mov ax, 4c00h")){
				break;
			}
			if(line.equals("mov ds, ax")){
				onMainBlock = true;
			}
			else if(onMainBlock){
				mainCode.add(line);
			}
		}
		code = (ArrayList<String>) mainCode.clone();
	}
	private void analyzeCode(){
		
		for(String line:code){
			line = line.trim();
		}
		
		String ifLabel = "";
		String endIfLabel = "";
		String endWhileLabel = "";
		String loopLabel = "";
		int doIndex = 0;
		Boolean ifLabelSet=false;
		Boolean ifEndLabelSet=false;
		Boolean whileSet = false;
		
		for(int i=0;i<code.size();i++){
			String currLine = code.get(i);
			//System.out.println(currLine);
			if(currLine.contains("inc "))
				javaMain.add(inc(currLine));
			else if(currLine.contains("dec "))
				javaMain.add(dec(currLine));
			else if(currLine.contains("add "))
				javaMain.add(add(currLine));
			else if(currLine.contains("sub "))
				javaMain.add(sub(currLine));
			else if(currLine.contains("mov ")){
				if(currLine.contains("offset "))
					currLine = currLine.replace("offset", "");
				javaMain.add(mov(currLine));
			}
			else if(currLine.contains("lea dx")){
				currLine = currLine.replace("lea", "mov");
				javaMain.add(mov(currLine));
			}
			else if(currLine.contains("int 21h"))
				javaMain.add(print());
			else if(currLine.contains("mul "))
				javaMain.add(mul());
			else if(currLine.contains("div "))
				javaMain.add(div());
			else if(currLine.contains("cmp ")&&!whileSet){
				String first = currLine.substring(currLine.indexOf("cmp")+4,currLine.indexOf(","));
				String second = currLine.substring(currLine.indexOf(",")+1).replace(" ", "");
				String nextLine = code.get(i+1);
				String jumpType = nextLine.substring(0,nextLine.indexOf(" "));
				ifLabel = nextLine.substring(nextLine.indexOf(" ")+1);
				ifLabelSet = true;
				String sample = "if("+first+comparator(jumpType)+second+"){";
				javaMain.add(sample);
			}
			else if(whileSet && currLine.contains("cmp") && code.get(i+1).contains(loopLabel)){
				
				javaMain.add(doIndex, "do{");
				String first = currLine.substring(currLine.indexOf("cmp")+4,currLine.indexOf(","));
				String second = currLine.substring(currLine.indexOf(",")+1).replace(" ", "");
				String nextLine = code.get(i+1);
				String jumpType = nextLine.substring(0,nextLine.indexOf(" "));
				javaMain.add("}while(!("+first+comparator(jumpType)+second+"));");
				whileSet = false;
			}
			else if(whileSet && currLine.contains("cmp")){
				String first = currLine.substring(currLine.indexOf("cmp")+4,currLine.indexOf(","));
				String second = currLine.substring(currLine.indexOf(",")+1).replace(" ", "");
				String nextLine = code.get(i+1);
				String jumpType = nextLine.substring(0,nextLine.indexOf(" "));
				String sample = "while("+first+comparator(jumpType)+second+"){";
				endWhileLabel = nextLine.substring(nextLine.indexOf(" ")+1);
				javaMain.add(sample);
			}
			else if(whileSet && currLine.equals(endWhileLabel+":")){
				javaMain.add("}");
				whileSet=false;
			}
			else if(ifLabelSet && currLine.contains("jmp")){
				endIfLabel = currLine.substring(4);
				ifEndLabelSet = true;
			}
			else if(ifLabelSet && currLine.equals(ifLabel+":")){
				javaMain.add("}");
				ifLabelSet = false;
				
				if(ifEndLabelSet){
					javaMain.add("else{");
				}	
			}
			else if(ifEndLabelSet && currLine.equals(endIfLabel+":")){
				ifEndLabelSet = false;
				javaMain.add("}");
			}
			else if(!ifEndLabelSet && !ifLabelSet && currLine.contains(":")){
				whileSet = true;
				loopLabel = currLine.substring(0, currLine.indexOf(":"));
				doIndex = javaMain.size();
			}
		}
	}
	
	private void printJavaFile(){
		fileName = fileName.substring(0,1).toUpperCase() + fileName.substring(1);
		java.append("public class "+fileName+ " {\n");
		java.append("public static void main(String[] args){\n");
		
		for(int i = 0; i< intVar.size();i++){
			java.append("int "+ intVar.get(i) + " = "+intVar.get(++i)+";\n");
		}
		for(int i = 0; i< stringVar.size();i++){
			java.append("String "+ stringVar.get(i) + " = "+stringVar.get(++i)+";\n");
		}
		
		java.append("int ax = 0;\n");
		java.append("int bx = 0;\n");
		java.append("int cx = 0;\n");
		java.append("int dx = 0;\n");
		java.append("int al = 0;\n");
		java.append("int ah = 0;\n");
		java.append("int bl = 0;\n");
		java.append("int bh = 0;\n");
		java.append("int cl = 0;\n");
		java.append("int ch = 0;\n");
		java.append("int dl = 0;\n");
		java.append("int dh = 0;\n");
		
		
		for(String line:javaMain){
			java.append(line + "\n");
		}
		
		java.append("}\n}");
		
		BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter outputStream = null;
		
		try
		{
			outputStream = new PrintWriter(new FileOutputStream(fileName+".java"));
			outputStream.println(java.toString());
			outputStream.close();
		}
		catch(FileNotFoundException e){
			System.out.println("File not found.");
		}
		catch(IOException e){
			System.out.println("Error");
		}	
	}
	
	private String inc(String line){
		line = line.substring(4);
		line = line + "++;";
		return line;
	}
	private String dec(String line){
		line = line.substring(4);
		line = line + "--;";
		return line;
	}
	private String mov(String line){
		line = line.substring(4);
		line = line.replace(" ","");
		line = line.replace(",", "=");
		line = line + ";";
		return line;
	}
	private String add(String line){
		line = line.substring(4);
		line = line.replace(" ","");
		String[] operand = line.split(",");
		line = operand[0] +"="+ operand[0]+"+"+operand[1]+";";
		return line;
	}
	private String sub(String line){
		line = line.substring(4);
		line = line.replace(" ","");
		String[] operand = line.split(",");
		line = operand[0] +"="+ operand[0]+"-"+operand[1]+";";
		return line;
	}
	private String print(){
		String line = "";
		String secondLine = javaMain.remove(javaMain.size()-1);
		String firstLine = javaMain.remove(javaMain.size()-1);
		firstLine = firstLine.substring(firstLine.indexOf("=")+1,firstLine.length()-1);
		line = "System.out.print("+firstLine+");";		
		return line;
	}
	private String mul(){
		String line = "";
		String secondLine = javaMain.remove(javaMain.size()-1);
		secondLine = secondLine.substring(secondLine.indexOf("=")+1,secondLine.length()-1);
		String firstLine = javaMain.remove(javaMain.size()-1);
		firstLine = firstLine.substring(firstLine.indexOf("=")+1,firstLine.length()-1);
		line = "ax="+firstLine+"*"+secondLine+";";
		return line;
	}
	private String div(){
		String line = "";
		String secondLine = javaMain.remove(javaMain.size()-1);
		secondLine = secondLine.substring(secondLine.indexOf("=")+1,secondLine.length()-1);
		String firstLine = javaMain.remove(javaMain.size()-1);
		firstLine = firstLine.substring(firstLine.indexOf("=")+1,firstLine.length()-1);
		line = "al="+firstLine+"*"+secondLine+";\n";
		line = line + "ah="+firstLine+"%"+secondLine+";"; 
		return line;
	}
	private String comparator(String line){
		switch(line){
		case "jne": return "==";
		case "je": return "!=";
		case "jl": return ">=";
		case "jg": return "<=";
		case "jle": return ">";
		case "jge": return "<";
		}
		return "";
	}
	
}
