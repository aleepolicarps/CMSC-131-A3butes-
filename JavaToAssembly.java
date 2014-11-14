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
			System.exit(1);
		}
		catch(IOException e){
			System.out.println("Error");
			System.exit(1);
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
				String leftHand = "";
				String rightHand = "";
				if(!element.contains("for")){
					leftHand = element.substring(4, element.indexOf("="));
					rightHand = element.substring(element.indexOf("=")+1, element.indexOf(';'));
				}
				else{
					leftHand = element.substring(element.indexOf("int")+3, element.indexOf("="));
					rightHand = element.substring(element.indexOf("=")+1, element.indexOf(';'));
				}
				leftHand = leftHand.replace(" ", "");
				rightHand = rightHand.replace(" ", "");
				intVariables.add(leftHand);
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
		asm.append("mov ds, ax"+"\n\n");
		////////MAIN////////////
		int ifCounter = 1;
		int doWhileCounter = 1;
		int whileCounter = 1;
		int forCounter = 1;
		
		boolean isBlock = false;
		boolean hasNoBraces = false;
		boolean isBlockElse = false;
		boolean hasNoBracesElse = false;
		
		boolean doWhile = false;
		boolean whileBlock = false;
		boolean forBlock = false;
		
		String temp = "";
		
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
				asm.append("endif" + (ifCounter-1) + ":\n\n");
			}
			else if(isBlockElse){
				if(line.contains("}")){
					isBlockElse=false;
					asm.append("\nendif" + (ifCounter-1) + ":\n\n");
				}
			}
			else if(hasNoBraces){
				hasNoBraces=false;
				asm.append("jmp endif" + (ifCounter-1) + "\n\n");
				asm.append("else" + (ifCounter-1) + ":\n");
				if(code.size()>j+1 && !code.get(j+1).contains("else"))
					asm.append("\nendif" + (ifCounter-1) + ":\n\n");
				else{
					if(code.get(j+1).contains("{") || code.get(j+2).contains("{"))
						isBlockElse=true;
					else{
						hasNoBracesElse=true;
						j++;
					}
				}
			}
			
			else if(isBlock){
				if(line.contains("}")){
					isBlock=false;
					asm.append("jmp endif" + (ifCounter-1) + "\n\n");
					asm.append("else" + (ifCounter-1) + ":\n");
					if(code.size()>j+1 && !code.get(j+1).contains("else"))
						asm.append("endif" + (ifCounter-1) + ":\n\n");
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
			
			else if(doWhile){
				if(line.contains("}")){
					doWhile=false;
					String jump = printCmp(line);	
					asm.append(jump + " doWhile" + doWhileCounter + "\n\n");
					doWhileCounter++;
				}
			}
			
			else if(whileBlock){
				if(line.contains("}")){
					whileBlock = false;
					String jump = printCmp(temp);
					asm.append(jump + " while" + whileCounter + "\n\n");
					asm.append("endWhile" + whileCounter + ":\n\n");
					whileCounter++;
				}
			}
			
			else if(forBlock){
				if(line.contains("}")){
					forBlock = false;
					String cmp = temp.substring(temp.indexOf(";")+1);
					cmp = "(" + cmp.substring(0, cmp.indexOf(";")) + ")";
					String jump = printCmp(cmp);
					asm.append(jump + " changeVal" + forCounter + "\n");
					asm.append("jmp endFor" + forCounter + "\n\n");
					asm.append("changeVal" + forCounter + ":\n");
					String var = temp.substring(temp.indexOf("(")+1, temp.indexOf("="));
					if(var.contains("int"))
						var = var.replace("int", "");
					var = var.replace(" ", "");
					if(temp.contains("+")){
						if(temp.contains("++"))
							asm.append("inc " + var + "\n\n");
						else if(temp.contains("+=")){
							String val = temp.substring(temp.indexOf("+=")+2, temp.indexOf(")"));
							asm.append("add " + var + ", " + val + "\n\n");
						}
						else{
							String val = temp.substring(temp.indexOf("+")+1, temp.indexOf(")"));
							asm.append("add " + var + ", " + val + "\n\n");
						}
					}
					else if(temp.contains("-")){
						if(temp.contains("--"))
							asm.append("dec " + var + "\n\n");
						else if(temp.contains("-=")){
							String val = temp.substring(temp.indexOf("-=")+2, temp.indexOf(")"));
							asm.append("sub " + var + ", " + val + "\n\n");
						}
						else{
							String val = temp.substring(temp.indexOf("-")+1, temp.indexOf(")"));
							asm.append("sub " + var + ", " + val + "\n\n");
						}
					}
					asm.append("jmp for" + forCounter + "\n\n");
					asm.append("endFor" + forCounter + ":\n\n");
					forCounter++;
				}
			}
			
			else if(line.contains("if")){
				asm.append("iff" + ifCounter + ":\n");
				String jump = printCmp(line);
				
				asm.append(jump + " then" + ifCounter + "\n");
				
				asm.append("jmp " + "else" + ifCounter + "\n\n");
				asm.append("then" + ifCounter + ":\n");
				ifCounter++;
				if(line.contains("{") || code.get(j+1).contains("{"))
					isBlock = true;
				else
					hasNoBraces = true;
			}
			
			else if(line.contains("do")){
				doWhile = true;
				asm.append("doWhile" + doWhileCounter + ":\n");
				if(!line.contains("{"))
					j++;
			}
			
			else if(line.contains("while") && doWhile==false){
				whileBlock = true;
				asm.append("preWhile" + whileCounter + ":\n");
				temp = line;
				String jump = printCmp(line);
				asm.append(jump + " while" + whileCounter + "\n");
				asm.append("jmp endWhile" + whileCounter + "\n\n");
				asm.append("while" + whileCounter + ":\n");
				if(!line.contains("{"))
					j++;
			}
			else if(line.contains("for")){
				forBlock = true;
				asm.append("preFor" + forCounter + ":\n");
				String var = line.substring(line.indexOf("(")+1, line.indexOf("="));
				if(var.contains("int"))
					var = var.replace("int", "");
				var = var.replace(" ", "");
				String val = line.substring(line.indexOf("=")+1, line.indexOf(";"));
				val = val.replace(" ", "");
				temp = line;
				asm.append("mov " + var + ", " + val + "\n\n");
				asm.append("for" + forCounter + ":\n");
				if(!line.contains("{"))
					j++;
			}
			if((line.contains("++") || line.contains("--")) && !line.contains("=")){
				String var="";
				if(line.contains("++")){
					var = line.substring(0, line.indexOf("+"));
					asm.append("inc " + var + "\n");
				}
				else{
					var = line.substring(0, line.indexOf("-"));
					asm.append("inc " + var + "\n");
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
		if(line.contains("int "))
			return 2;
		
		else
			return 0;
	}
	private String[] cmpJumpConditions(String line){
		String[] results = new String[3];
		
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
		results[0]=jump;
		results[1]=y;
		results[2]=z;
		return results;
	}
	
	private boolean countVar(String x, String y){
		boolean oneVariable = false;
		boolean bothVariables = false;
		for(int i=0;i<intVariables.size();i+=2){
			if(intVariables.get(i).equals(x)){
				if(oneVariable==true){
					bothVariables=true;
					oneVariable=false;
				}
				else
					oneVariable=true;
			}
			else if(intVariables.get(i).equals(y)){
				if(oneVariable==true){
					bothVariables=true;
					oneVariable=false;
				}
				else
					oneVariable=true;
			}
		}
		return bothVariables;
	}
	
	private String printCmp(String line){
		line = line.replaceAll(" ", "");
		String[] results = cmpJumpConditions(line);
		boolean bothVariables = countVar(results[1], results[2]);		
		if(bothVariables){
			asm.append("mov dl, " + results[2] + "\n");
			asm.append("cmp "+ results[1] +"," + " dl\n");
		}
		else
			asm.append("cmp "+ results[1] +", " + results[2] + "\n");
		
		return results[0];
	}

}