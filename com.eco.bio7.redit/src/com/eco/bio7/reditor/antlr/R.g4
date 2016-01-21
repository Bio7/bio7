/**
derived from http://svn.r-project.org/R/trunk/src/main/gram.y
http://cran.r-project.org/doc/manuals/R-lang.html#Parser
*/
grammar R;

@lexer::members {public static boolean skipwhitespace = true;}

prog:   (   expr (';'|NL|EOF)// Added!
        |   NL
        )*
        EOF
    ;

/*
expr_or_assign
    :   expr ('<-'|'='|'<<-') expr_or_assign
    |   expr 
    ;
*/

expr:  
        expr '[[' sublist ']' ']'  #e1// '[[' follows R's yacc grammar
    |   expr '[' sublist ']'	#e2
    |   expr ('::'|':::') expr	#e3
    |   expr ('$'|'@') expr	#e4
    |   expr '^'<assoc=right> expr	#e5
    |   ('-'|'+') expr	#e6
    |   expr ':' expr	#e7
    |   expr USER_OP expr 	#e8// anything wrappedin %: '%' .* '%'
    |   expr ('*'|'/') expr	#e9
    |   expr ('+'|'-') expr	#e10
    |   expr ('>'|'>='|'<'|'<='|'=='|'!=') expr	#e11
    |   '!' expr	#e12
    |   expr ('&'|'&&') expr	#e13
    |   expr ('|'|'||') expr	#e14   
    |   '~' expr	#e15  
    |   expr '~' expr	#e16 
    |   expr ASS_OP expr	#e17VariableDeclaration
    |   '{' exprlist '}' 	#e18// compound statement
    |   'function' '(' formlist? ')' expr #e19DefFunction// define function
    |   expr '(' sublist ')'   	#e20CallFunction           // call function
    |   'if' '(' expr ')' expr	#e21
    |   'if' '(' expr ')' expr 'else' expr	#e22
    |   'for' '(' ID 'in' expr ')' expr	#e23
    |   'while' '(' expr ')' expr	#e24
    |   'repeat' expr	#e25
    |   '?' expr 	#e26// get help on expr, usually string or ID
    |   'next'	#e27
    |   'break'	#e28
    |   '(' expr ')'	#e29
    |   ID	#e30
    |   STRING	#e31
    |   HEX	#e32
    |   INT	#e32
    |   FLOAT	#e34
    |   COMPLEX	#e35
    |   'NULL'	#e36
    |   'NA'	#e37
    |   'Inf'	#e38
    |   'NaN'	#e39
    |   'TRUE'	#e40
    |   'FALSE'	#e41
    | 	'(' expr ')' extra=')' 							#err1
    //|   expr '(' sublist    							#err2
    |   expr '(' sublist ')' extra=')'   				#err3
    //|   'function' '(' formlist?  expr 					#err4
    |   'function' '(' formlist? ')' extra=')' expr 	#err5
    //|   'if' '(' extra='(' expr ')'  expr								#err6
    |   'if' '(' expr ')' extra=')'  expr				#err7
    |   expr '[[' sublist ']' ']' extra=']'	    		#err8
    |   expr '[' sublist ']' extra=']'					#err9
   // |	'(' expr         							    #e28Error2
   // | 	'{'  exprlist  	 								#err10
    |	'{' exprlist  '}' extra='}'						#err11
    |	extra='true'									#warn12
    |	extra='false'									#warn13
    |	extra='null'									#warn14
    |	extra='na'										#warn15
    |   'while' '(' expr ')' extra=')' expr				#err16
    //|   'while' '(' expr  expr							#err17
    |   'for' '(' ID 'in' expr ')' extra=')' expr		#err18
    //|   'for' '(' ID 'in' expr expr						#err19
    |	expr '=>' expr										#err20
    |	expr '=<' expr										#err21
    |   unknowns											#err22
    ;
unknowns : UNKNOWN+ ;//Catch all unknown tokens and delegate them to the parser!

 

exprlist
    :   expr ((';'|NL) expr?)*
    |
    ;

formlist : form (',' form)* ;

form:   ID
    |   ID '=' expr
    |   '...'
    ;

sublist : sub (',' sub)* ;

sub :   expr
    |   ID '='
    |   ID '=' expr
    |   STRING '='
    |   STRING '=' expr
    |   'NULL' '='
    |   'NULL' '=' expr
    |   '...'
    |
    ;
    
ASS_OP: ('<-'|'<<-'|'='|'->'|'->>'|':=');

HEX :   '0' ('x'|'X') HEXDIGIT+ [Ll]? ;

INT :   DIGIT+ [Ll]? ;

fragment
HEXDIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

FLOAT:  DIGIT+ '.' DIGIT* EXP? [Ll]?
    |   DIGIT+ EXP? [Ll]?
    |   '.' DIGIT+ EXP? [Ll]?
    ;
fragment
DIGIT:  '0'..'9' ; 
fragment
EXP :   ('E' | 'e') ('+' | '-')? INT ;

COMPLEX
    :   INT 'i'
    |   FLOAT 'i'
    ;

STRING
    :   '"' ( ESC | ~[\\"] )*? '"'
    |   '\'' ( ESC | ~[\\'] )*? '\''
    |   '`' ( ESC | ~[\\'] )*? '`'
    ;

fragment
ESC :   '\\' [abtnfrv"'\\]
    |   UNICODE_ESCAPE
    |   HEX_ESCAPE
    |   OCTAL_ESCAPE
    ;

fragment
UNICODE_ESCAPE
    :   '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    |   '\\' 'u' '{' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT '}'
    ;

fragment
OCTAL_ESCAPE
    :   '\\' [0-3] [0-7] [0-7]
    |   '\\' [0-7] [0-7]
    |   '\\' [0-7]
    ;

fragment
HEX_ESCAPE
    :   '\\' HEXDIGIT HEXDIGIT?
    ;

ID  :   '.' (LETTER|'_'|'.') (LETTER|DIGIT|'_'|'.')*
    |   LETTER (LETTER|DIGIT|'_'|'.')*
    |   '.' // Added! 
    ;
    
fragment LETTER  : [a-zA-Z] ;

USER_OP :   '%' .*? '%' ;

COMMENT :      '#' ~[\r\n]*  -> type(NL);

// Match both UNIX and Windows newlines
NL      :   '\r'? '\n' ;

// Changed for TokenRewriter, see http://stackoverflow.com/questions/21889071/antlr4-tokenstreamrewriter-output-doesnt-have-proper-format-removes-whitespac
//WS		 :  {skipwhitespace==false}? WS1 | {skipwhitespace}? WS2;

WS      :   [ \t]+ ->  channel(HIDDEN);

//WS       :    [ \t]+ ->  skip   ;// Removed!;

UNKNOWN : . ;//Unknown tokens!
