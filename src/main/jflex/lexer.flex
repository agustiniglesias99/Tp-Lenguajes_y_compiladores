 package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.ParserSym;
import lyc.compiler.constants.Constants;import lyc.compiler.model.*;import lyc.compiler.symbolTable.SymbolTableGenerator;

%%

%public
%class Lexer
%unicode
%cup
%line
%column
%throws CompilerException
%eofval{
  return symbol(ParserSym.EOF);
%eofval}


%{
	private Symbol symbol(int type) {
		return new Symbol(type, yyline, yycolumn);
	}
	private Symbol symbol(int type, Object value) {
		return new Symbol(type, yyline, yycolumn, value);
	}
	private boolean esLongitudIDValida() {
		return yylength() <= Constants.MAX_ID_LENGTH;
	}
	private boolean esLongitudStringValida() {
		return yylength() <= Constants.MAX_STRING_LITERAL_LENGTH;
	}

	private boolean esRangoCteEnteraValido(){
		int cteEntera = Integer.parseInt(yytext());
		return cteEntera >= Constants.MIN_INTEGER_CONSTANT && cteEntera <= Constants.MAX_INTEGER_CONSTANT;
	}

	private boolean esRangoCteFloatValido(){
		double cteFloat = Math.abs(Double.parseDouble(yytext()));
		return cteFloat >= Constants.MIN_FLOAT_CONSTANT && cteFloat <= Constants.MAX_FLOAT_CONSTANT;
	}

	private void guardarToken() {
		SymbolTableGenerator.getInstance().addToken(yytext());
	}

	private void guardarCTE(String dataType){
	  	SymbolTableGenerator.getInstance().addToken(yytext(),dataType);
	}
%}


LineTerminator 		= 	\r|\n|\r\n
InputCharacter 		= 	[^\r\n]
Identation 			=  	[ \t\f]

DIGITO				=	[0-9]
LETRA				=	[a-zA-Z]

INIT				=	"init"
NUMERAL				=   "#"
INCLUDE				=	"include"
PUNTO           	=   "."
INTEGER				=	"int"
FLOAT				=	"float"
STRING				=	"string"
MAIN				=	"main"
ELSE				=	"else"
CORCH_ABRE			=	"["
CORCH_CIERRA		=	"]"
IGUAL_IGUAL         =	"=="
ASIGNACION			=	":="
PUNTO_COMA			=	";"
SYS_OUT				=	"write"
SYS_IN				=	"read"
AND					=	"AND"
OR					=	"OR"
POINTER				=	"&"
BARRA				=	"\/"
ASTERISCO			=	"\*"
SALTO_LINEA			=	"\n|\r\n|\r"
MENOS_ASTERISCO		=	[^*]
CUALQUIER_CARACTER 	= 	[^\r\n]
COMENTARIO_SINGLE   =   "#+" ([^])+ "+#"
OP_RES		        =	"-"
CTE		            =	("0"|( ("-")? [1-9]{DIGITO}*))
CADENA              = \"([^\"\\]|\\.)*\"

ID			        =	{LETRA}({LETRA}|{DIGITO})*

SLICEANDCONCAT      =   "sliceAndConcat"
NEGCALCULATION      =   "negativeCalculation"

OP_SUM		        =	"+"
OP_MUL              =	"*"

OP_DIV              =	"/"
PA			        =	"("
PC			        =	")"
IF                  =	"if"
WHILE               =	"while"
COMA                =	","
DISTINTO            =	"!="
NEGADO				=	"NOT"
MENOR_IGUAL         =	"<="
MAYOR_IGUAL         =	">="
MENOR               =	"<"
MAYOR               =	">"
LLA     			=	"{"
LLC   				=	"}"
CTE_REAL            =	{CTE}? {PUNTO} {DIGITO}*//[0]{0,1}[1-9]{0,11}[.][0-9]{0,11}
DP                  =   ":"

%%
/* keywords */

<YYINITIAL> {

{INIT}		    	{	return symbol(ParserSym.INIT,yytext()); 		}
{NUMERAL}		    {	return symbol(ParserSym.NUMERAL,yytext()); 		}
{INCLUDE}		    {	return symbol(ParserSym.INCLUDE,yytext()); 		}
{PUNTO}		        {	return symbol(ParserSym.PUNTO,yytext()); 		}
{MAIN}		        {	return symbol(ParserSym.MAIN,yytext());			}
{STRING}		    {	return symbol(ParserSym.STRING,yytext());		}
{CADENA}            {   return symbol(ParserSym.CADENA,yytext());       }

{CORCH_ABRE}		{	return symbol(ParserSym.CORCH_ABRE,yytext());	}
{CORCH_CIERRA}		{	return symbol(ParserSym.CORCH_CIERRA,yytext());	}
{ASIGNACION}		{	return symbol(ParserSym.ASIGNACION,yytext());	}
{PUNTO_COMA}	    {	return symbol(ParserSym.PUNTO_COMA,yytext());	}
{SYS_OUT}	        {	return symbol(ParserSym.SYS_OUT,yytext());		}
{SYS_IN}	        {	return symbol(ParserSym.SYS_IN,yytext());		}
{COMENTARIO_SINGLE} {	/*ignorar*/										}
{AND}	            {	return symbol(ParserSym.AND,yytext());			}
{OR}	            {	return symbol(ParserSym.OR,yytext());			}
{POINTER}	        {	return symbol(ParserSym.POINTER,yytext());		}
{ELSE}	            {	return symbol(ParserSym.ELSE,yytext());		}
{OP_RES}			{	return symbol(ParserSym.OP_RES, yytext());		}
{INTEGER}		    {	return symbol(ParserSym.INTEGER, yytext());		}
{FLOAT}		        {	return symbol(ParserSym.FLOAT, yytext());		}
{IF}                {	return symbol(ParserSym.IF, yytext());			}
{NEGADO}			{	return symbol(ParserSym.NEGADO, yytext());		}
{WHILE}             {	return symbol(ParserSym.WHILE, yytext());		}
{COMA}	            {	return symbol(ParserSym.COMA, yytext());		}
{CTE}			    {
      					if(!esRangoCteEnteraValido())
      						throw new InvalidIntegerException(yytext() + " esta fuera de rango.");
						guardarCTE("int");

      					return symbol(ParserSym.CTE, yytext());

	  				}
{CTE_REAL}	        {
      					if(!esRangoCteFloatValido())
      						throw new InvalidIntegerException(yytext() + " esta fuera de rango.");
      					guardarCTE("float");
      					return symbol(ParserSym.CTE_REAL, yytext());
	  				}
{SLICEANDCONCAT}    {   return symbol(ParserSym.SLICEANDCONCAT, yytext()); }

{NEGCALCULATION}    {   return symbol(ParserSym.NEGCALCULATION, yytext()); }

{ID}			    {
      					if(!esLongitudIDValida())
      						throw new InvalidLengthException("\"" + yytext() + "\""+ " excede el maximo permitido");
		  				guardarToken();
		  				return symbol(ParserSym.ID, yytext());
	  				}
{MENOR_IGUAL}	    {	return symbol(ParserSym.MENOR_IGUAL, yytext());	}
{MAYOR_IGUAL}	    {	return symbol(ParserSym.MAYOR_IGUAL, yytext());	}
{MENOR}			    {	return symbol(ParserSym.MENOR, yytext());			}
{MAYOR}			    {	return symbol(ParserSym.MAYOR, yytext());			}
{LLA}				{	return symbol(ParserSym.LLA, yytext());	}
{LLC}				{	return symbol(ParserSym.LLC, yytext());	}
{DISTINTO}	        {	return symbol(ParserSym.DISTINTO, yytext());		}
{IGUAL_IGUAL}	    {	return symbol(ParserSym.IGUAL_IGUAL, yytext());		}
{OP_SUM}			{	return symbol(ParserSym.OP_SUM, yytext());		}
{OP_MUL}			{	return symbol(ParserSym.OP_MUL, yytext());		}
{OP_DIV}			{	return symbol(ParserSym.OP_DIV, yytext());		}
{PA}				{	return symbol(ParserSym.PA, yytext());			}
{PC}				{	return symbol(ParserSym.PC, yytext());			}
{DP}                {   return symbol(ParserSym.DP, yytext());          }

"\n"				{}
"\t"				{}
"\n\t"				{}
" "					{}
"\r\n"				{}
}


/* error fallback */
[^]                              { throw new UnknownCharacterException(yytext()); }