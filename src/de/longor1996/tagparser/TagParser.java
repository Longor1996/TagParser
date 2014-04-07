package de.longor1996.tagparser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

public class TagParser
{
	IEncoding encoding;
	ITagTypeDeterminer tagTypeDeterminer;
	
	public long result_time = 0;
	public TagNode result_document = null;
	
	public TagParser()
	{
		this.encoding = PlainEncoding.instance;
	}
	
	public TagNode parse(InputStream __source) throws TagParserException, IOException
	{
		long time = System.nanoTime();
		BufferedInputStream source = new BufferedInputStream(__source);
		ParseState state = new ParseState(source);
		
		while(true)
		{
			int in = this.readNext(source);
			
			if(in == -1)
			{
				break;
			}
			
			boolean consume = state.nextSymbol(in);
			
			if(consume)
			{
				this.consumeSymbol(state);
			}
			
			state.endNextSymbol(in);
			
		}
		
		// System.out.println();
		
		// Sanity check!
		if(state.containerStack.size() != 1)
		{
			throw new TagParserException(Integer.MAX_VALUE, Integer.MAX_VALUE, "A tag is not correctly escaped.");
		}
		
		this.result_time = (System.nanoTime() - time);
		this.result_document = state.containerStack.pop();
		
		// System.out.println(">> Time: " + this.result_time + "ns");
		
		return this.result_document;
	}
	
	private int readNext(BufferedInputStream source) throws IOException
	{
		return this.encoding.next(source);
	}
	
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
		
		/*
		if(state.nowC == '\n')
		{
			System.out.print("\\n");
		}
		else if(state.nowC == '\r')
		{
			System.out.print("\\r");
		}
		else
		{
			System.out.print(state.nowC);
		}
		//*/
		
	}
	
	private void consumeSymbolOutOfTag(ParseState state)
	{
		
		if(state.nowC == '<')
		{
			state.finishLiteral();
			state.isReadingTag = true;
			state.inTagPointer = 0;
			state.consumedTagTypeName = false;
			state.bufferLiteral.setLength(0);
			state.bufferTag = new TagNode();
			return;
		}
		
		state.bufferLiteral.append(state.nowC);
		
	}
	
	private void consumeSymbolInTag(ParseState state) throws TagParserException
	{
		
		if(state.nowC == '>')
		{
			if(!state.consumedTagTypeName)
			{
				state.bufferTag.typeName = state.bufferLiteral.toString();
				state.bufferLiteral.setLength(0);
			}
			
			if(state.bufferTag.typeName == null)
			{
				throw state.makeException("Type-Name is null referenced! >> " + state.bufferLiteral);
			}
			
			state.finishTag();
			state.isReadingTag = false;
			return;
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
			state.bufferTag.typeName = trimSubstring(state.bufferLiteral);
			state.bufferLiteral.setLength(0);
			state.consumedTagTypeName = true;
			return;
		}
		
		state.bufferLiteral.append(state.nowC);
		
	}

	class ParseState
	{
		int row;
		int column;
		
		int lastI;
		int nowI;
		
		char lastC;
		char nowC;
		
		int inTagPointer;
		
		Stack<TagNode> containerStack;
		StringBuffer bufferLiteral;
		TagNode bufferTag;
		
		BufferedInputStream source;
		
		boolean pre;
		
		// Same as 'isInsideTag'!
		boolean isReadingTag;
		boolean isReadingStringInTag;
		boolean consumedTagTypeName;
		
		public ParseState(BufferedInputStream source)
		{
			this.source = source;
			this.bufferLiteral = new StringBuffer(4096);
			
			// Create NodeContainer-Stack and push the document-root on it.
			this.containerStack = new Stack<TagNode>();
			this.containerStack.push(new TagNode().setTypeName("document-root"));
			
			this.pre = false;
			
			this.isReadingTag = false;
			this.isReadingStringInTag = false;
			this.consumedTagTypeName = false;
			
			this.row = this.column = 0;
			this.lastI = this.nowI = ' ';
			this.lastC = this.nowC = ' ';
			
		}
		
		public void finishTag() throws TagParserException
		{
			// System.out.print("NT"+this.mLD());
			
			TagNode container = this.containerStack.peek();
			TagNode tag = this.bufferTag;
			
			tag.tagType = this.determineTagType(tag, container);
			
			// Depending on the Type of the Tag, we will either...
			//    POP the current container from the containerStack, ignoring this Tag as 'end'-tag. But only if the current container has the same name!
			//    PUSH this tag as an 'start'-tag onto the containerStack.
			//    ADD this tag to the container as a 'single'-tag.
			switch(tag.tagType)
			{
			case 0: // START-TAG
				container.add(tag);
				this.containerStack.push(tag);
				this.bufferTag = null;
				break;
				
			case 1: // END-TAG
				this.containerStack.pop();
				this.bufferTag = null;
				break;
				
			case 2: // SINGLE-TAG
				container.add(tag);
				this.bufferTag = null;
				break;
				
			case -1: default:
				throw this.makeException("TagType unresolved or unknown: " + tag.tagType);
			}
			
			/*
			{
				String te = trimSubstring(this.bufferLiteral);
				this.bufferLiteral.setLength(0);
				this.bufferLiteral.append(te);
			}
			//*/
			
			if(this.bufferLiteral.length() > 0)
			{
				tag.addProperties(this.transformProperties(this.bufferLiteral.toString()));
			}
			
			this.bufferLiteral.setLength(0);
		}

		private HashMap<String, String> transformProperties(String string)
		{
			HashMap<String, String> map = new HashMap<String, String>(2);
			// String initialString = string;
			
			// Consume the String...
			
			while(string.length() > 0)
			{
				string = string.trim();
				
				// Take out a Word?
				// property_key
				// =
				// "a literal value !"
				// property_key> (Mind the '>'!)
				
				int endOfTheWord = this.findEndOfWord(string);
				
				String key = null;
				
				// Cut out the next word! (which is a 'key')
				{
					String word = string.substring(0, endOfTheWord);
					// System.out.println(">  "+endOfTheWord+"  [["+initialString+"]]   [[" + string + "]]       [[" + word + "]]");
					string = string.substring(endOfTheWord).trim();
					key = word;
				}
				
				if(string.length() == 0)
				{
					// key only
					// System.out.println("KEY: " + key);
					map.put(key, "");
					break;
				}
				
				if(string.charAt(0) == '=')
				{
					// key/value
					string = string.substring(1).trim();
					String value = null;
					
					int endOfTheWord2 = this.findEndOfWord(string);
					{
						String word = string.substring(0, endOfTheWord2);
						// System.out.println(">  "+endOfTheWord2+"  [["+initialString+"]]   [[" + string + "]]       [[" + word + "]]");
						string = string.substring(endOfTheWord2).trim();
						value = word;
					}
					
					if((value.length() > 2) && (value.charAt(0) == '"') && (value.charAt(value.length()-1) == '"'))
					{
						value = value.substring(1, value.length()-1);
					}
					
					// System.out.println("KEY/VALUE: " + key + " = " + value);
					map.put(key, value);
				}
				else
				{
					// key only
					// System.out.println("KEY: " + key);
					map.put(key, "");
				}
			}
			
			return map;
		}

		private int findEndOfWord(String string)
		{
			
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

		public void finishLiteral()
		{
			// Finish the last Literal if there is any!
			String literal = trimSubstring(this.bufferLiteral);
			
			if(literal.length() > 0)
			{
				// System.out.print("ND"+this.mLD());
				
				TagNode container = this.containerStack.peek();
				DataNode data = new DataNode(literal);
				
				container.add(data);
			}
			
			this.bufferLiteral.setLength(0);
			
		}

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
			
			this.nowI = in;
			this.nowC = (char) in;
			
			if(!this.pre)
			{
				if(this.nowC == '\t')
				{
					this.nowI = this.nowC = ' ';
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
		
		private void nextLine()
		{
			this.row++;
			this.column = 0;
		}
		
		public void endNextSymbol(int in)
		{
			this.lastI = this.nowI;
			this.lastC = this.nowC;
		}
		
		public TagParserException makeException(String message)
		{
			return new TagParserException(this.row, this.column-1, message);
		}
		
		public String mLD()
		{
			return "@[" + this.row + "|" + (this.column-1) + "]";
		}
		
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
		 * or return -1 if we reached the EOF.
		 * @throws IOException If a IO problem occurred.
		 **/
		public int next(BufferedInputStream input) throws IOException;
		
	}
	
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

		public TagParserException(int row, int column, String message)
		{
			super("Exception @[row:"+row+", column:"+column+"]: " + message);
		}
		
	}
	
	public static String trimSubstring(StringBuffer sb)
	{
	    int first, last;
	    
	    for (first=0; first<sb.length(); first++)
	    {
			if (!Character.isWhitespace(sb.charAt(first)))
			{
				break;
			}
		}
	    
	    for (last=sb.length(); last>first; last--)
	    {
			if (!Character.isWhitespace(sb.charAt(last-1)))
			{
				break;
			}
		}
	    
	    return sb.substring(first, last);
	}
	
}
