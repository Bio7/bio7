/**
derived from http://svn.r-project.org/R/trunk/src/main/gram.y
http://cran.r-project.org/doc/manuals/R-lang.html#Parser
*/
grammar R;

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
    |   expr ('<-'|'<<-'|'='|'->'|'->>'|':=') expr	#VariableDeclaration
    |   '{' exprlist '}' 	#e19// compound statement
    |   'function' '(' formlist? ')' expr #DefFunction// define function
    |   expr '(' sublist ')'   	#CallFunction           // call function
    |   'if' '(' expr ')' expr	#e20expr
    |   'if' '(' expr ')' expr 'else' expr	#e21
    |   'for' '(' ID 'in' expr ')' expr	#e22
    |   'while' '(' expr ')' expr	#e23
    |   'repeat' expr	#e24
    |   '?' expr 	#e25// get help on expr, usually string or ID
    |   'next'	#e26
    |   'break'	#e27
    |   '(' expr ')'	#e28
    |   ID	#e29
    |   STRING	#e30
    |   HEX	#e31
    |   INT	#e2
    |   FLOAT	#e32
    |   COMPLEX	#e33
    |   'NULL'	#e34
    |   'NA'	#e35
    |   'Inf'	#e36
    |   'NaN'	#e37
    |   'TRUE'	#e38
    |   'FALSE'	#e39
    |   expr '(' sublist    						{notifyErrorListeners("Missing closing parentheses in function call!");}#CallFunctionError1
    |   expr '(' sublist ')' ')'   					{notifyErrorListeners("Too many parentheses in function call!");}#CallFunctionError2
    |   'function' '(' formlist?  expr 				{notifyErrorListeners("Missing closing parentheses in function definition!");}#DefFunctionError1// define function
    |   'function' '(' formlist? ')' ')' expr 		{notifyErrorListeners("Too many parentheses in function definition!");}#DefFunctionError2// define function
    |   'if' '(' expr  expr							{notifyErrorListeners("Missing closing parentheses in if condition!!");}#e20Error1
    |   'if' '(' expr ')' ')'  expr					{notifyErrorListeners("Err20:Too many parentheses in if condition!");}#e20Error1
    |   expr '[' sublist 	    					{notifyErrorListeners("Missing closing brackets!");}#e2Error1
    |   expr '[' sublist ']' ']'					{notifyErrorListeners("Too many brackets!");}#e2Error2
    | 	'(' expr ')' ')' 							{notifyErrorListeners("Too many parentheses!");}# e28Error1
   // |	'(' expr         							{notifyErrorListeners("Missing closing parentheses!");}#e28Error2
    | 	'{'  exprlist  	 							{notifyErrorListeners("Missing closing braces!");} #e19Error1
    |	'{' exprlist  '}' '}'						{notifyErrorListeners("Too many braces!");} #e19Error2
    ;

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

WS      :   [ \t]+ -> skip ;// Removed!
