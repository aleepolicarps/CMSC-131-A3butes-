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
	private ArrayList<String> intVariables = new ArrayList<String>();
	private StringBuilder asm = new StringBuilder();
	private int codeIndex = 0;
	
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
				String leftHand = element.substring(4, element.indexOf("="));
				leftHand = leftHand.replace(" ", "");
				intVariables.add(leftHand);
				String rightHand = element.substring(element.indexOf("=")+1, element.indexOf(';'));
				rightHand = rightHand.replace(" ", "");
				intVariables.add(rightHand);
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
		for(int i=0; i<intVariables.size();i=i+2){
			asm.append(intVariables.get(i));
			int x = Integer.parseInt(intVariables.get(i+1));
			if(x<=255)
				asm.append(" db ");
			else 
				asm.append(" dw ");
			asm.append(intVariables.get(i+1)+"\n");
		}
		asm.append(".code"+"\n");
		asm.append("main proc"+"\n");
		asm.append("mov ax, @data"+"\n");
		asm.append("mov ds, ax"+"\n");
		////////MAIN////////////
		int labelCounter = 1;
		boolean isBlock = false;
		boolean hasNoBraces = false;
		boolean isBlockElse = false;
		boolean hasNoBracesElse = false;
		for(int j = 0; j<code.size();j++){
			String line = code.get(j);
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
					String x = line.substring(line.indexOf("(")+1, line.indexOf(")"));
					if(stringVariables.contains(x)){
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
					else{
						for(int i=0;i<intVariables.size();i=i+2){
							if(line.contains(intVariables.get(i))){
								int y = Integer.parseInt(intVariables.get(i+1));
								if(y<=255)
									asm.append("mov dl, "+intVariables.get(i)+"\n");
								else{
									asm.append("xor dx, dx"+"\n");
									asm.append("mov dx, "+intVariables.get(i)+"\n");
								}
								asm.append("mov ah, 02h"+"\n");
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
			if(hasNoBracesElse){
				hasNoBracesElse=false;
				asm.append("endif" + (labelCounter-1) + ":\n\n");
			}
			if(isBlockElse){
				if(line.contains("}")){
					isBlockElse=false;
					asm.append("endif" + (labelCounter-1) + ":\n\n");
				}
			}
			if(hasNoBraces){
				hasNoBraces=false;
				asm.append("jmp endif" + (labelCounter-1) + "\n\n");
				asm.append("else" + (labelCounter-1) + ":\n");
				if(code.size()>j+1 && !code.get(j+1).contains("else"))
					asm.append("endif" + (labelCounter-1) + ":\n");
				else{
					if(code.get(j+1).contains("{") || code.get(j+2).contains("{"))
						isBlockElse=true;
					else{
						hasNoBracesElse=true;
						j++;
					}
				}
			}
			
			if(isBlock){
				if(line.contains("}")){
					isBlock=false;
					asm.append("jmp endif" + (labelCounter-1) + "\n\n");
					asm.append("else" + (labelCounter-1) + ":\n");
					if(code.size()>j+1 && !code.get(j+1).contains("else"))
						asm.append("endif" + (labelCounter-1) + ":\n");
					else{
						if(code.get(j+1).contains("{") || code.get(j+2).contains("{"))
							isBlockElse=true;
						else{
							hasNoBracesElse=true;
							j++;
						}
					}
				}
			}
			
			
			if(line.contains("if")){
				String x = line.substring(line.indexOf("(")+1, line.indexOf(")"));
				String y="";
				String z="";
				String jump = "";
				if(x.contains("==")){
					y = x.substring(0,x.indexOf("="));
					z = x.substring(x.indexOf("=")+2, x.length());
					jump = "je";
				}
				else if(x.contains(">")){
					if(x.contains(">=")){
						z = x.substring(x.indexOf(">")+2, x.length());
						jump = "jge";
					}
					else{
						z = x.substring(x.indexOf(">")+1, x.length());
						jump = "jg";
					}
					y = x.substring(0,x.indexOf(">"));
				}
				else if(x.contains("<")){
					if(x.contains("<=")){
						z = x.substring(x.indexOf("<")+2, x.length());
						jump = "jle";
					}
					else{
						z = x.substring(x.indexOf("<")+1, x.length());
						jump = "jl";
					}
					y = x.substring(0,x.indexOf("<"));
				}
				
					
				if(intVariables.contains(y)){
					for(int i=0;i<intVariables.size();i+=2){
						if(line.contains(intVariables.get(i))){
							asm.append("\ncmp "+intVariables.get(i)+"," + z + "\n");
							asm.append(jump + " then" + labelCounter + "\n");
							break;	
						}
					}
				}
				else{
					asm.append("\ncmp "+y+"," + z + "\n");
					asm.append(jump + " then" + labelCounter + "\n");
				}
				asm.append("jmp " + "else" + labelCounter + "\n");
				asm.append("then" + labelCounter + ":\n");
				labelCounter++;
				if(line.contains("{") || code.get(j+1).contains("{"))
					isBlock = true;
				else
					hasNoBraces = true;
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
		if(line.contains("int "))
			return 2;
		
		else
			return 0;
	}
	

}
