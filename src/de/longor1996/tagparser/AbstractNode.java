package de.longor1996.tagparser;

/**
 * Abstract class to describe a 'Node' in a HTML-style document.
 * There are only two types of nodes, namely 'Tag' and 'Data'.
 * 
 * <hr>
 * <ul>
 * <li>A {@code AbstractNode} can only be of the types {@code Tag} <i>or</i>
 * {@code Data}. <br/>
 * 
 * <li>A {@code AbstractNode} is <i>always</i> contained within a
 * {@code NodeContainer}.
 * 
 * <li>{@code AbstractNode}'s of either {@code Tag} and {@code Data} can be
 * stored in a {@code NodeContainer} in <i>any</i> order, layout and amount.
 * 
 * <li>A {@code Data}-{@code AbstractNode} contains only textual data.
 * 
 * <li>A {@code Tag}-{@code AbstractNode} contains 'properties', which is simply
 * a key/value-map made of strings.
 * 
 * <li>A {@code Tag}-{@code AbstractNode} <i>can</i> contain a
 * {@code NodeContainer}, thus containing even more {@code Tag} and {@code Data}
 * {@code AbstractNode}'s.
 * 
 * </ul>
 **/
public abstract class AbstractNode
{
	// empty
}
