package com.eco.bio7.reditors;

import org.eclipse.jface.text.rules.*;
/*Here we search for the assignment operators and exclude logical combinations with '='*/
public class AssignmentRule implements IRule {
	private final IToken tokenOperator;
	private final IToken tokenAssign;
   
	public AssignmentRule(IToken tokenOperator,IToken tokenAssign) {
		/*Tokens from RCodeScanner!*/
		this.tokenOperator = tokenOperator;
		this.tokenAssign = tokenAssign;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		/*Scan for <- and <<- and exclude <=*/
		int c = scanner.read();
		if (c == '<') {
			c = scanner.read();
			if (c == '-') {

				return tokenAssign;
			}
			

			else if (c == '<') {
				c = scanner.read();
				if (c == '-') {

					return tokenAssign;
				}
				
				
			}
			else if(c=='='){
				return tokenOperator;
			}
			scanner.unread();
		
			
		} 
		/*Scan for the special infix operator :=  defined by e.g. by the data.table package!*/
		else if(c == ':'){
			c = scanner.read();
			if (c == '=') {

				return tokenAssign;
			}
			scanner.unread();
		}
		/*Exclude >=*/
		else if (c == '>') {
			c = scanner.read();
			 if(c=='='){
				return tokenOperator;
			}
		}
		
		/*Exclude !=*/
		else if (c == '!') {
			c = scanner.read();
			 if(c=='='){
				return tokenOperator;
			}
		}
		/*Scan for -> and ->>*/
		else if (c == '-') {

			c = scanner.read();
			if (c == '>') {

				c = scanner.read();
				if (c == '>') {
					return tokenAssign;
				} 
				scanner.unread();
				return tokenAssign;

			}
			
			else{
				scanner.unread();
			}
		}
		/*Scan for = and exclude ==*/
		 else if (c == '=') {
			 c = scanner.read();
			 if (c == '=') {
				 return tokenOperator;
			 }
			 scanner.unread();
			return tokenAssign;
		 }
		
		scanner.unread();
		return Token.UNDEFINED;
	}
}