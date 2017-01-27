package com.eco.bio7.markdownedit.completion;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;


public class RMarkdownCompletionProcessor implements IContentAssistProcessor {

	

	public RMarkdownCompletionProcessor() {
		
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

		// IDocument document = viewer.getDocument();

		ICompletionProposal[] result = new ICompletionProposal[33];
		IContextInformation[] resContext=new IContextInformation[33];

		String[] textContext = {"```{r}"+System.lineSeparator()+System.lineSeparator()+"```",
				"```{r echo=FALSE}"+System.lineSeparator()+System.lineSeparator()+"```" ,
				"```{r eval=FALSE}"+System.lineSeparator()+System.lineSeparator()+"```" ,
				"```{r}"+System.lineSeparator()+"source('myFile.R')"+System.lineSeparator()+"```",
				"```{r}"+System.lineSeparator()+"source(paste(getwd(),'myFile.R'))"+System.lineSeparator()+"```",
				"#",
				"##",
				"###",
				"####",
				"#####",
				"#####",
				"  ",
				"*italics example*",
				"_italics example_",
				"**bold example**",
				"__bold example__",
				"superscript^2^",
				"~~strikethrough~~",
				"[link](http:\\bio7.org)",
				"--",
				"---",
				"...",
				"$E = m*c^{2}$",
				"$$E = m*c^{2}$$",
				"[](path/myImage.png)",
				"***",
				">",
				"* item"+System.lineSeparator()+"\t" +"+ subitem1"+System.lineSeparator()+"\t" +"+ subitem2",
				"1. item"+System.lineSeparator()+"\t" +"+ subitem1"+System.lineSeparator()+"\t" +"+ subitem2"
						,
						"Table Header | Another Header"+
						System.lineSeparator()+
						"------------- | -------------"+
						System.lineSeparator()+
						"Cell 1.1 | Cell 1.2"+
						System.lineSeparator()+
						"Cell 2.1 | Cell 2.2",
						"```"+System.lineSeparator()+"Code section"+System.lineSeparator()+"```",
						"This is `inline code`",
						"\newpage"};
		
		
		String[] text = { "R markdown section",
				"R markdown section without echo",
				"R markdown section without evaluation and including of results",
				"R markdown section with a referenced R file",
				"R markdown section with a referenced R file from the current working directory",
				"Header 1",
				"Header 2",
				"Header 3",
				"Header 4",
				"Header 5",
				"Header 6", 
				"Start a new  paragraph (two spaces at the end of a line)",
				"Italic text",
				"Italic text",
				"Bold text",
				"Bold text" ,
				"Superscript text",
				"Strikethrough text",
				"A link",
				"Endash",
				"Emdash",
				"Ellipsis",
				"Inline equation",
				"Display equation",
				"Image",
				"Horizontal rule/slide break/page break",
				"Block quote",
				"Unordered list",
				"Ordered list"
				,"A table",
				"Code block",
				"Inline code",
				"Page break"};
		for (int i = 0; i < text.length; i++) {
			resContext[i]=new ContextInformation(textContext[i],textContext[i]);
			result[i] = new CompletionProposal(textContext[i], offset, 0, text[i].length()+1,null,text[i],resContext[i], textContext[i]);
		}

		return result;

	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		/*
		 * IContextInformation[] NO_CONTEXTS = new IContextInformation[1];
		 * String text = "A title for the roxygen documentation"; NO_CONTEXTS[0]
		 * = new ContextInformation("'@Title", text);
		 */
		return null;
	}

	// add the chars for Completion here !!!

	public char[] getCompletionProposalAutoActivationCharacters() {

		/*if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			String ac = store.getString("ACTIVATION_CHARS");
			// return "abcdefghijklmnopqrstuvwxyz".toCharArray();
			if (ac == null || ac.isEmpty()) {

				return null;
			}
			return ac.toCharArray();
		}*/

		return null;

	}

	
	private static final class ImageContentProposal extends org.eclipse.jface.fieldassist.ContentProposal {

		private Image image;

		public ImageContentProposal(String content, String label, String description, int cursorPosition, Image image) {
			super(content, label, description, cursorPosition);
			this.image = image;
		}

		public Image getImage() {
			return this.image;
		}
	}


	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

}
