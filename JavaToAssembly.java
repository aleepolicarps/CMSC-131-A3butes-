package program_proper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class JavaToAssembly {
	
	private ArrayList<String> code = new ArrayList<String>();
	private ArrayList<String> toPrint = new ArrayList<String>();
	private ArrayList<String> stringVariables = new ArrayList<String>();
	private StringBuilder asm = new StringBuilder();
	
	public JavaToAssembly(String fileName) {
		loadFile(fileName);
		cleanCode();
		parseCode();
		makeAssemblyCode();
		outputToAsm(fileName);
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
	private void outputToAsm(String fileName){
		BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter outputStream = null;
		
		String asmFileName = fileName.substring(0, fileName.indexOf("."));
		try
		{
			outputStream = new PrintWriter(new FileOutputStream(asmFileName+".asm"));
			outputStream.println(asm.toString());
			outputStream.close();
		}
		catch(FileNotFoundException e){
			System.out.println("File not found.");
		}
		catch(IOException e){
			System.out.println("Error");
		}	
	}
	
	private void cleanCode(){
		for(int i = 0; i<code.size();i++){
			if(code.get(i).contains("package")){
				code.remove(i);
				i--;
			}			
			else if(code.get(i).contains("import")){
				code.remove(i);
				i--;
			}			
			else if(code.get(i).contains("class")){
				code.remove(code.size()-1);
				code.remove(i);
				i--;
			}			
			else if(code.get(i).contains("public static void main(String[] args)")){
				code.remove(code.size()-1);
				code.remove(i);
				i--;
			}			
		}
		
	}
	private void  parseCode(){
		for(String element:code){
			if(element.contains("System.out.println(\"")){
				element=element.substring(20);
				element=element.substring(0,element.indexOf("\""));
				toPrint.add(element);
			}
			else if(element.contains("System.out.print(\"")){
				element=element.substring(18);
				element=element.substring(0,element.indexOf("\""));
				toPrint.add(element);
			}
			else if(isVariableDeclaration(element)==1){
				String leftHand = element.substring(7, element.indexOf("="));
				leftHand = leftHand.replace(" ", "");
				stringVariables.add(leftHand);
				String rightHand = element.substring(element.indexOf("\"")+1);
				rightHand = rightHand.substring(0,rightHand.length()-2);
				stringVariables.add(rightHand);
			}
			else if(isVariableDeclaration(element)==2){
				//element = element.substring(7);
				//stringVariables.add(element);
			}
		}
	}
	private void makeAssemblyCode(){
		asm.append(".model small"+"\n");
		asm.append(".stack 100h"+"\n");
		asm.append(".data"+"\n");
		//toPrint
		for(int i = 0; i<toPrint.size();i++){
			asm.append("var"+Integer.toString(i)+" db \""+toPrint.get(i)+"\",\"$\"\n");
		}
		for(int i=0; i<stringVariables.size();i=i+2){
			asm.append(stringVariables.get(i)+" db \""+stringVariables.get(i+1)+"\",\"$\"\n");
		}
		asm.append(".code"+"\n");
		asm.append("main proc"+"\n");
		asm.append("mov ax, @data"+"\n");
		asm.append("mov ds, ax"+"\n");
		////////MAIN////////////
		
		for(String line:code){
			if(line.contains("System.out.print")){
				if(line.contains("\"")){
					for(int i=0;i<toPrint.size();i++){
						if(line.contains(toPrint.get(i))){
							asm.append("mov dx, offset "+"var"+Integer.toString(i)+"\n");
							asm.append("mov ah, 09h"+"\n");
							asm.append("int 21h"+"\n");
							if(line.contains("println")){
								asm.append("mov dl, 0ah"+"\n");
								asm.append("mov ah, 02h"+"\n");
								asm.append("int 21h"+"\n");
							}
							break;	
						}
					}
				}
				else{
					for(int i=0;i<stringVariables.size();i=i+2){
						if(line.contains(stringVariables.get(i))){
							asm.append("mov dx, offset "+stringVariables.get(i)+"\n");
							asm.append("mov ah, 09h"+"\n");
							asm.append("int 21h"+"\n");
							if(line.contains("println")){
								asm.append("mov dl, 0ah"+"\n");
								asm.append("mov ah, 02h"+"\n");
								asm.append("int 21h"+"\n");
							}
							break;	
						}
					}
				}
			}
		}
		
		////////END OF MAIN//////
		asm.append("mov ax, 4c00h"+"\n");
		asm.append("int 21h"+"\n");
		asm.append("main endp"+"\n");
		asm.append("end main"+"\n");
		
		
		System.out.println(asm.toString());
	}
	private int isVariableDeclaration(String line){
		if(line.contains("String"))
			return 1;
		if(line.contains("int"))
			return 2;
		
		else
			return 0;
	}
	

}
