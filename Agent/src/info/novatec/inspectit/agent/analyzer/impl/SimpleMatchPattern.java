package info.novatec.inspectit.agent.analyzer.impl;

import info.novatec.inspectit.agent.analyzer.IMatchPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern matcher for simple strings with '*' wildcard characters. The supplied template may take
 * the form "xxx" for an exact match, "xxx*" for a match on leading characters only, "*xxx" to match
 * on trailing characters only, or "xxx*yyy" to match on both leading and trailing. It can also
 * include multiple '*' characters when more than one part of the match is wildcarded.
 * <p>
 * <b>IMPORTANT:</b> The class code is copied/taken from <a
 * href="http://www.ibm.com/developerworks/java/library/j-dyn0203.html">IBM developers Works</a>.
 * Original author is Dennis Sosnoski. License info can be found <a
 * href="http://www.ibm.com/developerworks/apps/download/index.jsp
 * ?contentid=10908&filename=j-dyn0203-source.zip&method=http&locale=">here</a>.
 * 
 * @author Dennis Sosnoski
 */
public class SimpleMatchPattern implements IMatchPattern {

	/**
	 * Text components to be matched.
	 */
	private String[] components;

	/**
	 * Flag for leading text to be matched.
	 */
	private boolean isLeadText;

	/**
	 * Flag for trailing text to be matched.
	 */
	private boolean isTrailText;

	/**
	 * The template string used for matching.
	 */
	private String template;

	/**
	 * Is set to true if the template equals '*'. As this matches everything, we skip the whole
	 * compare process.
	 */
	private boolean everything = false;

	/**
	 * Constructor.
	 * 
	 * @param template
	 *            match text template
	 */
	public SimpleMatchPattern(final String template) {
		if ("*".equals(template)) {
			everything = true;
			isLeadText = false;
			isTrailText = false;
			return;
		}

		int mark = template.indexOf('*');
		List<String> comps = new ArrayList<String>();

		if (mark < 0) {
			// set up for exact match
			comps.add(template);
			isLeadText = true;
			isTrailText = true;
		} else {
			// handle leading wildcard
			int base = 0;
			if (mark == 0) {
				isLeadText = false;
				base = 1;
				mark = template.indexOf('*', 1);
			} else {
				isLeadText = true;
			}
			// loop for all text components to be matched
			int limit = template.length() - 1;
			while (mark > 0) {
				comps.add(template.substring(base, mark));
				base = mark + 1;
				if (mark == limit) {
					break;
				}
				mark = template.indexOf('*', base);
			}
			comps.add(template.substring(base));
			isTrailText = mark != limit;
		}

		// save array of text components to be matched
		components = (String[]) comps.toArray(new String[comps.size()]);

		this.template = template;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean match(String match) {
		if (everything) {
			return true;
		}

		// first check for required leading text
		int start = 0;
		int end = match.length();
		int index = 0;
		if (isLeadText) {
			if (match.startsWith(components[0])) {
				start = components[0].length();
				index = 1;
			} else {
				return false;
			}
		}

		// next check for required trailing text
		int limit = components.length;
		if (isTrailText) {
			limit--;
			if (match.endsWith(components[limit])) {
				end -= components[limit].length();
			} else {
				return false;
			}
		}

		// finally match all floating comparison components
		while (index < limit) {
			String comp = components[index++];
			start = match.indexOf(comp, start);
			if (start >= 0) {
				start += comp.length();
				if (start > end) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPattern() {
		return template;
	}

	/**
	 * Checks if the supplied {@link String} is a pattern.
	 * 
	 * @param txt
	 *            The text to check for.
	 * @return Returns if the supplied String is a pattern.
	 */
	public static boolean isPattern(String txt) {
		return txt.indexOf('*') > -1;
	}
}
