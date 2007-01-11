/* 
 * Copyright  (c) 2006-2007 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

package ejip2.jtcpip;

import java.util.Random;

import ejip2.jtcpip.util.Debug;

/**
 * This class contains some util functions
 * 
 * @author Ulrich Feichter
 * @author Tobias Kellner
 * @author Christof Rath
 * @version $Rev: 849 $ $Date: 2007/01/11 19:00:30 $
 */
public class Util
{
	/** random number generator */
	public static Random rand ;

	/** Invalid MAC address format */
	private static JtcpipException macException ;
	
	public static void init(){
		
		rand = new Random();
		macException = new JtcpipException("Invalid MAC Address String");

	}
	
	/**
	 * A unsigned test if a greater b.
	 * 
	 * @param a
	 * @param b
	 * @return 1 if a > b, 0 if a = b and -1 if a < b
	 */
	public static byte unsignedGt(byte a, byte b)
	{
		return relGt(a, b, (byte) 0);
	}

	/**
	 * A unsigned test if a greater b relative to a third number. Example: If
	 * zeroPt = -10 then -10 is the smallest number and -11 the greatest or if
	 * zeroPt = 0 then 0 is the smallest number and -128 is the greatest
	 * Returns:
	 * <ul>
	 * <li>1 if a > b
	 * <li>0 if a = b
	 * <li>-1 if a < b
	 * </ul>
	 * 
	 * @param a
	 * @param b
	 * @param zeroPt
	 * @return 1 if a > b, 0 if a = b and -1 if a < b
	 */
	public static byte relGt(byte a, byte b, byte zeroPt)
	{
		if (a >= zeroPt)
			if (b >= zeroPt)
				return (byte) (a > b ? 1 : (a < b ? -1 : 0));
			else
				return -1;
		else if (b < zeroPt)
			return (byte) (a > b ? 1 : (a < b ? -1 : 0));
		else
			return 1;
	}

	/**
	 * A unsigned test if a greater b.
	 * 
	 * @param a
	 * @param b
	 * @return 1 if a > b, 0 if a = b and -1 if a < b
	 */
	public static byte unsignedGt(short a, short b)
	{
		return relGt(a, b, (short) 0);
	}

	/**
	 * A unsigned test if a greater b relative to a third number. Example: If
	 * zeroPt = -10 then -10 is the smallest number and -11 the greatest or if
	 * zeroPt = 0 then 0 is the smallest number and -128 is the greatest
	 * 
	 * @param a
	 * @param b
	 * @param zeroPt
	 * @return 1 if a > b, 0 if a = b and -1 if a < b
	 */
	public static byte relGt(short a, short b, short zeroPt)
	{
		if (a >= zeroPt)
			if (b >= zeroPt)
				return (byte) (a > b ? 1 : (a < b ? -1 : 0));
			else
				return -1;
		else if (b < zeroPt)
			return (byte) (a > b ? 1 : (a < b ? -1 : 0));
		else
			return 1;
	}

	/**
	 * A unsigned test if a greater b.
	 * 
	 * @param a
	 * @param b
	 * @return 1 if a > b, 0 if a = b and -1 if a < b
	 */
	public static byte unsignedGt(int a, int b)
	{
		return relGt(a, b, 0);
	}

	/**
	 * A unsigned test if a greater b relative to a third number. Example: If
	 * zeroPt = -10 then -10 is the smallest number and -11 the greatest or if
	 * zeroPt = 0 then 0 is the smallest number and -128 is the greatest
	 * 
	 * @param a
	 * @param b
	 * @param zeroPt
	 * @return 1 if a > b, 0 if a = b and -1 if a < b
	 */
	public static byte relGt(int a, int b, int zeroPt)
	{
		if (a >= zeroPt)
			if (b >= zeroPt)
				return (byte) (a > b ? 1 : (a < b ? -1 : 0));
			else
				return -1;
		else if (b < zeroPt)
			return (byte) (a > b ? 1 : (a < b ? -1 : 0));
		else
			return 1;
	}

	/**
	 * Return the rounded up integer division result of a / b.
	 * 
	 * @param a
	 *            dividend
	 * @param b
	 *            divisor
	 * @return rounded up divison result
	 */
	public static int divRoundUp(int a, int b)
	{
		return a / b + (a % b != 0 ? 1 : 0);
	}

	/**
	 * Return the rounded up integer division result of a / b.
	 * 
	 * @param a
	 *            dividend
	 * @param b
	 *            divisor
	 * @return rounded up divison result
	 */
	public static short divRoundUp(short a, short b)
	{
		return (short) (a / b + (a % b != 0 ? 1 : 0));
	}

	/**
	 * Return the rounded up integer division result of a / b.
	 * 
	 * @param a
	 *            dividend
	 * @param b
	 *            divisor
	 * @return rounded up divison result
	 */
	public static byte divRoundUp(byte a, byte b)
	{
		return (byte) (a / b + (a % b != 0 ? 1 : 0));
	}

	/**
	 * Calculate the difference between two values. Considers overflow cases.
	 * 
	 * @param biggerVal
	 *            The bigger Value
	 * @param smallerVal
	 *            The smaller Value
	 * @return the difference
	 */
	public static int calcDiffWithOverflow(int biggerVal, int smallerVal)
	{
		if (biggerVal < smallerVal)
			return (Integer.MAX_VALUE - smallerVal) + (biggerVal - Integer.MIN_VALUE) + 1;
		else
			return biggerVal - smallerVal;
	}

	/**
	 * Converts a string "00:00:00:00:00:00" into the corresponding int[]
	 * 
	 * @param macAddr
	 * @param buffer
	 * @throws JtcpipException
	 */
	public static void macStrToByteArr(String macAddr, int[] buffer) throws JtcpipException
	{
		char[] c = macAddr.toCharArray();
		int j = 0;
		String s = "";

		if (buffer == null)
			buffer = new int[6];

		for (int i = 0; i < c.length; i++)
		{
			if (c[i] != ':')
				s += c[i];

			if (c[i] == ':' || i == (c.length - 1))
			{
				buffer[j] = Integer.parseInt(s, 16);
				if (buffer[j] < 0 || buffer[j] > 255)
					throw macException;

				s = "";
				j++;

				if (j == buffer.length)
				{
					if (i < (c.length - 1))
						throw macException;
					return;
				}
			}
		}

		if (j != 6)
			throw macException;

	}

	/**
	 * Tests if a value is between two other values. Considers overflow cases.
	 * 
	 * @param smallerVal
	 *            The smaller boundary value
	 * @param biggerVal
	 *            The bigger boundare value
	 * @param testVal
	 *            The value to test
	 * @return true if testVal is between smallerVal and biggerVal
	 */
	public static boolean isBetween(int smallerVal, int biggerVal, int testVal)
	{
		if (biggerVal < smallerVal)
			return ((testVal > smallerVal) && (testVal <= Integer.MAX_VALUE))
					|| ((testVal >= Integer.MIN_VALUE) && (testVal < biggerVal));
		else
			return (testVal > smallerVal) && (testVal < biggerVal);
	}

	/**
	 * Tests if a value is between two other values (or equal to the higher
	 * value). Considers overflow cases.
	 * 
	 * @param smallerVal
	 *            The smaller boundary value
	 * @param biggerVal
	 *            The bigger boundare value
	 * @param testVal
	 *            The value to test
	 * @return true if testVal is between smallerVal and biggerVal (or equal to
	 *         biggerVal)
	 */
	public static boolean isBetweenOrEqualBigger(int smallerVal, int biggerVal, int testVal)
	{
		if (biggerVal < smallerVal)
			return ((testVal > smallerVal) && (testVal <= Integer.MAX_VALUE))
					|| ((testVal >= Integer.MIN_VALUE) && (testVal <= biggerVal));
		else
			return (testVal > smallerVal) && (testVal <= biggerVal);
	}

	/**
	 * Tests if a value is between two other values (or equal to the lower
	 * value). Considers overflow cases.
	 * 
	 * @param smallerVal
	 *            The smaller boundary value
	 * @param biggerVal
	 *            The bigger boundare value
	 * @param testVal
	 *            The value to test
	 * @return true if testVal is between smallerVal and biggerVal (or equal to
	 *         smallerVal)
	 */
	public static boolean isBetweenOrEqualSmaller(int smallerVal, int biggerVal, int testVal)
	{
		if (biggerVal < smallerVal)
			return ((testVal >= smallerVal) && (testVal <= Integer.MAX_VALUE))
					|| ((testVal >= Integer.MIN_VALUE) && (testVal < biggerVal));
		else
			return (testVal >= smallerVal) && (testVal < biggerVal);
	}

	/**
	 * Retrieves the ip address value from a string.
	 * 
	 * @param connectorString
	 *            of the form //ip_addr:port[;option=value...]
	 * @return the ip address as integer
	 */
	public static int getAddrFromConnectorStr(String connectorString)
	{
		int offset = 0;
		if (connectorString.charAt(0) == '/')
			offset = 2;

		int colon = connectorString.indexOf(':');

		if (colon < 1)
			throw new IllegalArgumentException("no ':' in connector string");

		if (colon == 1)
			return 0xFFFFFFFF; // 255.255.255.255

		try
		{
			return IP.ipStringToInt(connectorString.substring(offset, colon));
		} catch (JtcpipException e)
		{
			Debug.println("Wrong IP address in connector string  connectorString + ",
					Debug.DBG_OTHER);
			return 0;
		}
	}

	/**
	 * Retrieves the port value from a string.
	 * 
	 * @param connectorString
	 *            of the form //ip_addr:port[;option=value...]
	 * @return the port as int or -1 in case of a failure
	 */
	public static int getPortFromConnectorStr(String connectorString)
	{
		int colon = connectorString.indexOf(':');
		int semic = connectorString.indexOf(';'); // in case there are
													// options...

		if (colon < 1)
			return -1;

		try
		{
			if (semic < 1)
				semic = Integer.parseInt(connectorString.substring(colon + 1));
			else
				semic = Integer.parseInt(connectorString.substring(colon + 1, semic));
		} catch (Exception e)
		{
			semic = -1;
		}

		if (semic < 0 || semic > 0xFFFF)
		{
			Debug.println(
					"Port number out of bounds in connector string  ",
					Debug.DBG_OTHER);
			return -1;
		}
		else
			return semic;
	}
}
