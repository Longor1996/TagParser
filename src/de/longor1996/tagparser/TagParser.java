package de.longor1996.tagparser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

/**
 * 
 * A small lightweight parser for HTML-style documents.<hr>
 * This parser works 'streaming', which means that it parses the entire document in a single pass.<br><br>
 * Note that many functions are not supported, but its enough to read basic HTML/XML-style documents.
 * The parser itself is very fast, since it does not perform any kind of actual sanity/error-checks,
 * but as a side-effect it can create a lot of garbage memory-wise.
 **/
public class TagParser
{
	/**
	 * The character encoder to be used to read the document.
	 **/
	IEncoding encoding;
	
	/**
	 * ???
	 **/
	ITagTypeDeterminer tagTypeDeterminer;
	
	// Parsing Result
	public long result_time = 0;
	public TagNode result_document = null;
	
	public TagParser()
	{
		this.encoding = PlainEncoding.instance;
	}
	
	public TagNode parse(InputStream __source) throws TagParserException, IOException
	{
		// Prepare
		long time = System.nanoTime();
		BufferedInputStream source = new BufferedInputStream(__source);
		ParseState state = new ParseState();
		
		// Read the document, character by character, byte by byte.
		while(true)
		{
			// Read the next character/symbol.
			int in = this.readNext(source);
			
			// Check for End-Of-Stream
			if(in == -1)
			{
				break;
			}
			
			// State-Update, also determine if we have to consume this character.
			boolean consume = state.nextSymbol(in);
			
			// Consume the character if needed.
			if(consume)
			{
				this.consumeSymbol(state);
			}
			
			// State-Update
			state.endNextSymbol(in);
			
		}
		
		// Sanity check!
		if(state.containerStack.size() != 1)
		{
			throw new TagParserException(Integer.MAX_VALUE, Integer.MAX_VALUE, "A tag is not correctly escaped."); // Which one?
		}
		
		// Store the result
		this.result_time = (System.nanoTime() - time);
		this.result_document = state.containerStack.pop();
		
		// end
		return this.result_document;
	}
	
	/**
	 * Reads the next symbol from the given stream, using the IEncoding set for this parser.
	 **/
	private int readNext(BufferedInputStream source) throws IOException
	{
		return this.encoding.next(source);
	}
	
	/**
	 * Consume the next symbol.
	 **/
	private void consumeSymbol(ParseState state) throws TagParserException
	{
		
		if(state.isReadingTag)
		{
			this.consumeSymbolInTag(state);
		}
		else
		{
			this.consumeSymbolOutOfTag(state);
		}
		
	}
	
	/**
	 * Consume a symbol outside of a tag.
	 * @throws TagParserException
	 **/
	private void consumeSymbolOutOfTag(ParseState state) throws TagParserException
	{
		
		if(state.nowC == '<')
		{
			state.finishLiteral();
			state.isReadingTag = true;
			state.inTagPointer = 0;
			state.consumedTagTypeName = false;
			state.literalBuffer.setLength(0);
			state.temporaryTag = new TagNode();
			return;
		}
		
		// error-checking
		if(state.nowC == '>')
		{
			throw state.makeException("Tag end-symbol('>') without preceding start-symbol('<')!");
		}
		
		// Append the current symbol to the literal-buffer.
		state.literalBuffer.append(state.nowC);
		
	}
	
	/**
	 * Consume a symbol inside of a tag.
	 **/
	private void consumeSymbolInTag(ParseState state) throws TagParserException
	{
		
		if(state.nowC == '>')
		{
			if(!state.consumedTagTypeName)
			{
				state.temporaryTag.typeName = state.literalBuffer.toString();
				state.literalBuffer.setLength(0);
			}
			
			if(state.temporaryTag.typeName == null)
			{
				throw state.makeException("Type-Name is null referenced! >> " + state.literalBuffer);
			}
			
			state.finishTag();
			state.isReadingTag = false;
			return;
		}
		
		// error-checking
		if(state.nowC == '<')
		{
			throw state.makeException("Tag start-symbol('<') cannot be inside tag!");
		}
		
		// We do NOT wan't the typeName to start with a whitespace.
		if((state.nowC == ' ') && (state.inTagPointer <= 0))
		{
			throw state.makeException("Tag cannot start with whitespace at the beginning.");
		}
		
		// We do NOT want the pointer to increase if its zero and a whitespace is given.
		if((state.inTagPointer <= 0) && (state.nowC != ' '))
		{
			state.inTagPointer++;
		}
		
		// Consume the Tag's TypeName.
		if(!state.consumedTagTypeName && (state.nowC == ' '))
		{
			state.temporaryTag.typeName = trimSubstring(state.literalBuffer);
			state.literalBuffer.setLength(0);
			state.consumedTagTypeName = true;
			return;
		}
		
		state.literalBuffer.append(state.nowC);
		
	}
	
	private class ParseState
	{
		int row;
		int column;
		
		char lastC;
		char nowC;
		
		int inTagPointer;
		
		Stack<TagNode> containerStack;
		StringBuffer literalBuffer;
		TagNode temporaryTag;
		
		boolean pre;
		
		// Same as 'isInsideTag'!
		boolean isReadingTag;
		boolean consumedTagTypeName;
		
		public ParseState()
		{
			this.literalBuffer = new StringBuffer(4096);
			
			// Create NodeContainer-Stack and push the document-root on it.
			this.containerStack = new Stack<TagNode>();
			this.containerStack.push(new TagNode().setTypeName("document-root"));
			
			this.pre = false;
			
			this.isReadingTag = false;
			this.consumedTagTypeName = false;
			
			this.row = this.column = 0;
			this.lastC = this.nowC = ' ';
			
		}
		
		public void finishTag() throws TagParserException
		{
			// System.out.print("NT"+this.mLD());
			
			TagNode container = this.containerStack.peek();
			TagNode tag = this.temporaryTag;
			
			// Parse the Attributes if there are any.
			if(this.literalBuffer.length() > 0)
			{
				// Though before we do so,check if there is a '/' at the end of the literal-buffer.
				if(this.literalBuffer.charAt(this.literalBuffer.length()-1) == '/')
				{
					// There IS a '/' at the end of the buffer. Cut it away and add it to the typeName of the current Tag.
					tag.typeName += '/';
					this.literalBuffer.setLength(this.literalBuffer.length()-1);
				}
				
				// Now parse the attributes
				tag.addAttributes(this.transformAttributes(this.literalBuffer.toString()));
			}
			
			// Determine the Tag-Type.
			tag.tagType = this.determineTagType(tag, container);
			
			// Depending on the Type of the Tag, we will either...
			//    POP the current container from the containerStack, ignoring this Tag as 'end'-tag. But only if the current container has the same name!
			//    PUSH this tag as an 'start'-tag onto the containerStack.
			//    ADD this tag to the container as a 'single'-tag.
			//
			// By doing this, we can easily create a tag-tree while parsing, without doing multiple passes of the data.
			switch(tag.tagType)
			{
			case 0: // START-TAG
				container.addChild(tag);
				this.containerStack.push(tag);
				this.temporaryTag = null;
				break;
				
			case 1: // END-TAG
				this.containerStack.pop();
				this.temporaryTag = null;
				break;
				
			case 2: // SINGLE-TAG
				container.addChild(tag);
				this.temporaryTag = null;
				break;
				
				// This should NOT happen.
			case -1: default:
				throw this.makeException("TagType unresolved or unknown: " + tag.tagType);
			}
			
			// Clear the literal-buffer to parse the next literal.
			this.literalBuffer.setLength(0);
		}
		
		/**
		 * Transforms a string containing HTML-style formatted attributes into a practical HashMap.
		 **/
		private HashMap<String, String> transformAttributes(String string)
		{
			// Prepare the attribute-map!
			HashMap<String, String> map = new HashMap<String, String>(2);
			
			// Consume the String word by word...
			while(string.length() > 0)
			{
				// Trim whitespaces, if there are any.
				string = string.trim();
				
				// Possible words we may encounter:
				//   property_key
				//   =
				//   "a literal value !"
				//   property_key>           (Mind the '>'!)
				
				// Find the end of the next word
				int endOfTheWord = this.findEndOfAttributeWord(string);
				
				// temporary key field
				String key = null;
				
				// Cut out the next word! (which is a 'key')
				{
					String word = string.substring(0, endOfTheWord);
					string = string.substring(endOfTheWord).trim();
					key = word;
				}
				
				if(string.length() == 0)
				{
					// key only: Treat the key as a 'flag'-attribute (which has no content other than itself).
					map.put(key, "");
					break;
				}
				
				if(string.charAt(0) == '=')
				{
					// key/value
					string = string.substring(1).trim();
					String value = null;
					
					// Find the end of the value word!
					int endOfTheWord2 = this.findEndOfAttributeWord(string);
					{
						String word = string.substring(0, endOfTheWord2);
						string = string.substring(endOfTheWord2).trim();
						value = word;
					}
					
					// If the value is surrounded by '"' on both start and end, cut them away.
					if((value.length() >= 2) && (value.charAt(0) == '"') && (value.charAt(value.length()-1) == '"'))
					{
						value = value.substring(1, value.length()-1);
					}
					
					map.put(key, value);
				}
				else
				{
					// key only: Treat the key as a 'flag'-attribute (which has no content other than itself).
					map.put(key, key);
				}
			}
			
			// If there is nothing inside the map, throw it away and return null!
			if(map.size() == 0)
			{
				return null;
			}
			
			// Return the attributes for further usage.
			return map;
		}
		
		/**
		 * Finds the end-index of the next attribute-word in the given string.
		 **/
		private int findEndOfAttributeWord(String string)
		{
			// for_each {character} in {string} do check()
			for(int i = 0; i < string.length(); i++)
			{
				char c = string.charAt(i);
				
				if(c == ' ')
				{
					return i;
				}
				
				if((i > 0) && (c == '='))
				{
					return i;
				}
			}
			
			return string.length();
		}
		
		/**
		 * Finishes the last literal if there is any.
		 **/
		public void finishLiteral()
		{
			// Finish the last Literal if there is any!
			String literal = trimSubstring(this.literalBuffer);
			
			if(literal.length() > 0)
			{
				// System.out.print("ND"+this.mLD());
				
				TagNode container = this.containerStack.peek();
				DataNode data = new DataNode(literal);
				
				container.addChild(data);
			}
			
			this.literalBuffer.setLength(0);
			
		}
		
		/**
		 * Start processing the next symbol, by updating the state.
		 **/
		public boolean nextSymbol(int in)
		{
			if(in == '\n')
			{
				this.nextLine();
				return false;
			}
			else
			{
				this.column++;
			}
			
			this.nowC = (char) in;
			
			// This is useful for 'pre'-block parsing. (NYI!)
			if(!this.pre)
			{
				if(this.nowC == '\t')
				{
					this.nowC = ' ';
				}
				
				if((this.nowC == '\r') || (this.nowC == '\n'))
				{
					return false;
				}
				
				if(((this.lastC == ' ') && (this.nowC == ' ')))
				{
					return false;
				}
			}
			
			return true;
		}
		
		/**
		 * Updates the state to the next line.
		 **/
		private void nextLine()
		{
			this.row++;
			this.column = 0;
		}
		
		/**
		 * This method is called after a symbol has been consumed.
		 **/
		public void endNextSymbol(int in)
		{
			this.lastC = this.nowC;
		}
		
		/**
		 * Creates an exception with a additional 'we are here in the file'-note.
		 **/
		public TagParserException makeException(String message)
		{
			return new TagParserException(this.row, this.column-1, message);
		}
		
		/**
		 * Determine's the type of the given Tag, using the Tag itself and its containing Tag.
		 **/
		private int determineTagType(TagNode tag, TagNode container) throws TagParserException
		{
			if(TagParser.this.tagTypeDeterminer != null)
			{
				int result = TagParser.this.tagTypeDeterminer.determineTagType(tag, container);
				
				if(result != TagNode.TAGTYPE_UNDEFINED)
				{
					return result;
				}
			}
			
			if(tag.typeName.startsWith("/"))
			{
				// END TAG
				tag.typeName = tag.typeName.substring(1);
				
				if(tag.typeName.equals(container.typeName))
				{
					return TagNode.TAGTYPE_ENDTAG;
				}
				else
				{
					throw this.makeException("Misplaced End-Tag '" + tag.typeName + "' for Start-Tag '"+container.typeName+"'.");
				}
			}
			
			// If the Tag's typeName ends with '/', it has to be a single-tag.
			// (This is not conform to the HTML/XML standard...)
			if(tag.typeName.endsWith("/"))
			{
				tag.typeName = tag.typeName.substring(0,tag.typeName.length()-1);
				
				// SINGLE TAG
				return TagNode.TAGTYPE_SINGLETAG;
			}
			
			// START TAG
			return TagNode.TAGTYPE_STARTTAG;
		}
		
	}
	
	public interface ITagTypeDeterminer
	{
		/**
		 * Determine's the Tag-Type using the tag itself, and its container.
		 * If it returns -1, the TagParser will guess what the Tag-Type is like it always does.
		 **/
		public int determineTagType(TagNode tag, TagNode container);
		
	}
	
	public static interface IEncoding
	{
		/**
		 * Encode the next character into a Java-Character and return it as an Integer,
		 * or return -1 if we reached the end of the stream.
		 * @throws IOException If a problem occurred while reading from the stream.
		 **/
		public int next(BufferedInputStream input) throws IOException;
		
	}
	
	/**
	 * Plain and simple ASCII/UTF-8 encoding. (?)
	 **/
	public static class PlainEncoding implements IEncoding
	{
		public static IEncoding instance = new PlainEncoding();
		
		@Override
		public int next(BufferedInputStream input) throws IOException
		{
			return input.read();
		}
	}
	
	public static class TagParserException extends IOException
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 6892274044051619690L;
		
		/**
		 * Constructs a new TagParserException with the given position and message.
		 **/
		public TagParserException(int row, int column, String message)
		{
			super("Exception @[row:"+row+", column:"+column+"]: " + message);
		}
		
	}
	
	/**
	 * This method performs a 'trim'-method on the given StringBuffer.
	 * The code for this method was copied directly without remorse from a Stack-Overflow answer.
	 * 
	 * Source: Stack-Overflow
	 **/
	public static String trimSubstring(StringBuffer sb)
	{
	    int first, last;
	    
	    for(first = 0; first < sb.length(); first++)
	    {
			if(!Character.isWhitespace(sb.charAt(first)))
			{
				break;
			}
		}
	    
	    for(last = sb.length(); last > first; last--)
	    {
			if(!Character.isWhitespace(sb.charAt(last - 1)))
			{
				break;
			}
		}
	    
	    return sb.substring(first, last);
	}
	
}
