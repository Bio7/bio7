package com.eco.bio7.javaeditor.ast;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimplePropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.Document;

import com.eco.bio7.javaeditors.ClassModel;

public class CompilerAst {

	private ASTParser parser;
	
	private List<ImportDeclaration> imports;

	private PackageDeclaration packDecl;

	public CompilationUnit cu;
	
	public CompilerAst(String source) {
		Document doc = new Document(source);
		parser = ASTParser.newParser(AST.JLS8);

		Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);

		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setSource(doc.get().toCharArray());

	}

	public void getProblems() {
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		cu.recordModifications();
		AST ast = cu.getAST();

		IProblem[] l = cu.getProblems();
		for (int i = 0; i < l.length; i++) {

			boolean error = false;
			StringBuffer buffer = new StringBuffer();
			buffer.append(l[i].getMessage());
			buffer.append(" line: ");
			buffer.append(l[i].getSourceLineNumber());
			String msg = buffer.toString();
			if (l[i].isError()) {
				error = true;
				msg = "Error:\n" + msg;
			} else if (l[i].isWarning())
				msg = "Warning:\n" + msg;

		}
	}

	public void parseAst(ClassModel cm) {
		cu = (CompilationUnit) parser.createAST(null);
		cu.recordModifications();
		AST ast = cu.getAST();
		imports = cu.imports();
		packDecl=cu.getPackage();
		for (int i = 0; i < imports.size(); i++) {
			
			ImportDeclaration id=imports.get(i);
			//cm.importNames.add(cuna+" "+cu.getLineNumber(id.getStartPosition()));
			
			JavaAstContainer jac=new JavaAstContainer();
    		jac.setDescription(imports.get(i).getName().getFullyQualifiedName());
    		jac.setLineNumber(cu.getLineNumber(id.getStartPosition()));
    		cm.importNames.add(jac);
			
			
			
			//System.out.println(cuna+" "+cu.getLineNumber(id.getStartPosition()));
		}
		imports.clear();

		List<AbstractTypeDeclaration> types = cu.types();
		for(AbstractTypeDeclaration type : types) {
		    if(type.getNodeType() == ASTNode.TYPE_DECLARATION) {
		        // Class def found
		        List<BodyDeclaration> bodies = type.bodyDeclarations();
		        for(BodyDeclaration body : bodies) {
		            if(body.getNodeType() == ASTNode.METHOD_DECLARATION) {
		                MethodDeclaration method = (MethodDeclaration)body;
		                	             
		               //cm.methodNames.add(method.getName().getFullyQualifiedName()+" "+ cu.getLineNumber(method.getStartPosition()));
		               
		               JavaAstContainer jac=new JavaAstContainer();
	            		jac.setDescription(method.getName().getFullyQualifiedName());
	            		jac.setLineNumber(cu.getLineNumber(method.getStartPosition()));
	            		cm.methodNames.add(jac);
		               
		               
		            }
		            else if(body.getNodeType() == ASTNode.FIELD_DECLARATION){
		            	FieldDeclaration field = (FieldDeclaration)body;
		            	List<VariableDeclarationFragment> vdfs = field.fragments();
		            	for (VariableDeclarationFragment vdf : vdfs) {
		            		
		            		//cm.fieldNames.add(vdf.getName().getFullyQualifiedName()+" "+cu.getLineNumber(vdf.getStartPosition()));
		            		JavaAstContainer jac=new JavaAstContainer();
		            		jac.setDescription(vdf.getName().getFullyQualifiedName());
		            		jac.setLineNumber(cu.getLineNumber(vdf.getStartPosition()));
		            		cm.fieldNames.add(jac);
		            	}		            	
		            	
		            }
		           
		        }
		        bodies.clear();
		    }
	    
		}
	
		
		types.clear();
        imports.clear();
		
	}

	private void print(ASTNode node) {
		List properties = node.structuralPropertiesForType();

		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			Object desciptor = iterator.next();

			if (desciptor instanceof SimplePropertyDescriptor) {
				SimplePropertyDescriptor simple = (SimplePropertyDescriptor) desciptor;
				Object value = node.getStructuralProperty(simple);
				System.out.println(simple.getId() + " (" + value.toString() + ")");
			} else if (desciptor instanceof ChildPropertyDescriptor) {
				ChildPropertyDescriptor child = (ChildPropertyDescriptor) desciptor;
				ASTNode childNode = (ASTNode) node.getStructuralProperty(child);
				if (childNode != null) {
					System.out.println("Child (" + child.getId() + ") {");
					print(childNode);
					System.out.println("}");
				}
			} else {
				ChildListPropertyDescriptor list = (ChildListPropertyDescriptor) desciptor;
				System.out.println("List (" + list.getId() + "){");
				print((List) node.getStructuralProperty(list));
				System.out.println("}");
			}
		}
	}

	private void print(List nodes) {
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
			ASTNode node = (ASTNode) iterator.next();
			print(node);
		}
	}

	public CompilationUnit getCu() {
		return cu;
	}

}