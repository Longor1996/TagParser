package de.longor1996.tagparser;

import java.util.HashMap;
import java.util.Map;

public class TagNode extends AbstractNode
{
	// The Tag types.
	public static final int TAGTYPE_UNDEFINED = -1;
	public static final int TAGTYPE_STARTTAG = 0;
	public static final int TAGTYPE_ENDTAG = 1;
	public static final int TAGTYPE_SINGLETAG = 2;
	public static final int TAGTYPE_GHOST_STARTTAG = 3;
	public static final int TAGTYPE_GHOST_ENDTAG = 4;
	
	/**
	 * Integer that determine's what kind of tag this is.<br><br>
	 * 
	 * Possible types:
	 * <ul>
	 *  <li>TAGTYPE_STARTTAG - {@literal <}div{@literal >}
	 *  <li>TAGTYPE_ENDTAG - {@literal <}/div{@literal >}
	 *  <li>TAGTYPE_SINGLETAG - {@literal <}div/{@literal >}
	 * </ul>
	 **/
	int tagType = TAGTYPE_UNDEFINED;
	
	/**
	 * The tag type-name is the first word in a tag,
	 * so if you where to write "<i>{@literal <}people count=100{@literal >}</i>" then the typeName will be "<i>people</i>".
	 **/
	String typeName;
	
	/**
	 * The NodeContainer that holds the child-nodes. Is null if there are no childs.
	 **/
	NodeContainer container;
	
	/**
	 * The attributes of this Tag. May be null if there are none.
	 **/
	Map<String, String> attributes;
	
	/**
	 * Constructor for a new TagNode.
	 **/
	public TagNode()
	{
		
	}
	
	/**
	 * Constructor for a new TagNode.
	 **/
	public TagNode(String typeName)
	{
		this.typeName = typeName;
	}
	
	public TagNode setTypeName(String newTypeName)
	{
		this.typeName = newTypeName;
		return this;
	}
	
	/**
	 * Adds a new child-node to this tag.
	 **/
	public TagNode addChild(AbstractNode node)
	{
		if(this.container == null)
		{
			this.container = new NodeContainer();
		}
		
		this.container.addChild(node);
		return this;
	}
	
	public int getChildCount()
	{
		return this.container == null ? 0 : this.container.getChildCount();
	}
	
	public AbstractNode getChild(int index)
	{
		return this.container == null ? null : this.container.getChild(index);
	}
	
	@Override
	public String toString()
	{
		boolean flag = ((this.attributes != null) && (this.attributes.size() > 0));
		
		if(flag)
		{
			return "node:Tag:{typeName:"+this.typeName+", attributes:"+this.attributes+"}";
		}
		
		return "node:Tag:{typeName:"+this.typeName+"}";
	}
	
	/**
	 * Adds the given Map of Attributes of key/value-pairs to this tags attribute-map.
	 **/
	public void addAttributes(Map<String, String> inHashMap)
	{
		if(inHashMap == null)
		{
			throw new IllegalArgumentException("Input HashMap cannot be null!");
		}
		
		if(inHashMap.size() <= 0)
		{
			return;
		}
		
		if(this.attributes == null)
		{
			this.attributes = new HashMap<String, String>(inHashMap);
		}
		else
		{
			this.attributes.putAll(inHashMap);
		}
	}
	
	public String getAttribute(String key)
	{
		if(this.attributes == null)
		{
			return null;
		}
		
		return this.attributes.get(key);
	}
	
	public String getAttribute(String key, String defaultTo)
	{
		if(this.attributes == null)
		{
			return defaultTo;
		}
		
		String out = this.attributes.get(key);
		return out == null ? defaultTo : out;
	}
	
	public TagNode setAttribute(String key, String value)
	{
		if(this.attributes == null)
		{
			this.attributes = new HashMap<String, String>();
		}
		
		this.attributes.put(key, value);
		
		return this;
	}
	
}
