package com.botdetector.model;

import lombok.Value;

/**
 * A string wrapper that makes .equals a caseInsensitive match
 * <p>
 *     a collection that wraps a String mapping in CaseInsensitiveStrings will still accept a String but will now
 *     return a caseInsensitive match rather than a caseSensitive one
 * </p>
 * <p>
 *     Yoinked from https://stackoverflow.com/questions/39026195/caseinsensitive-map-key-java
 * </p>
 */
@Value
public class CaseInsensitiveString
{
	String str;

	public static CaseInsensitiveString wrap(String str)
	{
		return new CaseInsensitiveString(str);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}

		if (o == null)
		{
			return false;
		}

		if (o.getClass() == getClass())
		{
			// Is another CaseInsensitiveString
			CaseInsensitiveString that = (CaseInsensitiveString) o;
			return (str != null) ? str.equalsIgnoreCase(that.str) : that.str == null;
		}

		if (o.getClass() == String.class)
		{
			// Is just a regular String
			String that = (String) o;
			return str.equalsIgnoreCase(that);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return (str != null) ? str.toUpperCase().hashCode() : 0;
	}

	@Override
	public String toString()
	{
		return str;
	}
}