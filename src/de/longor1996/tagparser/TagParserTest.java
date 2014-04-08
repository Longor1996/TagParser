package de.longor1996.tagparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

import de.longor1996.tagparser.TagParser.TagParserException;

/**
 * A simple class with a main-method to test the TagParser-class.
 **/
public class TagParserTest
{
	/**
	 * Main-Method for the test.
	 * Remove it if you want to actually use the parser.
	 **/
	public static void main(String[] args)
	{
		// Make File Handle
		File file = new File("parser-test.txt");
		
		// If the test-file does not exist, create it.
		if(!file.exists())
		{
			try
			{
				// Create the file...
				file.createNewFile();
				
				// Then fill it with basic test-data...
				PrintStream out = new PrintStream(file);
				
				out.println("TagParser Test-File");
				out.println();
				out.println("<div>");
				out.println("  This is a Text-Block!<br/>");
				out.println("  The tag before this sentence is marked as a 'single-tag', so it will not start/end a branch in the tag-tree.");
				out.println("</div>");
				out.println();
				out.println("Testing attribute parsing here!");
				out.println();
				out.println("<div/ attributeKey = \"attributeValue\" c = 0>");
				out.println("<div/ attributeKey = attributeValue >");
				out.println("<div/ attributeKey = 4 >");
				out.println("<div/ attributeKey = 128px >");
				out.println("<div/ attributeKey = 100% >");
				out.println("<div/ attributeKey >");
				out.println("<div/ attributeKey = =>");
				out.println();
				
				// Close it
				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		// Create a new parser.
		TagParser parser = new TagParser();
		TagNode parser_output = null;
		
		// Parse the File!
		try
		{
			parser_output = parser.parse(new FileInputStream(file));
		}
		catch (TagParserException e)
		{
			// The parser had a problem parsing the document.
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// The parser encountered a problem while reading the stream.
			e.printStackTrace();
		}
		
		System.out.println("Parser took " + parser.result_time + " milliseconds to parse the file.");
		
		// If the tag got read without problems, print it out to the console.
		if(parser_output != null)
		{
			print("# ", "  ", parser_output);
		}
		
	}
	
	/**
	 * Pretty silly method to print the tag-tree to the console.
	 **/
	public static void print(String postFix, String appendum, TagNode tag)
	{
		System.out.print(postFix);
		System.out.print(tag.toString());
		System.out.println();
		
		if(tag.container != null)
		{
			for(AbstractNode node : tag.container.list())
			{
				if(node instanceof TagNode)
				{
					print(postFix + appendum, appendum, (TagNode) node);
				}
				else
				{
					System.out.print(postFix + appendum);
					System.out.print(node.toString());
					System.out.println();
				}
			}
		}
	}
	
}
