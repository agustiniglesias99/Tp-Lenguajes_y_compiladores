package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import lyc.compiler.symbolTable.SymbolTableData;
import lyc.compiler.symbolTable.SymbolTableGenerator;

public class AsmCodeGenerator implements FileGenerator {

    private int MAX_TEXT_SIZE = 50;


    public AsmCodeGenerator(){

    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        String asm = "include macros2.asm\ninclude number.asm\n .MODEL  LARGE\n.386\n.STACK 200h\n\n";
        asm += generarDATA();
        fileWriter.write(asm + generarCabecera() + "\nmov ax, 4C00h\nint 21h\nEND START");
    }

    public String generarCabecera(){
        return ".CODE\nSTART:\nmov AX,@DATA\nmov DS,AX\nmov es,ax\n\n";
    }


    private String generarDATA(){
        Map<String, SymbolTableData> symbols = SymbolTableGenerator.getInstance().getSymbols();
        String asm_DATA = ".DATA\n";
        for (Map.Entry<String, SymbolTableData> entry : symbols.entrySet()) {
            SymbolTableData data = entry.getValue();
            if(!data.getType().equals("string")){
                asm_DATA += entry.getKey().replace(".","_") + "\t" + "dd ";
                if(data.getValue() == null)
                    asm_DATA += "? \n";
                else{
                    String value = "";
                    if(data.getType().equals("int"))
                        value = data.getValue() + ".0";
                    else
                        value = data.getValue();
                    asm_DATA += value + "\n";
                }
            }
            else{
                if(entry.getKey().startsWith("_")){
                    int largo = this.MAX_TEXT_SIZE - Integer.valueOf(data.getLength());
                    asm_DATA += entry.getKey().replaceAll("[^a-zA-z0-9]*","") + "\t" + "db " + data.getValue() + ",'$'," + largo + " dup (?)\n";
                }
                else{
                    asm_DATA += entry.getKey().replaceAll("[^a-zA-z0-9]*","") + "\t" + "db " + MAX_TEXT_SIZE + " dup (?),'$'\n";
                }
            }
        }

        return asm_DATA + "\n";
    }

}
